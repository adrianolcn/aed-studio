package com.aedstudio.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Registra o progresso de um usuário em um tópico específico.
 *
 * Cada linha representa (usuário, tópico) → estado.
 * O estado é sincronizado do front-end a cada navegação e
 * conclusão de quiz/desafio.
 */
@Entity
@Table(name = "topic_progress",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"user_id", "topic_id"})
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Relacionamento ─────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // ID do tópico como string (ex: "arrays", "bst", "dc")
    @Column(name = "topic_id", nullable = false, length = 50)
    private String topicId;

    // ── Estado ────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TopicState state = TopicState.VISITED;

    // XP ganho neste tópico (0 se só visitou, +5 quiz, +50 code challenge)
    @Column(nullable = false)
    @Builder.Default
    private Integer xpEarned = 0;

    // ── Timestamps ────────────────────────────────────
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime firstVisitedAt = LocalDateTime.now();

    @Column
    private LocalDateTime completedAt;

    // ── Helpers ───────────────────────────────────────

    public void markCompleted() {
        if (this.state != TopicState.COMPLETED) {
            this.state = TopicState.COMPLETED;
            this.completedAt = LocalDateTime.now();
        }
    }

    public void addXp(int amount) {
        this.xpEarned = this.xpEarned + amount;
    }

    public boolean isCompleted() {
        return state == TopicState.COMPLETED;
    }
}
