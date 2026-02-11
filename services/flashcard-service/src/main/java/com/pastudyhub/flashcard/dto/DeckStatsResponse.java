package com.pastudyhub.flashcard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Deck-level review statistics returned by GET /api/v1/decks/{deckId}/stats. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeckStatsResponse {
    private int totalCards;
    private int cardsDueToday;
    private int cardsMastered;
    private double averageEaseFactor;
}
