package com.pastudyhub.flashcard.dto;

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
    private boolean isPublic;
    private int cardCount;
    private int cardsToReview;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
