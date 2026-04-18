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
    private List<String> passedChecks;
    private List<String> failedChecks;
    private String feedback;
    private Integer awarded;
    private ProgressResponse progress;
}
