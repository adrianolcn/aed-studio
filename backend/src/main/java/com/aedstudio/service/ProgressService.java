package com.aedstudio.service;

import com.aedstudio.dto.*;
import com.aedstudio.exception.InvalidTopicException;
import com.aedstudio.exception.LockedTopicException;
import com.aedstudio.model.*;
import com.aedstudio.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Serviço de progresso do estudante.
 *
 * Responsabilidades:
 *  - Registrar visitas a tópicos (cria ou encontra TopicProgress)
 *  - Conceder XP de forma idempotente via XpEvent
 *  - Calcular e retornar o estado completo de progresso
 *  - Sincronizar streak diária
 *
 * A idempotência de XP é crítica: o front-end pode reenviar
 * eventos ao recarregar a página, e o banco garante unicidade
 * de (user_id, event_key) na tabela xp_events.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProgressService {

    private final UserRepository          userRepository;
    private final TopicProgressRepository topicProgressRepository;
    private final XpEventRepository       xpEventRepository;
    private final ExerciseAttemptRepository exerciseAttemptRepository;
    private final UserBadgeRepository     userBadgeRepository;
    private final GeneratedExerciseRepository generatedExerciseRepository;
    private final SimulationEventRepository simulationEventRepository;
    private final TopicCatalog            topicCatalog;
    private final ObjectMapper            objectMapper;

    // ── Registrar visita ────────────────────────────────────────────

    @Transactional
    public ProgressResponse recordVisit(User user, String topicId) {
        validateTopic(topicId);
        ensureTopicAvailable(user, topicId);

        boolean alreadyVisited = topicProgressRepository.existsByUserAndTopicId(user, topicId);
        if (!alreadyVisited) {
            TopicProgress tp = TopicProgress.builder()
                    .user(user)
                    .topicId(topicId)
                    .state(TopicState.VISITED)
                    .build();
            topicProgressRepository.save(tp);
            awardEventIfNew(user, "visit_" + topicId, 5);
            log.debug("Nova visita registrada: user={} topic={}", user.getId(), topicId);
        }
        // Atualiza streak independentemente
        user.updateStreak();
        userRepository.save(user);
        return getProgress(user);
    }

    // ── Conceder XP (idempotente) ───────────────────────────────────

    @Transactional
    public int awardXp(User user, XpAwardRequest req) {
        String eventKey = req.getReason();
        validateTopic(req.getTopicId());
        ensureTopicAvailable(user, req.getTopicId());

        // Idempotência: se já ganhou este evento, retorna 0
        if (xpEventRepository.existsByUserAndEventKey(user, eventKey)) {
            log.debug("XP já concedido para user={} event={}", user.getId(), eventKey);
            return 0;
        }

        if (!awardEventIfNew(user, eventKey, req.getAmount())) {
            return 0;
        }

        log.info("XP concedido: user={} event={} amount={}", user.getId(), eventKey, req.getAmount());
        awardBadges(userRepository.findById(user.getId()).orElse(user));
        return req.getAmount();
    }

    @Transactional(readOnly = true)
    public List<ExerciseDto> getExercises(User user, String topicId) {
        validateTopic(topicId);
        ensureTopicAvailable(user, topicId);
        return topicCatalog.exercisesForTopic(topicId).stream()
                .map(this::toExerciseDto)
                .toList();
    }

    @Transactional
    public ExerciseSubmitResponse submitExercise(User user, ExerciseSubmitRequest req) {
        TopicCatalog.ExerciseDefinition exercise = topicCatalog.exercise(req.getExerciseId())
                .orElseThrow(() -> new InvalidTopicException("exerciseId não cadastrado: " + req.getExerciseId()));
        validateTopic(exercise.topicId());
        ensureTopicAvailable(user, exercise.topicId());

        boolean correct = normalizeAnswer(req.getAnswer()).equals(normalizeAnswer(exercise.correctAnswer()));
        ExerciseAttempt attempt = ExerciseAttempt.builder()
                .user(user)
                .topicId(exercise.topicId())
                .exerciseId(exercise.id())
                .type(exercise.type())
                .answer(req.getAnswer())
                .correct(correct)
                .timeSpentSeconds(req.getTimeSpentSeconds())
                .build();
        exerciseAttemptRepository.save(attempt);

        int awarded = 0;
        if (correct) {
            awarded += awardEventIfNew(user, "exercise_" + exercise.id(), exercise.xp()) ? exercise.xp() : 0;
        }

        boolean completed = maybeCompleteTopic(user, exercise.topicId());
        if (completed) {
            awarded += awardEventIfNew(user, "complete_" + exercise.topicId(), 25) ? 25 : 0;
        }

        user.updateStreak();
        userRepository.save(user);
        User fresh = userRepository.findById(user.getId()).orElse(user);
        awardBadges(fresh);

        return ExerciseSubmitResponse.builder()
                .exerciseId(exercise.id())
                .topicId(exercise.topicId())
                .correct(correct)
                .feedback(correct ? exercise.correctFeedback() : exercise.wrongFeedback())
                .awarded(awarded)
                .topicCompleted(completed)
                .progress(getProgress(fresh))
                .build();
    }

    @Transactional
    public GeneratedExerciseDto generateExercise(User user, GeneratedExerciseRequest req) {
        validateTopic(req.getTopicId());
        ensureTopicAvailable(user, req.getTopicId());

        TopicCatalog.TopicDefinition topic = topicCatalog.topic(req.getTopicId()).orElseThrow();
        long sequence = generatedExerciseRepository.countByUserAndTopicId(user, req.getTopicId()) + 1;
        int difficulty = req.getDifficulty() == null ? 1 : Math.max(1, Math.min(3, req.getDifficulty()));
        GeneratedTemplate template = buildGeneratedTemplate(topic, sequence, difficulty);
        GeneratedExercise generated = GeneratedExercise.builder()
                .user(user)
                .generatedId("gen-" + user.getId() + "-" + topic.id() + "-" + sequence)
                .topicId(topic.id())
                .type(template.type())
                .difficulty(difficulty)
                .prompt(template.prompt())
                .options(serializeOptions(template.options()))
                .correctAnswer(template.correctAnswer())
                .correctFeedback(template.correctFeedback())
                .wrongFeedback(template.wrongFeedback())
                .build();
        return toGeneratedExerciseDto(generatedExerciseRepository.save(generated));
    }

    @Transactional(readOnly = true)
    public List<GeneratedExerciseDto> generatedExerciseHistory(User user) {
        return generatedExerciseRepository.findTop20ByUserOrderByCreatedAtDesc(user).stream()
                .map(this::toGeneratedExerciseDto)
                .toList();
    }

    @Transactional
    public ExerciseSubmitResponse submitGeneratedExercise(
            User user,
            String generatedExerciseId,
            GeneratedExerciseSubmitRequest req) {
        GeneratedExercise exercise = generatedExerciseRepository.findByUserAndGeneratedId(user, generatedExerciseId)
                .orElseThrow(() -> new InvalidTopicException("generatedExerciseId não cadastrado: " + generatedExerciseId));
        validateTopic(exercise.getTopicId());
        ensureTopicAvailable(user, exercise.getTopicId());

        boolean correct = normalizeAnswer(req.getAnswer()).equals(normalizeAnswer(exercise.getCorrectAnswer()));
        ExerciseAttempt attempt = ExerciseAttempt.builder()
                .user(user)
                .topicId(exercise.getTopicId())
                .exerciseId(exercise.getGeneratedId())
                .type(exercise.getType())
                .answer(req.getAnswer())
                .correct(correct)
                .timeSpentSeconds(req.getTimeSpentSeconds())
                .build();
        exerciseAttemptRepository.save(attempt);

        exercise.setAnswered(true);
        generatedExerciseRepository.save(exercise);

        int awarded = 0;
        if (correct) {
            int xp = 8 + (exercise.getDifficulty() * 4);
            awarded += awardEventIfNew(user, "generated_" + exercise.getGeneratedId(), xp) ? xp : 0;
        }

        boolean completed = maybeCompleteTopic(user, exercise.getTopicId());
        if (completed) {
            awarded += awardEventIfNew(user, "complete_" + exercise.getTopicId(), 25) ? 25 : 0;
        }

        user.updateStreak();
        userRepository.save(user);
        User fresh = userRepository.findById(user.getId()).orElse(user);
        awardBadges(fresh);

        return ExerciseSubmitResponse.builder()
                .exerciseId(exercise.getGeneratedId())
                .topicId(exercise.getTopicId())
                .correct(correct)
                .feedback(correct ? exercise.getCorrectFeedback() : exercise.getWrongFeedback())
                .awarded(awarded)
                .topicCompleted(completed)
                .progress(getProgress(fresh))
                .build();
    }

    @Transactional
    public SimulationEventResponse recordSimulationEvent(User user, SimulationEventRequest req) {
        validateTopic(req.getTopicId());
        ensureTopicAvailable(user, req.getTopicId());

        SimulationEvent event = SimulationEvent.builder()
                .user(user)
                .topicId(req.getTopicId())
                .simulatorType(req.getSimulatorType())
                .action(req.getAction())
                .milestone(req.getMilestone())
                .stateSnapshot(req.getStateSnapshot())
                .build();
        simulationEventRepository.save(event);

        String eventKey = simulationXpEventKey(req);
        int xp = simulationXpAmount(req);
        int awarded = 0;
        if (eventKey != null && xp > 0) {
            awarded = awardEventIfNew(user, eventKey, xp) ? xp : 0;
        }

        user.updateStreak();
        userRepository.save(user);
        User fresh = userRepository.findById(user.getId()).orElse(user);
        awardBadges(fresh);

        return SimulationEventResponse.builder()
                .eventId(event.getId())
                .eventKey(eventKey)
                .awarded(awarded)
                .alreadyEarned(eventKey != null && awarded == 0)
                .progress(getProgress(fresh))
                .build();
    }

    @Transactional(readOnly = true)
    public List<SimulationMissionDto> simulationMissions(User user, String topicId) {
        validateTopic(topicId);
        ensureTopicAvailable(user, topicId);
        return missionsForTopic(topicId).stream()
                .map(mission -> mission.toDto(xpEventRepository.existsByUserAndEventKey(user, "mission_" + mission.id())))
                .toList();
    }

    @Transactional
    public SimulationMissionSubmitResponse submitSimulationMission(
            User user,
            String missionId,
            SimulationMissionSubmitRequest req) {
        SimulationMission mission = missionById(missionId)
                .orElseThrow(() -> new InvalidTopicException("missionId não cadastrado: " + missionId));
        validateTopic(mission.topicId());
        ensureTopicAvailable(user, mission.topicId());

        MissionCheck check = checkMission(mission, req.getStateSnapshot());
        int awarded = 0;
        if (check.completed()) {
            awarded = awardEventIfNew(user, "mission_" + mission.id(), mission.xp()) ? mission.xp() : 0;
        }

        SimulationEvent event = SimulationEvent.builder()
                .user(user)
                .topicId(mission.topicId())
                .simulatorType(mission.simulatorType())
                .action("mission")
                .milestone(check.completed() ? "MISSION_COMPLETED" : "MISSION_ATTEMPT")
                .stateSnapshot(req.getStateSnapshot())
                .build();
        simulationEventRepository.save(event);

        user.updateStreak();
        userRepository.save(user);
        User fresh = userRepository.findById(user.getId()).orElse(user);
        awardBadges(fresh);

        return SimulationMissionSubmitResponse.builder()
                .missionId(mission.id())
                .completed(check.completed())
                .feedback(check.feedback())
                .awarded(awarded)
                .progress(getProgress(fresh))
                .build();
    }

    // ── Buscar progresso completo ───────────────────────────────────

    @Transactional(readOnly = true)
    public ProgressResponse getProgress(User user) {
        List<TopicProgress> allProgress = topicProgressRepository.findByUser(user);
        List<String> earnedKeys = xpEventRepository.findEventKeysByUser(user);
        List<ExerciseAttempt> attempts = exerciseAttemptRepository.findByUser(user);

        Map<String, String> topicsMap = allProgress.stream()
                .collect(Collectors.toMap(
                        TopicProgress::getTopicId,
                        tp -> tp.getState().name()
                ));

        long visited   = allProgress.stream()
                .filter(tp -> tp.getState() == TopicState.VISITED || tp.getState() == TopicState.COMPLETED)
                .count();
        long completed = allProgress.stream()
                .filter(TopicProgress::isCompleted)
                .count();

        int pct = (int) Math.round((completed * 100.0) / topicCatalog.totalTopics());

        // Recarrega user para ter XP e streak atualizados
        User fresh = userRepository.findById(user.getId()).orElse(user);
        Map<String, TopicStatusDto> topicStates = buildTopicStates(fresh, allProgress, attempts);
        List<LearningTrackDto> tracks = buildTracks(topicStates);
        List<TopicStatusDto> next = topicStates.values().stream()
                .filter(t -> "AVAILABLE".equals(t.getState()) || "VISITED".equals(t.getState()))
                .filter(t -> !"COMPLETED".equals(t.getState()))
                .sorted(Comparator.comparing(TopicStatusDto::getOrderIndex))
                .limit(3)
                .toList();

        return ProgressResponse.builder()
                .totalXp(fresh.getXp())
                .streakDays(fresh.getStreakDays())
                .topicsVisited((int) visited)
                .topicsCompleted((int) completed)
                .totalTopics(topicCatalog.totalTopics())
                .progressPercent(pct)
                .lastStudyDate(fresh.getLastStudyDate())
                .topics(topicsMap)
                .earnedEventKeys(earnedKeys)
                .level(calculateLevel(fresh.getXp()))
                .badges(userBadgeRepository.findByUser(fresh).stream().map(this::toBadgeDto).toList())
                .tracks(tracks)
                .topicStates(topicStates)
                .nextRecommendedTopics(next)
                .build();
    }

    // ── Helpers privados ────────────────────────────────────────────

    private boolean maybeCompleteTopic(User user, String topicId) {
        if (topicProgressRepository.findByUserAndTopicId(user, topicId)
                .map(TopicProgress::isCompleted)
                .orElse(false)) {
            return false;
        }

        int required = Math.max(1, topicCatalog.requiredExercisesForTopic(topicId));
        long correctRequired = exerciseAttemptRepository.findByUserAndTopicId(user, topicId).stream()
                .filter(ExerciseAttempt::getCorrect)
                .map(ExerciseAttempt::getExerciseId)
                .distinct()
                .count();
        int score = (int) Math.round(correctRequired * 100.0 / required);
        if (score < TopicCatalog.COMPLETION_THRESHOLD_PERCENT) {
            return false;
        }

        markTopicCompleted(user, topicId, 0);
        return true;
    }

    private void markTopicCompleted(User user, String topicId, int amount) {
        topicProgressRepository
                .findByUserAndTopicId(user, topicId)
                .ifPresentOrElse(
                        tp -> {
                            tp.addXp(amount);
                            if (!tp.isCompleted()) {
                                tp.markCompleted();
                                user.setTopicsCompleted(user.getTopicsCompleted() + 1);
                                userRepository.save(user);
                            }
                            topicProgressRepository.save(tp);
                        },
                        () -> {
                            // Visita implícita ao completar
                            TopicProgress tp = TopicProgress.builder()
                                    .user(user)
                                    .topicId(topicId)
                                    .state(TopicState.COMPLETED)
                                    .xpEarned(amount)
                                    .build();
                            tp.markCompleted();
                            topicProgressRepository.save(tp);
                            user.setTopicsCompleted(user.getTopicsCompleted() + 1);
                            userRepository.save(user);
                        }
                );
    }

    private Map<String, TopicStatusDto> buildTopicStates(
            User user,
            List<TopicProgress> progress,
            List<ExerciseAttempt> attempts) {
        Map<String, TopicProgress> progressByTopic = progress.stream()
                .collect(Collectors.toMap(TopicProgress::getTopicId, Function.identity()));
        Set<String> completed = progress.stream()
                .filter(TopicProgress::isCompleted)
                .map(TopicProgress::getTopicId)
                .collect(Collectors.toSet());

        return topicCatalog.topics().stream()
                .map(topic -> toTopicStatus(topic, progressByTopic.get(topic.id()), attempts, completed))
                .collect(Collectors.toMap(TopicStatusDto::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    private TopicStatusDto toTopicStatus(
            TopicCatalog.TopicDefinition topic,
            TopicProgress progress,
            List<ExerciseAttempt> attempts,
            Set<String> completedTopics) {
        String state;
        if (progress != null && progress.isCompleted()) {
            state = TopicState.COMPLETED.name();
        } else if (progress != null) {
            state = TopicState.VISITED.name();
        } else if (completedTopics.containsAll(topic.prerequisites())) {
            state = TopicState.AVAILABLE.name();
        } else {
            state = TopicState.LOCKED.name();
        }

        List<TopicCatalog.ExerciseDefinition> exercises = topicCatalog.exercisesForTopic(topic.id());
        Set<String> correctExercises = attempts.stream()
                .filter(a -> a.getTopicId().equals(topic.id()))
                .filter(ExerciseAttempt::getCorrect)
                .map(ExerciseAttempt::getExerciseId)
                .collect(Collectors.toSet());
        int required = Math.max(1, (int) exercises.stream().filter(TopicCatalog.ExerciseDefinition::required).count());
        int score = (int) Math.round(correctExercises.size() * 100.0 / required);

        return TopicStatusDto.builder()
                .id(topic.id())
                .title(topic.title())
                .description(topic.description())
                .trackId(topic.trackId())
                .path(topic.path())
                .orderIndex(topic.orderIndex())
                .prerequisites(topic.prerequisites())
                .state(state)
                .exerciseCount(exercises.size())
                .requiredCorrect((int) Math.ceil(required * (TopicCatalog.COMPLETION_THRESHOLD_PERCENT / 100.0)))
                .correctExercises(correctExercises.size())
                .bestScorePercent(Math.min(100, score))
                .build();
    }

    private List<LearningTrackDto> buildTracks(Map<String, TopicStatusDto> topicStates) {
        return topicCatalog.tracks().stream()
                .map(track -> {
                    List<TopicStatusDto> topics = topicStates.values().stream()
                            .filter(topic -> topic.getTrackId().equals(track.id()))
                            .sorted(Comparator.comparing(TopicStatusDto::getOrderIndex))
                            .toList();
                    int completed = (int) topics.stream().filter(t -> "COMPLETED".equals(t.getState())).count();
                    int available = (int) topics.stream().filter(t -> !"LOCKED".equals(t.getState())).count();
                    int pct = topics.isEmpty() ? 0 : (int) Math.round(completed * 100.0 / topics.size());
                    return LearningTrackDto.builder()
                            .id(track.id())
                            .name(track.name())
                            .description(track.description())
                            .orderIndex(track.orderIndex())
                            .totalTopics(topics.size())
                            .completedTopics(completed)
                            .availableTopics(available)
                            .progressPercent(pct)
                            .topics(topics)
                            .build();
                })
                .toList();
    }

    private void ensureTopicAvailable(User user, String topicId) {
        List<TopicProgress> progress = topicProgressRepository.findByUser(user);
        Set<String> completed = progress.stream()
                .filter(TopicProgress::isCompleted)
                .map(TopicProgress::getTopicId)
                .collect(Collectors.toSet());
        TopicCatalog.TopicDefinition topic = topicCatalog.topic(topicId)
                .orElseThrow(() -> new InvalidTopicException("topicId não cadastrado: " + topicId));
        if (!completed.containsAll(topic.prerequisites())) {
            throw new LockedTopicException("Complete os pré-requisitos antes de acessar: " + topic.prerequisites());
        }
    }

    private ExerciseDto toExerciseDto(TopicCatalog.ExerciseDefinition exercise) {
        return ExerciseDto.builder()
                .id(exercise.id())
                .topicId(exercise.topicId())
                .type(exercise.type())
                .prompt(exercise.prompt())
                .options(exercise.options())
                .required(exercise.required())
                .xp(exercise.xp())
                .build();
    }

    private String normalizeAnswer(String answer) {
        return answer == null ? "" : answer.trim().toUpperCase(Locale.ROOT);
    }

    private LevelDto calculateLevel(int xp) {
        int level = Math.max(1, (xp / 100) + 1);
        int current = (level - 1) * 100;
        int next = level * 100;
        return LevelDto.builder()
                .level(level)
                .currentXp(xp)
                .xpForCurrentLevel(current)
                .xpForNextLevel(next)
                .progressPercent(Math.min(100, (int) Math.round((xp - current) * 100.0 / (next - current))))
                .build();
    }

    private void awardBadges(User user) {
        if (user.getTopicsCompleted() >= 1) awardBadge(user, "first-topic", "Primeiro tópico", "Concluiu seu primeiro tópico.");
        if (user.getStreakDays() >= 7) awardBadge(user, "streak-7", "7 dias de estudo", "Manteve uma sequência de 7 dias.");
        if (user.getXp() >= 100) awardBadge(user, "xp-100", "100 XP", "Acumulou os primeiros 100 XP.");
    }

    private void awardBadge(User user, String id, String name, String description) {
        if (userBadgeRepository.existsByUserAndBadgeId(user, id)) return;
        userBadgeRepository.save(UserBadge.builder()
                .user(user)
                .badgeId(id)
                .name(name)
                .description(description)
                .build());
    }

    private BadgeDto toBadgeDto(UserBadge badge) {
        return BadgeDto.builder()
                .id(badge.getBadgeId())
                .name(badge.getName())
                .description(badge.getDescription())
                .earnedAt(badge.getEarnedAt())
                .build();
    }

    private GeneratedTemplate buildGeneratedTemplate(TopicCatalog.TopicDefinition topic, long sequence, int difficulty) {
        int variant = (int) ((sequence + difficulty) % 4);
        String title = topic.title();
        String description = topic.description();
        if (variant == 0) {
            return new GeneratedTemplate(
                    "MULTIPLE_CHOICE",
                    "No território \"" + title + "\", qual decisão preserva melhor a ideia central do tópico?",
                    List.of(
                            "Escolher a estrutura mais complexa antes de analisar o problema.",
                            description,
                            "Ignorar custo temporal quando o exemplo é pequeno.",
                            "Trocar invariantes por tentativa e erro."
                    ),
                    "B",
                    "Correto: a escolha preserva o conceito principal do tópico.",
                    "Revise a clareira conceitual deste tópico: " + description);
        }
        if (variant == 1) {
            boolean trueAnswer = sequence % 2 == 0;
            String prompt = trueAnswer
                    ? "Verdadeiro ou falso: " + description
                    : "Verdadeiro ou falso: este tópico recomenda ignorar invariantes para acelerar a implementação.";
            return new GeneratedTemplate(
                    "TRUE_FALSE",
                    prompt,
                    List.of("Verdadeiro", "Falso"),
                    trueAnswer ? "A" : "B",
                    "Correto: você avaliou a afirmação a partir da regra do tópico.",
                    "Observe a formulação: o objetivo é preservar contrato, custo e invariantes.");
        }
        if (variant == 2) {
            int base = 2 + difficulty + (int) (sequence % 4);
            return new GeneratedTemplate(
                    "TRACE_OUTPUT",
                    "Rastreie a sequência de operações do tópico \"" + title + "\": inserir " + base
                            + ", inserir " + (base + 2) + ", buscar " + base + ". Qual resultado final é esperado?",
                    List.of(
                            "A busca encontra " + base + " e a estrutura mantém sua regra.",
                            "A busca falha porque toda inserção remove o valor anterior.",
                            "O resultado é indefinido sem reiniciar a estrutura.",
                            "A operação correta sempre ignora o primeiro valor."
                    ),
                    "A",
                    "Correto: o rastreamento preservou a sequência e o invariante.",
                    "Refaça a sequência passo a passo e acompanhe o estado após cada operação.");
        }
        return new GeneratedTemplate(
                "SIMULATOR_TASK",
                "Mini expedição guiada: use o simulador relacionado a \"" + title
                        + "\" para executar inserir, buscar e resetar. Ao concluir, marque a tarefa como feita.",
                List.of("Concluído", "Ainda não concluí"),
                "A",
                "Boa: manipular a estrutura ajuda a fixar o comportamento real.",
                "Antes de marcar, execute ao menos uma inserção e uma busca no simulador.");
    }

    private GeneratedExerciseDto toGeneratedExerciseDto(GeneratedExercise exercise) {
        return GeneratedExerciseDto.builder()
                .id(exercise.getGeneratedId())
                .topicId(exercise.getTopicId())
                .type(exercise.getType())
                .difficulty(exercise.getDifficulty())
                .prompt(exercise.getPrompt())
                .options(deserializeOptions(exercise.getOptions()))
                .answered(exercise.getAnswered())
                .createdAt(exercise.getCreatedAt())
                .build();
    }

    private String serializeOptions(List<String> options) {
        return options == null ? "" : String.join("\n", options);
    }

    private List<String> deserializeOptions(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return Arrays.stream(raw.split("\\R")).toList();
    }

    private String simulationXpEventKey(SimulationEventRequest req) {
        String milestone = req.getMilestone() == null ? "" : req.getMilestone().trim().toUpperCase(Locale.ROOT);
        String simulator = sanitizeKey(req.getSimulatorType());
        String topic = sanitizeKey(req.getTopicId());
        if ("FIRST_RUN".equals(milestone)) return "sim_" + simulator + "_" + topic + "_first_run";
        if ("CHALLENGE_COMPLETED".equals(milestone)) return "sim_" + simulator + "_" + topic + "_challenge";
        return null;
    }

    private int simulationXpAmount(SimulationEventRequest req) {
        String milestone = req.getMilestone() == null ? "" : req.getMilestone().trim().toUpperCase(Locale.ROOT);
        if ("FIRST_RUN".equals(milestone)) return 8;
        if ("CHALLENGE_COMPLETED".equals(milestone)) return 15;
        return 0;
    }

    private String sanitizeKey(String value) {
        return normalizeAnswer(value).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "-");
    }

    private List<SimulationMission> missionsForTopic(String topicId) {
        return switch (topicId) {
            case "arrays" -> List.of(new SimulationMission("arrays-map-3", "arrays", "ARRAY", "Mapa contíguo",
                    "Insira pelo menos três valores e faça uma busca antes de validar.",
                    List.of("inserir", "inserir", "inserir", "buscar"), "values.length >= 3", 18));
            case "pilhas" -> List.of(
                    new SimulationMission("stack-two-pop", "pilhas", "STACK", "Topo da clareira",
                            "Empilhe ao menos dois valores e observe o topo antes de remover.",
                            List.of("push", "push", "pop"), "values.length >= 1", 16),
                    new SimulationMission("queue-three-front", "pilhas", "QUEUE", "Fila da expedição",
                            "Enfileire três valores e mantenha a frente visível.",
                            List.of("enqueue", "enqueue", "enqueue"), "values.length >= 3", 16));
            case "ll" -> List.of(new SimulationMission("linked-chain-3", "ll", "LINKED_LIST", "Corrente de nós",
                    "Monte uma lista com ao menos três nós encadeados.",
                    List.of("inserir", "inserir", "inserir"), "values.length >= 3", 18));
            case "bst" -> List.of(new SimulationMission("bst-branching", "bst", "BST", "Raiz e dois ramos",
                    "Insira ao menos três valores para formar raiz, ramo esquerdo e ramo direito.",
                    List.of("inserir raiz", "inserir menor", "inserir maior"), "has lower and higher than root", 20));
            case "hash" -> List.of(new SimulationMission("hash-collision", "hash", "HASH_TABLE", "Colisão controlada",
                    "Crie uma colisão inserindo dois valores que caiam no mesmo balde módulo 5.",
                    List.of("inserir", "inserir", "observar colisão"), "two values share mod 5", 20));
            case "grafos" -> List.of(new SimulationMission("graph-path-4", "grafos", "GRAPH", "Trilha conectada",
                    "Monte um grafo com quatro nós e ao menos três arestas.",
                    List.of("inserir nós", "conectar arestas", "percorrer"), "values.length >= 4 && edges.length >= 3", 22));
            default -> List.of();
        };
    }

    private Optional<SimulationMission> missionById(String missionId) {
        return topicCatalog.topicIds().stream()
                .flatMap(topicId -> missionsForTopic(topicId).stream())
                .filter(mission -> mission.id().equals(missionId))
                .findFirst();
    }

    private MissionCheck checkMission(SimulationMission mission, String snapshot) {
        JsonNode root;
        try {
            root = objectMapper.readTree(snapshot);
        } catch (Exception e) {
            return new MissionCheck(false, "Não consegui ler o estado do simulador. Execute a missão novamente.");
        }
        JsonNode valuesNode = root.path("values");
        JsonNode edgesNode = root.path("edges");
        List<Integer> values = new ArrayList<>();
        if (valuesNode.isArray()) {
            valuesNode.forEach(v -> { if (v.canConvertToInt()) values.add(v.asInt()); });
        }
        int edges = edgesNode.isArray() ? edgesNode.size() : 0;
        boolean ok = switch (mission.id()) {
            case "arrays-map-3", "linked-chain-3" -> values.size() >= 3;
            case "stack-two-pop" -> values.size() >= 1;
            case "queue-three-front" -> values.size() >= 3;
            case "bst-branching" -> hasBstBranches(values);
            case "hash-collision" -> hasHashCollision(values);
            case "graph-path-4" -> values.size() >= 4 && edges >= 3;
            default -> false;
        };
        return new MissionCheck(ok, ok
                ? "Missão validada: você demonstrou o comportamento essencial da estrutura."
                : "Ainda falta cumprir o critério: " + mission.successCriteria() + ".");
    }

    private boolean hasBstBranches(List<Integer> values) {
        if (values.size() < 3) return false;
        int root = values.get(0);
        return values.stream().anyMatch(v -> v < root) && values.stream().anyMatch(v -> v > root);
    }

    private boolean hasHashCollision(List<Integer> values) {
        Map<Integer, Long> buckets = values.stream()
                .collect(Collectors.groupingBy(v -> Math.abs(v) % 5, Collectors.counting()));
        return buckets.values().stream().anyMatch(count -> count >= 2);
    }

    private record SimulationMission(
            String id,
            String topicId,
            String simulatorType,
            String title,
            String instructions,
            List<String> requiredActions,
            String successCriteria,
            int xp
    ) {
        SimulationMissionDto toDto(boolean completed) {
            return SimulationMissionDto.builder()
                    .id(id)
                    .topicId(topicId)
                    .simulatorType(simulatorType)
                    .title(title)
                    .instructions(instructions)
                    .requiredActions(requiredActions)
                    .successCriteria(successCriteria)
                    .xp(xp)
                    .completed(completed)
                    .build();
        }
    }

    private record MissionCheck(boolean completed, String feedback) {}

    private record GeneratedTemplate(
            String type,
            String prompt,
            List<String> options,
            String correctAnswer,
            String correctFeedback,
            String wrongFeedback
    ) {}

    private boolean awardEventIfNew(User user, String eventKey, int amount) {
        if (xpEventRepository.existsByUserAndEventKey(user, eventKey)) {
            log.debug("XP já concedido para user={} event={}", user.getId(), eventKey);
            return false;
        }

        XpEvent event = XpEvent.builder()
                .user(user)
                .eventKey(eventKey)
                .amount(amount)
                .build();
        try {
            xpEventRepository.saveAndFlush(event);
        } catch (DataIntegrityViolationException e) {
            log.debug("XP já concedido em corrida: user={} event={}", user.getId(), eventKey);
            return false;
        }

        user.setXp(user.getXp() + amount);
        userRepository.save(user);
        return true;
    }

    private void validateTopic(String topicId) {
        if (!topicCatalog.contains(topicId)) {
            throw new InvalidTopicException("topicId não cadastrado: " + topicId);
        }
    }
}
