package com.pastudyhub.flashcard.engine;

import java.time.LocalDate;

/**
 * Immutable record holding the result of an SM-2 algorithm calculation.
 *
 * <p>Returned by {@link SpacedRepetitionEngine#calculateNextReview(int)}.
 * The caller persists these values to the {@link com.pastudyhub.flashcard.model.ReviewSchedule} entity.
 *
 * @param newInterval     days until the next review
 * @param newEaseFactor   updated ease factor (minimum 1.3)
 * @param newRepetitions  updated consecutive correct response count
 * @param nextReviewDate  the exact date the card should be reviewed next
 */
public record ReviewResult(
        int newInterval,
        double newEaseFactor,
        int newRepetitions,
        LocalDate nextReviewDate
) {

    /**
     * Factory method for creating a ReviewResult.
     *
     * @param newInterval    days until next review
     * @param newEaseFactor  updated ease factor
     * @param newRepetitions updated repetitions count
     * @param nextReviewDate next review date
     * @return a new ReviewResult
     */
    public static ReviewResult of(
            int newInterval,
            double newEaseFactor,
            int newRepetitions,
            LocalDate nextReviewDate) {
        return new ReviewResult(newInterval, newEaseFactor, newRepetitions, nextReviewDate);
    }

    /**
     * Returns true if the card is considered "mastered" (interval >= 21 days).
     * Used for the deck stats endpoint to count mastered cards.
     */
    public boolean isMastered() {
        return newInterval >= 21;
    }
}
