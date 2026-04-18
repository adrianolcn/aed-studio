package com.aedstudio.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "exercise_attempts",
       indexes = {
           @Index(name = "idx_exercise_attempts_user", columnList = "user_id"),
           @Index(name = "idx_exercise_attempts_user_topic", columnList = "user_id,topic_id")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "topic_id", nullable = false, length = 50)
    private String topicId;

    @Column(name = "exercise_id", nullable = false, length = 80)
    private String exerciseId;

    @Column(nullable = false, length = 24)
    private String type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String answer;

    @Column(nullable = false)
    private Boolean correct;

    @Column(name = "time_spent_seconds")
    private Integer timeSpentSeconds;

    @Column(name = "attempted_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime attemptedAt = LocalDateTime.now();
}
