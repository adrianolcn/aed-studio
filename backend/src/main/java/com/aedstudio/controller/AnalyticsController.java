package com.aedstudio.controller;

import com.aedstudio.dto.AnalyticsOverviewDto;
import com.aedstudio.dto.TopicAnalyticsDto;
import com.aedstudio.dto.TrackAnalyticsDto;
import com.aedstudio.dto.XpHistoryPointDto;
import com.aedstudio.model.User;
import com.aedstudio.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/overview")
    public ResponseEntity<AnalyticsOverviewDto> overview(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(analyticsService.overview(user));
    }

    @GetMapping("/topics")
    public ResponseEntity<List<TopicAnalyticsDto>> topics(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(analyticsService.topics(user));
    }

    @GetMapping("/trails")
    public ResponseEntity<List<TrackAnalyticsDto>> trails(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(analyticsService.trails(user));
    }

    @GetMapping("/xp-history")
    public ResponseEntity<List<XpHistoryPointDto>> xpHistory(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(analyticsService.xpHistory(user));
    }
}
