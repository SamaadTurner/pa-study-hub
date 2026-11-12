package com.pastudyhub.flashcard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pastudyhub.flashcard.model.MedicalCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/** Response DTO for deck endpoints. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeckResponse {
    private UUID id;
    private UUID userId;
    private String title;
    private String description;
    private MedicalCategory category;
    // Without this the JSON field is called "public", because of how the getter
    // for a boolean named isPublic gets its name. The frontend reads "isPublic".
    @JsonProperty("isPublic")
    private boolean isPublic;
    private int cardCount;
    private int cardsToReview;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
