package com.pastudyhub.exam.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerOptionResponse {
    private UUID id;
    private String text;
    private int orderIndex;
    /** Only included in results — never sent during active exam. */
    private Boolean isCorrect;
}
