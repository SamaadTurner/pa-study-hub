package com.pastudyhub.exam.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Detailed per-question result included in the exam results view.
 * Reveals correct answer and explanation after exam completion.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamAnswerDetailResponse {
    private UUID questionId;
    private String stem;
    private String clinicalVignette;
    private List<AnswerOptionResponse> answerOptions;  // includes isCorrect=true
    private UUID selectedOptionId;
    private boolean isCorrect;
    private String explanation;
    private Integer timeSpentSeconds;
}
