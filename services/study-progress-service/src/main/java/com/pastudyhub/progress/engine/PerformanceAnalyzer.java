package com.pastudyhub.progress.engine;

import com.pastudyhub.progress.model.ActivityLog;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Analyzes a list of activity logs to produce per-category performance metrics.
 *
 * <p>Pure domain object — no Spring dependencies.
 */
public class PerformanceAnalyzer {

    private final List<ActivityLog> logs;

    public PerformanceAnalyzer(List<ActivityLog> logs) {
        this.logs = logs;
    }

    /**
     * Returns percentage correct per category (only categories with > 0 total).
     */
    public Map<String, Double> categoryAccuracy() {
        return logs.stream()
                .collect(Collectors.groupingBy(ActivityLog::getCategory))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> {
                            int total = e.getValue().stream().mapToInt(ActivityLog::getTotalCount).sum();
                            int correct = e.getValue().stream().mapToInt(ActivityLog::getCorrectCount).sum();
                            if (total == 0) return 0.0;
                            return Math.round((double) correct / total * 1000.0) / 10.0;
                        }
                ));
    }

    /**
     * Returns the weakest category (lowest accuracy), or "N/A" if no data.
     */
    public String weakestCategory() {
        Map<String, Double> accuracy = categoryAccuracy();
        return accuracy.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");
    }

    /**
     * Returns total cards/questions reviewed across all logs.
     */
    public int totalItemsReviewed() {
        return logs.stream().mapToInt(ActivityLog::getTotalCount).sum();
    }

    /**
     * Returns total study time in minutes.
     */
    public int totalStudyMinutes() {
        return logs.stream().mapToInt(ActivityLog::getDurationMinutes).sum();
    }

    /**
     * Returns overall accuracy across all categories.
     */
    public double overallAccuracy() {
        int total = totalItemsReviewed();
        if (total == 0) return 0.0;
        int correct = logs.stream().mapToInt(ActivityLog::getCorrectCount).sum();
        return Math.round((double) correct / total * 1000.0) / 10.0;
    }
}
