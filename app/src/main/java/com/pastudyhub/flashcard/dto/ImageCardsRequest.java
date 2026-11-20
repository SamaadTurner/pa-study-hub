package com.pastudyhub.flashcard.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Request body for POST /api/v1/decks/{deckId}/image-cards: an uploaded picture and
 * the boxes drawn over it. Every box becomes its own card.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImageCardsRequest {

    @NotNull(message = "Upload a picture first")
    private UUID imageId;

    /** The question shown above the picture. A default is used when it is empty. */
    @Size(max = 2000, message = "The question must not exceed 2000 characters")
    private String prompt;

    /** Optional notes shown with the answer. */
    @Size(max = 5000, message = "Extra notes must not exceed 5000 characters")
    private String extra;

    // Checked in the service (OcclusionBoxes), so the message can name the box
    private List<OcclusionBox> boxes;

    private List<String> tags;
}
