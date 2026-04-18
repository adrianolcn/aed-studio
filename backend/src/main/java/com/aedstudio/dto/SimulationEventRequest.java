package com.aedstudio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimulationEventRequest {

    @NotBlank(message = "topicId é obrigatório")
    private String topicId;

    @NotBlank(message = "simulatorType é obrigatório")
    private String simulatorType;

    @NotBlank(message = "action é obrigatória")
    private String action;

    private String milestone;

    @Size(max = 4000, message = "stateSnapshot excede o tamanho máximo")
    private String stateSnapshot;
}
