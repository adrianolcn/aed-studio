package com.aedstudio.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedExerciseRequest {
    @NotBlank(message = "topicId é obrigatório")
    private String topicId;
    private Integer difficulty;
}
