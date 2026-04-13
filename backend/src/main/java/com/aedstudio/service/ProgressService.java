package com.aedstudio.service;

import com.aedstudio.dto.ProgressResponse;
import com.aedstudio.dto.XpAwardRequest;
import com.aedstudio.model.*;
import com.aedstudio.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
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

    private static final int TOTAL_TOPICS = 22;

    private final UserRepository          userRepository;
    private final TopicProgressRepository topicProgressRepository;
    private final XpEventRepository       xpEventRepository;

    // ── Registrar visita ────────────────────────────────────────────

    @Transactional
    public void recordVisit(User user, String topicId) {
        boolean alreadyVisited = topicProgressRepository.existsByUserAndTopicId(user, topicId);
        if (!alreadyVisited) {
            TopicProgress tp = TopicProgress.builder()
                    .user(user)
                    .topicId(topicId)
                    .state(TopicState.VISITED)
                    .build();
            topicProgressRepository.save(tp);
            log.debug("Nova visita registrada: user={} topic={}", user.getId(), topicId);
        }
        // Atualiza streak independentemente
        user.updateStreak();
        userRepository.save(user);
    }

    // ── Conceder XP (idempotente) ───────────────────────────────────

    @Transactional
    public int awardXp(User user, XpAwardRequest req) {
        String eventKey = req.getReason();

        // Idempotência: se já ganhou este evento, retorna 0
        if (xpEventRepository.existsByUserAndEventKey(user, eventKey)) {
            log.debug("XP já concedido para user={} event={}", user.getId(), eventKey);
            return 0;
        }

        // Registra o evento
        XpEvent event = XpEvent.builder()
                .user(user)
                .eventKey(eventKey)
                .amount(req.getAmount())
                .build();
        xpEventRepository.save(event);

        // Incrementa XP do usuário
        userRepository.addXp(user.getId(), req.getAmount());

        // Se o reason indica conclusão de quiz/código, marca tópico como completo
        if (isCompletionEvent(eventKey)) {
            markTopicCompleted(user, req.getTopicId());
        }

        log.info("XP concedido: user={} event={} amount={}", user.getId(), eventKey, req.getAmount());
        return req.getAmount();
    }

    // ── Buscar progresso completo ───────────────────────────────────

    @Transactional(readOnly = true)
    public ProgressResponse getProgress(User user) {
        List<TopicProgress> allProgress = topicProgressRepository.findByUser(user);
        List<String> earnedKeys = xpEventRepository.findEventKeysByUser(user);

        Map<String, String> topicsMap = allProgress.stream()
                .collect(Collectors.toMap(
                        TopicProgress::getTopicId,
                        tp -> tp.getState().name()
                ));

        long visited   = allProgress.size();
        long completed = allProgress.stream()
                .filter(TopicProgress::isCompleted)
                .count();

        int pct = (int) Math.round((visited * 100.0) / TOTAL_TOPICS);

        // Recarrega user para ter XP e streak atualizados
        User fresh = userRepository.findById(user.getId()).orElse(user);

        return ProgressResponse.builder()
                .totalXp(fresh.getXp())
                .streakDays(fresh.getStreakDays())
                .topicsVisited((int) visited)
                .topicsCompleted((int) completed)
                .totalTopics(TOTAL_TOPICS)
                .progressPercent(pct)
                .lastStudyDate(fresh.getLastStudyDate())
                .topics(topicsMap)
                .earnedEventKeys(earnedKeys)
                .build();
    }

    // ── Helpers privados ────────────────────────────────────────────

    private void markTopicCompleted(User user, String topicId) {
        topicProgressRepository
                .findByUserAndTopicId(user, topicId)
                .ifPresentOrElse(
                        tp -> {
                            if (!tp.isCompleted()) {
                                tp.markCompleted();
                                topicProgressRepository.save(tp);
                                userRepository.incrementTopicsCompleted(user.getId());
                            }
                        },
                        () -> {
                            // Visita implícita ao completar
                            TopicProgress tp = TopicProgress.builder()
                                    .user(user)
                                    .topicId(topicId)
                                    .state(TopicState.COMPLETED)
                                    .xpEarned(0)
                                    .build();
                            tp.markCompleted();
                            topicProgressRepository.save(tp);
                            userRepository.incrementTopicsCompleted(user.getId());
                        }
                );
    }

    /**
     * Eventos que indicam conclusão real do tópico (quiz ou código).
     * Visitas simples ("visit_*") não completam — apenas registram.
     */
    private boolean isCompletionEvent(String eventKey) {
        return eventKey.startsWith("quiz_") || eventKey.startsWith("code_");
    }
}
