package com.pastudyhub.progress.repository;

import com.pastudyhub.progress.model.DailyGoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DailyGoalRepository extends JpaRepository<DailyGoal, UUID> {
    Optional<DailyGoal> findByUserId(UUID userId);
}
