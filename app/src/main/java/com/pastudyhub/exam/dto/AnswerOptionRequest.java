package com.pastudyhub.exam.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One answer option in a question a user writes. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerOptionRequest {

    @NotBlank(message = "Every answer option needs text")
    @Size(max = 1000, message = "An answer option must not exceed 1000 characters")
    private String text;

    // Same naming problem as isPublic on decks: without this Jackson reads "correct"
    @JsonProperty("isCorrect")
    private boolean isCorrect;
}
