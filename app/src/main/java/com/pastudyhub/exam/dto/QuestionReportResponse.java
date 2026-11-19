package com.pastudyhub.exam.dto;

import com.pastudyhub.exam.model.ReportReason;

import java.time.LocalDateTime;

/** A report as the question's author sees it. Who reported it is never shown. */
public record QuestionReportResponse(ReportReason reason, String note, LocalDateTime createdAt) {
}
