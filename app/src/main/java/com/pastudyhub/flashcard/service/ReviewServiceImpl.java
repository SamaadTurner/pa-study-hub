package com.pastudyhub.flashcard.service;

import com.pastudyhub.flashcard.dto.CardResponse;
import com.pastudyhub.flashcard.dto.ReviewRequest;
import com.pastudyhub.flashcard.dto.ReviewResponse;
import com.pastudyhub.flashcard.engine.ReviewResult;
import com.pastudyhub.flashcard.engine.SpacedRepetitionEngine;
import com.pastudyhub.flashcard.exception.CardNotFoundException;
import com.pastudyhub.flashcard.exception.DeckNotFoundException;
import com.pastudyhub.flashcard.exception.UnauthorizedDeckAccessException;
import com.pastudyhub.flashcard.mapper.CardMapper;
import com.pastudyhub.flashcard.model.Card;
import com.pastudyhub.flashcard.model.Deck;
import com.pastudyhub.flashcard.model.ReviewSchedule;
import com.pastudyhub.flashcard.repository.CardRepository;
import com.pastudyhub.flashcard.repository.DeckRepository;
import com.pastudyhub.flashcard.repository.ReviewScheduleRepository;
import com.pastudyhub.progress.dto.LogActivityRequest;
import com.pastudyhub.progress.model.ActivityType;
import com.pastudyhub.progress.service.ProgressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for flashcard review operations using the SM-2 algorithm.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl {

    // Most cards handed out for one review session
    private static final int MAX_CARDS_PER_SESSION = 20;

    private final CardRepository cardRepository;
    private final DeckRepository deckRepository;
    private final ReviewScheduleRepository reviewScheduleRepository;
    private final CardMapper cardMapper;
    private final ProgressService progressService;

    /**
     * Get the cards to study in a deck right now (max 20).
     *
     * <p>Cards that are due come first, most overdue first. If there is room left,
     * cards the user has never reviewed are added, oldest first. Without the second
     * part a brand new deck would never have anything to review, because a card only
     * gets a schedule after its first review.
     */
    @Transactional(readOnly = true)
    public List<CardResponse> getCardsForReview(UUID deckId, UUID userId) {
        Deck deck = deckRepository.findByIdAndNotDeleted(deckId)
                .orElseThrow(() -> new DeckNotFoundException(deckId));

        // You can study your own decks and public ones, but not someone else's private deck
        if (!deck.isPublic() && !deck.getUserId().equals(userId)) {
            throw new UnauthorizedDeckAccessException();
        }

        List<CardResponse> cards = new ArrayList<>();

        List<ReviewSchedule> dueSchedules = reviewScheduleRepository.findDueForReview(
                deckId, userId, LocalDate.now(), PageRequest.of(0, MAX_CARDS_PER_SESSION));
        for (ReviewSchedule schedule : dueSchedules) {
            cards.add(cardMapper.toResponse(schedule.getCard(), schedule));
        }

        int roomLeft = MAX_CARDS_PER_SESSION - cards.size();
        if (roomLeft > 0) {
            List<Card> newCards = cardRepository.findNeverReviewed(deckId, userId, PageRequest.of(0, roomLeft));
            for (Card card : newCards) {
                // No schedule yet, so the response has no review info
                cards.add(cardMapper.toResponse(card, null));
            }
        }

        return cards;
    }

    /**
     * Submit a review for a card. Runs the SM-2 algorithm, updates the schedule,
     * and records the review so it counts toward the user's streak and daily goal.
     */
    @Transactional
    public ReviewResponse submitReview(UUID cardId, UUID userId, ReviewRequest request) {
        Card card = cardRepository.findByIdAndNotDeleted(cardId)
                .orElseThrow(() -> new CardNotFoundException(cardId));

        // Find or create the review schedule for this user+card combination
        ReviewSchedule schedule = reviewScheduleRepository
                .findByCardIdAndUserId(cardId, userId)
                .orElseGet(() -> ReviewSchedule.builder()
                        .card(card)
                        .userId(userId)
                        .easeFactor(2.5)
                        .interval(0)
                        .repetitions(0)
                        .build());

        // Run the SM-2 algorithm
        SpacedRepetitionEngine engine = new SpacedRepetitionEngine(schedule);
        ReviewResult result = engine.calculateNextReview(request.getQuality());

        // Update the schedule with the new values
        schedule.setInterval(result.newInterval());
        schedule.setEaseFactor(result.newEaseFactor());
        schedule.setRepetitions(result.newRepetitions());
        schedule.setNextReviewDate(result.nextReviewDate());
        schedule.setLastReviewedAt(LocalDateTime.now());
        schedule.setLastQuality(request.getQuality());

        reviewScheduleRepository.save(schedule);

        logReviewActivity(userId, card.getDeck().getCategory().name(), request.getQuality());

        log.debug("Review submitted: cardId={}, userId={}, quality={}, nextReview={}",
                cardId, userId, request.getQuality(), result.nextReviewDate());

        // Build human-readable message
        String message = buildReviewMessage(result);

        return ReviewResponse.builder()
                .nextReviewDate(result.nextReviewDate())
                .interval(result.newInterval())
                .easeFactor(result.newEaseFactor())
                .repetitions(result.newRepetitions())
                .message(message)
                .build();
    }

    private String buildReviewMessage(ReviewResult result) {
        long daysUntilNext = ChronoUnit.DAYS.between(LocalDate.now(), result.nextReviewDate());
        if (daysUntilNext == 0) {
            return "Review again today";
        } else if (daysUntilNext == 1) {
            return "Review tomorrow";
        } else if (daysUntilNext < 7) {
            return "Review in " + daysUntilNext + " days";
        } else if (daysUntilNext < 21) {
            return "Review in " + (daysUntilNext / 7) + " week(s)";
        } else {
            return "Card mastered! Review in " + daysUntilNext + " days";
        }
    }

    /**
     * Records one reviewed card in the activity log.
     *
     * <p>This used to be an HTTP call to a separate progress service. Everything is one
     * app now, so that call went to a host that no longer exists and nothing was ever
     * saved. Calling the service directly also means the review and the activity are
     * saved in the same transaction, so one can't be saved without the other.
     */
    private void logReviewActivity(UUID userId, String category, int quality) {
        // A quality of 3 or higher means the user got the card right
        int correct = quality >= 3 ? 1 : 0;

        LogActivityRequest activity = LogActivityRequest.builder()
                .userId(userId)
                .activityType(ActivityType.FLASHCARD_REVIEW)
                .category(category)
                .durationMinutes(0)
                .cardsReviewed(1)
                .correctCount(correct)
                .totalCount(1)
                .build();

        progressService.logActivity(activity);
    }
}
