package com.pastudyhub.flashcard.dto;

import com.pastudyhub.flashcard.model.MedicalCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Request body for POST /api/v1/decks. */
@Data
public class CreateDeckRequest {

    @NotBlank(message = "Deck title is required")
    @Size(max = 100, message = "Title must not exceed 100 characters")
    private String title;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotNull(message = "Category is required")
    private MedicalCategory category;

    private boolean isPublic = false;
}
