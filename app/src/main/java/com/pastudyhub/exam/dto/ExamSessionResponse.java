package com.pastudyhub.exam.dto;

import com.pastudyhub.exam.model.DifficultyLevel;
import com.pastudyhub.exam.model.ExamStatus;
import com.pastudyhub.exam.model.QuestionCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamSessionResponse {
    private UUID id;
    private UUID userId;
    private int questionCount;
    private int timeLimitMinutes;
    private QuestionCategory categoryFilter;
    private DifficultyLevel difficultyFilter;
    private ExamStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    /** Questions for this session — options omit isCorrect during exam */
    private List<QuestionResponse> questions;
    /** Progress: how many answered so far */
    private int answeredCount;
}
