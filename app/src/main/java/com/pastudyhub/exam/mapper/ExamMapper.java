package com.pastudyhub.exam.mapper;

import com.pastudyhub.exam.dto.*;
import com.pastudyhub.exam.engine.PerformanceBand;
import com.pastudyhub.exam.engine.ScoreResult;
import com.pastudyhub.exam.model.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    /**
     * Builds the exam as the user sees it while taking it: every question of the exam
     * in order, each with the option the user has picked so far (if any).
     */
    public ExamSessionResponse toSessionResponse(ExamSession session, List<Question> questions,
                                                 List<ExamAnswer> answers) {
        // Look up the picked option by question
        Map<UUID, UUID> pickedOptionByQuestion = new HashMap<>();
        for (ExamAnswer answer : answers) {
            if (answer.getSelectedOption() != null) {
                pickedOptionByQuestion.put(answer.getQuestion().getId(), answer.getSelectedOption().getId());
            }
        }

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
                        .map(q -> {
                            // never reveal the right answer during the exam
                            QuestionResponse response = toQuestionResponse(q, false);
                            response.setSelectedOptionId(pickedOptionByQuestion.get(q.getId()));
                            return response;
                        })
                        .toList())
                .answeredCount(pickedOptionByQuestion.size())
                .timeRemainingSeconds(secondsRemaining(session))
                .build();
    }

    // The page used to restart its countdown from the full time limit every time it
    // loaded, so the limit never really ran out. The server knows when the exam
    // started, so it says how much time is left.
    private Integer secondsRemaining(ExamSession session) {
        if (session.getTimeLimitMinutes() <= 0 || session.getStatus() != ExamStatus.IN_PROGRESS) {
            return null;
        }
        long secondsUsed = ChronoUnit.SECONDS.between(session.getStartedAt(), LocalDateTime.now());
        long secondsLeft = session.getTimeLimitMinutes() * 60L - secondsUsed;
        return (int) Math.max(0, secondsLeft);
    }

    /**
     * One question of a finished exam with the right answer shown.
     *
     * @param answer the user's answer to this question, or null if they never answered it
     */
    public ExamAnswerDetailResponse toAnswerDetail(Question q, ExamAnswer answer) {
        if (answer == null) {
            // Not answered: nothing was picked, and it counts as wrong
            return ExamAnswerDetailResponse.builder()
                    .questionId(q.getId())
                    .stem(q.getStem())
                    .clinicalVignette(q.getClinicalVignette())
                    .answerOptions(q.getAnswerOptions().stream()
                            .map(opt -> toAnswerOptionResponse(opt, true))
                            .toList())
                    .selectedOptionId(null)
                    .isCorrect(false)
                    .explanation(q.getExplanation())
                    .timeSpentSeconds(null)
                    .build();
        }

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
                                                List<Question> questions, List<ExamAnswer> answers) {
        PerformanceBand band = scoreResult.performanceBand();

        Map<UUID, ExamAnswer> answerByQuestion = new HashMap<>();
        for (ExamAnswer answer : answers) {
            answerByQuestion.put(answer.getQuestion().getId(), answer);
        }
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
                // One entry per question of the exam, in exam order, answered or not
                .answerDetails(questions.stream()
                        .map(q -> toAnswerDetail(q, answerByQuestion.get(q.getId())))
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
