package com.pastudyhub.progress.service;

import com.pastudyhub.progress.dto.*;
import com.pastudyhub.progress.engine.PerformanceAnalyzer;
import com.pastudyhub.progress.engine.StreakCalculator;
import com.pastudyhub.progress.model.ActivityLog;
import com.pastudyhub.progress.model.DailyGoal;
import com.pastudyhub.progress.repository.ActivityLogRepository;
import com.pastudyhub.progress.repository.DailyGoalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProgressService {

    private final ActivityLogRepository activityLogRepository;
    private final DailyGoalRepository dailyGoalRepository;

    /**
     * Logs an activity event. Called by other services (fire-and-forget POST).
     */
    @Transactional
    public void logActivity(LogActivityRequest request) {
        ActivityLog log = ActivityLog.builder()
                .userId(request.getUserId())
                .activityType(request.getActivityType())
                .category(request.getCategory())
                .durationMinutes(request.getDurationMinutes())
                .cardsReviewed(request.getCardsReviewed())
                .correctCount(request.getCorrectCount())
                .totalCount(request.getTotalCount())
                .activityDate(LocalDate.now())
                .build();
        activityLogRepository.save(log);
        this.log.debug("Activity logged: userId={}, type={}, category={}",
                request.getUserId(), request.getActivityType(), request.getCategory());
    }

    /**
     * Builds the full progress dashboard for a user.
     */
    @Transactional(readOnly = true)
    public ProgressDashboardResponse getDashboard(UUID userId) {
        List<ActivityLog> allLogs = activityLogRepository.findByUserIdOrderByActivityDateDesc(userId);
        List<LocalDate> studyDates = activityLogRepository.findDistinctStudyDates(userId);

        StreakCalculator streakCalc = new StreakCalculator(studyDates, LocalDate.now());
        PerformanceAnalyzer analyzer = new PerformanceAnalyzer(allLogs);

        DailyGoal goal = dailyGoalRepository.findByUserId(userId)
                .orElseGet(() -> DailyGoal.builder().userId(userId).build());

        GoalProgressResponse todayGoal = buildTodayGoal(userId, goal);
        List<DailyProgressPoint> last30Days = buildLast30Days(userId, allLogs, goal);

        return ProgressDashboardResponse.builder()
                .currentStreak(streakCalc.calculateCurrentStreak())
                .longestStreak(streakCalc.calculateLongestStreak())
                .totalStudyDays(streakCalc.totalStudyDays())
                .totalStudyMinutes(analyzer.totalStudyMinutes())
                .totalCardsReviewed(analyzer.totalItemsReviewed())
                .overallAccuracy(analyzer.overallAccuracy())
                .weakestCategory(analyzer.weakestCategory())
                .categoryAccuracy(analyzer.categoryAccuracy())
                .last30DaysActivity(last30Days)
                .todayGoal(todayGoal)
                .build();
    }

    /**
     * Returns or creates the daily goal for a user.
     */
    @Transactional(readOnly = true)
    public GoalProgressResponse getGoalProgress(UUID userId) {
        DailyGoal goal = dailyGoalRepository.findByUserId(userId)
                .orElseGet(() -> DailyGoal.builder().userId(userId).build());
        return buildTodayGoal(userId, goal);
    }

    /**
     * Upserts the user's daily goal settings.
     */
    @Transactional
    public GoalProgressResponse updateGoal(UUID userId, UpdateGoalRequest request) {
        DailyGoal goal = dailyGoalRepository.findByUserId(userId)
                .orElseGet(() -> DailyGoal.builder().userId(userId).build());
        goal.setTargetCardsPerDay(request.getTargetCardsPerDay());
        goal.setTargetMinutesPerDay(request.getTargetMinutesPerDay());
        dailyGoalRepository.save(goal);
        return buildTodayGoal(userId, goal);
    }

    // ---- Private helpers ---------------------------------------------------

    private GoalProgressResponse buildTodayGoal(UUID userId, DailyGoal goal) {
        LocalDate today = LocalDate.now();
        int cardsToday = orZero(activityLogRepository.sumTotalCountForDate(userId, today));
        int minutesToday = orZero(activityLogRepository.sumDurationMinutesForDate(userId, today));

        double cardPct = goal.getTargetCardsPerDay() > 0
                ? Math.min(100.0, (double) cardsToday / goal.getTargetCardsPerDay() * 100)
                : 0;
        double timePct = goal.getTargetMinutesPerDay() > 0
                ? Math.min(100.0, (double) minutesToday / goal.getTargetMinutesPerDay() * 100)
                : 0;

        return GoalProgressResponse.builder()
                .targetCardsPerDay(goal.getTargetCardsPerDay())
                .targetMinutesPerDay(goal.getTargetMinutesPerDay())
                .cardsReviewedToday(cardsToday)
                .minutesStudiedToday(minutesToday)
                .cardGoalMet(cardsToday >= goal.getTargetCardsPerDay())
                .timeGoalMet(minutesToday >= goal.getTargetMinutesPerDay())
                .cardGoalPercent(Math.round(cardPct * 10.0) / 10.0)
                .timeGoalPercent(Math.round(timePct * 10.0) / 10.0)
                .build();
    }

    private List<DailyProgressPoint> buildLast30Days(UUID userId, List<ActivityLog> allLogs, DailyGoal goal) {
        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysAgo = today.minusDays(29);

        // Group logs by date
        var logsByDate = allLogs.stream()
                .filter(l -> !l.getActivityDate().isBefore(thirtyDaysAgo))
                .collect(Collectors.groupingBy(ActivityLog::getActivityDate));

        List<DailyProgressPoint> points = new ArrayList<>();
        for (int i = 29; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            List<ActivityLog> dayLogs = logsByDate.getOrDefault(date, List.of());

            int cards = dayLogs.stream().mapToInt(ActivityLog::getTotalCount).sum();
            int minutes = dayLogs.stream().mapToInt(ActivityLog::getDurationMinutes).sum();
            int correct = dayLogs.stream().mapToInt(ActivityLog::getCorrectCount).sum();
            double accuracy = cards > 0 ? Math.round((double) correct / cards * 1000.0) / 10.0 : 0.0;
            boolean goalMet = cards >= goal.getTargetCardsPerDay() || minutes >= goal.getTargetMinutesPerDay();

            points.add(DailyProgressPoint.builder()
                    .date(date)
                    .cardsReviewed(cards)
                    .minutesStudied(minutes)
                    .accuracy(accuracy)
                    .goalMet(goalMet)
                    .build());
        }
        return points;
    }

    private int orZero(Integer value) {
        return value != null ? value : 0;
    }
}
