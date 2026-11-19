package com.pastudyhub.exam.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pastudyhub.exam.model.DifficultyLevel;
import com.pastudyhub.exam.model.QuestionCategory;
import com.pastudyhub.exam.model.TaskArea;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * A question as its author sees it, right answer included. Only ever sent to the
 * user who wrote the question.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WrittenQuestionResponse {
    private UUID id;
    private String clinicalVignette;
    private String stem;
    private List<AnswerOptionResponse> answerOptions;
    private String explanation;
    private QuestionCategory category;
    private TaskArea taskArea;
    private DifficultyLevel difficulty;
    @JsonProperty("isPublic")
    private boolean isPublic;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
