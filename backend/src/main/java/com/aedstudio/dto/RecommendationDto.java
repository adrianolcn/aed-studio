package com.aedstudio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationDto {
    private String topicId;
    private String title;
    private String trackId;
    private String category;
    private String action;
    private String reason;
    private Integer priority;
    private Double confidence;
}
