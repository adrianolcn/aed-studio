package com.aedstudio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LevelDto {
    private Integer level;
    private Integer currentXp;
    private Integer xpForCurrentLevel;
    private Integer xpForNextLevel;
    private Integer progressPercent;
}
