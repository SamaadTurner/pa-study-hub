package com.pastudyhub.exam.engine;

import com.pastudyhub.exam.model.ExamAnswer;
import com.pastudyhub.exam.model.ExamSession;
import com.pastudyhub.exam.model.QuestionCategory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Calculates exam scores from a completed set of exam answers.
 *
 * <p>This is a pure domain object — no Spring dependencies, fully testable
 * with plain JUnit without a Spring context.
 */
public class ScoringEngine {

    private final List<ExamAnswer> answers;

    public ScoringEngine(List<ExamAnswer> answers) {
        if (answers == null || answers.isEmpty()) {
            throw new IllegalArgumentException("Cannot score an exam with no answers");
        }
        this.answers = answers;
    }

    /**
     * Calculates the total number of correct answers.
     */
    public int calculateRawScore() {
        return (int) answers.stream().filter(ExamAnswer::isCorrect).count();
    }

    /**
     * Calculates score as a percentage (0.0–100.0), rounded to 1 decimal.
     */
    public double calculateScorePercent() {
        if (answers.isEmpty()) return 0.0;
        double percent = (double) calculateRawScore() / answers.size() * 100.0;
        return Math.round(percent * 10.0) / 10.0;
    }

    /**
     * Returns a map of category -> percentage correct for that category.
     * Only includes categories that appear in the answer set.
     */
    public Map<String, Double> calculateCategoryBreakdown() {
        Map<QuestionCategory, List<ExamAnswer>> byCategory = answers.stream()
                .collect(Collectors.groupingBy(a -> a.getQuestion().getCategory()));

        return byCategory.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().name(),
                        e -> {
                            long correct = e.getValue().stream().filter(ExamAnswer::isCorrect).count();
                            double pct = (double) correct / e.getValue().size() * 100.0;
                            return Math.round(pct * 10.0) / 10.0;
                        }
                ));
    }

    /**
     * Returns a human-readable performance band.
     */
    public PerformanceBand getPerformanceBand() {
        double pct = calculateScorePercent();
        if (pct >= 80) return PerformanceBand.EXCELLENT;
        if (pct >= 70) return PerformanceBand.GOOD;
        if (pct >= 60) return PerformanceBand.PASSING;
        return PerformanceBand.NEEDS_IMPROVEMENT;
    }

    /**
     * Calculates the average time spent per question in seconds.
     */
    public double calculateAvgTimePerQuestion() {
        return answers.stream()
                .filter(a -> a.getTimeSpentSeconds() != null)
                .mapToInt(ExamAnswer::getTimeSpentSeconds)
                .average()
                .orElse(0.0);
    }

    /**
     * Returns IDs of questions answered incorrectly, for AI explanation feature.
     */
    public List<java.util.UUID> getIncorrectQuestionIds() {
        return answers.stream()
                .filter(a -> !a.isCorrect())
                .map(a -> a.getQuestion().getId())
                .collect(Collectors.toList());
    }

    /**
     * Builds a complete ScoreResult record from all calculations.
     */
    public ScoreResult calculate() {
        return new ScoreResult(
                calculateRawScore(),
                answers.size(),
                calculateScorePercent(),
                calculateCategoryBreakdown(),
                getPerformanceBand(),
                calculateAvgTimePerQuestion(),
                getIncorrectQuestionIds()
        );
    }
}
