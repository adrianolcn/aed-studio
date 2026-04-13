package com.aedstudio.config;

import com.aedstudio.model.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Serviço responsável por toda a lógica JWT.
 *
 * Access token: curta duração (padrão 24h), contém email + role.
 * Refresh token: longa duração (7 dias), armazenado em banco.
 *
 * Analogia para o tópico de AED:
 *   O JWT é uma Tabela Hash assinada — a chave secreta é o HMAC que
 *   garante integridade. Qualquer alteração no payload invalida a assinatura.
 */
@Slf4j
@Component
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration-ms}")
    private long accessTokenExpirationMs;

    // ── Token generation ────────────────────────────────────────────

    /**
     * Gera um access token para o usuário.
     * Claims extras: role do usuário (para RBAC no front-end).
     */
    public String generateAccessToken(User user) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", user.getRole().name());
        extraClaims.put("username", user.getUsername());
        extraClaims.put("fullName", user.getFullName());
        return buildToken(extraClaims, user.getEmail(), accessTokenExpirationMs);
    }

    /**
     * Gera um token de refresh — sem claims extras, só o subject.
     * O token em si não carrega dados — a validação real é feita consultando o banco.
     */
    public String generateRefreshToken(User user) {
        return buildToken(new HashMap<>(), user.getEmail(), 604800000L); // 7 dias
    }

    private String buildToken(Map<String, Object> extraClaims, String subject, long expirationMs) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    // ── Token validation ───────────────────────────────────────────

    public boolean isTokenValid(String token, String email) {
        try {
            final String subject = extractEmail(token);
            return subject.equals(email) && !isTokenExpired(token);
        } catch (JwtException e) {
            log.warn("JWT inválido: {}", e.getMessage());
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // ── Claims extraction ──────────────────────────────────────────

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public long extractExpiresIn(String token) {
        long expiresAt = extractExpiration(token).getTime();
        return Math.max(0, (expiresAt - System.currentTimeMillis()) / 1000);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        // A chave precisa ter ao menos 256 bits para HS256.
        // Se a chave configurada for menor, padding com Base64.
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secretKey);
        } catch (Exception e) {
            // Fallback: usa os bytes diretos (para chaves em texto plano no dev)
            keyBytes = secretKey.getBytes();
        }
        // Garante 256 bits mínimo
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
