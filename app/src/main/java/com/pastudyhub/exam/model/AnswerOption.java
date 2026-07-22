package com.pastudyhub.exam.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * One of 4–5 multiple-choice answer options for a Question.
 * Exactly one option per question should have isCorrect = true.
 */
@Entity
@Table(name = "answer_options",
        indexes = {
                @Index(name = "idx_answer_options_question_id", columnList = "question_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnswerOption {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(nullable = false, length = 1000)
    private String text;

    @Column(nullable = false)
    private boolean isCorrect;

    /**
     * Display order: A=0, B=1, C=2, D=3, E=4
     */
    @Column(nullable = false)
    private int orderIndex;
}
