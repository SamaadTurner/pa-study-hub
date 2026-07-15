package com.pastudyhub.flashcard.repository;

import com.pastudyhub.flashcard.model.Deck;
import com.pastudyhub.flashcard.model.MedicalCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Data access layer for {@link Deck} entities.
 *
 * <p>All queries are parameterized JPA queries — SQL injection safe.
 */
@Repository
public interface DeckRepository extends JpaRepository<Deck, UUID> {

    /**
     * Find all non-deleted decks for a user, optionally filtered by category.
     * Returns most recently updated decks first.
     *
     * <p>Safe: parameterized JPQL with Spring Data — no injection risk.
     */
    @Query("SELECT d FROM Deck d WHERE d.userId = :userId AND d.isDeleted = false " +
           "AND (:category IS NULL OR d.category = :category) " +
           "ORDER BY d.updatedAt DESC")
    Page<Deck> findByUserIdAndNotDeleted(
            @Param("userId") UUID userId,
            @Param("category") MedicalCategory category,
            Pageable pageable);

    /**
     * Find a specific non-deleted deck.
     */
    @Query("SELECT d FROM Deck d WHERE d.id = :id AND d.isDeleted = false")
    Optional<Deck> findByIdAndNotDeleted(@Param("id") UUID id);

    /**
     * Browse public decks from all users (for the public deck explorer).
     * Optionally filter by category.
     *
     * <p>Safe: parameterized JPQL.
     */
    @Query("SELECT d FROM Deck d WHERE d.isPublic = true AND d.isDeleted = false " +
           "AND (:category IS NULL OR d.category = :category) " +
           "ORDER BY d.updatedAt DESC")
    Page<Deck> findPublicDecks(@Param("category") MedicalCategory category, Pageable pageable);

    /**
     * Count non-deleted cards in a deck.
     *
     * <p>Safe: parameterized JPQL.
     */
    @Query("SELECT COUNT(c) FROM Card c WHERE c.deck.id = :deckId AND c.isDeleted = false")
    int countCardsByDeckId(@Param("deckId") UUID deckId);
}
