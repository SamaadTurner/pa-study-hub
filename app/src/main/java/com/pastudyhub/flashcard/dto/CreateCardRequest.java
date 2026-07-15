package com.pastudyhub.flashcard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** Request body for POST /api/v1/decks/{deckId}/cards. */
@Data
public class CreateCardRequest {

    @NotBlank(message = "Card front (question) is required")
    @Size(max = 2000, message = "Front must not exceed 2000 characters")
    private String front;

    @NotBlank(message = "Card back (answer) is required")
    @Size(max = 5000, message = "Back must not exceed 5000 characters")
    private String back;

    @Size(max = 500, message = "Hint must not exceed 500 characters")
    private String hint;

    @Size(max = 500, message = "Image URL must not exceed 500 characters")
    private String imageUrl;

    private List<String> tags;
}
