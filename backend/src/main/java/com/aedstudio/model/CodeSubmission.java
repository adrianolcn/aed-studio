package com.aedstudio.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "code_submissions",
       indexes = {
           @Index(name = "idx_code_submissions_user", columnList = "user_id"),
           @Index(name = "idx_code_submissions_user_topic", columnList = "user_id,topic_id"),
           @Index(name = "idx_code_submissions_user_exercise", columnList = "user_id,exercise_id")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "topic_id", nullable = false, length = 50)
    private String topicId;

    @Column(name = "exercise_id", nullable = false, length = 90)
    private String exerciseId;

    @Column(name = "source_code", nullable = false, columnDefinition = "TEXT")
    private String sourceCode;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "total_tests", nullable = false)
    @Builder.Default
    private Integer totalTests = 0;

    @Column(name = "passed_tests", nullable = false)
    @Builder.Default
    private Integer passedTests = 0;

    @Column(name = "execution_time_ms", nullable = false)
    @Builder.Default
    private Long executionTimeMs = 0L;

    @Column(name = "passed_checks", columnDefinition = "TEXT")
    private String passedChecks;

    @Column(name = "failed_checks", columnDefinition = "TEXT")
    private String failedChecks;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
