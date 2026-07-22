package com.pastudyhub.exam.service;

import com.pastudyhub.exam.dto.*;
import com.pastudyhub.exam.engine.QuestionRandomizer;
import com.pastudyhub.exam.engine.ScoreResult;
import com.pastudyhub.exam.engine.ScoringEngine;
import com.pastudyhub.exam.exception.ExamAlreadyCompletedException;
import com.pastudyhub.exam.exception.ExamSessionNotFoundException;
import com.pastudyhub.exam.exception.InsufficientQuestionsException;
import com.pastudyhub.exam.mapper.ExamMapper;
import com.pastudyhub.exam.model.*;
import com.pastudyhub.exam.repository.AnswerOptionRepository;
import com.pastudyhub.exam.repository.ExamAnswerRepository;
import com.pastudyhub.exam.repository.ExamSessionRepository;
import com.pastudyhub.exam.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExamServiceImpl implements ExamService {

    private final QuestionRepository questionRepository;
    private final ExamSessionRepository examSessionRepository;
    private final ExamAnswerRepository examAnswerRepository;
    private final AnswerOptionRepository answerOptionRepository;
    private final ExamMapper examMapper;

    @Override
    @Transactional
    public ExamSessionResponse startExam(StartExamRequest request, UUID userId) {
        List<Question> pool = questionRepository.findActiveByFilters(
                request.getCategoryFilter(), request.getDifficultyFilter());

        QuestionRandomizer randomizer = new QuestionRandomizer(pool);
        List<Question> selected;
        try {
            selected = randomizer.select(request.getQuestionCount(),
                    request.getCategoryFilter(), request.getDifficultyFilter());
        } catch (IllegalStateException ex) {
            throw new InsufficientQuestionsException(request.getQuestionCount(), pool.size());
        }

        ExamSession session = ExamSession.builder()
                .userId(userId)
                .questionCount(request.getQuestionCount())
                .timeLimitMinutes(request.getTimeLimitMinutes())
                .categoryFilter(request.getCategoryFilter())
                .difficultyFilter(request.getDifficultyFilter())
                .status(ExamStatus.IN_PROGRESS)
                .build();

        ExamSession saved = examSessionRepository.save(session);
        log.info("Exam started: sessionId={}, userId={}, questionCount={}", saved.getId(), userId, selected.size());

        return examMapper.toSessionResponse(saved, selected, 0);
    }

    @Override
    @Transactional(readOnly = true)
    public ExamSessionResponse getExamSession(UUID sessionId, UUID userId) {
        ExamSession session = findSessionForUser(sessionId, userId);

        // Rebuild the question list from existing answers + ordering
        List<ExamAnswer> answers = examAnswerRepository.findByExamSessionId(sessionId);
        List<Question> questions = answers.stream()
                .map(ExamAnswer::getQuestion)
                .distinct()
                .toList();

        return examMapper.toSessionResponse(session, questions, answers.size());
    }

    @Override
    @Transactional
    public ExamSessionResponse submitAnswer(UUID sessionId, UUID questionId,
                                             SubmitAnswerRequest request, UUID userId) {
        ExamSession session = findSessionForUser(sessionId, userId);

        if (session.getStatus() != ExamStatus.IN_PROGRESS) {
            throw new ExamAlreadyCompletedException(sessionId);
        }

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ExamSessionNotFoundException(questionId));

        AnswerOption selectedOption = null;
        boolean correct = false;

        if (request.getSelectedOptionId() != null) {
            selectedOption = answerOptionRepository.findById(request.getSelectedOptionId())
                    .orElseThrow(() -> new ExamSessionNotFoundException(request.getSelectedOptionId()));
            correct = selectedOption.isCorrect();
        }

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
        examAnswerRepository.save(answer);

        List<ExamAnswer> allAnswers = examAnswerRepository.findByExamSessionId(sessionId);
        List<Question> questions = allAnswers.stream().map(ExamAnswer::getQuestion).distinct().toList();

        return examMapper.toSessionResponse(session, questions, allAnswers.size());
    }

    @Override
    @Transactional
    public ExamResultResponse completeExam(UUID sessionId, UUID userId) {
        ExamSession session = findSessionForUser(sessionId, userId);

        if (session.getStatus() != ExamStatus.IN_PROGRESS) {
            throw new ExamAlreadyCompletedException(sessionId);
        }

        List<ExamAnswer> answers = examAnswerRepository.findByExamSessionId(sessionId);
        ScoringEngine engine = new ScoringEngine(answers);
        ScoreResult result = engine.calculate();

        session.setStatus(ExamStatus.COMPLETED);
        session.setScore(result.rawScore());
        session.setScorePercent(result.scorePercent());
        session.setCompletedAt(LocalDateTime.now());
        session.setDurationSeconds(
                (int) ChronoUnit.SECONDS.between(session.getStartedAt(), session.getCompletedAt()));

        examSessionRepository.save(session);

        log.info("Exam completed: sessionId={}, userId={}, score={}/{}  ({:.1f}%)",
                sessionId, userId, result.rawScore(), result.totalQuestions(), result.scorePercent());

        return examMapper.toResultResponse(session, result, answers);
    }

    @Override
    @Transactional
    public void abandonExam(UUID sessionId, UUID userId) {
        ExamSession session = findSessionForUser(sessionId, userId);
        if (session.getStatus() == ExamStatus.IN_PROGRESS) {
            session.setStatus(ExamStatus.ABANDONED);
            session.setCompletedAt(LocalDateTime.now());
            examSessionRepository.save(session);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ExamResultResponse getExamResult(UUID sessionId, UUID userId) {
        ExamSession session = findSessionForUser(sessionId, userId);
        if (session.getStatus() == ExamStatus.IN_PROGRESS) {
            throw new ExamAlreadyCompletedException(sessionId);
        }
        List<ExamAnswer> answers = examAnswerRepository.findByExamSessionId(sessionId);
        ScoringEngine engine = new ScoringEngine(answers);
        ScoreResult result = engine.calculate();
        return examMapper.toResultResponse(session, result, answers);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExamHistorySummary> getExamHistory(UUID userId, int page, int size) {
        return examSessionRepository.findByUserIdOrderByStartedAtDesc(userId, PageRequest.of(page, size))
                .map(examMapper::toHistorySummary);
    }

    private ExamSession findSessionForUser(UUID sessionId, UUID userId) {
        return examSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ExamSessionNotFoundException(sessionId));
    }
}
