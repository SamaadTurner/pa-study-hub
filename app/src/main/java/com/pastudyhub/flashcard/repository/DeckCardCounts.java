package com.pastudyhub.flashcard.repository;

import java.util.UUID;

/**
 * The counts a deck list shows for one deck, as a grouped query returns them.
 *
 * @param cardCount the deck's cards that are not deleted
 * @param toReview  the ones to study today: due, or never reviewed by this user
 */
public record DeckCardCounts(UUID deckId, long cardCount, long toReview) {
}
