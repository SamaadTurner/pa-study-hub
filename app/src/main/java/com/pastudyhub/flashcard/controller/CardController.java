package com.pastudyhub.flashcard.controller;

import com.pastudyhub.flashcard.dto.CardResponse;
import com.pastudyhub.flashcard.dto.CreateCardRequest;
import com.pastudyhub.flashcard.dto.ClozeCardsRequest;
import com.pastudyhub.flashcard.dto.ImageCardsRequest;
import com.pastudyhub.flashcard.dto.UpdatePictureCardsRequest;
import com.pastudyhub.flashcard.service.CardServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for flashcard (card) CRUD operations within a deck.
 * userId is extracted from the X-User-Id header forwarded by the API Gateway.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Flashcards", description = "Create, edit, and manage individual flashcards within a deck")
@SecurityRequirement(name = "bearerAuth")
public class CardController {

    private final CardServiceImpl cardService;

    @GetMapping("/decks/{deckId}/cards")
    @Operation(summary = "List all cards in a deck")
    public Page<CardResponse> getCardsForDeck(
            @PathVariable UUID deckId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return cardService.getCardsForDeck(deckId, userId, page, size);
    }

    @PostMapping("/decks/{deckId}/cards")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a card to a deck")
    public CardResponse createCard(
            @PathVariable UUID deckId,
            @Valid @RequestBody CreateCardRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return cardService.createCard(deckId, request, userId);
    }

    @PostMapping("/decks/{deckId}/cloze-cards")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add fill-in-the-blank cards: one card per blank number ({{c1::answer}})")
    public List<CardResponse> createClozeCards(
            @PathVariable UUID deckId,
            @Valid @RequestBody ClozeCardsRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return cardService.createClozeCards(deckId, request, userId);
    }

    @PostMapping("/decks/{deckId}/image-cards")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add picture cards: one card per box drawn over an uploaded picture")
    public List<CardResponse> createImageCards(
            @PathVariable UUID deckId,
            @Valid @RequestBody ImageCardsRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return cardService.createImageCards(deckId, request, userId);
    }

    @PutMapping("/cards/{cardId}/picture")
    @Operation(summary = "Change the boxes, question, notes and tags of all cards of one picture")
    public List<CardResponse> updatePictureCards(
            @PathVariable UUID cardId,
            @Valid @RequestBody UpdatePictureCardsRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return cardService.updatePictureCards(cardId, request, userId);
    }

    @PutMapping("/cards/{cardId}")
    @Operation(summary = "Update an existing card")
    public CardResponse updateCard(
            @PathVariable UUID cardId,
            @Valid @RequestBody CreateCardRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return cardService.updateCard(cardId, request, userId);
    }

    @DeleteMapping("/cards/{cardId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a card (soft delete)")
    public void deleteCard(
            @PathVariable UUID cardId,
            @RequestHeader("X-User-Id") UUID userId) {
        cardService.deleteCard(cardId, userId);
    }
}
