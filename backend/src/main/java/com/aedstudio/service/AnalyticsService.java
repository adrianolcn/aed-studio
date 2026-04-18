package com.aedstudio.service;

import com.aedstudio.dto.*;
import com.aedstudio.model.ExerciseAttempt;
import com.aedstudio.model.SimulationEvent;
import com.aedstudio.model.User;
import com.aedstudio.model.XpEvent;
import com.aedstudio.repository.ExerciseAttemptRepository;
import com.aedstudio.repository.SimulationEventRepository;
import com.aedstudio.repository.XpEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ProgressService progressService;
    private final TopicCatalog topicCatalog;
    private final ExerciseAttemptRepository exerciseAttemptRepository;
    private final SimulationEventRepository simulationEventRepository;
    private final XpEventRepository xpEventRepository;

    @Transactional(readOnly = true)
    public AnalyticsOverviewDto overview(User user) {
        List<ExerciseAttempt> attempts = exerciseAttemptRepository.findByUser(user);
        List<SimulationEvent> simulationEvents = simulationEventRepository.findByUser(user);
        ProgressResponse progress = progressService.getProgress(user);
        List<TopicAnalyticsDto> topics = topics(user);

        int totalAttempts = attempts.size();
        int correct = (int) attempts.stream().filter(a -> Boolean.TRUE.equals(a.getCorrect())).count();
        int accuracy = percent(correct, totalAttempts);

        List<TopicAnalyticsDto> strongest = topics.stream()
                .filter(t -> t.getAttempts() > 0)
                .sorted(Comparator.comparing(TopicAnalyticsDto::getAccuracyPercent).reversed()
                        .thenComparing(TopicAnalyticsDto::getAttempts).reversed())
                .limit(3)
                .toList();

        List<TopicAnalyticsDto> attention = topics.stream()
                .filter(t -> t.getAttempts() > 0 && t.getAccuracyPercent() < TopicCatalog.COMPLETION_THRESHOLD_PERCENT
                        || ("VISITED".equals(t.getState()) && t.getAttempts() == 0))
                .sorted(Comparator.comparing(TopicAnalyticsDto::getAccuracyPercent))
                .limit(4)
                .toList();

        List<String> suggestions = new ArrayList<>();
        if (totalAttempts == 0) {
            suggestions.add("Comece pela primeira expedição disponível e responda um exercício para abrir seu mapa de desempenho.");
        }
        if (!attention.isEmpty()) {
            suggestions.add("Revise " + attention.get(0).getTitle() + ": os dados indicam ponto de atenção nessa clareira.");
        }
        if (progress.getStreakDays() < 3) {
            suggestions.add("Estude em dias consecutivos para consolidar memória e aumentar sua sequência.");
        }
        if (simulationEvents.isEmpty()) {
            suggestions.add("Use um simulador para transformar o conceito em manipulação visual.");
        }

        return AnalyticsOverviewDto.builder()
                .totalAttempts(totalAttempts)
                .correctAttempts(correct)
                .overallAccuracyPercent(accuracy)
                .simulatorInteractions(simulationEvents.size())
                .totalXp(progress.getTotalXp())
                .streakDays(progress.getStreakDays())
                .consistencyLabel(consistency(progress.getStreakDays()))
                .strongestTopics(strongest)
                .attentionTopics(attention)
                .practicalSuggestions(suggestions)
                .build();
    }

    @Transactional(readOnly = true)
    public List<TopicAnalyticsDto> topics(User user) {
        List<ExerciseAttempt> attempts = exerciseAttemptRepository.findByUser(user);
        Map<String, List<ExerciseAttempt>> attemptsByTopic = attempts.stream()
                .collect(Collectors.groupingBy(ExerciseAttempt::getTopicId));
        Map<String, Long> simulationsByTopic = simulationEventRepository.findByUser(user).stream()
                .collect(Collectors.groupingBy(SimulationEvent::getTopicId, Collectors.counting()));
        ProgressResponse progress = progressService.getProgress(user);
        Map<String, TopicStatusDto> states = progress.getTopicStates() == null ? Map.of() : progress.getTopicStates();

        return topicCatalog.topics().stream()
                .map(topic -> {
                    List<ExerciseAttempt> topicAttempts = attemptsByTopic.getOrDefault(topic.id(), List.of());
                    int correct = (int) topicAttempts.stream().filter(a -> Boolean.TRUE.equals(a.getCorrect())).count();
                    int total = topicAttempts.size();
                    int accuracy = percent(correct, total);
                    LocalDateTime last = topicAttempts.stream()
                            .map(ExerciseAttempt::getAttemptedAt)
                            .max(LocalDateTime::compareTo)
                            .orElse(null);
                    String state = Optional.ofNullable(states.get(topic.id())).map(TopicStatusDto::getState).orElse("LOCKED");
                    int simulatorCount = simulationsByTopic.getOrDefault(topic.id(), 0L).intValue();
                    return TopicAnalyticsDto.builder()
                            .topicId(topic.id())
                            .title(topic.title())
                            .trackId(topic.trackId())
                            .state(state)
                            .attempts(total)
                            .correctAttempts(correct)
                            .accuracyPercent(accuracy)
                            .simulatorInteractions(simulatorCount)
                            .lastAttemptAt(last)
                            .insight(topicInsight(state, total, accuracy, simulatorCount))
                            .build();
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TrackAnalyticsDto> trails(User user) {
        List<TopicAnalyticsDto> topics = topics(user);
        Map<String, TopicAnalyticsDto> byTopic = topics.stream()
                .collect(Collectors.toMap(TopicAnalyticsDto::getTopicId, Function.identity()));
        ProgressResponse progress = progressService.getProgress(user);

        return progress.getTracks().stream()
                .map(track -> {
                    List<TopicAnalyticsDto> trackTopics = track.getTopics().stream()
                            .map(t -> byTopic.get(t.getId()))
                            .filter(Objects::nonNull)
                            .toList();
                    int attempts = trackTopics.stream().mapToInt(TopicAnalyticsDto::getAttempts).sum();
                    int correct = trackTopics.stream().mapToInt(TopicAnalyticsDto::getCorrectAttempts).sum();
                    int accuracy = percent(correct, attempts);
                    return TrackAnalyticsDto.builder()
                            .trackId(track.getId())
                            .name(track.getName())
                            .totalTopics(track.getTotalTopics())
                            .completedTopics(track.getCompletedTopics())
                            .progressPercent(track.getProgressPercent())
                            .attempts(attempts)
                            .accuracyPercent(accuracy)
                            .insight(trackInsight(track.getProgressPercent(), attempts, accuracy))
                            .build();
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<XpHistoryPointDto> xpHistory(User user) {
        List<XpEvent> events = xpEventRepository.findByUserOrderByEarnedAtAsc(user);
        Map<java.time.LocalDate, Integer> byDate = events.stream()
                .collect(Collectors.groupingBy(
                        event -> event.getEarnedAt().toLocalDate(),
                        TreeMap::new,
                        Collectors.summingInt(XpEvent::getAmount)));
        List<XpHistoryPointDto> history = new ArrayList<>();
        int cumulative = 0;
        for (Map.Entry<java.time.LocalDate, Integer> entry : byDate.entrySet()) {
            cumulative += entry.getValue();
            history.add(XpHistoryPointDto.builder()
                    .date(entry.getKey())
                    .xp(entry.getValue())
                    .cumulativeXp(cumulative)
                    .build());
        }
        return history;
    }

    private String topicInsight(String state, int attempts, int accuracy, int simulatorCount) {
        if ("LOCKED".equals(state)) return "Território bloqueado por pré-requisitos.";
        if ("COMPLETED".equals(state) && accuracy >= 70) return "Marco consolidado; bom ponto de apoio para avançar.";
        if (attempts >= 2 && accuracy < 70) return "Erro recorrente detectado; vale revisar antes de seguir.";
        if ("VISITED".equals(state) && attempts == 0) return "Tópico visitado sem prática registrada.";
        if (simulatorCount > 0 && attempts == 0) return "Boa exploração visual; falta transformar em resposta.";
        return "Sem sinais críticos até agora.";
    }

    private String trackInsight(int progressPercent, int attempts, int accuracy) {
        if (progressPercent == 100) return "Trilha concluída.";
        if (attempts == 0) return "Trilha ainda sem prática mensurável.";
        if (accuracy < 70) return "Avance com revisão: a taxa de acerto ainda está instável.";
        return "Trilha em avanço saudável.";
    }

    private String consistency(int streakDays) {
        if (streakDays >= 7) return "expedição constante";
        if (streakDays >= 3) return "ritmo em formação";
        if (streakDays >= 1) return "primeiro acampamento";
        return "sem sequência ativa";
    }

    private int percent(int value, int total) {
        return total == 0 ? 0 : (int) Math.round(value * 100.0 / total);
    }
}
