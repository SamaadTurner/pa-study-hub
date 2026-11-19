package com.pastudyhub.exam.repository;

import com.pastudyhub.exam.model.ExamQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExamQuestionRepository extends JpaRepository<ExamQuestion, UUID> {

    // The questions of one exam, first question first
    @Query("SELECT eq FROM ExamQuestion eq WHERE eq.examSession.id = :sessionId ORDER BY eq.orderIndex ASC")
    List<ExamQuestion> findBySessionInOrder(@Param("sessionId") UUID sessionId);

    Optional<ExamQuestion> findByExamSessionIdAndQuestionId(UUID examSessionId, UUID questionId);

    // True once the question has been picked for any exam, finished or not
    boolean existsByQuestionId(UUID questionId);

    // True when the question was part of one of the user's finished exams
    @Query("SELECT COUNT(eq) > 0 FROM ExamQuestion eq WHERE eq.question.id = :questionId "
            + "AND eq.examSession.userId = :userId "
            + "AND eq.examSession.status = com.pastudyhub.exam.model.ExamStatus.COMPLETED")
    boolean wasInFinishedExamOf(@Param("questionId") UUID questionId, @Param("userId") UUID userId);

    /**
     * For every task area, how many questions the user was asked in finished exams
     * and how many of those they got right. Unanswered questions have no answer row,
     * so the left join leaves them out of the right ones and they count as wrong.
     * One query for all exams, instead of loading every exam and its answers.
     */
    @Query("SELECT new com.pastudyhub.exam.repository.TaskAreaTotals(q.taskArea, COUNT(eq), "
            + "SUM(CASE WHEN a.isCorrect = true THEN 1 ELSE 0 END)) "
            + "FROM ExamQuestion eq JOIN eq.question q JOIN eq.examSession s "
            + "LEFT JOIN ExamAnswer a ON a.examSession = s AND a.question = q "
            + "WHERE s.userId = :userId AND s.status = com.pastudyhub.exam.model.ExamStatus.COMPLETED "
            + "GROUP BY q.taskArea")
    List<TaskAreaTotals> totalsByTaskArea(@Param("userId") UUID userId);
}
