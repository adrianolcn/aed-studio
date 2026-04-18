package com.aedstudio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseSubmitResponse {
    private String exerciseId;
    private String topicId;
    private Boolean correct;
    private String feedback;
    private Integer awarded;
    private Boolean topicCompleted;
    private ProgressResponse progress;
}
