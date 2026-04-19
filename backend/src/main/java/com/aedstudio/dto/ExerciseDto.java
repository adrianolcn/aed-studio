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
public class ExerciseDto {
    private String id;
    private String topicId;
    private String type;
    private String prompt;
    private List<String> options;
    private Boolean required;
    private Integer xp;
}
