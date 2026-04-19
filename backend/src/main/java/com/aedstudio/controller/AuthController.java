package com.aedstudio.controller;

import com.aedstudio.dto.AuthResponse;
import com.aedstudio.dto.LoginRequest;
import com.aedstudio.dto.RefreshTokenRequest;
import com.aedstudio.dto.RegisterRequest;
import com.aedstudio.dto.UserSummaryDto;
import com.aedstudio.model.User;
import com.aedstudio.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> loginJwt(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.loginJwt(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logoutJwt(@AuthenticationPrincipal User user) {
        authService.logoutJwt(user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserSummaryDto> me(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(authService.toUserSummary(user));
    }
}
