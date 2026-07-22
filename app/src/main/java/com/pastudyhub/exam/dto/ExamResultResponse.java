package com.pastudyhub.exam.dto;

import com.pastudyhub.exam.engine.PerformanceBand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamResultResponse {
    private UUID sessionId;
    private int rawScore;
    private int totalQuestions;
    private double scorePercent;
    private PerformanceBand performanceBand;
    private String performanceMessage;
    private Map<String, Double> categoryBreakdown;
    private double avgTimePerQuestionSeconds;
    private int durationSeconds;
    private LocalDateTime completedAt;
    /** Full questions with correct answers revealed and explanations */
    private List<ExamAnswerDetailResponse> answerDetails;
}
