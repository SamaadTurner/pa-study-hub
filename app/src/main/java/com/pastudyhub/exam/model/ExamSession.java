package com.pastudyhub.exam.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A timed exam session taken by a specific user.
 * Tracks the selected questions, answers submitted, score, and timing.
 */
@Entity
@Table(name = "exam_sessions",
        indexes = {
                @Index(name = "idx_exam_sessions_user_id", columnList = "user_id"),
                @Index(name = "idx_exam_sessions_started_at", columnList = "started_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    /** Number of questions requested by user at exam setup. */
    @Column(nullable = false)
    private int questionCount;

    /** Time limit in minutes (0 = untimed). */
    @Column(nullable = false)
    @Builder.Default
    private int timeLimitMinutes = 0;

    /** Optional category filter. NULL means all categories. */
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private QuestionCategory categoryFilter;

    /** Optional difficulty filter. NULL means all difficulties. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private DifficultyLevel difficultyFilter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ExamStatus status = ExamStatus.IN_PROGRESS;

    /** Raw score: number of correct answers. */
    @Column
    private Integer score;

    /** Score as a percentage (0–100). */
    @Column
    private Double scorePercent;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime startedAt;

    @Column
    private LocalDateTime completedAt;

    /** Actual time taken in seconds. */
    @Column
    private Integer durationSeconds;

    @OneToMany(mappedBy = "examSession", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ExamAnswer> answers = new ArrayList<>();
}
