package com.pastudyhub.flashcard.dto;

import com.pastudyhub.flashcard.model.CardType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Response DTO for card endpoints. Includes review schedule if available. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardResponse {
    private UUID id;
    private UUID deckId;
    private String front;
    private String back;
    private String hint;
    private CardType cardType;
    /** For a cloze card, the blank number it asks for. */
    private Integer clozeIndex;
    private String imageUrl;
    /** For a picture card: the picture to load from /api/v1/images/{imageId}. */
    private UUID imageId;
    /** For a picture card: all boxes; clozeIndex says which one this card asks for. */
    private List<OcclusionBox> boxes;
    private List<String> tags;
    private ReviewScheduleInfo reviewSchedule;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Embedded review schedule info. Null if card has never been reviewed. */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewScheduleInfo {
        private LocalDate nextReviewDate;
        private int interval;
        private double easeFactor;
        private int repetitions;
    }
}
