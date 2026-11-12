package com.pastudyhub.flashcard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pastudyhub.flashcard.model.MedicalCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request body for POST /api/v1/decks. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDeckRequest {

    @NotBlank(message = "Deck title is required")
    @Size(max = 100, message = "Title must not exceed 100 characters")
    private String title;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotNull(message = "Category is required")
    private MedicalCategory category;

    // Read from "isPublic" in the request body. Without the annotation the field
    // would only be filled from a property called "public", so the checkbox in the
    // deck form was ignored and every deck was saved as private.
    @JsonProperty("isPublic")
    @Builder.Default
    private boolean isPublic = false;
}
