package com.pastudyhub.flashcard.repository;

import com.pastudyhub.flashcard.model.Card;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Data access layer for {@link Card} entities.
 */
@Repository
public interface CardRepository extends JpaRepository<Card, UUID> {

    /**
     * Find all non-deleted cards in a deck (paginated).
     *
     * <p>Safe: parameterized JPQL.
     */
    @Query("SELECT c FROM Card c WHERE c.deck.id = :deckId AND c.isDeleted = false")
    Page<Card> findByDeckIdAndNotDeleted(@Param("deckId") UUID deckId, Pageable pageable);

    /**
     * Find a specific non-deleted card.
     */
    @Query("SELECT c FROM Card c WHERE c.id = :id AND c.isDeleted = false")
    Optional<Card> findByIdAndNotDeleted(@Param("id") UUID id);

    /**
     * Count non-deleted cards in a deck.
     */
    @Query("SELECT COUNT(c) FROM Card c WHERE c.deck.id = :deckId AND c.isDeleted = false")
    int countByDeckIdAndNotDeleted(@Param("deckId") UUID deckId);

    /**
     * Find cards in a deck that this user has never reviewed, oldest first.
     * A card only gets a review schedule after its first review, so "never reviewed"
     * means there is no schedule row for this user and card.
     *
     * <p>Safe: parameterized JPQL.
     */
    @Query("SELECT c FROM Card c WHERE c.deck.id = :deckId AND c.isDeleted = false " +
           "AND NOT EXISTS (SELECT rs.id FROM ReviewSchedule rs WHERE rs.card = c AND rs.userId = :userId) " +
           "ORDER BY c.positionInDeck ASC, c.createdAt ASC, c.id ASC")
    List<Card> findNeverReviewed(@Param("deckId") UUID deckId, @Param("userId") UUID userId, Pageable pageable);

    /**
     * Count cards in a deck that this user has never reviewed.
     */
    @Query("SELECT COUNT(c) FROM Card c WHERE c.deck.id = :deckId AND c.isDeleted = false " +
           "AND NOT EXISTS (SELECT rs.id FROM ReviewSchedule rs WHERE rs.card = c AND rs.userId = :userId)")
    int countNeverReviewed(@Param("deckId") UUID deckId, @Param("userId") UUID userId);

    /**
     * All cards of a deck in deck order (used when a deck is copied).
     *
     * <p>Safe: parameterized JPQL.
     */
    @Query("SELECT c FROM Card c WHERE c.deck.id = :deckId AND c.isDeleted = false " +
           "ORDER BY c.positionInDeck ASC, c.createdAt ASC, c.id ASC")
    List<Card> findAllInDeckOrder(@Param("deckId") UUID deckId);

    /**
     * The highest position used in a deck so far, 0 for a deck without cards.
     * Deleted cards are counted too, so a position is never handed out twice.
     *
     * <p>Safe: parameterized JPQL.
     */
    @Query("SELECT COALESCE(MAX(c.positionInDeck), 0) FROM Card c WHERE c.deck.id = :deckId")
    int findLastPositionInDeck(@Param("deckId") UUID deckId);
}
