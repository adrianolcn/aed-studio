package com.aedstudio.controller;

import com.aedstudio.dto.ExerciseDto;
import com.aedstudio.dto.ExerciseSubmitRequest;
import com.aedstudio.dto.ExerciseSubmitResponse;
import com.aedstudio.dto.GeneratedExerciseDto;
import com.aedstudio.dto.GeneratedExerciseRequest;
import com.aedstudio.dto.GeneratedExerciseSubmitRequest;
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
            @Valid @RequestBody ExerciseSubmitRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(progressService.submitExercise(user, request));
    }

    @PostMapping("/generated-exercises")
    public ResponseEntity<GeneratedExerciseDto> generate(
            @Valid @RequestBody GeneratedExerciseRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(progressService.generateExercise(user, request));
    }

    @GetMapping("/generated-exercises/history")
    public ResponseEntity<List<GeneratedExerciseDto>> generatedHistory(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(progressService.generatedExerciseHistory(user));
    }

    @PostMapping("/generated-exercises/{generatedExerciseId}/attempts")
    public ResponseEntity<ExerciseSubmitResponse> submitGenerated(
            @PathVariable String generatedExerciseId,
            @Valid @RequestBody GeneratedExerciseSubmitRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(progressService.submitGeneratedExercise(user, generatedExerciseId, request));
    }
}
