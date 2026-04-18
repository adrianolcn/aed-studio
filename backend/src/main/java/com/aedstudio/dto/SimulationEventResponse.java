package com.aedstudio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationEventResponse {
    private Long eventId;
    private String eventKey;
    private Integer awarded;
    private Boolean alreadyEarned;
    private ProgressResponse progress;
}
