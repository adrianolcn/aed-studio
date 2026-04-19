package com.aedstudio.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodeRunRequest {
    @NotBlank(message = "challengeId é obrigatório")
    private String challengeId;

    @NotBlank(message = "code é obrigatório")
    private String code;
}
