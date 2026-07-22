package com.pastudyhub.flashcard.mapper;

import com.pastudyhub.flashcard.dto.CardResponse;
import com.pastudyhub.flashcard.dto.CreateCardRequest;
import com.pastudyhub.flashcard.model.Card;
import com.pastudyhub.flashcard.model.Deck;
import com.pastudyhub.flashcard.model.ReviewSchedule;
import org.springframework.stereotype.Component;

/**
 * Manually-coded mapper for Card entity ↔ DTO conversion.
 */
@Component
public class CardMapper {

    /**
     * Converts a Card entity to a CardResponse DTO.
     * Optionally includes the review schedule if provided (null if never reviewed).
     *
     * @param card     the card entity
     * @param schedule the review schedule, or null if the card has never been reviewed
     * @return CardResponse DTO
     */
    public CardResponse toResponse(Card card, ReviewSchedule schedule) {
        if (card == null) return null;

        CardResponse.ReviewScheduleInfo scheduleInfo = null;
        if (schedule != null) {
            scheduleInfo = CardResponse.ReviewScheduleInfo.builder()
                    .nextReviewDate(schedule.getNextReviewDate())
                    .interval(schedule.getInterval())
                    .easeFactor(schedule.getEaseFactor())
                    .repetitions(schedule.getRepetitions())
                    .build();
        }

        return CardResponse.builder()
                .id(card.getId())
                .deckId(card.getDeck().getId())
                .front(card.getFront())
                .back(card.getBack())
                .hint(card.getHint())
                .imageUrl(card.getImageUrl())
                .tags(card.getTagsList())
                .reviewSchedule(scheduleInfo)
                .createdAt(card.getCreatedAt())
                .updatedAt(card.getUpdatedAt())
                .build();
    }

    /**
     * Creates a new Card entity from a CreateCardRequest and its parent Deck.
     *
     * @param request the creation request DTO
     * @param deck    the parent deck entity
     * @return a new unsaved Card entity
     */
    public Card toEntity(CreateCardRequest request, Deck deck) {
        Card card = Card.builder()
                .deck(deck)
                .front(request.getFront().trim())
                .back(request.getBack().trim())
                .hint(request.getHint())
                .imageUrl(request.getImageUrl())
                .isDeleted(false)
                .build();
        card.setTagsList(request.getTags());
        return card;
    }
}
