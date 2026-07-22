package com.pastudyhub.progress.controller;

import com.pastudyhub.progress.dto.*;
import com.pastudyhub.progress.service.ProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for study progress analytics and goal tracking.
 * Also exposes the internal /log endpoint used by flashcard-service and exam-service.
 */
@RestController
@RequestMapping("/api/v1/progress")
@RequiredArgsConstructor
@Tag(name = "Study Progress", description = "Streaks, analytics, and daily goal tracking")
@SecurityRequirement(name = "bearerAuth")
public class ProgressController {

    private final ProgressService progressService;

    /**
     * Internal endpoint called by other services to log study activity.
     * Not exposed through the API Gateway to end users.
     */
    @PostMapping("/log")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Internal: log a study activity event (called by other services)")
    public void logActivity(@Valid @RequestBody LogActivityRequest request) {
        progressService.logActivity(request);
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get the full progress dashboard for the current user")
    public ProgressDashboardResponse getDashboard(
            @RequestHeader("X-User-Id") UUID userId) {
        return progressService.getDashboard(userId);
    }

    @GetMapping("/goal")
    @Operation(summary = "Get today's goal progress")
    public GoalProgressResponse getGoalProgress(
            @RequestHeader("X-User-Id") UUID userId) {
        return progressService.getGoalProgress(userId);
    }

    @PutMapping("/goal")
    @Operation(summary = "Update daily study goal settings")
    public GoalProgressResponse updateGoal(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody UpdateGoalRequest request) {
        return progressService.updateGoal(userId, request);
    }
}
