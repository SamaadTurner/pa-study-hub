package com.pastudyhub.exam.mapper;

import com.pastudyhub.exam.model.AnswerOption;
import com.pastudyhub.exam.model.Question;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The order in which an exam shows a question's answer options.
 *
 * <p>The ids in these tests are fixed numbers, so the shuffles come out the same on
 * every run and the tests can't fail by bad luck.
 */
@DisplayName("Order of answer options in an exam")
class ExamMapperTest {

    private final ExamMapper mapper = new ExamMapper();

    // A question stored the way the question bank stores them: right answer first
    private Question questionWithId(long number) {
        Question question = Question.builder()
                .id(new UUID(0, number))
                .stem("Question " + number)
                .build();

        for (int i = 0; i < 4; i++) {
            question.getAnswerOptions().add(AnswerOption.builder()
                    .id(new UUID(number, i))
                    .question(question)
                    .text("Option " + i)
                    .isCorrect(i == 0)
                    .orderIndex(i)
                    .build());
        }
        return question;
    }

    private int positionOfRightAnswer(UUID examId, Question question) {
        List<AnswerOption> shown = mapper.optionsInExamOrder(examId, question);
        for (int position = 0; position < shown.size(); position++) {
            if (shown.get(position).isCorrect()) {
                return position;
            }
        }
        throw new AssertionError("The right answer is missing from the options");
    }

    @Test
    @DisplayName("Every option is still there, each one once")
    void shuffleKeepsAllOptions() {
        Question question = questionWithId(1);

        List<AnswerOption> shown = mapper.optionsInExamOrder(new UUID(7, 7), question);

        assertThat(shown).containsExactlyInAnyOrderElementsOf(question.getAnswerOptions());
    }

    @Test
    @DisplayName("The same exam always shows a question's options in the same order")
    void sameExam_sameOrder() {
        Question question = questionWithId(1);
        UUID examId = new UUID(7, 7);

        List<AnswerOption> first = mapper.optionsInExamOrder(examId, question);
        List<AnswerOption> second = mapper.optionsInExamOrder(examId, question);

        assertThat(second).isEqualTo(first);
    }

    @Test
    @DisplayName("Within one exam the right answer is not always option A")
    void rightAnswer_isNotAlwaysFirst() {
        UUID examId = new UUID(7, 7);
        Set<Integer> positions = new HashSet<>();

        for (long number = 1; number <= 10; number++) {
            positions.add(positionOfRightAnswer(examId, questionWithId(number)));
        }

        // With the stored order every one of these would be position 0
        assertThat(positions.size()).isGreaterThan(1);
    }

    @Test
    @DisplayName("Another exam shows the same question with its options in another order")
    void otherExam_otherOrder() {
        Question question = questionWithId(1);
        Set<Integer> positions = new HashSet<>();

        for (long exam = 1; exam <= 10; exam++) {
            positions.add(positionOfRightAnswer(new UUID(exam, exam), question));
        }

        assertThat(positions.size()).isGreaterThan(1);
    }

    @Test
    @DisplayName("The stored order of the question is left untouched")
    void shuffleDoesNotChangeTheQuestion() {
        Question question = questionWithId(1);

        mapper.optionsInExamOrder(new UUID(7, 7), question);

        assertThat(question.getAnswerOptions().get(0).isCorrect()).isTrue();
        assertThat(question.getAnswerOptions())
                .extracting(AnswerOption::getOrderIndex)
                .containsExactly(0, 1, 2, 3);
    }
}
