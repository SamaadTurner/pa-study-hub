package com.pastudyhub.ai;

import com.pastudyhub.ai.dto.GenerateFlashcardsRequest;
import com.pastudyhub.ai.dto.GeneratedCard;
import com.pastudyhub.ai.usage.AiUsageLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * The optional AI endpoints. The userId comes from the X-User-Id header that
 * UserIdHeaderFilter fills in from the login token, the same as in the other controllers.
 */
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Tag(name = "AI", description = "Optional AI help. Off unless the server has an AI_API_KEY.")
@SecurityRequirement(name = "bearerAuth")
public class AiController {

    private final ChatClient chatClient;
    private final FlashcardGenerator flashcardGenerator;
    private final AnswerExplainer answerExplainer;
    private final AiUsageLimiter usageLimiter;

    public record StatusResponse(boolean enabled, String model, Integer remainingToday) {
    }

    public record FlashcardsResponse(List<GeneratedCard> cards) {
    }

    public record ExplanationResponse(String explanation) {
    }

    // The frontend asks this first, so it can hide the AI buttons when the feature
    // is off and show how many requests are left
    @GetMapping("/status")
    @Operation(summary = "Whether the AI features are on, and how many requests you have left today")
    public StatusResponse status(@RequestHeader("X-User-Id") UUID userId) {
        if (!chatClient.isEnabled()) {
            return new StatusResponse(false, null, null);
        }
        return new StatusResponse(true, chatClient.model(), usageLimiter.remainingToday(userId));
    }

    @PostMapping("/flashcards")
    @Operation(summary = "Suggest flashcards from notes. Nothing is saved.")
    public FlashcardsResponse generateFlashcards(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody GenerateFlashcardsRequest request) {
        return new FlashcardsResponse(flashcardGenerator.generate(userId, request.getNotes(), request.getCount()));
    }

    @PostMapping("/exams/{sessionId}/questions/{questionId}/explanation")
    @Operation(summary = "Explain one question of a finished exam, including why your answer was wrong")
    public ExplanationResponse explainAnswer(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID sessionId,
            @PathVariable UUID questionId) {
        return new ExplanationResponse(answerExplainer.explain(userId, sessionId, questionId));
    }
}
