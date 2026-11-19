package com.pastudyhub.exam.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pastudyhub.exam.model.DifficultyLevel;
import com.pastudyhub.exam.model.QuestionCategory;
import com.pastudyhub.exam.model.TaskArea;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Request body for writing or editing a practice exam question.
 * That exactly one option is right is checked in the service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionRequest {

    @Size(max = 1000, message = "The patient scenario must not exceed 1000 characters")
    private String clinicalVignette;

    @NotBlank(message = "The question is required")
    @Size(max = 3000, message = "The question must not exceed 3000 characters")
    private String stem;

    @NotNull(message = "Pick between 2 and 5 answer options")
    @Size(min = 2, max = 5, message = "Pick between 2 and 5 answer options")
    @Valid
    @Builder.Default
    private List<AnswerOptionRequest> answerOptions = new ArrayList<>();

    @NotBlank(message = "The explanation is required")
    @Size(max = 2000, message = "The explanation must not exceed 2000 characters")
    private String explanation;

    @NotNull(message = "Category is required")
    private QuestionCategory category;

    @NotNull(message = "Task area is required")
    private TaskArea taskArea;

    @NotNull(message = "Difficulty is required")
    private DifficultyLevel difficulty;

    @JsonProperty("isPublic")
    private boolean isPublic;
}
