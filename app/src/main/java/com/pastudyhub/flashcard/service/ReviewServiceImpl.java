package com.pastudyhub.flashcard.service;

import com.pastudyhub.flashcard.dto.CardResponse;
import com.pastudyhub.flashcard.dto.ReviewRequest;
import com.pastudyhub.flashcard.dto.ReviewResponse;
import com.pastudyhub.flashcard.engine.FsrsEngine;
import com.pastudyhub.flashcard.engine.ReviewResult;
import com.pastudyhub.flashcard.engine.SchedulingProperties;
import com.pastudyhub.flashcard.engine.SpacedRepetitionEngine;
import com.pastudyhub.flashcard.exception.CardNotFoundException;
import com.pastudyhub.flashcard.exception.DeckNotFoundException;
import com.pastudyhub.flashcard.exception.UnauthorizedDeckAccessException;
import com.pastudyhub.flashcard.mapper.CardMapper;
import com.pastudyhub.flashcard.model.Card;
import com.pastudyhub.flashcard.model.Deck;
import com.pastudyhub.flashcard.model.ReviewLog;
import com.pastudyhub.flashcard.model.ReviewSchedule;
import com.pastudyhub.flashcard.repository.CardRepository;
import com.pastudyhub.flashcard.repository.DeckRepository;
import com.pastudyhub.flashcard.repository.ReviewLogRepository;
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
 * Service for flashcard reviews. Every review runs both SM-2 and FSRS; the
 * "scheduling.algorithm" setting decides which one sets the next review date.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl {

    // Most cards handed out for one review session
    private static final int MAX_CARDS_PER_SESSION = 20;

    // Most study time one card can add (5 minutes). The time comes from the browser,
    // so a card left open over lunch should not count as an hour of studying.
    private static final int MAX_SECONDS_PER_CARD = 300;

    private final CardRepository cardRepository;
    private final DeckRepository deckRepository;
    private final ReviewScheduleRepository reviewScheduleRepository;
    private final ReviewLogRepository reviewLogRepository;
    private final CardMapper cardMapper;
    private final ProgressService progressService;
    private final SchedulingProperties schedulingProperties;

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
        if (!deck.canBeViewedBy(userId)) {
            throw new UnauthorizedDeckAccessException("You do not have permission to view this deck");
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

        // Same rule as reading the deck: no reviewing cards from someone else's private deck
        if (!card.getDeck().canBeViewedBy(userId)) {
            throw new UnauthorizedDeckAccessException("You do not have permission to view this deck");
        }

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

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        // Calendar days, not 24-hour periods: a card seen at 11 pm and again at 8 am
        // the next morning counts as one day, the way schedulers count intervals.
        // Null on the first review.
        Integer daysSinceLastReview = null;
        if (schedule.getLastReviewedAt() != null) {
            daysSinceLastReview = (int) ChronoUnit.DAYS.between(schedule.getLastReviewedAt().toLocalDate(), today);
        }

        // Written before the schedule changes, because it records what the card
        // looked like when the user saw it (the planned interval, the last review)
        logReview(card, userId, schedule, request, now, daysSinceLastReview);

        // Both schedulers run on every review, so each keeps a complete history of
        // the card and the setting can be switched at any time
        ReviewResult sm2 = new SpacedRepetitionEngine(schedule).calculateNextReview(request.getQuality());
        FsrsEngine.Result fsrs = runFsrs(schedule, daysSinceLastReview, request.getQuality());

        schedule.setInterval(sm2.newInterval());
        schedule.setEaseFactor(sm2.newEaseFactor());
        schedule.setRepetitions(sm2.newRepetitions());
        schedule.setFsrsStability(fsrs.memory().stability());
        schedule.setFsrsDifficulty(fsrs.memory().difficulty());

        // The setting decides whose interval is used
        int plannedDays = schedulingProperties.getAlgorithm() == SchedulingProperties.Algorithm.FSRS
                ? fsrs.intervalDays()
                : sm2.newInterval();
        LocalDate nextReviewDate = today.plusDays(plannedDays);

        schedule.setPlannedIntervalDays(plannedDays);
        schedule.setNextReviewDate(nextReviewDate);
        schedule.setLastReviewedAt(now);
        schedule.setLastQuality(request.getQuality());

        reviewScheduleRepository.save(schedule);

        logReviewActivity(userId, card.getDeck().getCategory().name(), request.getQuality(),
                secondsToCount(request.getTimeSpentSeconds()));

        log.debug("Review submitted: cardId={}, userId={}, quality={}, scheduler={}, nextReview={}",
                cardId, userId, request.getQuality(), schedulingProperties.getAlgorithm(), nextReviewDate);

        return ReviewResponse.builder()
                .nextReviewDate(nextReviewDate)
                .interval(plannedDays)
                .easeFactor(sm2.newEaseFactor())
                .repetitions(sm2.newRepetitions())
                .message(buildReviewMessage(today, nextReviewDate))
                .build();
    }

    private FsrsEngine.Result runFsrs(ReviewSchedule schedule, Integer daysSinceLastReview, int quality) {
        FsrsEngine engine = new FsrsEngine(schedulingProperties.getDesiredRetention());
        int grade = FsrsEngine.gradeFromQuality(quality);

        // No FSRS memory yet: a new card, or one last reviewed before FSRS was added
        if (schedule.getFsrsStability() == null || schedule.getFsrsDifficulty() == null) {
            return engine.review(null, 0, grade);
        }

        FsrsEngine.MemoryState memory = new FsrsEngine.MemoryState(
                schedule.getFsrsStability(), schedule.getFsrsDifficulty());
        // The server clock could in theory go backwards; never pass a negative gap
        int elapsed = daysSinceLastReview == null ? 0 : Math.max(0, daysSinceLastReview);
        return engine.review(memory, elapsed, grade);
    }

    /**
     * Adds this review to the review history. It runs in the same transaction as
     * the schedule update, so a review can't be saved without its history row.
     */
    private void logReview(Card card, UUID userId, ReviewSchedule schedule, ReviewRequest request,
                           LocalDateTime reviewedAt, Integer daysSinceLastReview) {
        Integer timeSpent = request.getTimeSpentSeconds() == null
                ? null
                : secondsToCount(request.getTimeSpentSeconds());

        reviewLogRepository.save(ReviewLog.builder()
                .userId(userId)
                .card(card)
                .quality(request.getQuality())
                .reviewedAt(reviewedAt)
                .daysSinceLastReview(daysSinceLastReview)
                .scheduledDays(schedule.getPlannedIntervalDays())
                .timeSpentSeconds(timeSpent)
                .build());
    }

    // The time is optional in the request, and anything over the cap is cut down to it
    private int secondsToCount(Integer timeSpentSeconds) {
        if (timeSpentSeconds == null) {
            return 0;
        }
        return Math.min(timeSpentSeconds, MAX_SECONDS_PER_CARD);
    }

    private String buildReviewMessage(LocalDate today, LocalDate nextReviewDate) {
        long daysUntilNext = ChronoUnit.DAYS.between(today, nextReviewDate);
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
    private void logReviewActivity(UUID userId, String category, int quality, int secondsSpent) {
        // A quality of 3 or higher means the user got the card right
        int correct = quality >= 3 ? 1 : 0;

        LogActivityRequest activity = LogActivityRequest.builder()
                .userId(userId)
                .activityType(ActivityType.FLASHCARD_REVIEW)
                .category(category)
                .durationSeconds(secondsSpent)
                .cardsReviewed(1)
                .correctCount(correct)
                .totalCount(1)
                .build();

        progressService.logActivity(activity);
    }
}
