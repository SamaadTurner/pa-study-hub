package com.pastudyhub.flashcard.mapper;

import com.pastudyhub.flashcard.dto.CreateDeckRequest;
import com.pastudyhub.flashcard.dto.DeckResponse;
import com.pastudyhub.flashcard.model.Deck;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Manually-coded mapper for Deck entity ↔ DTO conversion.
 *
 * <p>Manual mapping keeps the conversion logic explicit and avoids a code-generator
 * dependency. It also makes it trivial to add business logic during mapping (e.g.,
 * computing cardsToReview from a separate query result).
 */
@Component
public class DeckMapper {

    /**
     * Converts a Deck entity to a DeckResponse DTO.
     * cardCount and cardsToReview are injected separately (they come from queries).
     *
     * @param deck         the deck entity
     * @param cardCount    total number of non-deleted cards
     * @param cardsToReview number of cards due for review today
     * @return DeckResponse DTO
     */
    public DeckResponse toResponse(Deck deck, int cardCount, int cardsToReview) {
        if (deck == null) return null;
        return DeckResponse.builder()
                .id(deck.getId())
                .userId(deck.getUserId())
                .title(deck.getTitle())
                .description(deck.getDescription())
                .category(deck.getCategory())
                .isPublic(deck.isPublic())
                .cardCount(cardCount)
                .cardsToReview(cardsToReview)
                .createdAt(deck.getCreatedAt())
                .updatedAt(deck.getUpdatedAt())
                .build();
    }

    /**
     * Creates a new Deck entity from a CreateDeckRequest and the user's UUID.
     *
     * @param request the creation request DTO
     * @param userId  the authenticated user's UUID
     * @return a new unsaved Deck entity
     */
    public Deck toEntity(CreateDeckRequest request, UUID userId) {
        return Deck.builder()
                .userId(userId)
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .category(request.getCategory())
                .isPublic(request.isPublic())
                .isDeleted(false)
                .build();
    }
}
