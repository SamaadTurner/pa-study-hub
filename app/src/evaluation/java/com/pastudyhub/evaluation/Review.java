package com.pastudyhub.evaluation;

/**
 * One review from a review history.
 *
 * @param cardId which card (any text, only used to group reviews)
 * @param day    the day of the review, counted from any fixed starting day
 * @param rating 1 Again, 2 Hard, 3 Good, 4 Easy (the FSRS and Anki scale)
 */
public record Review(String cardId, int day, int rating) {

    public Review {
        if (rating < 1 || rating > 4) {
            throw new IllegalArgumentException("Rating must be 1 to 4, got " + rating + " for card " + cardId);
        }
    }

    /** Anything but Again counts as remembered. */
    public boolean remembered() {
        return rating > 1;
    }
}
