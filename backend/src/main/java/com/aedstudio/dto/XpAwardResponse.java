package com.aedstudio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XpAwardResponse {
    private Integer awarded;
    private String eventKey;
    private Boolean alreadyEarned;
    private ProgressResponse progress;
}
