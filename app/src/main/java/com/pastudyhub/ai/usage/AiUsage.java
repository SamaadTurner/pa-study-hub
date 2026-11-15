package com.pastudyhub.ai.usage;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/** How many AI requests one user made on one day (UTC). */
@Entity
@Table(name = "ai_usage",
        uniqueConstraints = {
                @UniqueConstraint(name = "idx_ai_usage_user_date", columnNames = {"user_id", "usage_date"})
        },
        indexes = {
                @Index(name = "idx_ai_usage_date", columnList = "usage_date")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    @Column(name = "request_count", nullable = false)
    private int requestCount;
}
