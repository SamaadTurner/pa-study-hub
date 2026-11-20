package com.pastudyhub.ai.dto;

import com.pastudyhub.flashcard.model.CardType;

/**
 * A suggested flashcard. It is not saved until the user adds it to a deck.
 * For a fill-in-the-blank card, front is the text with {{c1::...}} blanks and back
 * the optional extra notes.
 */
public record GeneratedCard(String front, String back, CardType cardType) {

    /** A question-and-answer card. */
    public GeneratedCard(String front, String back) {
        this(front, back, CardType.BASIC);
    }
}
