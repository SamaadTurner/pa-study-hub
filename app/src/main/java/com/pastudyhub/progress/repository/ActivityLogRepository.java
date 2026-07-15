package com.pastudyhub.progress.repository;

import com.pastudyhub.progress.model.ActivityLog;
import com.pastudyhub.progress.model.ActivityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, UUID> {

    List<ActivityLog> findByUserIdOrderByActivityDateDesc(UUID userId);

    List<ActivityLog> findByUserIdAndActivityDateBetweenOrderByActivityDateDesc(
            UUID userId, LocalDate start, LocalDate end);

    @Query("SELECT DISTINCT a.activityDate FROM ActivityLog a " +
           "WHERE a.userId = :userId ORDER BY a.activityDate DESC")
    List<LocalDate> findDistinctStudyDates(@Param("userId") UUID userId);

    @Query("SELECT DISTINCT a.activityDate FROM ActivityLog a " +
           "WHERE a.userId = :userId AND a.activityDate >= :since ORDER BY a.activityDate DESC")
    List<LocalDate> findDistinctStudyDatesSince(@Param("userId") UUID userId,
                                                 @Param("since") LocalDate since);

    @Query("SELECT SUM(a.totalCount) FROM ActivityLog a " +
           "WHERE a.userId = :userId AND a.activityDate = :date")
    Integer sumTotalCountForDate(@Param("userId") UUID userId, @Param("date") LocalDate date);

    @Query("SELECT SUM(a.durationMinutes) FROM ActivityLog a " +
           "WHERE a.userId = :userId AND a.activityDate = :date")
    Integer sumDurationMinutesForDate(@Param("userId") UUID userId, @Param("date") LocalDate date);

    List<ActivityLog> findByUserIdAndActivityType(UUID userId, ActivityType activityType);
}
