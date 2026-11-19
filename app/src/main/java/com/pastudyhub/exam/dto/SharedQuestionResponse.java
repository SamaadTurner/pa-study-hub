package com.pastudyhub.exam.dto;

import com.pastudyhub.exam.model.DifficultyLevel;
import com.pastudyhub.exam.model.QuestionCategory;
import com.pastudyhub.exam.model.TaskArea;

import java.util.List;
import java.util.UUID;

/** A question another user shared, for the browse page. The author is not shown. */
public record SharedQuestionResponse(
        UUID id,
        String clinicalVignette,
        String stem,
        List<AnswerOptionResponse> answerOptions,
        String explanation,
        QuestionCategory category,
        TaskArea taskArea,
        DifficultyLevel difficulty) {
}
