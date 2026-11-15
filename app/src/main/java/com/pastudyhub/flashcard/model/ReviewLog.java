package com.pastudyhub.flashcard.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One review of one card by one user. Rows are only ever added, never changed.
 *
 * <p>{@link ReviewSchedule} holds the current state of a card; this is the history
 * that led to it. See V12__log_every_review.sql for why it exists.
 */
@Entity
@Table(name = "review_logs",
        indexes = {
                @Index(name = "idx_review_logs_user_card_time", columnList = "user_id, card_id, reviewed_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    /** The button pressed: 1 Again, 2 Hard, 4 Good, 5 Easy. */
    @Column(name = "quality", nullable = false)
    private int quality;

    @Column(name = "reviewed_at", nullable = false)
    private LocalDateTime reviewedAt;

    /** Calendar days since the previous review of this card. Null on the first review. */
    @Column(name = "days_since_last_review")
    private Integer daysSinceLastReview;

    /** The interval the scheduler had set before this review, in days. */
    @Column(name = "scheduled_days", nullable = false)
    private int scheduledDays;

    @Column(name = "time_spent_seconds")
    private Integer timeSpentSeconds;
}
