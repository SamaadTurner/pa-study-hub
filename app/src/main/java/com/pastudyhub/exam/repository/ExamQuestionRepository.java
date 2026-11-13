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
}
