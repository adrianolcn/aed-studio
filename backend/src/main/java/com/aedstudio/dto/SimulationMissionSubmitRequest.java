package com.aedstudio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimulationMissionSubmitRequest {

    @NotBlank(message = "stateSnapshot é obrigatório")
    @Size(max = 4000, message = "stateSnapshot excede o tamanho máximo")
    private String stateSnapshot;
}
