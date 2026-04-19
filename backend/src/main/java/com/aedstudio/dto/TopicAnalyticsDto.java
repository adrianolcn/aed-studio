package com.aedstudio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicAnalyticsDto {
    private String topicId;
    private String title;
    private String trackId;
    private String state;
    private Integer attempts;
    private Integer correctAttempts;
    private Integer accuracyPercent;
    private Integer simulatorInteractions;
    private Integer codeSubmissions;
    private Integer codeSuccessPercent;
    private Integer codeErrorPercent;
    private LocalDateTime lastAttemptAt;
    private String trendLabel;
    private String riskLevel;
    private Boolean abandoned;
    private Boolean improving;
    private Boolean regressing;
    private String insight;
}
