package com.pastudyhub.exam.repository;

import com.pastudyhub.exam.model.ExamSession;
import com.pastudyhub.exam.model.ExamStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExamSessionRepository extends JpaRepository<ExamSession, UUID> {

    Page<ExamSession> findByUserIdOrderByStartedAtDesc(UUID userId, Pageable pageable);

    Optional<ExamSession> findByIdAndUserId(UUID id, UUID userId);

    @Query("SELECT AVG(e.scorePercent) FROM ExamSession e " +
           "WHERE e.userId = :userId AND e.status = 'COMPLETED'")
    Double findAverageScoreForUser(@Param("userId") UUID userId);

    long countByUserIdAndStatus(UUID userId, ExamStatus status);

    // Timed exams that are still running. The time check itself is done in Java,
    // because adding minutes to a timestamp is written differently in H2 and PostgreSQL.
    @Query("SELECT e.id FROM ExamSession e WHERE e.status = com.pastudyhub.exam.model.ExamStatus.IN_PROGRESS "
            + "AND e.timeLimitMinutes > 0")
    List<UUID> findIdsOfTimedExamsInProgress();
}
