package com.pastudyhub.progress.controller;

import com.pastudyhub.progress.dto.*;
import com.pastudyhub.progress.service.ProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for study progress analytics and goal tracking.
 */
@RestController
@RequestMapping("/api/v1/progress")
@RequiredArgsConstructor
@Tag(name = "Study Progress", description = "Streaks, analytics, and daily goal tracking")
@SecurityRequirement(name = "bearerAuth")
public class ProgressController {

    private final ProgressService progressService;

    // There is no endpoint for logging activity. It is recorded by the app itself when
    // a user reviews a card, so a client can't post made-up activity for another user.

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
