package com.pastudyhub.ai.usage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface AiUsageRepository extends JpaRepository<AiUsage, UUID> {

    Optional<AiUsage> findByUserIdAndUsageDate(UUID userId, LocalDate usageDate);

    /** All requests on one day, by every user together. */
    @Query("SELECT COALESCE(SUM(u.requestCount), 0) FROM AiUsage u WHERE u.usageDate = :day")
    long totalRequestsOn(@Param("day") LocalDate day);
}
