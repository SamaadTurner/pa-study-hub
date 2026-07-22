package com.pastudyhub.exam.repository;

import com.pastudyhub.exam.model.DifficultyLevel;
import com.pastudyhub.exam.model.Question;
import com.pastudyhub.exam.model.QuestionCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface QuestionRepository extends JpaRepository<Question, UUID> {

    @Query("SELECT q FROM Question q WHERE q.isActive = true " +
           "AND (:category IS NULL OR q.category = :category) " +
           "AND (:difficulty IS NULL OR q.difficulty = :difficulty)")
    List<Question> findActiveByFilters(
            @Param("category") QuestionCategory category,
            @Param("difficulty") DifficultyLevel difficulty);

    @Query("SELECT COUNT(q) FROM Question q WHERE q.isActive = true " +
           "AND (:category IS NULL OR q.category = :category)")
    long countActiveByCategory(@Param("category") QuestionCategory category);

    List<Question> findByCategoryAndIsActiveTrue(QuestionCategory category);
}
