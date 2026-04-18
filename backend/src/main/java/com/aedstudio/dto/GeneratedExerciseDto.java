package com.aedstudio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedExerciseDto {
    private String id;
    private String topicId;
    private String type;
    private Integer difficulty;
    private String prompt;
    private List<String> options;
    private Boolean answered;
    private LocalDateTime createdAt;
}
