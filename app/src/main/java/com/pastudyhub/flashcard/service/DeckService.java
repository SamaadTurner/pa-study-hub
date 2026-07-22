package com.pastudyhub.flashcard.service;

import com.pastudyhub.flashcard.dto.*;
import com.pastudyhub.flashcard.model.MedicalCategory;
import org.springframework.data.domain.Page;

import java.util.UUID;

/** Deck management service interface — dependency inversion principle. */
public interface DeckService {
    Page<DeckResponse> getUserDecks(UUID userId, MedicalCategory category, int page, int size);
    DeckResponse getDeck(UUID deckId, UUID userId);
    DeckResponse createDeck(CreateDeckRequest request, UUID userId);
    DeckResponse updateDeck(UUID deckId, CreateDeckRequest request, UUID userId);
    void deleteDeck(UUID deckId, UUID userId);
    Page<DeckResponse> getPublicDecks(MedicalCategory category, int page, int size);
    DeckResponse cloneDeck(UUID sourceDeckId, UUID userId);
    DeckStatsResponse getDeckStats(UUID deckId, UUID userId);
}
