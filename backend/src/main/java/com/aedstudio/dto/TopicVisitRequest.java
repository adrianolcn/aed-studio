package com.aedstudio.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopicVisitRequest {

    @NotBlank(message = "topicId é obrigatório")
    private String topicId;
}
