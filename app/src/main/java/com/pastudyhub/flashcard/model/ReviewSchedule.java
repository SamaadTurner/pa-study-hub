package com.pastudyhub.flashcard.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tracks the SM-2 spaced repetition schedule for a specific card and user.
 *
 * <p>One ReviewSchedule row exists per (userId, cardId) combination. The first time
 * a user reviews a card, a new ReviewSchedule is created. Subsequent reviews update
 * the existing record.
 *
 * <p>The key fields are:
 * <ul>
 *   <li>{@code easeFactor} — how easy this card is for this user (default 2.5, min 1.3)</li>
 *   <li>{@code interval} — days until next review</li>
 *   <li>{@code repetitions} — consecutive correct reviews (resets to 0 on incorrect)</li>
 *   <li>{@code nextReviewDate} — when to show this card next (indexed for fast "due today" queries)</li>
 * </ul>
 */
@Entity
@Table(name = "review_schedules",
    indexes = {
        @Index(name = "idx_review_card_id", columnList = "card_id"),
        @Index(name = "idx_review_user_id", columnList = "user_id"),
        @Index(name = "idx_review_next_date", columnList = "next_review_date"),
        @Index(name = "idx_review_user_card", columnList = "user_id, card_id", unique = true)
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * The SM-2 ease factor (default 2.5). Higher = card is easier for this user.
     * Minimum: 1.3 (enforced by SpacedRepetitionEngine).
     */
    @Column(name = "ease_factor", nullable = false)
    @Builder.Default
    private double easeFactor = 2.5;

    /**
     * Days until the next review. Starts at 0 (review today/now).
     */
    @Column(name = "interval", nullable = false)
    @Builder.Default
    private int interval = 0;

    /**
     * Consecutive correct review count. Resets to 0 on any incorrect response.
     */
    @Column(name = "repetitions", nullable = false)
    @Builder.Default
    private int repetitions = 0;

    /**
     * The date this card is scheduled to be reviewed next.
     * Indexed for fast "due today" queries: WHERE next_review_date <= CURRENT_DATE
     */
    @Column(name = "next_review_date")
    private LocalDate nextReviewDate;

    @Column(name = "last_reviewed_at")
    private LocalDateTime lastReviewedAt;

    /**
     * The quality rating from the last review (1-5).
     * Stored for debugging and analytics — not used in the SM-2 calculation itself.
     */
    @Column(name = "last_quality")
    private Integer lastQuality;
}
