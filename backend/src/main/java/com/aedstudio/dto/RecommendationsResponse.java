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
public class RecommendationsResponse {
    private RecommendationDto primary;
    private List<RecommendationDto> nextSteps;
    private List<RecommendationDto> review;
    private List<RecommendationDto> trailFocus;
}
