package com.pastudyhub.evaluation;

import com.pastudyhub.flashcard.engine.ReviewResult;
import com.pastudyhub.flashcard.engine.SpacedRepetitionEngine;

/**
 * SM-2 only chooses intervals; it never says how likely a card is to be remembered.
 * To score it at all, this assumes what SM-2 implicitly aims for: a 90% chance of
 * remembering on the day its interval ends, falling off exponentially:
 * p = 0.9 ^ (elapsed days / interval). The README states this assumption, because
 * the result for SM-2 depends on it.
 */
public class Sm2Predictor implements Predictor {

    private int interval = 0;
    private int repetitions = 0;
    private double easeFactor = 2.5;

    @Override
    public double chanceOfRemembering(int elapsedDays) {
        return Math.pow(0.9, (double) elapsedDays / Math.max(interval, 1));
    }

    @Override
    public void review(int elapsedDays, int rating) {
        ReviewResult result = new SpacedRepetitionEngine(interval, repetitions, easeFactor)
                .calculateNextReview(qualityFor(rating));
        interval = result.newInterval();
        repetitions = result.newRepetitions();
        easeFactor = result.newEaseFactor();
    }

    // The same buttons as the app: Again 1, Hard 2, Good 4, Easy 5
    static int qualityFor(int rating) {
        return switch (rating) {
            case 1 -> 1;
            case 2 -> 2;
            case 3 -> 4;
            default -> 5;
        };
    }
}
