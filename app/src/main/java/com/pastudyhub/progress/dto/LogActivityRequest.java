package com.pastudyhub.progress.dto;

import com.pastudyhub.progress.model.ActivityType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
public class LogActivityRequest {

    @NotNull(message = "userId is required")
    private UUID userId;

    @NotNull(message = "activityType is required")
    private ActivityType activityType;

    @NotBlank(message = "category is required")
    private String category;

    @Min(0)
    private int durationMinutes;

    @Min(0)
    private int cardsReviewed;

    @Min(0)
    private int correctCount;

    @Min(0)
    private int totalCount;
}
