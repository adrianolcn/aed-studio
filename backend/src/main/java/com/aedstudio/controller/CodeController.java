package com.aedstudio.controller;

import com.aedstudio.dto.CodeChallengeDto;
import com.aedstudio.dto.CodeRunRequest;
import com.aedstudio.dto.CodeRunResponse;
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
}
