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

    // A skipped question is simply never sent, so an answer always has an option
    @NotNull(message = "selectedOptionId is required")
    private UUID selectedOptionId;

    @Min(value = 0, message = "timeSpentSeconds cannot be negative")
    private int timeSpentSeconds;
}
