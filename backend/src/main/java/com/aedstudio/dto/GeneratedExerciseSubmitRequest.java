package com.aedstudio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedExerciseSubmitRequest {

    @NotBlank(message = "answer é obrigatório")
    private String answer;

    @PositiveOrZero(message = "timeSpentSeconds não pode ser negativo")
    private Integer timeSpentSeconds = 0;
}
