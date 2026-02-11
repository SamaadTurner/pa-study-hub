package com.pastudyhub.exam.controller;

import com.pastudyhub.exam.dto.*;
import com.pastudyhub.exam.service.ExamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for PANCE-style practice exam operations.
 * userId is extracted from the X-User-Id header forwarded by the API Gateway.
 */
@RestController
@RequestMapping("/api/v1/exams")
@RequiredArgsConstructor
@Tag(name = "Exams", description = "PANCE-style timed practice exams")
@SecurityRequirement(name = "bearerAuth")
public class ExamController {

    private final ExamService examService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Start a new exam session")
    public ExamSessionResponse startExam(
            @Valid @RequestBody StartExamRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return examService.startExam(request, userId);
    }

    @GetMapping("/{sessionId}")
    @Operation(summary = "Get current state of an exam session")
    public ExamSessionResponse getExamSession(
            @PathVariable UUID sessionId,
            @RequestHeader("X-User-Id") UUID userId) {
        return examService.getExamSession(sessionId, userId);
    }

    @PostMapping("/{sessionId}/questions/{questionId}/answer")
    @Operation(summary = "Submit an answer for a question")
    public ExamSessionResponse submitAnswer(
            @PathVariable UUID sessionId,
            @PathVariable UUID questionId,
            @Valid @RequestBody SubmitAnswerRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return examService.submitAnswer(sessionId, questionId, request, userId);
    }

    @PostMapping("/{sessionId}/complete")
    @Operation(summary = "Complete the exam and receive scored results")
    public ExamResultResponse completeExam(
            @PathVariable UUID sessionId,
            @RequestHeader("X-User-Id") UUID userId) {
        return examService.completeExam(sessionId, userId);
    }

    @PostMapping("/{sessionId}/abandon")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Abandon (cancel) an in-progress exam")
    public void abandonExam(
            @PathVariable UUID sessionId,
            @RequestHeader("X-User-Id") UUID userId) {
        examService.abandonExam(sessionId, userId);
    }

    @GetMapping("/{sessionId}/result")
    @Operation(summary = "Retrieve results of a completed exam")
    public ExamResultResponse getExamResult(
            @PathVariable UUID sessionId,
            @RequestHeader("X-User-Id") UUID userId) {
        return examService.getExamResult(sessionId, userId);
    }

    @GetMapping("/history")
    @Operation(summary = "Get paginated exam history for the current user")
    public Page<ExamHistorySummary> getExamHistory(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return examService.getExamHistory(userId, page, size);
    }
}
