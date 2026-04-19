package com.aedstudio.controller;

import com.aedstudio.dto.SimulationEventRequest;
import com.aedstudio.dto.SimulationEventResponse;
import com.aedstudio.dto.SimulationMissionDto;
import com.aedstudio.dto.SimulationMissionSubmitRequest;
import com.aedstudio.dto.SimulationMissionSubmitResponse;
import com.aedstudio.model.User;
import com.aedstudio.service.ProgressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/simulations")
@RequiredArgsConstructor
public class SimulationController {

    private final ProgressService progressService;

    @PostMapping("/events")
    public ResponseEntity<SimulationEventResponse> record(
            @Valid @RequestBody SimulationEventRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(progressService.recordSimulationEvent(user, request));
    }

    @GetMapping("/topics/{topicId}/missions")
    public ResponseEntity<List<SimulationMissionDto>> missions(
            @PathVariable String topicId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(progressService.simulationMissions(user, topicId));
    }

    @PostMapping("/missions/{missionId}/submit")
    public ResponseEntity<SimulationMissionSubmitResponse> submitMission(
            @PathVariable String missionId,
            @Valid @RequestBody SimulationMissionSubmitRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(progressService.submitSimulationMission(user, missionId, request));
    }
}
