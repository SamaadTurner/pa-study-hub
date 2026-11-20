package com.pastudyhub.flashcard.repository;

import com.pastudyhub.flashcard.model.Card;
import com.pastudyhub.flashcard.model.ReviewLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewLogRepository extends JpaRepository<ReviewLog, UUID> {

    /** Every review of one card by one user, oldest first. */
    List<ReviewLog> findByUserIdAndCardIdOrderByReviewedAtAsc(UUID userId, UUID cardId);

    // Moves one student's review history for a card to another card (see DeckHandoverService)
    @Modifying
    @Query("UPDATE ReviewLog r SET r.card = :to WHERE r.card = :from AND r.userId = :userId")
    int moveToCard(@Param("from") Card from, @Param("to") Card to, @Param("userId") UUID userId);
}
