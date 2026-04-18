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
public class TopicStatusDto {
    private String id;
    private String title;
    private String description;
    private String trackId;
    private String path;
    private Integer orderIndex;
    private List<String> prerequisites;
    private String state;
    private Integer exerciseCount;
    private Integer requiredCorrect;
    private Integer correctExercises;
    private Integer bestScorePercent;
}
