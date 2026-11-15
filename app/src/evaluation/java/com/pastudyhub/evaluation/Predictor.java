package com.pastudyhub.evaluation;

/**
 * A scheduler as the evaluation sees it: before each review it predicts the chance
 * the card is remembered, then it learns the real rating. One instance per card.
 */
public interface Predictor {

    /** The chance of remembering the card {@code elapsedDays} after its last review. */
    double chanceOfRemembering(int elapsedDays);

    /** Updates the card's state with a review. {@code elapsedDays} is 0 for the first review. */
    void review(int elapsedDays, int rating);
}
