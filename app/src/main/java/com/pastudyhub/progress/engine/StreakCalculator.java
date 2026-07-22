package com.pastudyhub.progress.engine;

import java.time.LocalDate;
import java.util.List;

/**
 * Calculates study streaks from a list of active study dates.
 *
 * <p>A streak is a consecutive sequence of calendar days with at least
 * one study activity. Missing a day resets the streak to 0.
 * Studying on today counts toward the current streak.
 *
 * <p>Pure domain object — no Spring dependencies.
 */
public class StreakCalculator {

    private final List<LocalDate> studyDates;
    private final LocalDate today;

    public StreakCalculator(List<LocalDate> studyDates, LocalDate today) {
        // Deduplicate and sort descending (most recent first)
        this.studyDates = studyDates.stream()
                .distinct()
                .sorted(java.util.Comparator.reverseOrder())
                .toList();
        this.today = today;
    }

    /**
     * Returns the current active streak (days including today or yesterday
     * — streak is not broken if today hasn't been studied yet).
     */
    public int calculateCurrentStreak() {
        if (studyDates.isEmpty()) return 0;

        LocalDate mostRecent = studyDates.get(0);

        // If most recent study day is more than 1 day ago, streak is broken
        if (mostRecent.isBefore(today.minusDays(1))) return 0;

        // Count consecutive days backwards from most recent
        int streak = 1;
        for (int i = 1; i < studyDates.size(); i++) {
            LocalDate expected = studyDates.get(i - 1).minusDays(1);
            if (studyDates.get(i).equals(expected)) {
                streak++;
            } else {
                break;
            }
        }
        return streak;
    }

    /**
     * Returns the longest streak ever achieved in the given date list.
     */
    public int calculateLongestStreak() {
        if (studyDates.isEmpty()) return 0;

        // Sort ascending for this calculation
        List<LocalDate> ascending = studyDates.stream()
                .sorted()
                .toList();

        int maxStreak = 1;
        int currentStreak = 1;

        for (int i = 1; i < ascending.size(); i++) {
            if (ascending.get(i).equals(ascending.get(i - 1).plusDays(1))) {
                currentStreak++;
                maxStreak = Math.max(maxStreak, currentStreak);
            } else {
                currentStreak = 1;
            }
        }
        return maxStreak;
    }

    /**
     * Returns the total number of unique days with study activity.
     */
    public int totalStudyDays() {
        return studyDates.size();
    }
}
