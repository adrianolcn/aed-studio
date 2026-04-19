package com.aedstudio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeRunResponse {
    private String challengeId;
    private Boolean accepted;
    private String status;
    private Long submissionId;
    private List<String> passedChecks;
    private List<String> failedChecks;
    private Integer passedCount;
    private Integer totalChecks;
    private Long executionTimeMs;
    private String feedback;
    private String hint;
    private Integer awarded;
    private ProgressResponse progress;
}
