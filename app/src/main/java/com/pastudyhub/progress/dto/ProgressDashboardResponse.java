package com.pastudyhub.progress.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Aggregated progress data for the user dashboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressDashboardResponse {
    private int currentStreak;
    private int longestStreak;
    private int totalStudyDays;
    private int totalStudyMinutes;
    private int totalCardsReviewed;
    private double overallAccuracy;
    private String weakestCategory;
    private Map<String, Double> categoryAccuracy;
    /** Accuracy per day for the last 30 days — for the trend chart. */
    private List<DailyProgressPoint> last30DaysActivity;
    /** Today's goal progress. */
    private GoalProgressResponse todayGoal;
}
