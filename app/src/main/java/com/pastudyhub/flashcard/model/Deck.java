package com.pastudyhub.flashcard.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A flashcard deck owned by a specific user.
 *
 * <p>Decks are the top-level organizing unit. Each deck has a category
 * (one of the NCCPA blueprint categories), which enables filtering and
 * per-category performance analytics.
 *
 * <p>Public decks can be discovered and cloned by other users.
 */
@Entity
@Table(name = "decks",
    indexes = {
        @Index(name = "idx_decks_user_id", columnList = "user_id"),
        @Index(name = "idx_decks_category", columnList = "category"),
        @Index(name = "idx_decks_updated_at", columnList = "updated_at")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Deck {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** The user who created and owns this deck. Indexed for fast user deck queries. */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private MedicalCategory category;

    /**
     * When true, this deck appears in the public deck browser and can be
     * cloned by other users. The original deck remains unaffected by clones.
     */
    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private boolean isPublic = false;

    /**
     * Soft-delete flag. Deleted decks and their cards are hidden from all queries
     * but retained in the database for potential recovery.
     */
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean isDeleted = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
