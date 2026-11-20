package com.pastudyhub.flashcard.repository;

import com.pastudyhub.flashcard.model.CardImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.UUID;

public interface CardImageRepository extends JpaRepository<CardImage, UUID> {

    long countByOwnerId(UUID ownerId);

    /** All stored pictures together, in bytes. */
    @Query("SELECT COALESCE(SUM(i.sizeBytes), 0) FROM CardImage i")
    long totalBytes();

    /**
     * Deletes pictures that no card uses any more (uploaded and then not saved, or
     * whose cards were all deleted), once they are older than the given time.
     */
    @Modifying
    @Query("DELETE FROM CardImage i WHERE i.createdAt < :before AND NOT EXISTS "
            + "(SELECT c.id FROM Card c WHERE c.imageId = i.id AND c.isDeleted = false)")
    int deleteUnusedBefore(@Param("before") LocalDateTime before);
}
