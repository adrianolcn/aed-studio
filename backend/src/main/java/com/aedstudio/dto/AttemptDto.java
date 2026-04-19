package com.aedstudio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttemptDto {
    private Long id;
    private String topicId;
    private String exerciseId;
    private String type;
    private Boolean correct;
    private Integer timeSpentSeconds;
    private LocalDateTime attemptedAt;
}
