package com.pastudyhub.ai.dto;

import com.pastudyhub.flashcard.model.CardType;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Request body for POST /api/v1/ai/flashcards. */
@Data
public class GenerateFlashcardsRequest {

    // A cap on the input keeps each request small, which matters on a free tier
    // that counts tokens per minute
    @NotBlank(message = "Paste some notes to make flashcards from")
    @Size(max = 6000, message = "Notes must not exceed 6000 characters")
    private String notes;

    @Min(value = 1, message = "Ask for at least 1 card")
    @Max(value = 10, message = "Ask for at most 10 cards at a time")
    private int count = 5;

    /** BASIC (question and answer, the default) or CLOZE (fill in the blank). */
    private CardType cardType = CardType.BASIC;
}
