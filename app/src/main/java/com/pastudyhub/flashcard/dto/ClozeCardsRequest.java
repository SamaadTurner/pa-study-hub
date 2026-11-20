package com.pastudyhub.flashcard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request body for POST /api/v1/decks/{deckId}/cloze-cards: one text with blanks
 * like {{c1::answer}}. Every blank number becomes its own card.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClozeCardsRequest {

    @NotBlank(message = "The text is required")
    @Size(max = 2000, message = "The text must not exceed 2000 characters")
    private String text;

    /** Optional notes shown with the answer. */
    @Size(max = 5000, message = "Extra notes must not exceed 5000 characters")
    private String extra;

    private List<String> tags;
}
