package com.aedstudio.controller;

import com.aedstudio.dto.*;
import com.aedstudio.model.User;
import com.aedstudio.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Controller de autenticação.
 *
 * Rotas públicas (sem autenticação):
 *   POST /api/auth/register       → cadastro + JWT imediato
 *   POST /api/auth/login          → login JWT (API / SPA)
 *   POST /api/auth/login-web      → login sessão (front-end web)
 *   POST /api/auth/refresh        → renovar access token via refresh token
 *
 * Rotas protegidas (requerem autenticação):
 *   POST /api/auth/logout         → logout JWT (revoga refresh token)
 *   POST /api/auth/logout-web     → logout sessão
 *   GET  /api/auth/me             → dados do usuário logado
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ── POST /api/auth/register ──────────────────────────────────────

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest req) {
        AuthResponse response = authService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ── POST /api/auth/login (JWT) ───────────────────────────────────

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> loginJwt(
            @Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.loginJwt(req));
    }

    // ── POST /api/auth/login-web (Sessão) ────────────────────────────

    /**
     * Login para o front-end web.
     * Cria sessão HTTP e retorna os dados do usuário em JSON.
     * O navegador receberá automaticamente o cookie JSESSIONID.
     */
    @PostMapping("/login-web")
    public ResponseEntity<UserSummaryDto> loginWeb(
            @Valid @RequestBody LoginRequest req,
            HttpServletRequest httpRequest) {
        UserSummaryDto user = authService.loginSession(req, httpRequest);
        return ResponseEntity.ok(user);
    }

    // ── POST /api/auth/refresh ───────────────────────────────────────

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest req) {
        return ResponseEntity.ok(authService.refreshToken(req));
    }

    // ── POST /api/auth/logout (JWT) ──────────────────────────────────

    @PostMapping("/logout")
    public ResponseEntity<Void> logoutJwt(
            @AuthenticationPrincipal User user) {
        authService.logoutJwt(user);
        return ResponseEntity.noContent().build();
    }

    // ── POST /api/auth/logout-web (Sessão) ──────────────────────────

    @PostMapping("/logout-web")
    public ResponseEntity<Void> logoutWeb(HttpServletRequest request) {
        authService.logoutSession(request);
        return ResponseEntity.noContent().build();
    }

    // ── GET /api/auth/me ─────────────────────────────────────────────

    @GetMapping("/me")
    public ResponseEntity<UserSummaryDto> me(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(authService.toUserSummary(user));
    }
}
