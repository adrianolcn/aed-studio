package com.aedstudio.service;

import com.aedstudio.config.JwtService;
import com.aedstudio.dto.*;
import com.aedstudio.exception.AuthException;
import com.aedstudio.exception.DuplicateResourceException;
import com.aedstudio.model.RefreshToken;
import com.aedstudio.model.Role;
import com.aedstudio.model.User;
import com.aedstudio.repository.RefreshTokenRepository;
import com.aedstudio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Serviço de autenticação.
 *
 * Suporta JWT stateless para clientes API, mobile e SPA desacoplada.
 *    POST /api/auth/login  → retorna { accessToken, refreshToken }
 *    Authorization: Bearer <token> em cada requisição
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository          userRepository;
    private final RefreshTokenRepository  refreshTokenRepository;
    private final PasswordEncoder         passwordEncoder;
    private final JwtService              jwtService;
    private final AuthenticationManager   authenticationManager;

    @org.springframework.beans.factory.annotation.Value("${jwt.refresh-expiration-ms}")
    private long refreshTokenExpirationMs;

    // ── Registro ────────────────────────────────────────────────────

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        // Validações de unicidade
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new DuplicateResourceException("E-mail já cadastrado: " + req.getEmail());
        }
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new DuplicateResourceException("Username já em uso: " + req.getUsername());
        }

        User user = User.builder()
                .username(req.getUsername())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .fullName(req.getFullName())
                .role(Role.STUDENT)
                .build();

        user = userRepository.save(user);
        log.info("Novo usuário registrado: {} ({})", user.getUsername(), user.getEmail());

        return buildAuthResponse(user);
    }

    // ── Login JWT (API) ─────────────────────────────────────────────

    @Transactional
    public AuthResponse loginJwt(LoginRequest req) {
        User user = authenticate(req.getEmail(), req.getPassword());
        user.updateStreak();
        userRepository.save(user);
        return buildAuthResponse(user);
    }

    // ── Refresh Token ───────────────────────────────────────────────

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest req) {
        RefreshToken stored = refreshTokenRepository
                .findByToken(req.getRefreshToken())
                .orElseThrow(() -> new AuthException("Refresh token inválido"));

        if (stored.isExpired()) {
            refreshTokenRepository.delete(stored);
            throw new AuthException("Refresh token expirado. Faça login novamente.");
        }

        User user = stored.getUser();

        // Rotation: revoga o token antigo e emite novo par
        refreshTokenRepository.delete(stored);
        log.debug("Refresh token rotacionado para: {}", user.getEmail());

        return buildAuthResponse(user);
    }

    // ── Logout ──────────────────────────────────────────────────────

    @Transactional
    public void logoutJwt(User user) {
        refreshTokenRepository.deleteByUser(user);
        log.info("Logout JWT — refresh tokens revogados para: {}", user.getEmail());
    }

    // ── Helpers privados ────────────────────────────────────────────

    private User authenticate(String email, String password) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password));
            return (User) auth.getPrincipal();
        } catch (BadCredentialsException e) {
            throw new AuthException("E-mail ou senha inválidos");
        }
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken  = jwtService.generateAccessToken(user);
        String refreshToken = generateAndSaveRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtService.extractExpiresIn(accessToken))
                .user(toUserSummary(user))
                .build();
    }

    private String generateAndSaveRefreshToken(User user) {
        // Revoga token anterior (1 refresh token por usuário)
        refreshTokenRepository.deleteByUser(user);
        refreshTokenRepository.flush();

        String token = UUID.randomUUID().toString();
        RefreshToken rt = RefreshToken.builder()
                .token(token)
                .user(user)
                .expiresAt(Instant.now().plusMillis(refreshTokenExpirationMs))
                .build();
        refreshTokenRepository.save(rt);
        return token;
    }

    public UserSummaryDto toUserSummary(User user) {
        return UserSummaryDto.builder()
                .id(user.getId())
                .username(user.getProfileUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .xp(user.getXp())
                .streakDays(user.getStreakDays())
                .topicsCompleted(user.getTopicsCompleted())
                .lastStudyDate(user.getLastStudyDate())
                .build();
    }
}
