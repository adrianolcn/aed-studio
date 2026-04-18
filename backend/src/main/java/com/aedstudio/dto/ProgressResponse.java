package com.aedstudio.dto;

import lombok.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressResponse {
    private Integer totalXp;
    private Integer streakDays;
    private Integer topicsVisited;
    private Integer topicsCompleted;
    private Integer totalTopics;
    private Integer progressPercent;
    private LocalDate lastStudyDate;
    private Map<String, String> topics;
    private List<String> earnedEventKeys;
    private LevelDto level;
    private List<BadgeDto> badges;
    private List<LearningTrackDto> tracks;
    private Map<String, TopicStatusDto> topicStates;
    private List<TopicStatusDto> nextRecommendedTopics;
}
