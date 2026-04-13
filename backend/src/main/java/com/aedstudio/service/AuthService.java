package com.aedstudio.service;

import com.aedstudio.config.JwtService;
import com.aedstudio.dto.*;
import com.aedstudio.exception.AuthException;
import com.aedstudio.model.RefreshToken;
import com.aedstudio.model.Role;
import com.aedstudio.model.User;
import com.aedstudio.repository.RefreshTokenRepository;
import com.aedstudio.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Serviço de autenticação.
 *
 * Suporta dois modos conforme RFC padrão da indústria:
 *
 * ① JWT (stateless) — para clientes API / mobile / SPA desacoplado
 *    POST /api/auth/login  → retorna { accessToken, refreshToken }
 *    Authorization: Bearer <token> em cada requisição
 *
 * ② Sessão (stateful) — para o front-end web servido junto com o back
 *    POST /api/auth/login-web → autentica e seta JSESSIONID no cookie
 *    Navegador envia o cookie automaticamente
 *    Sessão armazenada no PostgreSQL via Spring Session JDBC
 *
 * Ambos usam o mesmo UserDetailsService e o mesmo BCrypt hash.
 * A diferença está no que é retornado e como o estado é mantido.
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

    private final SecurityContextHolderStrategy securityContextStrategy =
            SecurityContextHolder.getContextHolderStrategy();

    // ── Registro ────────────────────────────────────────────────────

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        // Validações de unicidade
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new AuthException("E-mail já cadastrado: " + req.getEmail());
        }
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new AuthException("Username já em uso: " + req.getUsername());
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

    // ── Login Sessão (Web) ──────────────────────────────────────────

    /**
     * Autentica via form-login e cria sessão HTTP.
     * O Spring Security armazena o SecurityContext na sessão
     * que fica persistida no PostgreSQL (Spring Session JDBC).
     */
    @Transactional
    public UserSummaryDto loginSession(LoginRequest req, HttpServletRequest httpRequest) {
        User user = authenticate(req.getEmail(), req.getPassword());
        user.updateStreak();
        userRepository.save(user);

        // Cria contexto de segurança e salva na sessão
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(
                        user, null, user.getAuthorities());

        SecurityContext context = securityContextStrategy.createEmptyContext();
        context.setAuthentication(authToken);
        securityContextStrategy.setContext(context);

        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                context);

        log.info("Login web (sessão) — usuário: {}", user.getEmail());
        return toUserSummary(user);
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

    public void logoutSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        log.info("Logout web — sessão invalidada");
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

        String token = UUID.randomUUID().toString();
        RefreshToken rt = RefreshToken.builder()
                .token(token)
                .user(user)
                .expiresAt(Instant.now().plusMillis(604_800_000L)) // 7 dias
                .build();
        refreshTokenRepository.save(rt);
        return token;
    }

    public UserSummaryDto toUserSummary(User user) {
        return UserSummaryDto.builder()
                .id(user.getId())
                .username(user.getUsername())
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
