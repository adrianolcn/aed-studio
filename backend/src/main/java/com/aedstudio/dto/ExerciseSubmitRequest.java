package com.aedstudio.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseSubmitRequest {
    @NotBlank(message = "exerciseId é obrigatório")
    private String exerciseId;

    @NotBlank(message = "answer é obrigatório")
    private String answer;

    private Integer timeSpentSeconds;
}
