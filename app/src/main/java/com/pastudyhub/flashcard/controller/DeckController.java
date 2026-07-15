package com.pastudyhub.flashcard.controller;

import com.pastudyhub.flashcard.dto.*;
import com.pastudyhub.flashcard.model.MedicalCategory;
import com.pastudyhub.flashcard.service.DeckService;
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
 * REST controller for flashcard deck operations.
 * userId is extracted from the X-User-Id header forwarded by the API Gateway.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Flashcard Decks", description = "Create, browse, and manage flashcard decks")
@SecurityRequirement(name = "bearerAuth")
public class DeckController {

    private final DeckService deckService;

    @GetMapping("/decks")
    @Operation(summary = "List user's decks")
    public Page<DeckResponse> getUserDecks(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(required = false) MedicalCategory category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return deckService.getUserDecks(userId, category, page, size);
    }

    @GetMapping("/decks/{deckId}")
    @Operation(summary = "Get a specific deck")
    public DeckResponse getDeck(
            @PathVariable UUID deckId,
            @RequestHeader("X-User-Id") UUID userId) {
        return deckService.getDeck(deckId, userId);
    }

    @PostMapping("/decks")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new deck")
    public DeckResponse createDeck(
            @Valid @RequestBody CreateDeckRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return deckService.createDeck(request, userId);
    }

    @PutMapping("/decks/{deckId}")
    @Operation(summary = "Update a deck")
    public DeckResponse updateDeck(
            @PathVariable UUID deckId,
            @Valid @RequestBody CreateDeckRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return deckService.updateDeck(deckId, request, userId);
    }

    @DeleteMapping("/decks/{deckId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a deck (soft delete)")
    public void deleteDeck(
            @PathVariable UUID deckId,
            @RequestHeader("X-User-Id") UUID userId) {
        deckService.deleteDeck(deckId, userId);
    }

    @GetMapping("/decks/public")
    @Operation(summary = "Browse public decks")
    public Page<DeckResponse> getPublicDecks(
            @RequestParam(required = false) MedicalCategory category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return deckService.getPublicDecks(category, page, size);
    }

    @PostMapping("/decks/{deckId}/clone")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Clone a public deck into your collection")
    public DeckResponse cloneDeck(
            @PathVariable UUID deckId,
            @RequestHeader("X-User-Id") UUID userId) {
        return deckService.cloneDeck(deckId, userId);
    }

    @GetMapping("/decks/{deckId}/stats")
    @Operation(summary = "Get deck review statistics")
    public DeckStatsResponse getDeckStats(
            @PathVariable UUID deckId,
            @RequestHeader("X-User-Id") UUID userId) {
        return deckService.getDeckStats(deckId, userId);
    }
}
