package com.pastudyhub.progress.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Records a single study activity event (flashcard review or exam completion).
 * Logged asynchronously by other services (flashcard-service, exam-service).
 */
@Entity
@Table(name = "activity_logs",
        indexes = {
                @Index(name = "idx_activity_user_date", columnList = "user_id, activity_date DESC"),
                @Index(name = "idx_activity_type", columnList = "activity_type"),
                @Index(name = "idx_activity_category", columnList = "category")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ActivityType activityType;

    @Column(nullable = false, length = 50)
    private String category;

    /** Duration of the study session in minutes (0 for quick reviews). */
    @Column(nullable = false)
    @Builder.Default
    private int durationMinutes = 0;

    /** For flashcard reviews: total cards reviewed. */
    @Column(nullable = false)
    @Builder.Default
    private int cardsReviewed = 0;

    /** Number of correct answers/reviews. */
    @Column(nullable = false)
    @Builder.Default
    private int correctCount = 0;

    /** Total questions or cards attempted. */
    @Column(nullable = false)
    @Builder.Default
    private int totalCount = 0;

    @Column(nullable = false)
    private LocalDate activityDate;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
