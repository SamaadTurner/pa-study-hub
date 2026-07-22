package com.pastudyhub.progress.dto;

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
public class UpdateGoalRequest {

    @Min(value = 1, message = "Minimum 1 card per day")
    @Max(value = 500, message = "Maximum 500 cards per day")
    private int targetCardsPerDay;

    @Min(value = 5, message = "Minimum 5 minutes per day")
    @Max(value = 480, message = "Maximum 8 hours per day")
    private int targetMinutesPerDay;
}
