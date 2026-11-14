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
     * The order comes from the Pageable.
     *
     * <p>Safe: parameterized JPQL with Spring Data — no injection risk.
     */
    @Query("SELECT d FROM Deck d WHERE d.userId = :userId AND d.isDeleted = false " +
           "AND (:category IS NULL OR d.category = :category)")
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
     * Optionally filter by category, and by text in the title or description.
     *
     * <p>pattern is a lower-case LIKE pattern built by the service, where "%" alone
     * matches every deck and "!" is the escape character. The order comes from
     * the Pageable.
     *
     * <p>Safe: parameterized JPQL.
     */
    @Query("SELECT d FROM Deck d WHERE d.isPublic = true AND d.isDeleted = false " +
           "AND (:category IS NULL OR d.category = :category) " +
           "AND (LOWER(d.title) LIKE :pattern ESCAPE '!' " +
           "     OR LOWER(d.description) LIKE :pattern ESCAPE '!')")
    Page<Deck> findPublicDecks(
            @Param("category") MedicalCategory category,
            @Param("pattern") String pattern,
            Pageable pageable);

    /**
     * Count non-deleted cards in a deck.
     *
     * <p>Safe: parameterized JPQL.
     */
    @Query("SELECT COUNT(c) FROM Card c WHERE c.deck.id = :deckId AND c.isDeleted = false")
    int countCardsByDeckId(@Param("deckId") UUID deckId);
}
