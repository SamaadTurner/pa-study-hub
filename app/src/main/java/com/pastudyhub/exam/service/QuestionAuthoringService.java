package com.pastudyhub.exam.service;

import com.pastudyhub.exam.dto.AnswerOptionRequest;
import com.pastudyhub.exam.dto.AnswerOptionResponse;
import com.pastudyhub.exam.dto.QuestionReportResponse;
import com.pastudyhub.exam.dto.QuestionRequest;
import com.pastudyhub.exam.dto.WrittenQuestionResponse;
import com.pastudyhub.exam.exception.InvalidQuestionException;
import com.pastudyhub.exam.exception.QuestionAccessDeniedException;
import com.pastudyhub.exam.exception.QuestionNotFoundException;
import com.pastudyhub.exam.model.AnswerOption;
import com.pastudyhub.exam.model.Question;
import com.pastudyhub.exam.model.QuestionReport;
import com.pastudyhub.exam.repository.ExamQuestionRepository;
import com.pastudyhub.exam.repository.QuestionReportRepository;
import com.pastudyhub.exam.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Practice exam questions that users write themselves.
 *
 * <p>Only the author can open, edit or delete a question. The starter questions
 * have no author, so nobody can change them through here.
 *
 * <p>Past exams point at the questions and answer options they used. A question
 * that is already part of an exam is therefore never changed or removed in place:
 * editing it saves a new question and retires the old one, and deleting it only
 * retires it. Old results keep showing what was actually asked.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionAuthoringService {

    private final QuestionRepository questionRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final QuestionReportRepository questionReportRepository;

    @Transactional(readOnly = true)
    public Page<WrittenQuestionResponse> getMyQuestions(UUID userId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        Page<Question> questions = questionRepository.findWrittenBy(userId, pageRequest);

        // The reports of the whole page in one query, grouped by question
        List<UUID> ids = questions.getContent().stream().map(Question::getId).toList();
        Map<UUID, List<QuestionReport>> reportsByQuestion = new HashMap<>();
        if (!ids.isEmpty()) {
            for (QuestionReport report : questionReportRepository.findByQuestionIds(ids)) {
                reportsByQuestion.computeIfAbsent(report.getQuestion().getId(), key -> new ArrayList<>()).add(report);
            }
        }
        return questions.map(q -> toResponse(q, reportsByQuestion.getOrDefault(q.getId(), List.of())));
    }

    @Transactional(readOnly = true)
    public WrittenQuestionResponse getMyQuestion(UUID questionId, UUID userId) {
        Question question = findOwnQuestion(questionId, userId);
        return toResponse(question, questionReportRepository.findByQuestionIds(List.of(questionId)));
    }

    @Transactional
    public WrittenQuestionResponse createQuestion(QuestionRequest request, UUID userId) {
        checkOptions(request.getAnswerOptions());

        Question question = new Question();
        question.setOwnerId(userId);
        copyRequestInto(request, question);

        // saveAndFlush so the dates are filled in for the response
        Question saved = questionRepository.saveAndFlush(question);
        log.info("Question written: questionId={}, userId={}", saved.getId(), userId);
        return toResponse(saved);
    }

    @Transactional
    public WrittenQuestionResponse updateQuestion(UUID questionId, QuestionRequest request, UUID userId) {
        checkOptions(request.getAnswerOptions());
        Question question = findOwnQuestion(questionId, userId);

        if (!examQuestionRepository.existsByQuestionId(questionId)) {
            // Never used in an exam, so it is safe to change it where it is
            question.getAnswerOptions().clear();
            copyRequestInto(request, question);
            return toResponse(questionRepository.saveAndFlush(question));
        }

        // Used in an exam: keep the old question for that exam's results
        question.setActive(false);
        questionRepository.save(question);

        Question newVersion = new Question();
        newVersion.setOwnerId(userId);
        copyRequestInto(request, newVersion);
        Question saved = questionRepository.saveAndFlush(newVersion);
        log.info("Question edited after use in an exam: old={}, new={}", questionId, saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public void deleteQuestion(UUID questionId, UUID userId) {
        Question question = findOwnQuestion(questionId, userId);
        if (examQuestionRepository.existsByQuestionId(questionId)) {
            question.setActive(false);
            questionRepository.save(question);
        } else {
            questionRepository.delete(question);
        }
    }

    private Question findOwnQuestion(UUID questionId, UUID userId) {
        Question question = questionRepository.findById(questionId)
                .filter(Question::isActive)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));
        if (!question.isWrittenBy(userId)) {
            throw new QuestionAccessDeniedException();
        }
        return question;
    }

    // Rules that the annotations on the request cannot express
    private void checkOptions(List<AnswerOptionRequest> options) {
        int rightOnes = 0;
        Set<String> seen = new HashSet<>();
        for (AnswerOptionRequest option : options) {
            if (option.isCorrect()) {
                rightOnes++;
            }
            // Two options with the same text would make the question impossible to answer
            if (!seen.add(option.getText().trim().toLowerCase(Locale.ROOT))) {
                throw new InvalidQuestionException("Two answer options have the same text");
            }
        }
        if (rightOnes != 1) {
            throw new InvalidQuestionException("Mark exactly one answer option as right");
        }
    }

    private void copyRequestInto(QuestionRequest request, Question question) {
        question.setClinicalVignette(blankToNull(request.getClinicalVignette()));
        question.setStem(request.getStem().trim());
        question.setExplanation(request.getExplanation().trim());
        question.setCategory(request.getCategory());
        question.setTaskArea(request.getTaskArea());
        question.setDifficulty(request.getDifficulty());
        question.setPublic(request.isPublic());
        question.setActive(true);

        // Options are stored in the order the author wrote them. Exams shuffle them.
        List<AnswerOptionRequest> options = request.getAnswerOptions();
        for (int i = 0; i < options.size(); i++) {
            question.getAnswerOptions().add(AnswerOption.builder()
                    .question(question)
                    .text(options.get(i).getText().trim())
                    .isCorrect(options.get(i).isCorrect())
                    .orderIndex(i)
                    .build());
        }
    }

    private String blankToNull(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return text.trim();
    }

    // A question that was just saved has no reports yet (a new version starts clean)
    private WrittenQuestionResponse toResponse(Question question) {
        return toResponse(question, List.of());
    }

    private WrittenQuestionResponse toResponse(Question question, List<QuestionReport> reports) {
        List<QuestionReportResponse> reportResponses = new ArrayList<>();
        for (QuestionReport report : reports) {
            reportResponses.add(new QuestionReportResponse(report.getReason(), report.getNote(), report.getCreatedAt()));
        }
        List<AnswerOptionResponse> options = new ArrayList<>();
        for (AnswerOption option : question.getAnswerOptions()) {
            options.add(AnswerOptionResponse.builder()
                    .id(option.getId())
                    .text(option.getText())
                    .orderIndex(option.getOrderIndex())
                    .isCorrect(option.isCorrect())
                    .build());
        }
        return WrittenQuestionResponse.builder()
                .id(question.getId())
                .clinicalVignette(question.getClinicalVignette())
                .stem(question.getStem())
                .answerOptions(options)
                .explanation(question.getExplanation())
                .category(question.getCategory())
                .taskArea(question.getTaskArea())
                .difficulty(question.getDifficulty())
                .isPublic(question.isPublic())
                .reports(reportResponses)
                .hiddenFromShared(question.isPublic() && reports.size() >= SharedQuestionService.REPORT_LIMIT)
                .createdAt(question.getCreatedAt())
                .updatedAt(question.getUpdatedAt())
                .build();
    }
}
