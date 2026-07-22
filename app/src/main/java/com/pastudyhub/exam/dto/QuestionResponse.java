package com.pastudyhub.exam.dto;

import com.pastudyhub.exam.model.DifficultyLevel;
import com.pastudyhub.exam.model.QuestionCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionResponse {
    private UUID id;
    private String stem;
    private String clinicalVignette;
    private QuestionCategory category;
    private DifficultyLevel difficulty;
    private List<AnswerOptionResponse> answerOptions;
    /** Only populated after exam is completed. */
    private String explanation;
}
