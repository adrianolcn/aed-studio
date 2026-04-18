package com.aedstudio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationMissionSubmitResponse {
    private String missionId;
    private Boolean completed;
    private String feedback;
    private Integer awarded;
    private ProgressResponse progress;
}
