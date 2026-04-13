package com.aedstudio.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Registra cada evento de XP ganho por um usuário.
 *
 * A constraint UNIQUE(user_id, event_key) garante idempotência:
 * o mesmo quiz nunca concede XP duas vezes, mesmo se o front-end
 * tentar enviar o evento repetidamente.
 *
 * event_key examples:
 *   "visit_arrays"        → 5 XP por visitar tópico
 *   "quiz_tad-q1"         → 10 XP por quiz correto
 *   "code_tad"            → 50 XP por desafio de código
 */
@Entity
@Table(name = "xp_events",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "event_key"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XpEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "event_key", nullable = false, length = 100)
    private String eventKey;

    @Column(nullable = false)
    private Integer amount;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime earnedAt = LocalDateTime.now();
}
