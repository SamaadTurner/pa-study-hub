package com.pastudyhub.exam.repository;

import com.pastudyhub.exam.model.QuestionReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface QuestionReportRepository extends JpaRepository<QuestionReport, UUID> {

    boolean existsByQuestionIdAndReporterId(UUID questionId, UUID reporterId);

    long countByQuestionId(UUID questionId);

    // The reports on a set of questions, oldest first (for the author's list)
    @Query("SELECT r FROM QuestionReport r WHERE r.question.id IN :questionIds ORDER BY r.createdAt")
    List<QuestionReport> findByQuestionIds(@Param("questionIds") Collection<UUID> questionIds);

    // Which of these questions this user has reported (for the results page)
    @Query("SELECT r.question.id FROM QuestionReport r WHERE r.reporterId = :reporterId "
            + "AND r.question.id IN :questionIds")
    List<UUID> findReportedQuestionIds(@Param("reporterId") UUID reporterId,
                                       @Param("questionIds") Collection<UUID> questionIds);
}
