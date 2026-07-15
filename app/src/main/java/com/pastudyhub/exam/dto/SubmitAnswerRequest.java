package com.pastudyhub.exam.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitAnswerRequest {

    @NotNull(message = "selectedOptionId is required (use null UUID to indicate skipped question)")
    private UUID selectedOptionId;  // null = question skipped

    @Min(value = 0, message = "timeSpentSeconds cannot be negative")
    private int timeSpentSeconds;
}
