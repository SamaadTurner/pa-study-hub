package com.pastudyhub.progress.engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StreakCalculator unit tests")
class StreakCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 2, 10);

    // ---- currentStreak ----------------------------------------------------

    @Test
    @DisplayName("currentStreak: 0 when no study dates")
    void currentStreak_emptyDates_zero() {
        StreakCalculator calc = new StreakCalculator(List.of(), TODAY);
        assertThat(calc.calculateCurrentStreak()).isEqualTo(0);
    }

    @Test
    @DisplayName("currentStreak: 1 when only studied today")
    void currentStreak_onlyToday_one() {
        StreakCalculator calc = new StreakCalculator(List.of(TODAY), TODAY);
        assertThat(calc.calculateCurrentStreak()).isEqualTo(1);
    }

    @Test
    @DisplayName("currentStreak: 1 when only studied yesterday (today not yet studied)")
    void currentStreak_onlyYesterday_one() {
        StreakCalculator calc = new StreakCalculator(List.of(TODAY.minusDays(1)), TODAY);
        assertThat(calc.calculateCurrentStreak()).isEqualTo(1);
    }

    @Test
    @DisplayName("currentStreak: 0 when last study was 2+ days ago")
    void currentStreak_twoDaysAgo_zero() {
        StreakCalculator calc = new StreakCalculator(List.of(TODAY.minusDays(2)), TODAY);
        assertThat(calc.calculateCurrentStreak()).isEqualTo(0);
    }

    @Test
    @DisplayName("currentStreak: 5 for five consecutive days ending today")
    void currentStreak_fiveConsecutive_five() {
        List<LocalDate> dates = List.of(
                TODAY,
                TODAY.minusDays(1),
                TODAY.minusDays(2),
                TODAY.minusDays(3),
                TODAY.minusDays(4)
        );
        StreakCalculator calc = new StreakCalculator(dates, TODAY);
        assertThat(calc.calculateCurrentStreak()).isEqualTo(5);
    }

    @Test
    @DisplayName("currentStreak: breaks when there's a gap")
    void currentStreak_gap_breaksStreak() {
        List<LocalDate> dates = List.of(
                TODAY,
                TODAY.minusDays(1),
                // gap on day -2
                TODAY.minusDays(3),
                TODAY.minusDays(4)
        );
        StreakCalculator calc = new StreakCalculator(dates, TODAY);
        assertThat(calc.calculateCurrentStreak()).isEqualTo(2);
    }

    @Test
    @DisplayName("currentStreak: deduplicates dates (same day counted once)")
    void currentStreak_duplicateDates_deduplicates() {
        List<LocalDate> dates = List.of(TODAY, TODAY, TODAY.minusDays(1), TODAY.minusDays(1));
        StreakCalculator calc = new StreakCalculator(dates, TODAY);
        assertThat(calc.calculateCurrentStreak()).isEqualTo(2);
    }

    // ---- longestStreak ----------------------------------------------------

    @Test
    @DisplayName("longestStreak: 0 when no study dates")
    void longestStreak_emptyDates_zero() {
        StreakCalculator calc = new StreakCalculator(List.of(), TODAY);
        assertThat(calc.calculateLongestStreak()).isEqualTo(0);
    }

    @Test
    @DisplayName("longestStreak: 7 for historical 7-day streak even if current streak is shorter")
    void longestStreak_historicalBest() {
        List<LocalDate> dates = List.of(
                // Recent: 2-day streak
                TODAY,
                TODAY.minusDays(1),
                // Gap
                // Historical: 7-day streak
                TODAY.minusDays(30),
                TODAY.minusDays(31),
                TODAY.minusDays(32),
                TODAY.minusDays(33),
                TODAY.minusDays(34),
                TODAY.minusDays(35),
                TODAY.minusDays(36)
        );
        StreakCalculator calc = new StreakCalculator(dates, TODAY);
        assertThat(calc.calculateLongestStreak()).isEqualTo(7);
        assertThat(calc.calculateCurrentStreak()).isEqualTo(2);
    }

    // ---- totalStudyDays ---------------------------------------------------

    @Test
    @DisplayName("totalStudyDays: counts unique days only")
    void totalStudyDays_uniqueDays() {
        List<LocalDate> dates = List.of(
                TODAY,
                TODAY,  // duplicate
                TODAY.minusDays(1),
                TODAY.minusDays(5)
        );
        StreakCalculator calc = new StreakCalculator(dates, TODAY);
        assertThat(calc.totalStudyDays()).isEqualTo(3);
    }
}
