package com.pastudyhub.flashcard.repository;

import com.pastudyhub.flashcard.model.ReviewSchedule;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Data access layer for {@link ReviewSchedule} entities.
 */
@Repository
public interface ReviewScheduleRepository extends JpaRepository<ReviewSchedule, UUID> {

    /**
     * Find the review schedule for a specific user and card.
     * Returns Optional.empty() if the card has never been reviewed by this user.
     *
     * <p>Safe: parameterized JPQL.
     */
    @Query("SELECT rs FROM ReviewSchedule rs WHERE rs.card.id = :cardId AND rs.userId = :userId")
    Optional<ReviewSchedule> findByCardIdAndUserId(
            @Param("cardId") UUID cardId,
            @Param("userId") UUID userId);

    /**
     * Find all cards due for review in a deck for a user.
     * "Due" means: nextReviewDate is today or earlier, OR the card has never been reviewed.
     * Returns up to maxResults, ordered by nextReviewDate ascending (most overdue first).
     *
     * <p>Safe: parameterized JPQL — no injection risk.
     */
    @Query("SELECT rs FROM ReviewSchedule rs " +
           "JOIN rs.card c " +
           "WHERE c.deck.id = :deckId " +
           "AND rs.userId = :userId " +
           "AND c.isDeleted = false " +
           "AND (rs.nextReviewDate IS NULL OR rs.nextReviewDate <= :today) " +
           "ORDER BY rs.nextReviewDate ASC NULLS FIRST")
    List<ReviewSchedule> findDueForReview(
            @Param("deckId") UUID deckId,
            @Param("userId") UUID userId,
            @Param("today") LocalDate today,
            Pageable pageable);

    /**
     * Count cards due for review in a deck for a user.
     *
     * <p>Safe: parameterized JPQL.
     */
    @Query("SELECT COUNT(rs) FROM ReviewSchedule rs " +
           "JOIN rs.card c " +
           "WHERE c.deck.id = :deckId " +
           "AND rs.userId = :userId " +
           "AND c.isDeleted = false " +
           "AND (rs.nextReviewDate IS NULL OR rs.nextReviewDate <= :today)")
    int countDueForReview(
            @Param("deckId") UUID deckId,
            @Param("userId") UUID userId,
            @Param("today") LocalDate today);

    /**
     * Count mastered cards (interval >= 21 days) in a deck for a user.
     *
     * <p>Safe: parameterized JPQL.
     */
    @Query("SELECT COUNT(rs) FROM ReviewSchedule rs " +
           "JOIN rs.card c " +
           "WHERE c.deck.id = :deckId " +
           "AND rs.userId = :userId " +
           "AND c.isDeleted = false " +
           "AND rs.interval >= 21")
    int countMastered(
            @Param("deckId") UUID deckId,
            @Param("userId") UUID userId);

    /**
     * Find all review schedules for cards in a deck (for deck stats).
     *
     * <p>Safe: parameterized JPQL.
     */
    @Query("SELECT AVG(rs.easeFactor) FROM ReviewSchedule rs " +
           "JOIN rs.card c " +
           "WHERE c.deck.id = :deckId AND rs.userId = :userId AND c.isDeleted = false")
    Double findAverageEaseFactorForDeck(
            @Param("deckId") UUID deckId,
            @Param("userId") UUID userId);
}
