package com.aedstudio.controller;

import com.aedstudio.dto.ProgressResponse;
import com.aedstudio.dto.TopicVisitRequest;
import com.aedstudio.dto.XpAwardRequest;
import com.aedstudio.dto.XpAwardResponse;
import com.aedstudio.model.User;
import com.aedstudio.service.ProgressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Controller de progresso do estudante.
 *
 * Todas as rotas requerem autenticação JWT.
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
    public ResponseEntity<ProgressResponse> visit(
            @Valid @RequestBody TopicVisitRequest req,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(progressService.recordVisit(user, req.getTopicId()));
    }

    // ── POST /api/progress/xp ────────────────────────────────────────

    @PostMapping("/xp")
    public ResponseEntity<XpAwardResponse> awardXp(
            @Valid @RequestBody XpAwardRequest req,
            @AuthenticationPrincipal User user) {
        int awarded = progressService.awardXp(user, req);
        return ResponseEntity.ok(XpAwardResponse.builder()
                .awarded(awarded)
                .eventKey(req.getReason())
                .alreadyEarned(awarded == 0)
                .progress(progressService.getProgress(user))
                .build());
    }
}
