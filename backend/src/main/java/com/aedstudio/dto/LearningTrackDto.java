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
public class LearningTrackDto {
    private String id;
    private String name;
    private String description;
    private Integer orderIndex;
    private Integer totalTopics;
    private Integer completedTopics;
    private Integer availableTopics;
    private Integer progressPercent;
    private List<TopicStatusDto> topics;
}
