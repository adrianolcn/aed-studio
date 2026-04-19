package com.aedstudio.controller;

import com.aedstudio.dto.ProgressResponse;
import com.aedstudio.dto.TopicVisitRequest;
import com.aedstudio.dto.XpAwardRequest;
import com.aedstudio.dto.XpAwardResponse;
import com.aedstudio.model.User;
import com.aedstudio.service.ProgressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;

    @GetMapping
    public ResponseEntity<ProgressResponse> getProgress(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(progressService.getProgress(user));
    }

    @PostMapping("/visit")
    public ResponseEntity<ProgressResponse> visit(
            @Valid @RequestBody TopicVisitRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(progressService.recordVisit(user, request.getTopicId()));
    }

    @PostMapping("/xp")
    public ResponseEntity<XpAwardResponse> awardXp(
            @Valid @RequestBody XpAwardRequest request,
            @AuthenticationPrincipal User user) {
        int awarded = progressService.awardXp(user, request);
        return ResponseEntity.ok(XpAwardResponse.builder()
                .awarded(awarded)
                .eventKey(request.getReason())
                .alreadyEarned(awarded == 0)
                .progress(progressService.getProgress(user))
                .build());
    }
}
