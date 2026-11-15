package com.pastudyhub.ai;

import com.pastudyhub.exam.exception.ExamNotCompletedException;
import com.pastudyhub.exam.exception.ExamSessionNotFoundException;
import com.pastudyhub.exam.exception.InvalidExamAnswerException;
import com.pastudyhub.exam.mapper.ExamMapper;
import com.pastudyhub.exam.model.AnswerOption;
import com.pastudyhub.exam.model.ExamAnswer;
import com.pastudyhub.exam.model.ExamQuestion;
import com.pastudyhub.exam.model.ExamSession;
import com.pastudyhub.exam.model.ExamStatus;
import com.pastudyhub.exam.model.Question;
import com.pastudyhub.exam.repository.ExamAnswerRepository;
import com.pastudyhub.exam.repository.ExamQuestionRepository;
import com.pastudyhub.exam.repository.ExamSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Writes the prompt for explaining one question of a finished exam.
 *
 * <p>It is its own class so the database work happens in one short read-only
 * transaction, before the slow call to the model, instead of holding a
 * database connection open while the model thinks.
 */
@Component
@RequiredArgsConstructor
public class ExplanationPromptBuilder {

    private final ExamSessionRepository examSessionRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final ExamAnswerRepository examAnswerRepository;
    private final ExamMapper examMapper;

    /**
     * @throws ExamSessionNotFoundException if the exam does not exist or belongs to someone else (404)
     * @throws ExamNotCompletedException    if the exam is not finished yet (409), because
     *                                      explaining the answer during the exam would give it away
     * @throws InvalidExamAnswerException   if the question is not part of this exam (400)
     */
    @Transactional(readOnly = true)
    public String build(UUID userId, UUID sessionId, UUID questionId) {
        ExamSession session = examSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ExamSessionNotFoundException(sessionId));

        if (session.getStatus() != ExamStatus.COMPLETED) {
            throw new ExamNotCompletedException(sessionId);
        }

        ExamQuestion examQuestion = examQuestionRepository.findByExamSessionIdAndQuestionId(sessionId, questionId)
                .orElseThrow(() -> new InvalidExamAnswerException("That question is not part of this exam"));
        Question question = examQuestion.getQuestion();

        AnswerOption picked = examAnswerRepository.findByExamSessionIdAndQuestionId(sessionId, questionId)
                .map(ExamAnswer::getSelectedOption)
                .orElse(null);

        // The same order and letters the user saw on the exam page
        List<AnswerOption> options = examMapper.optionsInExamOrder(sessionId, question);

        StringBuilder prompt = new StringBuilder();
        if (question.getClinicalVignette() != null && !question.getClinicalVignette().isBlank()) {
            prompt.append("Case: ").append(question.getClinicalVignette()).append("\n");
        }
        prompt.append("Question: ").append(question.getStem()).append("\n");

        String rightLetter = null;
        String pickedLetter = null;
        for (int i = 0; i < options.size(); i++) {
            AnswerOption option = options.get(i);
            String letter = String.valueOf((char) ('A' + i));
            prompt.append(letter).append(". ").append(option.getText()).append("\n");

            if (option.isCorrect()) {
                rightLetter = letter;
            }
            if (picked != null && option.getId().equals(picked.getId())) {
                pickedLetter = letter;
            }
        }

        prompt.append("Right answer: ").append(rightLetter).append("\n");
        if (pickedLetter == null) {
            prompt.append("The student did not answer this question.\n");
        } else {
            prompt.append("The student picked: ").append(pickedLetter).append("\n");
        }
        if (question.getExplanation() != null && !question.getExplanation().isBlank()) {
            prompt.append("Teaching note from the question bank: ").append(question.getExplanation()).append("\n");
        }

        return prompt.toString();
    }
}
