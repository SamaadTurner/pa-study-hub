package com.pastudyhub.ai.controller;

import com.pastudyhub.ai.dto.*;
import com.pastudyhub.ai.service.AiAssistantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Tag(name = "AI Assistant", description = "Claude-powered PA study features")
public class AiAssistantController {

    private final AiAssistantService aiService;

    private UUID userId(String header) {
        return UUID.fromString(header);
    }

    // ---- POST /ai/flashcards -----------------------------------------------

    @PostMapping("/flashcards")
    @Operation(summary = "Generate PANCE flashcard pairs for a topic")
    public ResponseEntity<GenerateFlashcardsResponse> generateFlashcards(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody @Valid GenerateFlashcardsRequest request) {

        return ResponseEntity.ok(aiService.generateFlashcards(userId(userIdHeader), request));
    }

    // ---- POST /ai/explain --------------------------------------------------

    @PostMapping("/explain")
    @Operation(summary = "Explain why a PANCE answer was wrong")
    public ResponseEntity<ExplainAnswerResponse> explainWrongAnswer(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody @Valid ExplainAnswerRequest request) {

        return ResponseEntity.ok(aiService.explainWrongAnswer(userId(userIdHeader), request));
    }

    // ---- POST /ai/study-plan -----------------------------------------------

    @PostMapping("/study-plan")
    @Operation(summary = "Generate a personalized PANCE study plan")
    public ResponseEntity<StudyPlanResponse> generateStudyPlan(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody @Valid StudyPlanRequest request) {

        return ResponseEntity.ok(aiService.generateStudyPlan(userId(userIdHeader), request));
    }

    // ---- POST /ai/chat -----------------------------------------------------

    @PostMapping("/chat")
    @Operation(summary = "Chat with the PA tutor (conversational)")
    public ResponseEntity<ChatResponse> chat(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody @Valid ChatRequest request) {

        return ResponseEntity.ok(aiService.chat(userId(userIdHeader), request));
    }
}
