package com.pastudyhub.exam.service;

import com.pastudyhub.exam.dto.*;
import com.pastudyhub.exam.engine.QuestionRandomizer;
import com.pastudyhub.exam.engine.ScoreResult;
import com.pastudyhub.exam.engine.ScoringEngine;
import com.pastudyhub.exam.exception.ExamAlreadyCompletedException;
import com.pastudyhub.exam.exception.ExamNotCompletedException;
import com.pastudyhub.exam.exception.ExamSessionNotFoundException;
import com.pastudyhub.exam.exception.InsufficientQuestionsException;
import com.pastudyhub.exam.exception.InvalidExamAnswerException;
import com.pastudyhub.exam.mapper.ExamMapper;
import com.pastudyhub.exam.model.*;
import com.pastudyhub.exam.repository.AnswerOptionRepository;
import com.pastudyhub.exam.repository.ExamAnswerRepository;
import com.pastudyhub.exam.repository.ExamQuestionRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExamServiceImpl implements ExamService {

    private final QuestionRepository questionRepository;
    private final ExamSessionRepository examSessionRepository;
    private final ExamAnswerRepository examAnswerRepository;
    private final ExamQuestionRepository examQuestionRepository;
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
    @Transactional(readOnly = true)
    public ExamSessionResponse getExamSession(UUID sessionId, UUID userId) {
        ExamSession session = findSessionForUser(sessionId, userId);
        return buildSessionResponse(session);
    }

    @Override
    @Transactional
    public ExamSessionResponse submitAnswer(UUID sessionId, UUID questionId,
                                             SubmitAnswerRequest request, UUID userId) {
        ExamSession session = findSessionForUser(sessionId, userId);

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

        // Score against every question of the exam. Unanswered ones count as wrong,
        // and an exam can be finished without any answers (for example when time runs out).
        List<Question> questions = findQuestionsOfExam(sessionId);
        List<ExamAnswer> answers = examAnswerRepository.findByExamSessionId(sessionId);
        ScoringEngine engine = new ScoringEngine(questions, answers);
        ScoreResult result = engine.calculate();

        session.setStatus(ExamStatus.COMPLETED);
        session.setScore(result.rawScore());
        session.setScorePercent(result.scorePercent());
        session.setCompletedAt(LocalDateTime.now());
        session.setDurationSeconds(
                (int) ChronoUnit.SECONDS.between(session.getStartedAt(), session.getCompletedAt()));

        examSessionRepository.save(session);

        log.info("Exam completed: sessionId={}, userId={}, score={}/{} ({}%)",
                sessionId, userId, result.rawScore(), result.totalQuestions(), result.scorePercent());

        return examMapper.toResultResponse(session, result, questions, answers);
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
        // Only a finished exam has results. One that is still running or was abandoned does not.
        if (session.getStatus() != ExamStatus.COMPLETED) {
            throw new ExamNotCompletedException(sessionId);
        }
        List<Question> questions = findQuestionsOfExam(sessionId);
        List<ExamAnswer> answers = examAnswerRepository.findByExamSessionId(sessionId);
        ScoringEngine engine = new ScoringEngine(questions, answers);
        ScoreResult result = engine.calculate();
        return examMapper.toResultResponse(session, result, questions, answers);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExamHistorySummary> getExamHistory(UUID userId, int page, int size) {
        return examSessionRepository.findByUserIdOrderByStartedAtDesc(userId, PageRequest.of(page, size))
                .map(examMapper::toHistorySummary);
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
