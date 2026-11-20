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
     * A shared question stays in other people's exams while it has no reports of its
     * own, or while its own reports plus its earlier versions' reports are under the
     * limit. Same rule as SharedQuestionService.isHiddenByReports.
     */
    String NOT_HIDDEN_BY_REPORTS = "(NOT EXISTS (SELECT r FROM QuestionReport r WHERE r.question = q) "
            + "OR (SELECT COUNT(r) FROM QuestionReport r WHERE r.question = q) + q.earlierReportCount < :reportLimit)";

    /**
     * Which questions an exam for this user can pick from: the starter questions, the
     * user's own questions, and, if asked for, questions other users shared that do
     * not have too many reports. Shared by the pool query and the counts below.
     */
    String POOL_CONDITION = "q.isActive = true "
            + "AND (q.ownerId IS NULL OR q.ownerId = :userId "
            + "     OR (:includeShared = true AND q.isPublic = true AND " + NOT_HIDDEN_BY_REPORTS + ")) ";

    @Query("SELECT q FROM Question q WHERE " + POOL_CONDITION
            + "AND (:category IS NULL OR q.category = :category) "
            + "AND (:difficulty IS NULL OR q.difficulty = :difficulty)")
    List<Question> findExamPool(
            @Param("userId") UUID userId,
            @Param("includeShared") boolean includeShared,
            @Param("reportLimit") long reportLimit,
            @Param("category") QuestionCategory category,
            @Param("difficulty") DifficultyLevel difficulty);

    // How many questions the pool has per category, for the exam setup page
    @Query("SELECT new com.pastudyhub.exam.repository.CategoryCount(q.category, COUNT(q)) "
            + "FROM Question q WHERE " + POOL_CONDITION + "GROUP BY q.category")
    List<CategoryCount> countExamPoolByCategory(
            @Param("userId") UUID userId,
            @Param("includeShared") boolean includeShared,
            @Param("reportLimit") long reportLimit);

    // Questions other users shared, for the browse page, newest first. pattern is a
    // lower-case LIKE pattern from the service ("%" alone matches everything, "!" escapes).
    @Query("SELECT q FROM Question q WHERE q.isActive = true AND q.isPublic = true "
            + "AND q.ownerId IS NOT NULL AND q.ownerId <> :userId "
            + "AND " + NOT_HIDDEN_BY_REPORTS + " "
            + "AND (:category IS NULL OR q.category = :category) "
            + "AND (LOWER(q.stem) LIKE :pattern ESCAPE '!' OR LOWER(q.clinicalVignette) LIKE :pattern ESCAPE '!') "
            + "ORDER BY q.createdAt DESC, q.id")
    Page<Question> findSharedByOthers(
            @Param("userId") UUID userId,
            @Param("reportLimit") long reportLimit,
            @Param("category") QuestionCategory category,
            @Param("pattern") String pattern,
            Pageable pageable);

    // The questions a user wrote and has not deleted, last changed first
    @Query("SELECT q FROM Question q WHERE q.isActive = true AND q.ownerId = :ownerId " +
           "ORDER BY q.updatedAt DESC, q.id")
    Page<Question> findWrittenBy(@Param("ownerId") UUID ownerId, Pageable pageable);

    @Query("SELECT COUNT(q) FROM Question q WHERE q.isActive = true " +
           "AND (:category IS NULL OR q.category = :category)")
    long countActiveByCategory(@Param("category") QuestionCategory category);

    List<Question> findByCategoryAndIsActiveTrue(QuestionCategory category);
}
