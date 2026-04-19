package com.aedstudio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeSubmissionDto {
    private Long id;
    private String topicId;
    private String exerciseId;
    private String status;
    private Integer totalTests;
    private Integer passedTests;
    private Long executionTimeMs;
    private List<String> passedChecks;
    private List<String> failedChecks;
    private String feedback;
    private LocalDateTime createdAt;
    private Boolean best;
}
