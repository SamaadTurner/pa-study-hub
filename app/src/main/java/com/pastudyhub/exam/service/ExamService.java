package com.pastudyhub.exam.service;

import com.pastudyhub.exam.dto.*;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface ExamService {
    ExamSessionResponse startExam(StartExamRequest request, UUID userId);
    ExamSessionResponse getExamSession(UUID sessionId, UUID userId);
    ExamSessionResponse submitAnswer(UUID sessionId, UUID questionId, SubmitAnswerRequest request, UUID userId);
    ExamResultResponse completeExam(UUID sessionId, UUID userId);
    void abandonExam(UUID sessionId, UUID userId);
    ExamResultResponse getExamResult(UUID sessionId, UUID userId);
    Page<ExamHistorySummary> getExamHistory(UUID userId, int page, int size);
}
