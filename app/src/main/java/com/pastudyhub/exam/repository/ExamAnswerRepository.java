package com.pastudyhub.exam.repository;

import com.pastudyhub.exam.model.ExamAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExamAnswerRepository extends JpaRepository<ExamAnswer, UUID> {

    List<ExamAnswer> findByExamSessionId(UUID examSessionId);

    Optional<ExamAnswer> findByExamSessionIdAndQuestionId(UUID examSessionId, UUID questionId);

    @Query("SELECT COUNT(a) FROM ExamAnswer a WHERE a.examSession.id = :sessionId AND a.isCorrect = true")
    long countCorrectBySessionId(@Param("sessionId") UUID sessionId);
}
