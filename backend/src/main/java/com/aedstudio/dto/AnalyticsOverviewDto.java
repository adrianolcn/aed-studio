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
public class AnalyticsOverviewDto {
    private Integer totalAttempts;
    private Integer correctAttempts;
    private Integer overallAccuracyPercent;
    private Integer simulatorInteractions;
    private Integer codeSubmissions;
    private Integer codeSuccessPercent;
    private Integer codeErrorPercent;
    private Integer totalXp;
    private Integer streakDays;
    private String consistencyLabel;
    private List<TopicAnalyticsDto> strongestTopics;
    private List<TopicAnalyticsDto> attentionTopics;
    private List<TopicAnalyticsDto> improvingTopics;
    private List<TopicAnalyticsDto> regressingTopics;
    private List<String> practicalSuggestions;
}
