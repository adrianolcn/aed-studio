package com.aedstudio.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

    @Min(value = 1, message = "difficulty mínima é 1")
    @Max(value = 3, message = "difficulty máxima é 3")
    private Integer difficulty = 1;
}
