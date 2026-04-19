package com.aedstudio.controller;

import com.aedstudio.dto.CodeChallengeDto;
import com.aedstudio.dto.CodeRunRequest;
import com.aedstudio.dto.CodeRunResponse;
import com.aedstudio.dto.CodeSubmissionDto;
import com.aedstudio.model.User;
import com.aedstudio.service.CodeExecutionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/code")
@RequiredArgsConstructor
public class CodeController {

    private final CodeExecutionService codeExecutionService;

    @GetMapping("/topics/{topicId}/challenges")
    public ResponseEntity<List<CodeChallengeDto>> challenges(
            @PathVariable String topicId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(codeExecutionService.challenges(user, topicId));
    }

    @PostMapping("/run")
    public ResponseEntity<CodeRunResponse> run(
            @Valid @RequestBody CodeRunRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(codeExecutionService.run(user, request));
    }

    @GetMapping("/submissions")
    public ResponseEntity<List<CodeSubmissionDto>> submissions(
            @RequestParam(required = false) String topicId,
            @RequestParam(required = false) String exerciseId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(codeExecutionService.submissions(user, topicId, exerciseId));
    }

    @GetMapping("/submissions/latest")
    public ResponseEntity<CodeSubmissionDto> latestSubmission(
            @RequestParam String exerciseId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(codeExecutionService.latestSubmission(user, exerciseId));
    }

    @GetMapping("/submissions/best")
    public ResponseEntity<CodeSubmissionDto> bestSubmission(
            @RequestParam String exerciseId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(codeExecutionService.bestSubmissionDto(user, exerciseId));
    }
}
