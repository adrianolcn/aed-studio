package com.aedstudio.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserSummaryDto {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String role;
    private Integer xp;
    private Integer streakDays;
    private Integer topicsCompleted;
    private LocalDate lastStudyDate;
}
