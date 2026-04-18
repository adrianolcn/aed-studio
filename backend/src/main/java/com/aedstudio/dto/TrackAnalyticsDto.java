package com.aedstudio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackAnalyticsDto {
    private String trackId;
    private String name;
    private Integer totalTopics;
    private Integer completedTopics;
    private Integer progressPercent;
    private Integer attempts;
    private Integer accuracyPercent;
    private String insight;
}
