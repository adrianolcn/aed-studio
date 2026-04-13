package com.aedstudio.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Refresh token armazenado em banco — permite revogar sessões individuais.
 *
 * Fluxo:
 *  1. Login → emite accessToken (15min) + refreshToken (7 dias)
 *  2. accessToken expirado → cliente chama POST /auth/refresh com o refreshToken
 *  3. Servidor valida, gera novo par, invalida o refresh token antigo (rotation)
 *  4. Logout → deleta o refreshToken do banco
 */
@Entity
@Table(name = "refresh_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 512)
    private String token;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Instant expiresAt;

    // ── Helper ────────────────────────────────────────

    public boolean isExpired() {
        return expiresAt.isBefore(Instant.now());
    }
}
