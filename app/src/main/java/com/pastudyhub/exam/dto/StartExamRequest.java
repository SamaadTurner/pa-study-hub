package com.pastudyhub.exam.dto;

import com.pastudyhub.exam.model.DifficultyLevel;
import com.pastudyhub.exam.model.QuestionCategory;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartExamRequest {

    @Min(value = 1, message = "Minimum 1 question")
    @Max(value = 120, message = "Maximum 120 questions per session")
    private int questionCount;

    /** Optional — null means all categories */
    private QuestionCategory categoryFilter;

    /** Optional — null means all difficulties */
    private DifficultyLevel difficultyFilter;

    /** 0 = untimed; positive = minutes */
    @Min(value = 0, message = "Time limit cannot be negative")
    @Max(value = 360, message = "Maximum 6-hour time limit")
    @Builder.Default
    private int timeLimitMinutes = 0;
}
