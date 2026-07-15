package com.pastudyhub.flashcard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** Response body after submitting a card review. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {
    private LocalDate nextReviewDate;
    private int interval;
    private double easeFactor;
    private int repetitions;
    /** Human-readable description of when the card will be reviewed next. */
    private String message;
}
