package com.pastudyhub.exam.mapper;

import com.pastudyhub.exam.dto.*;
import com.pastudyhub.exam.engine.PerformanceBand;
import com.pastudyhub.exam.engine.ScoreResult;
import com.pastudyhub.exam.model.*;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Manual mapper for exam domain objects. No MapStruct to keep dependencies minimal.
 */
@Component
public class ExamMapper {

    public AnswerOptionResponse toAnswerOptionResponse(AnswerOption option, boolean revealAnswer) {
        return AnswerOptionResponse.builder()
                .id(option.getId())
                .text(option.getText())
                .orderIndex(option.getOrderIndex())
                .isCorrect(revealAnswer ? option.isCorrect() : null)
                .build();
    }

    public QuestionResponse toQuestionResponse(Question question, boolean revealAnswer) {
        return QuestionResponse.builder()
                .id(question.getId())
                .stem(question.getStem())
                .clinicalVignette(question.getClinicalVignette())
                .category(question.getCategory())
                .difficulty(question.getDifficulty())
                .explanation(revealAnswer ? question.getExplanation() : null)
                .answerOptions(question.getAnswerOptions().stream()
                        .map(opt -> toAnswerOptionResponse(opt, revealAnswer))
                        .toList())
                .build();
    }

    public ExamSessionResponse toSessionResponse(ExamSession session, List<Question> questions, int answeredCount) {
        return ExamSessionResponse.builder()
                .id(session.getId())
                .userId(session.getUserId())
                .questionCount(session.getQuestionCount())
                .timeLimitMinutes(session.getTimeLimitMinutes())
                .categoryFilter(session.getCategoryFilter())
                .difficultyFilter(session.getDifficultyFilter())
                .status(session.getStatus())
                .startedAt(session.getStartedAt())
                .completedAt(session.getCompletedAt())
                .questions(questions.stream()
                        .map(q -> toQuestionResponse(q, false))  // never reveal during exam
                        .toList())
                .answeredCount(answeredCount)
                .build();
    }

    public ExamAnswerDetailResponse toAnswerDetail(ExamAnswer answer) {
        Question q = answer.getQuestion();
        return ExamAnswerDetailResponse.builder()
                .questionId(q.getId())
                .stem(q.getStem())
                .clinicalVignette(q.getClinicalVignette())
                .answerOptions(q.getAnswerOptions().stream()
                        .map(opt -> toAnswerOptionResponse(opt, true))
                        .toList())
                .selectedOptionId(answer.getSelectedOption() != null ? answer.getSelectedOption().getId() : null)
                .isCorrect(answer.isCorrect())
                .explanation(q.getExplanation())
                .timeSpentSeconds(answer.getTimeSpentSeconds())
                .build();
    }

    public ExamResultResponse toResultResponse(ExamSession session, ScoreResult scoreResult,
                                                List<ExamAnswer> answers) {
        PerformanceBand band = scoreResult.performanceBand();
        return ExamResultResponse.builder()
                .sessionId(session.getId())
                .rawScore(scoreResult.rawScore())
                .totalQuestions(scoreResult.totalQuestions())
                .scorePercent(scoreResult.scorePercent())
                .performanceBand(band)
                .performanceMessage(band.getMessage())
                .categoryBreakdown(scoreResult.categoryBreakdown())
                .avgTimePerQuestionSeconds(scoreResult.avgTimePerQuestionSeconds())
                .durationSeconds(session.getDurationSeconds())
                .completedAt(session.getCompletedAt())
                .answerDetails(answers.stream()
                        .map(this::toAnswerDetail)
                        .toList())
                .build();
    }

    public ExamHistorySummary toHistorySummary(ExamSession session) {
        PerformanceBand band = null;
        if (session.getScorePercent() != null) {
            double pct = session.getScorePercent();
            if (pct >= 80) band = PerformanceBand.EXCELLENT;
            else if (pct >= 70) band = PerformanceBand.GOOD;
            else if (pct >= 60) band = PerformanceBand.PASSING;
            else band = PerformanceBand.NEEDS_IMPROVEMENT;
        }
        return ExamHistorySummary.builder()
                .id(session.getId())
                .questionCount(session.getQuestionCount())
                .score(session.getScore())
                .scorePercent(session.getScorePercent())
                .performanceBand(band)
                .categoryFilter(session.getCategoryFilter())
                .difficultyFilter(session.getDifficultyFilter())
                .status(session.getStatus())
                .startedAt(session.getStartedAt())
                .completedAt(session.getCompletedAt())
                .durationSeconds(session.getDurationSeconds())
                .build();
    }
}
