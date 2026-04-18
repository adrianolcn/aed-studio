package com.aedstudio.service;

import com.aedstudio.dto.*;
import com.aedstudio.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final ProgressService progressService;
    private final AnalyticsService analyticsService;

    @Transactional(readOnly = true)
    public RecommendationsResponse recommendations(User user) {
        ProgressResponse progress = progressService.getProgress(user);
        List<TopicAnalyticsDto> topicAnalytics = analyticsService.topics(user);
        List<TrackAnalyticsDto> trackAnalytics = analyticsService.trails(user);

        List<RecommendationDto> nextSteps = progress.getTopicStates().values().stream()
                .filter(t -> "AVAILABLE".equals(t.getState()) || "VISITED".equals(t.getState()))
                .filter(t -> !"COMPLETED".equals(t.getState()))
                .sorted(Comparator.comparing(TopicStatusDto::getOrderIndex))
                .limit(4)
                .map(t -> RecommendationDto.builder()
                        .topicId(t.getId())
                        .title(t.getTitle())
                        .trackId(t.getTrackId())
                        .category("NEXT_STEP")
                        .action("explorar")
                        .priority(90 - t.getOrderIndex())
                        .confidence("AVAILABLE".equals(t.getState()) ? 0.86 : 0.78)
                        .reason("Você já concluiu os pré-requisitos e este território ainda não foi finalizado.")
                        .build())
                .toList();

        List<RecommendationDto> weaknessReview = topicAnalytics.stream()
                .filter(t -> t.getAttempts() >= 2 && t.getAccuracyPercent() < TopicCatalog.COMPLETION_THRESHOLD_PERCENT
                        || ("VISITED".equals(t.getState()) && t.getAttempts() == 0))
                .sorted(Comparator.comparingDouble(this::riskScore).reversed())
                .limit(3)
                .map(t -> RecommendationDto.builder()
                        .topicId(t.getTopicId())
                        .title(t.getTitle())
                        .trackId(t.getTrackId())
                        .category("REVIEW")
                        .action("revisar")
                        .priority((int) Math.round(riskScore(t)))
                        .confidence(Math.min(0.94, 0.55 + riskScore(t) / 200.0))
                        .reason(t.getAttempts() >= 2
                                ? "Seu desempenho neste tópico ficou abaixo de 70%; vale revisar antes de avançar."
                                : "Você visitou este tópico, mas ainda não registrou prática.")
                        .build())
                .toList();

        List<RecommendationDto> spacedReview = topicAnalytics.stream()
                .filter(t -> "COMPLETED".equals(t.getState()))
                .filter(t -> t.getLastAttemptAt() != null && t.getLastAttemptAt().isBefore(LocalDateTime.now().minusDays(7)))
                .sorted(Comparator.comparing(TopicAnalyticsDto::getLastAttemptAt))
                .limit(3)
                .map(t -> RecommendationDto.builder()
                        .topicId(t.getTopicId())
                        .title(t.getTitle())
                        .trackId(t.getTrackId())
                        .category("SPACED_REVIEW")
                        .action("revisar por espaçamento")
                        .priority(76)
                        .confidence(0.72)
                        .reason("Este marco foi concluído, mas está há mais de 7 dias sem contato; uma revisão curta ajuda retenção.")
                        .build())
                .toList();

        List<RecommendationDto> review = Stream.concat(weaknessReview.stream(), spacedReview.stream())
                .sorted(Comparator.comparing(RecommendationDto::getPriority).reversed())
                .limit(4)
                .toList();

        List<RecommendationDto> trailFocus = trackAnalytics.stream()
                .filter(t -> t.getProgressPercent() < 100)
                .sorted(Comparator.comparing(TrackAnalyticsDto::getProgressPercent).reversed())
                .limit(3)
                .map(t -> RecommendationDto.builder()
                        .topicId(null)
                        .title(t.getName())
                        .trackId(t.getTrackId())
                        .category("TRAIL_FOCUS")
                        .action("continuar trilha")
                        .priority(70)
                        .confidence(t.getAttempts() > 0 ? 0.74 : 0.58)
                        .reason("Esta trilha tem avanço parcial e ajuda a manter continuidade na jornada.")
                        .build())
                .toList();

        RecommendationDto primary = !review.isEmpty() && review.get(0).getConfidence() >= 0.85
                ? review.get(0)
                : nextSteps.stream().findFirst().orElse(null);

        return RecommendationsResponse.builder()
                .primary(primary)
                .nextSteps(nextSteps)
                .review(review)
                .trailFocus(trailFocus)
                .build();
    }

    private double riskScore(TopicAnalyticsDto topic) {
        double accuracyRisk = topic.getAttempts() == 0 ? 45 : Math.max(0, 100 - topic.getAccuracyPercent());
        double repeatedRisk = Math.min(25, topic.getAttempts() * 8);
        double recencyRisk = topic.getLastAttemptAt() == null ? 12 :
                Math.min(20, java.time.Duration.between(topic.getLastAttemptAt(), LocalDateTime.now()).toDays() * 2.0);
        return accuracyRisk + repeatedRisk + recencyRisk;
    }
}
