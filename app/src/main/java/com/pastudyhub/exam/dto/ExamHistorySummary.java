package com.pastudyhub.exam.dto;

import com.pastudyhub.exam.engine.PerformanceBand;
import com.pastudyhub.exam.model.DifficultyLevel;
import com.pastudyhub.exam.model.ExamStatus;
import com.pastudyhub.exam.model.QuestionCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/** Lightweight summary of a past exam session for the history list. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamHistorySummary {
    private UUID id;
    private int questionCount;
    private Integer score;
    private Double scorePercent;
    private PerformanceBand performanceBand;
    private QuestionCategory categoryFilter;
    private DifficultyLevel difficultyFilter;
    private ExamStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Integer durationSeconds;
}
