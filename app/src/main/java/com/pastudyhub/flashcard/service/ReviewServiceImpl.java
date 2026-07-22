package com.pastudyhub.flashcard.service;

import com.pastudyhub.flashcard.dto.CardResponse;
import com.pastudyhub.flashcard.dto.ReviewRequest;
import com.pastudyhub.flashcard.dto.ReviewResponse;
import com.pastudyhub.flashcard.engine.ReviewResult;
import com.pastudyhub.flashcard.engine.SpacedRepetitionEngine;
import com.pastudyhub.flashcard.exception.CardNotFoundException;
import com.pastudyhub.flashcard.exception.DeckNotFoundException;
import com.pastudyhub.flashcard.mapper.CardMapper;
import com.pastudyhub.flashcard.model.Card;
import com.pastudyhub.flashcard.model.ReviewSchedule;
import com.pastudyhub.flashcard.repository.CardRepository;
import com.pastudyhub.flashcard.repository.ReviewScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Service for flashcard review operations using the SM-2 algorithm.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl {

    private final CardRepository cardRepository;
    private final ReviewScheduleRepository reviewScheduleRepository;
    private final CardMapper cardMapper;
    private final WebClient progressServiceClient;

    @Value("${progress.service.url:http://study-progress-service:8083}")
    private String progressServiceUrl;

    /**
     * Get cards due for review in a deck (max 20, most overdue first).
     */
    @Transactional(readOnly = true)
    public List<CardResponse> getCardsForReview(UUID deckId, UUID userId) {
        List<ReviewSchedule> dueSchedules = reviewScheduleRepository.findDueForReview(
                deckId, userId, LocalDate.now(), PageRequest.of(0, 20));

        // Also find cards that have NEVER been reviewed (no ReviewSchedule yet)
        // Get all card IDs in the deck and find those without a schedule for this user
        return dueSchedules.stream()
                .map(schedule -> cardMapper.toResponse(schedule.getCard(), schedule))
                .toList();
    }

    /**
     * Submit a review for a card. Runs the SM-2 algorithm, updates the schedule,
     * and fires an activity log to the study-progress-service.
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

        // Fire-and-forget: log the activity to study-progress-service
        // We don't block on this — review should succeed even if progress service is down
        logActivityAsync(userId, card.getDeck().getCategory().name(), request.getQuality());

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
     * Asynchronously logs the review activity to study-progress-service.
     * This is fire-and-forget — errors are logged but don't fail the review.
     */
    private void logActivityAsync(UUID userId, String category, int quality) {
        try {
            progressServiceClient.post()
                    .uri("/api/v1/progress/log")
                    .bodyValue(java.util.Map.of(
                            "userId", userId.toString(),
                            "activityType", "FLASHCARD_REVIEW",
                            "category", category,
                            "durationMinutes", 0,
                            "cardsReviewed", 1,
                            "correctCount", quality >= 3 ? 1 : 0,
                            "totalCount", 1
                    ))
                    .retrieve()
                    .bodyToMono(Void.class)
                    .subscribe(
                            null,
                            err -> log.warn("Failed to log activity to progress service: {}", err.getMessage())
                    );
        } catch (Exception e) {
            log.warn("Could not contact progress service: {}", e.getMessage());
        }
    }
}
