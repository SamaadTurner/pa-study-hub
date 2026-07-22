package com.pastudyhub.flashcard.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request body for POST /api/v1/cards/{cardId}/review.
 *
 * <p>Quality mapping from UI buttons:
 * <ul>
 *   <li>1 = "Again" (totally forgot)</li>
 *   <li>2 = "Hard" (incorrect, remembered when shown)</li>
 *   <li>4 = "Good" (correct with hesitation)</li>
 *   <li>5 = "Easy" (effortless recall)</li>
 * </ul>
 */
@Data
public class ReviewRequest {

    @NotNull(message = "Quality rating is required")
    @Min(value = 1, message = "Quality must be at least 1 (Again)")
    @Max(value = 5, message = "Quality must be at most 5 (Easy)")
    private Integer quality;
}
