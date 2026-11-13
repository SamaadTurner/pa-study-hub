package com.pastudyhub.exam.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * One question that was picked for an exam session, and its place in the exam.
 *
 * <p>The questions are picked at random when the exam starts. They are saved here so
 * the exam can be loaded again later with the same questions in the same order, and
 * so a question the user never answered still counts when the exam is scored.
 */
@Entity
@Table(name = "exam_questions",
        uniqueConstraints = {
                @UniqueConstraint(name = "idx_exam_questions_session_question",
                        columnNames = {"exam_session_id", "question_id"})
        },
        indexes = {
                @Index(name = "idx_exam_questions_session_order", columnList = "exam_session_id, order_index")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_session_id", nullable = false)
    private ExamSession examSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    /** Place in the exam: 0 is the first question. */
    @Column(nullable = false)
    private int orderIndex;
}
