package com.aedstudio.controller;

import com.aedstudio.dto.*;
import com.aedstudio.model.User;
import com.aedstudio.service.ProgressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/learning")
@RequiredArgsConstructor
public class LearningController {

    private final ProgressService progressService;

    @GetMapping("/topics/{topicId}/exercises")
    public ResponseEntity<List<ExerciseDto>> exercises(
            @PathVariable String topicId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(progressService.getExercises(user, topicId));
    }

    @PostMapping("/attempts")
    public ResponseEntity<ExerciseSubmitResponse> submit(
            @Valid @RequestBody ExerciseSubmitRequest req,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(progressService.submitExercise(user, req));
    }

    @PostMapping("/generated-exercises")
    public ResponseEntity<GeneratedExerciseDto> generate(
            @Valid @RequestBody GeneratedExerciseRequest req,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(progressService.generateExercise(user, req));
    }

    @GetMapping("/generated-exercises/history")
    public ResponseEntity<List<GeneratedExerciseDto>> generatedHistory(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(progressService.generatedExerciseHistory(user));
    }

    @PostMapping("/generated-exercises/{generatedExerciseId}/attempts")
    public ResponseEntity<ExerciseSubmitResponse> submitGenerated(
            @PathVariable String generatedExerciseId,
            @Valid @RequestBody GeneratedExerciseSubmitRequest req,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(progressService.submitGeneratedExercise(user, generatedExerciseId, req));
    }
}
