package com.aedstudio.controller;

import com.aedstudio.dto.*;
import com.aedstudio.model.User;
import com.aedstudio.service.ProgressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/simulations")
@RequiredArgsConstructor
public class SimulationController {

    private final ProgressService progressService;

    @PostMapping("/events")
    public ResponseEntity<SimulationEventResponse> record(
            @Valid @RequestBody SimulationEventRequest req,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(progressService.recordSimulationEvent(user, req));
    }

    @GetMapping("/topics/{topicId}/missions")
    public ResponseEntity<java.util.List<SimulationMissionDto>> missions(
            @PathVariable String topicId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(progressService.simulationMissions(user, topicId));
    }

    @PostMapping("/missions/{missionId}/submit")
    public ResponseEntity<SimulationMissionSubmitResponse> submitMission(
            @PathVariable String missionId,
            @Valid @RequestBody SimulationMissionSubmitRequest req,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(progressService.submitSimulationMission(user, missionId, req));
    }
}
