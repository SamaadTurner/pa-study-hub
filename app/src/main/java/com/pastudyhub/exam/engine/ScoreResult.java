package com.pastudyhub.exam.engine;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable value object holding the complete result of scoring an exam session.
 */
public record ScoreResult(
        int rawScore,
        int totalQuestions,
        double scorePercent,
        Map<String, Double> categoryBreakdown,
        PerformanceBand performanceBand,
        double avgTimePerQuestionSeconds,
        List<UUID> incorrectQuestionIds
) {
    /**
     * Convenience: number of questions answered incorrectly.
     */
    public int incorrectCount() {
        return totalQuestions - rawScore;
    }
}
