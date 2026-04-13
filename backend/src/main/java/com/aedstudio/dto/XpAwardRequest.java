package com.aedstudio.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class XpAwardRequest {

    @NotBlank(message = "topicId é obrigatório")
    private String topicId;

    @NotBlank(message = "reason é obrigatório")
    private String reason;

    @Min(value = 1, message = "XP deve ser positivo")
    @Max(value = 500, message = "XP por evento não pode exceder 500")
    private Integer amount;
}
