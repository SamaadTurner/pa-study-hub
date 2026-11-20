package com.pastudyhub.exam.controller;

import com.pastudyhub.exam.dto.QuestionCountsResponse;
import com.pastudyhub.exam.dto.QuestionRequest;
import com.pastudyhub.exam.dto.ReportQuestionRequest;
import com.pastudyhub.exam.dto.SharedQuestionResponse;
import com.pastudyhub.exam.dto.WrittenQuestionResponse;
import com.pastudyhub.exam.model.QuestionCategory;
import com.pastudyhub.exam.service.QuestionAuthoringService;
import com.pastudyhub.exam.service.SharedQuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** Practice exam questions written by the current user. */
@RestController
@RequestMapping("/api/v1/questions")
@RequiredArgsConstructor
@Tag(name = "Questions", description = "Write your own practice exam questions")
@SecurityRequirement(name = "bearerAuth")
public class QuestionController {

    private final QuestionAuthoringService questionAuthoringService;
    private final SharedQuestionService sharedQuestionService;

    @GetMapping("/mine")
    @Operation(summary = "List the questions you wrote, last changed first")
    public Page<WrittenQuestionResponse> getMyQuestions(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return questionAuthoringService.getMyQuestions(userId, page, size);
    }

    @GetMapping("/{questionId}")
    @Operation(summary = "Get one of your questions, right answer included")
    public WrittenQuestionResponse getMyQuestion(
            @PathVariable UUID questionId,
            @RequestHeader("X-User-Id") UUID userId) {
        return questionAuthoringService.getMyQuestion(questionId, userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Write a new question")
    public WrittenQuestionResponse createQuestion(
            @Valid @RequestBody QuestionRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return questionAuthoringService.createQuestion(request, userId);
    }

    @PutMapping("/{questionId}")
    @Operation(summary = "Edit one of your questions (a question already used in an exam gets a new id)")
    public WrittenQuestionResponse updateQuestion(
            @PathVariable UUID questionId,
            @Valid @RequestBody QuestionRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return questionAuthoringService.updateQuestion(questionId, request, userId);
    }

    @DeleteMapping("/{questionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete one of your questions (past exam results keep it)")
    public void deleteQuestion(
            @PathVariable UUID questionId,
            @RequestHeader("X-User-Id") UUID userId) {
        questionAuthoringService.deleteQuestion(questionId, userId);
    }

    @PostMapping("/{questionId}/reports")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Report a question another user shared (after it was in one of your finished exams)")
    public void reportQuestion(
            @PathVariable UUID questionId,
            @Valid @RequestBody ReportQuestionRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        sharedQuestionService.report(questionId, userId, request);
    }

    @GetMapping("/shared")
    @Operation(summary = "Browse questions other users shared, optionally one category and a text search")
    public Page<SharedQuestionResponse> browseShared(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(required = false) QuestionCategory category,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return sharedQuestionService.browse(userId, category, search, page, size);
    }

    @GetMapping("/counts")
    @Operation(summary = "How many questions your exams can pick from, per category")
    public QuestionCountsResponse countExamPool(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(defaultValue = "false") boolean includeShared) {
        return sharedQuestionService.countExamPool(userId, includeShared);
    }
}
