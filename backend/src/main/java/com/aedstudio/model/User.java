package com.aedstudio.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Entidade de usuário.
 *
 * Implementa UserDetails para integração direta com Spring Security —
 * assim não precisamos de um adaptador separado.
 *
 * Campos de gamificação (xp, streak, lastStudyDate) vivem aqui
 * e são sincronizados com o front-end via API de progresso.
 */
@Entity
@Table(name = "users",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = "email"),
           @UniqueConstraint(columnNames = "username")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Identificação ──────────────────────────────────
    @NotBlank
    @Size(min = 2, max = 50)
    @Column(nullable = false, length = 50)
    private String username;

    @NotBlank
    @Email
    @Column(nullable = false, length = 100)
    private String email;

    @NotBlank
    @Column(nullable = false)
    private String password;  // sempre armazenado como BCrypt hash

    @NotBlank
    @Size(min = 2, max = 100)
    @Column(nullable = false, length = 100)
    private String fullName;

    // ── Papel / autorização ────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.STUDENT;

    // ── Gamificação ────────────────────────────────────
    @Column(nullable = false)
    @Builder.Default
    private Integer xp = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer streakDays = 0;

    @Column
    private LocalDate lastStudyDate;

    @Column(nullable = false)
    @Builder.Default
    private Integer topicsCompleted = 0;

    // ── Metadados da conta ─────────────────────────────
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean accountNonLocked = true;

    // ── UserDetails implementation ─────────────────────

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;  // usamos email como identificador principal
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    // ── Helpers ────────────────────────────────────────

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Atualiza a streak diária.
     * Regras:
     *  - Mesmo dia → não altera (já contou)
     *  - Dia seguinte → incrementa
     *  - Gap maior que 1 dia → reseta para 1
     */
    public void updateStreak() {
        LocalDate today = LocalDate.now();
        if (lastStudyDate == null) {
            streakDays = 1;
        } else if (lastStudyDate.equals(today)) {
            return;  // já foi registrado hoje
        } else if (lastStudyDate.equals(today.minusDays(1))) {
            streakDays++;
        } else {
            streakDays = 1;  // quebrou a streak
        }
        lastStudyDate = today;
    }

    /**
     * Retorna o nome exibido (preferência: fullName, fallback: username).
     */
    public String getDisplayName() {
        return (fullName != null && !fullName.isBlank()) ? fullName : username;
    }

    public String getProfileUsername() {
        return username;
    }
}
