package com.pastudyhub.flashcard.engine;

import com.pastudyhub.flashcard.model.ReviewSchedule;

import java.time.LocalDate;

/**
 * Implements the SuperMemo 2 (SM-2) spaced repetition algorithm.
 *
 * <p>SM-2 is the algorithm used by Anki and SuperMemo. It adaptively schedules
 * card reviews based on recall quality: easier cards come back less often, harder
 * cards come back more often. The ease factor adjusts per-card based on performance.
 *
 * <p><b>Algorithm specification:</b>
 * <pre>
 * Input: quality (0–5)
 *   5 = perfect recall  (mapped from UI "Easy" button)
 *   4 = correct with hesitation  (mapped from UI "Good" button)
 *   3 = barely correct  (not used in this UI)
 *   2 = incorrect but remembered when seen  (mapped from UI "Hard" button)
 *   1 = totally forgot  (mapped from UI "Again" button)
 *   0 = blackout (not used in this UI)
 *
 * If quality >= 3 (correct response):
 *   if repetitions == 0: interval = 1
 *   else if repetitions == 1: interval = 6
 *   else: interval = round(interval * easeFactor)
 *   repetitions += 1
 * Else (incorrect — quality < 3):
 *   repetitions = 0
 *   interval = 1
 *
 * easeFactor = easeFactor + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02))
 * if easeFactor < 1.3: easeFactor = 1.3
 *
 * nextReviewDate = today + interval days
 * </pre>
 *
 * <p><b>UI button → quality mapping:</b>
 * <ul>
 *   <li>"Again" → quality = 1 (totally forgot, resets progress)</li>
 *   <li>"Hard" → quality = 2 (incorrect, short interval)</li>
 *   <li>"Good" → quality = 4 (correct with effort)</li>
 *   <li>"Easy" → quality = 5 (effortless recall)</li>
 * </ul>
 *
 * <p>This class is a pure domain object with zero Spring dependencies — it can be
 * instantiated and tested in unit tests without any application context.
 *
 * <p>References:
 * <a href="https://www.supermemo.com/en/blog/application-of-a-computer-to-improve-the-results-obtained-in-working-with-the-supermemo-method">
 * SM-2 Algorithm by Piotr Wozniak</a>
 */
public class SpacedRepetitionEngine {

    private static final double MIN_EASE_FACTOR = 1.3;
    private static final double DEFAULT_EASE_FACTOR = 2.5;

    private final int currentInterval;
    private final int currentRepetitions;
    private final double currentEaseFactor;

    /**
     * Constructs the engine from an existing ReviewSchedule.
     *
     * @param schedule the current review schedule for the card
     */
    public SpacedRepetitionEngine(ReviewSchedule schedule) {
        this.currentInterval = schedule.getInterval();
        this.currentRepetitions = schedule.getRepetitions();
        this.currentEaseFactor = schedule.getEaseFactor();
    }

    /**
     * Constructs the engine with explicit initial values.
     * Used when creating a brand-new card (before any review schedule exists).
     *
     * @param interval    current interval in days (0 for new cards)
     * @param repetitions number of consecutive correct responses
     * @param easeFactor  current ease factor (default: 2.5)
     */
    public SpacedRepetitionEngine(int interval, int repetitions, double easeFactor) {
        this.currentInterval = interval;
        this.currentRepetitions = repetitions;
        this.currentEaseFactor = easeFactor;
    }

    /**
     * Convenience constructor for brand-new cards with default values.
     * interval=0, repetitions=0, easeFactor=2.5
     */
    public SpacedRepetitionEngine() {
        this(0, 0, DEFAULT_EASE_FACTOR);
    }

    /**
     * Calculates the next review schedule based on the user's quality rating.
     *
     * <p>This method is pure — it does not modify any state. The caller is responsible
     * for persisting the returned {@link ReviewResult} to the ReviewSchedule entity.
     *
     * @param quality the quality rating (1=Again, 2=Hard, 4=Good, 5=Easy)
     * @return a {@link ReviewResult} record containing the new interval, ease factor,
     *         repetitions count, and next review date
     * @throws IllegalArgumentException if quality is outside the valid range [0, 5]
     */
    public ReviewResult calculateNextReview(int quality) {
        if (quality < 0 || quality > 5) {
            throw new IllegalArgumentException("Quality must be between 0 and 5, got: " + quality);
        }

        int newInterval;
        int newRepetitions;

        if (quality >= 3) {
            // Correct response — advance the schedule
            if (currentRepetitions == 0) {
                newInterval = 1;
            } else if (currentRepetitions == 1) {
                newInterval = 6;
            } else {
                // Round to nearest integer — SM-2 spec uses rounding
                newInterval = (int) Math.round(currentInterval * currentEaseFactor);
            }
            newRepetitions = currentRepetitions + 1;
        } else {
            // Incorrect response — reset progress, review again soon
            newRepetitions = 0;
            newInterval = 1;
        }

        // Update ease factor regardless of correctness
        // This formula gradually adjusts the card's "difficulty" based on recall quality
        // High quality responses increase ease factor (card becomes easier)
        // Low quality responses decrease ease factor (card stays harder)
        double newEaseFactor = currentEaseFactor
                + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02));

        // Enforce minimum ease factor — prevents cards from being scheduled too aggressively
        if (newEaseFactor < MIN_EASE_FACTOR) {
            newEaseFactor = MIN_EASE_FACTOR;
        }

        LocalDate nextReviewDate = LocalDate.now().plusDays(newInterval);

        return ReviewResult.of(newInterval, newEaseFactor, newRepetitions, nextReviewDate);
    }
}
