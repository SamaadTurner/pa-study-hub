package com.pastudyhub.exam.service;

import com.pastudyhub.exam.dto.*;
import com.pastudyhub.exam.engine.BlueprintSelector;
import com.pastudyhub.exam.engine.ExamTimeLimit;
import com.pastudyhub.exam.engine.QuestionRandomizer;
import com.pastudyhub.exam.engine.ScoreResult;
import com.pastudyhub.exam.engine.ScoringEngine;
import com.pastudyhub.exam.engine.TaskAreaScore;
import com.pastudyhub.exam.exception.ExamAlreadyCompletedException;
import com.pastudyhub.exam.exception.ExamNotCompletedException;
import com.pastudyhub.exam.exception.ExamSessionNotFoundException;
import com.pastudyhub.exam.exception.ExamTimeUpException;
import com.pastudyhub.exam.exception.InsufficientQuestionsException;
import com.pastudyhub.exam.exception.InvalidExamAnswerException;
import com.pastudyhub.exam.mapper.ExamMapper;
import com.pastudyhub.exam.model.*;
import com.pastudyhub.exam.repository.AnswerOptionRepository;
import com.pastudyhub.exam.repository.ExamAnswerRepository;
import com.pastudyhub.exam.repository.ExamQuestionRepository;
import com.pastudyhub.exam.repository.ExamSessionRepository;
import com.pastudyhub.exam.repository.QuestionReportRepository;
import com.pastudyhub.exam.repository.QuestionRepository;
import com.pastudyhub.exam.repository.TaskAreaTotals;
import com.pastudyhub.progress.dto.LogActivityRequest;
import com.pastudyhub.progress.model.ActivityType;
import com.pastudyhub.progress.service.ProgressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExamServiceImpl implements ExamService {

    // Most study time one exam question can add (5 minutes), the same limit as a flashcard
    private static final int MAX_SECONDS_PER_QUESTION = 300;

    private final QuestionRepository questionRepository;
    private final ExamSessionRepository examSessionRepository;
    private final ExamAnswerRepository examAnswerRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final AnswerOptionRepository answerOptionRepository;
    private final ExamMapper examMapper;
    private final ProgressService progressService;
    private final QuestionReportRepository questionReportRepository;

    @Override
    @Transactional
    public ExamSessionResponse startExam(StartExamRequest request, UUID userId) {
        List<Question> pool = questionRepository.findExamPool(userId, request.isIncludeSharedQuestions(),
                SharedQuestionService.REPORT_LIMIT, request.getCategoryFilter(), request.getDifficultyFilter());

        List<Question> selected;
        try {
            if (request.isFollowBlueprint()) {
                selected = new BlueprintSelector(new Random()).select(pool, request.getQuestionCount());
            } else {
                QuestionRandomizer randomizer = new QuestionRandomizer(pool);
                selected = randomizer.select(request.getQuestionCount(),
                        request.getCategoryFilter(), request.getDifficultyFilter());
            }
        } catch (IllegalStateException ex) {
            throw new InsufficientQuestionsException(request.getQuestionCount(), pool.size());
        }

        ExamSession session = ExamSession.builder()
                .userId(userId)
                .questionCount(request.getQuestionCount())
                .timeLimitMinutes(request.getTimeLimitMinutes())
                .categoryFilter(request.getCategoryFilter())
                .difficultyFilter(request.getDifficultyFilter())
                .followsBlueprint(request.isFollowBlueprint())
                .status(ExamStatus.IN_PROGRESS)
                .build();

        // saveAndFlush so the start time is filled in before the response is built
        ExamSession saved = examSessionRepository.saveAndFlush(session);

        // Save the picked questions in order. Without this the exam could not be
        // loaded again, because nothing else records which questions were picked.
        List<ExamQuestion> examQuestions = new ArrayList<>();
        for (int i = 0; i < selected.size(); i++) {
            examQuestions.add(ExamQuestion.builder()
                    .examSession(saved)
                    .question(selected.get(i))
                    .orderIndex(i)
                    .build());
        }
        examQuestionRepository.saveAll(examQuestions);

        log.info("Exam started: sessionId={}, userId={}, questionCount={}", saved.getId(), userId, selected.size());

        return examMapper.toSessionResponse(saved, selected, List.of());
    }

    @Override
    @Transactional
    public ExamSessionResponse getExamSession(UUID sessionId, UUID userId) {
        ExamSession session = findSessionForUser(sessionId, userId);
        // An exam opened after its time ran out comes back finished, and the page
        // goes to the results
        finishIfTimeUp(session);
        return buildSessionResponse(session);
    }

    // noRollbackFor: a late answer hands the exam in and then refuses the answer.
    // Without it the exception would undo the hand-in as well.
    @Override
    @Transactional(noRollbackFor = ExamTimeUpException.class)
    public ExamSessionResponse submitAnswer(UUID sessionId, UUID questionId,
                                             SubmitAnswerRequest request, UUID userId) {
        ExamSession session = findSessionForUser(sessionId, userId);

        if (finishIfTimeUp(session)) {
            throw new ExamTimeUpException();
        }
        if (session.getStatus() != ExamStatus.IN_PROGRESS) {
            throw new ExamAlreadyCompletedException(sessionId);
        }

        // The question has to be one of the questions picked for this exam
        ExamQuestion examQuestion = examQuestionRepository
                .findByExamSessionIdAndQuestionId(sessionId, questionId)
                .orElseThrow(() -> new InvalidExamAnswerException("That question is not part of this exam"));
        Question question = examQuestion.getQuestion();

        // The option has to be one of that question's own options. Without this check
        // the right option of any other question could be sent and would count as correct.
        AnswerOption selectedOption = answerOptionRepository.findById(request.getSelectedOptionId())
                .orElseThrow(() -> new InvalidExamAnswerException("That answer option does not exist"));
        if (!selectedOption.getQuestion().getId().equals(questionId)) {
            throw new InvalidExamAnswerException("That answer option does not belong to this question");
        }
        boolean correct = selectedOption.isCorrect();

        // Upsert — allow re-answering a question during the same session
        ExamAnswer answer = examAnswerRepository
                .findByExamSessionIdAndQuestionId(sessionId, questionId)
                .orElseGet(() -> ExamAnswer.builder()
                        .examSession(session)
                        .question(question)
                        .build());

        answer.setSelectedOption(selectedOption);
        answer.setCorrect(correct);
        answer.setTimeSpentSeconds(request.getTimeSpentSeconds());
        examAnswerRepository.saveAndFlush(answer);

        return buildSessionResponse(session);
    }

    @Override
    @Transactional
    public ExamResultResponse completeExam(UUID sessionId, UUID userId) {
        ExamSession session = findSessionForUser(sessionId, userId);

        if (session.getStatus() != ExamStatus.IN_PROGRESS) {
            throw new ExamAlreadyCompletedException(sessionId);
        }
        return finish(session);
    }

    @Override
    @Transactional
    public boolean finishExamIfTimeUp(UUID sessionId) {
        return examSessionRepository.findById(sessionId)
                .map(this::finishIfTimeUp)
                .orElse(false);
    }

    // Hands the exam in when its time and the grace period are over. Returns true if it did.
    private boolean finishIfTimeUp(ExamSession session) {
        if (!ExamTimeLimit.isOver(session, LocalDateTime.now())) {
            return false;
        }
        finish(session);
        log.info("Exam handed in because the time ran out: sessionId={}", session.getId());
        return true;
    }

    /**
     * Scores the exam, marks it completed and logs the activity.
     *
     * <p>Every question of the exam counts. Unanswered ones count as wrong, and an
     * exam can be finished without any answers (for example when time runs out).
     */
    private ExamResultResponse finish(ExamSession session) {
        UUID sessionId = session.getId();
        List<Question> questions = findQuestionsOfExam(sessionId);
        List<ExamAnswer> answers = examAnswerRepository.findByExamSessionId(sessionId);
        ScoringEngine engine = new ScoringEngine(questions, answers);
        ScoreResult result = engine.calculate();

        session.setStatus(ExamStatus.COMPLETED);
        session.setScore(result.rawScore());
        session.setScorePercent(result.scorePercent());
        session.setCompletedAt(ExamTimeLimit.finishTime(session, LocalDateTime.now()));
        session.setDurationSeconds(
                (int) ChronoUnit.SECONDS.between(session.getStartedAt(), session.getCompletedAt()));

        // saveAndFlush so a version clash with another request shows up here,
        // before the activity is logged
        examSessionRepository.saveAndFlush(session);

        logExamActivity(session.getUserId(), questions, answers);

        log.info("Exam completed: sessionId={}, userId={}, score={}/{} ({}%)",
                sessionId, session.getUserId(), result.rawScore(), result.totalQuestions(), result.scorePercent());

        return markReported(examMapper.toResultResponse(session, result, questions, answers), session.getUserId());
    }

    // Marks the shared questions the user has reported, so the results page shows
    // "Reported" instead of the Report button
    private ExamResultResponse markReported(ExamResultResponse response, UUID userId) {
        List<UUID> shared = new ArrayList<>();
        for (ExamAnswerDetailResponse detail : response.getAnswerDetails()) {
            if (detail.getSource() == QuestionSource.ANOTHER_USER) {
                shared.add(detail.getQuestionId());
            }
        }
        if (shared.isEmpty()) {
            return response;
        }
        Set<UUID> reported = new HashSet<>(questionReportRepository.findReportedQuestionIds(userId, shared));
        for (ExamAnswerDetailResponse detail : response.getAnswerDetails()) {
            detail.setReportedByYou(reported.contains(detail.getQuestionId()));
        }
        return response;
    }

    @Override
    @Transactional
    public void abandonExam(UUID sessionId, UUID userId) {
        ExamSession session = findSessionForUser(sessionId, userId);
        // Once the time is up the exam is handed in, not abandoned
        if (finishIfTimeUp(session)) {
            return;
        }
        if (session.getStatus() == ExamStatus.IN_PROGRESS) {
            session.setStatus(ExamStatus.ABANDONED);
            session.setCompletedAt(LocalDateTime.now());
            examSessionRepository.save(session);
        }
    }

    @Override
    @Transactional
    public ExamResultResponse getExamResult(UUID sessionId, UUID userId) {
        ExamSession session = findSessionForUser(sessionId, userId);
        finishIfTimeUp(session);
        // Only a finished exam has results. One that is still running or was abandoned does not.
        if (session.getStatus() != ExamStatus.COMPLETED) {
            throw new ExamNotCompletedException(sessionId);
        }
        List<Question> questions = findQuestionsOfExam(sessionId);
        List<ExamAnswer> answers = examAnswerRepository.findByExamSessionId(sessionId);
        ScoringEngine engine = new ScoringEngine(questions, answers);
        ScoreResult result = engine.calculate();
        return markReported(examMapper.toResultResponse(session, result, questions, answers), userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExamHistorySummary> getExamHistory(UUID userId, int page, int size) {
        return examSessionRepository.findByUserIdOrderByStartedAtDesc(userId, PageRequest.of(page, size))
                .map(examMapper::toHistorySummary);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskAreaScore> getTaskAreaScores(UUID userId) {
        Map<TaskArea, TaskAreaTotals> totalsByArea = new EnumMap<>(TaskArea.class);
        for (TaskAreaTotals totals : examQuestionRepository.totalsByTaskArea(userId)) {
            totalsByArea.put(totals.taskArea(), totals);
        }

        // In blueprint order, and only the areas the user has had questions in
        List<TaskAreaScore> scores = new ArrayList<>();
        for (TaskAreaTotals totals : totalsByArea.values()) {
            scores.add(TaskAreaScore.of(totals.taskArea(), (int) totals.correct(), (int) totals.total()));
        }
        return scores;
    }

    /**
     * Records a finished exam in the activity log, so it counts toward the streak,
     * study time and accuracy on the dashboard. Exams used to leave no trace there.
     *
     * <p>One entry is saved per category in the exam, because the dashboard shows
     * accuracy per category and a mixed exam covers several.
     */
    private void logExamActivity(UUID userId, List<Question> questions, List<ExamAnswer> answers) {
        Set<QuestionCategory> categories = new LinkedHashSet<>();
        for (Question question : questions) {
            categories.add(question.getCategory());
        }

        for (QuestionCategory category : categories) {
            int total = 0;
            for (Question question : questions) {
                if (question.getCategory() == category) {
                    total++;
                }
            }

            int correct = 0;
            int seconds = 0;
            for (ExamAnswer answer : answers) {
                if (answer.getQuestion().getCategory() != category) {
                    continue;
                }
                if (answer.isCorrect()) {
                    correct++;
                }
                if (answer.getTimeSpentSeconds() != null) {
                    seconds += Math.min(answer.getTimeSpentSeconds(), MAX_SECONDS_PER_QUESTION);
                }
            }

            progressService.logActivity(LogActivityRequest.builder()
                    .userId(userId)
                    .activityType(ActivityType.EXAM_COMPLETED)
                    .category(category.name())
                    .durationSeconds(seconds)
                    .cardsReviewed(0)
                    .correctCount(correct)
                    .totalCount(total)
                    .build());
        }
    }

    // The questions that were picked for this exam, first question first
    private List<Question> findQuestionsOfExam(UUID sessionId) {
        List<Question> questions = new ArrayList<>();
        for (ExamQuestion examQuestion : examQuestionRepository.findBySessionInOrder(sessionId)) {
            questions.add(examQuestion.getQuestion());
        }
        return questions;
    }

    // All questions of the exam plus whatever the user has answered so far
    private ExamSessionResponse buildSessionResponse(ExamSession session) {
        List<Question> questions = findQuestionsOfExam(session.getId());
        List<ExamAnswer> answers = examAnswerRepository.findByExamSessionId(session.getId());
        return examMapper.toSessionResponse(session, questions, answers);
    }

    private ExamSession findSessionForUser(UUID sessionId, UUID userId) {
        return examSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ExamSessionNotFoundException(sessionId));
    }
}
