package com.pastudyhub.flashcard.controller;

import com.pastudyhub.flashcard.dto.CardResponse;
import com.pastudyhub.flashcard.dto.ReviewRequest;
import com.pastudyhub.flashcard.dto.ReviewResponse;
import com.pastudyhub.flashcard.service.ReviewServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for spaced-repetition review sessions.
 * userId is extracted from the X-User-Id header forwarded by the API Gateway.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "SM-2 spaced repetition review sessions")
@SecurityRequirement(name = "bearerAuth")
public class ReviewController {

    private final ReviewServiceImpl reviewService;

    @GetMapping("/decks/{deckId}/review")
    @Operation(summary = "Get cards due for review in a deck (max 20, most overdue first)")
    public List<CardResponse> getCardsForReview(
            @PathVariable UUID deckId,
            @RequestHeader("X-User-Id") UUID userId) {
        return reviewService.getCardsForReview(deckId, userId);
    }

    @PostMapping("/cards/{cardId}/review")
    @Operation(summary = "Submit a review result for a card (runs SM-2 algorithm)")
    public ReviewResponse submitReview(
            @PathVariable UUID cardId,
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody ReviewRequest request) {
        return reviewService.submitReview(cardId, userId, request);
    }
}
