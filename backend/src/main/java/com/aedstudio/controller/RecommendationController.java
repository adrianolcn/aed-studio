package com.aedstudio.controller;

import com.aedstudio.dto.RecommendationsResponse;
import com.aedstudio.model.User;
import com.aedstudio.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping
    public ResponseEntity<RecommendationsResponse> recommendations(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(recommendationService.recommendations(user));
    }
}
