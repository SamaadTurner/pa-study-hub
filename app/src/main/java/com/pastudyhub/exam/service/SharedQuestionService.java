package com.pastudyhub.exam.service;

import com.pastudyhub.exam.dto.AnswerOptionResponse;
import com.pastudyhub.exam.dto.QuestionCountsResponse;
import com.pastudyhub.exam.dto.ReportQuestionRequest;
import com.pastudyhub.exam.dto.SharedQuestionResponse;
import com.pastudyhub.exam.exception.QuestionNotFoundException;
import com.pastudyhub.exam.exception.QuestionReportException;
import com.pastudyhub.exam.model.AnswerOption;
import com.pastudyhub.exam.model.Question;
import com.pastudyhub.exam.model.QuestionCategory;
import com.pastudyhub.exam.model.QuestionReport;
import com.pastudyhub.exam.repository.CategoryCount;
import com.pastudyhub.exam.repository.ExamQuestionRepository;
import com.pastudyhub.exam.repository.QuestionReportRepository;
import com.pastudyhub.exam.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Questions users share with each other: reporting a bad one, browsing them, and
 * counting what an exam can pick from.
 *
 * <p>Nobody reviews shared questions, so other users keep them in check: a shared
 * question with {@link #REPORT_LIMIT} reports from different users is left out of
 * other people's exams. Its author still sees it, with the reports. Editing a
 * question that was in an exam saves a new version, which starts without reports.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SharedQuestionService {

    public static final long REPORT_LIMIT = 3;

    /**
     * Whether a shared question is left out of other people's exams. Reports on earlier
     * versions count too, so editing can't wipe them: a new version comes back to
     * shared exams (a real fix gets its chance), but if the earlier versions already
     * had 2 reports, 1 new report hides it again, and after 3 or more, any report
     * does. Same rule as QuestionRepository.NOT_HIDDEN_BY_REPORTS.
     */
    public static boolean isHiddenByReports(long ownReports, int earlierReports) {
        return ownReports > 0 && ownReports + earlierReports >= REPORT_LIMIT;
    }

    private final QuestionRepository questionRepository;
    private final QuestionReportRepository questionReportRepository;
    private final ExamQuestionRepository examQuestionRepository;

    @Transactional
    public void report(UUID questionId, UUID userId, ReportQuestionRequest request) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));

        // Only questions other users wrote and shared. Starter questions and your own
        // are not reported (your own you can just edit).
        if (question.getOwnerId() == null || question.isWrittenBy(userId) || !question.isPublic()) {
            throw new QuestionReportException("Only questions shared by other users can be reported",
                    HttpStatus.BAD_REQUEST);
        }
        if (!question.isActive()) {
            throw new QuestionReportException("The author has changed or removed this question already",
                    HttpStatus.CONFLICT);
        }
        // Only someone who had the question in a finished exam, so reports come from
        // people who actually read it
        if (!examQuestionRepository.wasInFinishedExamOf(questionId, userId)) {
            throw new QuestionReportException("You can report a question after it was in one of your finished exams",
                    HttpStatus.FORBIDDEN);
        }
        if (questionReportRepository.existsByQuestionIdAndReporterId(questionId, userId)) {
            throw new QuestionReportException("You already reported this question", HttpStatus.CONFLICT);
        }

        String note = request.getNote() == null || request.getNote().isBlank() ? null : request.getNote().trim();
        questionReportRepository.save(QuestionReport.builder()
                .question(question)
                .reporterId(userId)
                .reason(request.getReason())
                .note(note)
                .build());
        log.info("Shared question reported: questionId={}, reason={}", questionId, request.getReason());
    }

    @Transactional(readOnly = true)
    public Page<SharedQuestionResponse> browse(UUID userId, QuestionCategory category, String search,
                                               int page, int size) {
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        return questionRepository.findSharedByOthers(userId, REPORT_LIMIT, category, toLikePattern(search), pageRequest)
                .map(this::toSharedResponse);
    }

    // "Contains this text" for LIKE, the same way the public deck search does it:
    // % and _ are escaped with ! so they are searched for as plain characters.
    private String toLikePattern(String search) {
        if (search == null || search.isBlank()) {
            return "%";
        }
        String escaped = search.trim().toLowerCase(Locale.ROOT)
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return "%" + escaped + "%";
    }

    @Transactional(readOnly = true)
    public QuestionCountsResponse countExamPool(UUID userId, boolean includeShared) {
        Map<String, Long> byCategory = new LinkedHashMap<>();
        long total = 0;
        // In the order of the categories, so the setup page can show them as they come
        Map<QuestionCategory, Long> counts = new EnumMap<>(QuestionCategory.class);
        for (CategoryCount count : questionRepository.countExamPoolByCategory(userId, includeShared, REPORT_LIMIT)) {
            counts.put(count.category(), count.count());
        }
        for (Map.Entry<QuestionCategory, Long> entry : counts.entrySet()) {
            byCategory.put(entry.getKey().name(), entry.getValue());
            total += entry.getValue();
        }
        return new QuestionCountsResponse(total, byCategory);
    }

    private SharedQuestionResponse toSharedResponse(Question question) {
        List<AnswerOptionResponse> options = new ArrayList<>();
        for (AnswerOption option : question.getAnswerOptions()) {
            options.add(AnswerOptionResponse.builder()
                    .id(option.getId())
                    .text(option.getText())
                    .orderIndex(option.getOrderIndex())
                    .isCorrect(option.isCorrect())
                    .build());
        }
        return new SharedQuestionResponse(question.getId(), question.getClinicalVignette(), question.getStem(),
                options, question.getExplanation(), question.getCategory(), question.getTaskArea(),
                question.getDifficulty());
    }
}
