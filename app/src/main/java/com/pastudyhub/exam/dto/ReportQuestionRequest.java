package com.pastudyhub.exam.dto;

import com.pastudyhub.exam.model.ReportReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportQuestionRequest {

    @NotNull(message = "Pick a reason")
    private ReportReason reason;

    @Size(max = 300, message = "The note must not exceed 300 characters")
    private String note;
}
