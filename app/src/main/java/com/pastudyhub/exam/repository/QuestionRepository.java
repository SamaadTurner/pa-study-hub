package com.pastudyhub.exam.repository;

import com.pastudyhub.exam.model.DifficultyLevel;
import com.pastudyhub.exam.model.Question;
import com.pastudyhub.exam.model.QuestionCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface QuestionRepository extends JpaRepository<Question, UUID> {

    /**
     * The questions an exam for this user can pick from: the starter questions, the
     * user's own questions, and, if asked for, questions other users have shared.
     */
    @Query("SELECT q FROM Question q WHERE q.isActive = true " +
           "AND (q.ownerId IS NULL OR q.ownerId = :userId " +
           "     OR (:includeShared = true AND q.isPublic = true)) " +
           "AND (:category IS NULL OR q.category = :category) " +
           "AND (:difficulty IS NULL OR q.difficulty = :difficulty)")
    List<Question> findExamPool(
            @Param("userId") UUID userId,
            @Param("includeShared") boolean includeShared,
            @Param("category") QuestionCategory category,
            @Param("difficulty") DifficultyLevel difficulty);

    // The questions a user wrote and has not deleted, last changed first
    @Query("SELECT q FROM Question q WHERE q.isActive = true AND q.ownerId = :ownerId " +
           "ORDER BY q.updatedAt DESC, q.id")
    Page<Question> findWrittenBy(@Param("ownerId") UUID ownerId, Pageable pageable);

    @Query("SELECT COUNT(q) FROM Question q WHERE q.isActive = true " +
           "AND (:category IS NULL OR q.category = :category)")
    long countActiveByCategory(@Param("category") QuestionCategory category);

    List<Question> findByCategoryAndIsActiveTrue(QuestionCategory category);
}
