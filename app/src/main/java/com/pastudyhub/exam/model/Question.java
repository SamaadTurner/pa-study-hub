package com.pastudyhub.exam.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A single multiple-choice question aligned to an NCCPA content blueprint category.
 *
 * <p>A starter question has no owner and can be used in anyone's exam. A question a
 * user wrote has that user as its owner, and is only used in other users' exams when
 * it is shared and they asked for shared questions.
 */
@Entity
@Table(name = "questions",
        indexes = {
                @Index(name = "idx_questions_category", columnList = "category"),
                @Index(name = "idx_questions_difficulty", columnList = "difficulty"),
                @Index(name = "idx_questions_task_area", columnList = "task_area"),
                @Index(name = "idx_questions_is_active", columnList = "is_active"),
                @Index(name = "idx_questions_owner_id", columnList = "owner_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 3000)
    private String stem;  // The question text

    @Column(length = 1000)
    private String clinicalVignette;  // Optional patient scenario prefix

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private QuestionCategory category;

    /** What the question tests (diagnosis, pharmacology, ...), the blueprint's second dimension. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TaskArea taskArea;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DifficultyLevel difficulty;

    @Column(nullable = false, length = 2000)
    private String explanation;  // Shown after answering

    @Column(nullable = false)
    @Builder.Default
    private boolean isActive = true;

    /** The user who wrote the question, or null for a starter question. */
    @Column(name = "owner_id")
    private UUID ownerId;

    /** True when the owner shares the question with other users. */
    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private boolean isPublic = false;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("orderIndex ASC")
    @Builder.Default
    private List<AnswerOption> answerOptions = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public boolean isWrittenBy(UUID userId) {
        return ownerId != null && ownerId.equals(userId);
    }

    /**
     * Returns the correct answer option, or null if none is marked correct.
     */
    public AnswerOption getCorrectOption() {
        return answerOptions.stream()
                .filter(AnswerOption::isCorrect)
                .findFirst()
                .orElse(null);
    }
}
