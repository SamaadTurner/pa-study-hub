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
     * Find all card IDs in a deck (used for batch operations like deck cloning).
     *
     * <p>Safe: parameterized JPQL.
     */
    @Query("SELECT c.id FROM Card c WHERE c.deck.id = :deckId AND c.isDeleted = false")
    List<UUID> findCardIdsByDeckId(@Param("deckId") UUID deckId);
}
