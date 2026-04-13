package com.aedstudio.controller;

import com.aedstudio.dto.ProgressResponse;
import com.aedstudio.dto.TopicVisitRequest;
import com.aedstudio.dto.XpAwardRequest;
import com.aedstudio.model.User;
import com.aedstudio.service.ProgressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller de progresso do estudante.
 *
 * Todas as rotas requerem autenticação (JWT ou sessão).
 *
 *   GET  /api/progress          → estado completo de progresso
 *   POST /api/progress/visit    → registra visita a tópico
 *   POST /api/progress/xp       → concede XP por quiz/desafio (idempotente)
 */
@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;

    // ── GET /api/progress ────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<ProgressResponse> getProgress(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(progressService.getProgress(user));
    }

    // ── POST /api/progress/visit ─────────────────────────────────────

    @PostMapping("/visit")
    public ResponseEntity<Map<String, String>> visit(
            @Valid @RequestBody TopicVisitRequest req,
            @AuthenticationPrincipal User user) {
        progressService.recordVisit(user, req.getTopicId());
        return ResponseEntity.ok(Map.of(
                "topicId", req.getTopicId(),
                "status", "visited"
        ));
    }

    // ── POST /api/progress/xp ────────────────────────────────────────

    @PostMapping("/xp")
    public ResponseEntity<Map<String, Object>> awardXp(
            @Valid @RequestBody XpAwardRequest req,
            @AuthenticationPrincipal User user) {
        int awarded = progressService.awardXp(user, req);
        return ResponseEntity.ok(Map.of(
                "awarded", awarded,
                "eventKey", req.getReason(),
                "alreadyEarned", awarded == 0
        ));
    }
}
