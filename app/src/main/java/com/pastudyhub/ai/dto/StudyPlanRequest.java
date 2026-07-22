package com.pastudyhub.ai.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public record StudyPlanRequest(

        @NotEmpty(message = "weakCategories must not be empty")
        @Size(max = 15, message = "weakCategories must have 15 or fewer entries")
        List<@NotBlank String> weakCategories,

        @Min(value = 1, message = "daysUntilExam must be at least 1")
        @Max(value = 365, message = "daysUntilExam must be 365 or fewer")
        int daysUntilExam,

        @Min(value = 15, message = "dailyMinutes must be at least 15")
        @Max(value = 480, message = "dailyMinutes must be 480 or fewer")
        int dailyMinutes
) {}
