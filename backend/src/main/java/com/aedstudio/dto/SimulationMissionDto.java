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
public class SimulationMissionDto {
    private String id;
    private String topicId;
    private String simulatorType;
    private String title;
    private String instructions;
    private List<String> requiredActions;
    private String successCriteria;
    private Integer xp;
    private Boolean completed;
}
