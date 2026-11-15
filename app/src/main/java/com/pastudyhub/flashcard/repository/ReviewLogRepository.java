package com.pastudyhub.flashcard.repository;

import com.pastudyhub.flashcard.model.ReviewLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewLogRepository extends JpaRepository<ReviewLog, UUID> {

    /** Every review of one card by one user, oldest first. */
    List<ReviewLog> findByUserIdAndCardIdOrderByReviewedAtAsc(UUID userId, UUID cardId);
}
