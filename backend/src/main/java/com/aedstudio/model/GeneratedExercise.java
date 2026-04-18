package com.aedstudio.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "generated_exercises",
       uniqueConstraints = @UniqueConstraint(columnNames = "generated_id"),
       indexes = {
           @Index(name = "idx_generated_exercises_user", columnList = "user_id"),
           @Index(name = "idx_generated_exercises_user_topic", columnList = "user_id,topic_id")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "generated_id", nullable = false, length = 90)
    private String generatedId;

    @Column(name = "topic_id", nullable = false, length = 50)
    private String topicId;

    @Column(nullable = false, length = 24)
    private String type;

    @Column(nullable = false)
    @Builder.Default
    private Integer difficulty = 1;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String prompt;

    @Column(columnDefinition = "TEXT")
    private String options;

    @Column(name = "correct_answer", nullable = false, columnDefinition = "TEXT")
    private String correctAnswer;

    @Column(name = "correct_feedback", nullable = false, columnDefinition = "TEXT")
    private String correctFeedback;

    @Column(name = "wrong_feedback", nullable = false, columnDefinition = "TEXT")
    private String wrongFeedback;

    @Column(nullable = false)
    @Builder.Default
    private Boolean answered = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
