package com.pastudyhub.exam.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Records a single answer submitted by a user for one question in an exam session.
 */
@Entity
@Table(name = "exam_answers",
        indexes = {
                @Index(name = "idx_exam_answers_session_id", columnList = "exam_session_id"),
                @Index(name = "idx_exam_answers_question_id", columnList = "question_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_session_id", nullable = false)
    private ExamSession examSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    /** The option the user selected. NULL if skipped/unanswered. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_option_id")
    private AnswerOption selectedOption;

    @Column(nullable = false)
    private boolean isCorrect;

    /** Time taken to answer this question in seconds. */
    @Column
    private Integer timeSpentSeconds;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime answeredAt;
}
