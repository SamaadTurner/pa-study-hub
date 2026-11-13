package com.pastudyhub.exam.engine;

import com.pastudyhub.exam.model.ExamAnswer;
import com.pastudyhub.exam.model.Question;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Calculates the score of an exam from its questions and the answers that were given.
 *
 * <p>The score is out of every question in the exam, not just the answered ones. A
 * question with no answer counts as wrong. The engine used to look only at the
 * answers, so skipping the hard questions gave a better percentage, and an exam with
 * no answers at all could not be scored.
 *
 * <p>This is a pure domain object — no Spring dependencies, fully testable
 * with plain JUnit without a Spring context.
 */
public class ScoringEngine {

    private final List<Question> questions;
    private final List<ExamAnswer> answers;

    // IDs of the questions that have a correct answer
    private final Set<UUID> correctQuestionIds = new HashSet<>();

    /**
     * @param questions every question of the exam
     * @param answers   the answers the user gave; may be empty, and may be fewer than the questions
     */
    public ScoringEngine(List<Question> questions, List<ExamAnswer> answers) {
        if (questions == null || answers == null) {
            throw new IllegalArgumentException("Questions and answers are both required to score an exam");
        }
        this.questions = questions;
        this.answers = answers;

        for (ExamAnswer answer : answers) {
            if (answer.isCorrect()) {
                correctQuestionIds.add(answer.getQuestion().getId());
            }
        }
    }

    /**
     * Calculates the total number of correct answers.
     */
    public int calculateRawScore() {
        int correct = 0;
        for (Question question : questions) {
            if (correctQuestionIds.contains(question.getId())) {
                correct++;
            }
        }
        return correct;
    }

    /**
     * Calculates score as a percentage (0.0–100.0) of all questions, rounded to 1 decimal.
     */
    public double calculateScorePercent() {
        if (questions.isEmpty()) return 0.0;
        double percent = (double) calculateRawScore() / questions.size() * 100.0;
        return Math.round(percent * 10.0) / 10.0;
    }

    /**
     * Returns a map of category -> percentage correct for that category.
     * Every category that has a question in the exam is included, also when
     * none of its questions were answered (that category is then 0%).
     */
    public Map<String, Double> calculateCategoryBreakdown() {
        // TreeMap so the categories always come out in the same (alphabetical) order
        Map<String, Integer> totalByCategory = new TreeMap<>();
        Map<String, Integer> correctByCategory = new TreeMap<>();

        for (Question question : questions) {
            String category = question.getCategory().name();
            totalByCategory.merge(category, 1, Integer::sum);
            if (correctQuestionIds.contains(question.getId())) {
                correctByCategory.merge(category, 1, Integer::sum);
            }
        }

        Map<String, Double> breakdown = new TreeMap<>();
        for (Map.Entry<String, Integer> entry : totalByCategory.entrySet()) {
            int correct = correctByCategory.getOrDefault(entry.getKey(), 0);
            double pct = (double) correct / entry.getValue() * 100.0;
            breakdown.put(entry.getKey(), Math.round(pct * 10.0) / 10.0);
        }
        return breakdown;
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
     * Calculates the average time spent per answered question in seconds.
     * Questions without an answer have no time, so they are left out.
     */
    public double calculateAvgTimePerQuestion() {
        return answers.stream()
                .filter(a -> a.getTimeSpentSeconds() != null)
                .mapToInt(ExamAnswer::getTimeSpentSeconds)
                .average()
                .orElse(0.0);
    }

    /**
     * Returns IDs of the questions that were answered wrong or not answered at all.
     */
    public List<UUID> getIncorrectQuestionIds() {
        List<UUID> incorrect = new ArrayList<>();
        for (Question question : questions) {
            if (!correctQuestionIds.contains(question.getId())) {
                incorrect.add(question.getId());
            }
        }
        return incorrect;
    }

    /**
     * Builds a complete ScoreResult record from all calculations.
     */
    public ScoreResult calculate() {
        return new ScoreResult(
                calculateRawScore(),
                questions.size(),
                calculateScorePercent(),
                calculateCategoryBreakdown(),
                getPerformanceBand(),
                calculateAvgTimePerQuestion(),
                getIncorrectQuestionIds()
        );
    }
}
