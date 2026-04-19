package com.aedstudio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeChallengeDto {
    private String id;
    private String topicId;
    private String title;
    private String prompt;
    private String functionName;
    private String signature;
    private String returnType;
    private List<String> examples;
    private String starterCode;
    private List<String> expectedConcepts;
    private String conceptualHint;
    private String structuralHint;
    private String pseudoSkeleton;
    private Integer difficulty;
    private Integer xp;
}
