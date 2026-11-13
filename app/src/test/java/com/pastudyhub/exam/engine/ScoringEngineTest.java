package com.pastudyhub.exam.engine;

import com.pastudyhub.exam.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ScoringEngine unit tests")
class ScoringEngineTest {

    // ---- Helpers -----------------------------------------------------------

    private ExamAnswer answer(QuestionCategory category, boolean correct) {
        Question question = Question.builder()
                .id(UUID.randomUUID())
                .category(category)
                .difficulty(DifficultyLevel.MEDIUM)
                .stem("Test question")
                .explanation("Test explanation")
                .isActive(true)
                .build();

        return ExamAnswer.builder()
                .id(UUID.randomUUID())
                .question(question)
                .isCorrect(correct)
                .timeSpentSeconds(30)
                .build();
    }

    private ExamAnswer answerWithTime(QuestionCategory category, boolean correct, int seconds) {
        ExamAnswer a = answer(category, correct);
        a.setTimeSpentSeconds(seconds);
        return a;
    }

    private Question question(QuestionCategory category) {
        return Question.builder()
                .id(UUID.randomUUID())
                .category(category)
                .difficulty(DifficultyLevel.MEDIUM)
                .stem("Test question")
                .explanation("Test explanation")
                .isActive(true)
                .build();
    }

    // An exam where every question was answered: the questions are the answered ones
    private ScoringEngine engineFor(List<ExamAnswer> answers) {
        List<Question> questions = new ArrayList<>();
        for (ExamAnswer answer : answers) {
            questions.add(answer.getQuestion());
        }
        return new ScoringEngine(questions, answers);
    }

    // ---- Constructor -------------------------------------------------------

    @Test
    @DisplayName("Constructor throws for null questions or null answers")
    void constructor_nulls_throw() {
        assertThatThrownBy(() -> new ScoringEngine(null, new ArrayList<>()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ScoringEngine(new ArrayList<>(), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---- Unanswered questions ----------------------------------------------

    @Test
    @DisplayName("An exam with no answers scores 0 instead of failing")
    void noAnswers_scoresZero() {
        List<Question> questions = List.of(
                question(QuestionCategory.CARDIOLOGY),
                question(QuestionCategory.CARDIOLOGY),
                question(QuestionCategory.PULMONOLOGY)
        );

        ScoreResult result = new ScoringEngine(questions, new ArrayList<>()).calculate();

        assertThat(result.rawScore()).isEqualTo(0);
        assertThat(result.totalQuestions()).isEqualTo(3);
        assertThat(result.scorePercent()).isEqualTo(0.0);
        assertThat(result.performanceBand()).isEqualTo(PerformanceBand.NEEDS_IMPROVEMENT);
        assertThat(result.avgTimePerQuestionSeconds()).isEqualTo(0.0);
        assertThat(result.incorrectQuestionIds()).hasSize(3);
    }

    @Test
    @DisplayName("Unanswered questions count as wrong in the score")
    void unanswered_countAsWrong() {
        ExamAnswer right = answer(QuestionCategory.CARDIOLOGY, true);
        ExamAnswer alsoRight = answer(QuestionCategory.CARDIOLOGY, true);
        Question skipped = question(QuestionCategory.CARDIOLOGY);
        Question alsoSkipped = question(QuestionCategory.CARDIOLOGY);

        List<Question> questions = List.of(
                right.getQuestion(), alsoRight.getQuestion(), skipped, alsoSkipped);

        ScoreResult result = new ScoringEngine(questions, List.of(right, alsoRight)).calculate();

        // 2 right out of 4 questions, not 2 out of the 2 that were answered
        assertThat(result.rawScore()).isEqualTo(2);
        assertThat(result.totalQuestions()).isEqualTo(4);
        assertThat(result.scorePercent()).isEqualTo(50.0);
        assertThat(result.incorrectCount()).isEqualTo(2);
        assertThat(result.incorrectQuestionIds())
                .containsExactlyInAnyOrder(skipped.getId(), alsoSkipped.getId());
    }

    @Test
    @DisplayName("A category where nothing was answered shows up as 0%")
    void unansweredCategory_isZeroPercent() {
        ExamAnswer cardioRight = answer(QuestionCategory.CARDIOLOGY, true);
        Question pulmSkipped = question(QuestionCategory.PULMONOLOGY);

        Map<String, Double> breakdown = new ScoringEngine(
                List.of(cardioRight.getQuestion(), pulmSkipped), List.of(cardioRight))
                .calculateCategoryBreakdown();

        assertThat(breakdown.get("CARDIOLOGY")).isEqualTo(100.0);
        assertThat(breakdown.get("PULMONOLOGY")).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Average time only uses the questions that were answered")
    void avgTime_ignoresUnanswered() {
        ExamAnswer answered = answerWithTime(QuestionCategory.CARDIOLOGY, true, 40);
        Question skipped = question(QuestionCategory.CARDIOLOGY);

        double avg = new ScoringEngine(List.of(answered.getQuestion(), skipped), List.of(answered))
                .calculateAvgTimePerQuestion();

        assertThat(avg).isEqualTo(40.0);
    }

    // ---- calculateRawScore -------------------------------------------------

    @Test
    @DisplayName("calculateRawScore: all correct")
    void rawScore_allCorrect() {
        List<ExamAnswer> answers = List.of(
                answer(QuestionCategory.CARDIOLOGY, true),
                answer(QuestionCategory.CARDIOLOGY, true),
                answer(QuestionCategory.CARDIOLOGY, true)
        );
        assertThat(engineFor(answers).calculateRawScore()).isEqualTo(3);
    }

    @Test
    @DisplayName("calculateRawScore: none correct")
    void rawScore_noneCorrect() {
        List<ExamAnswer> answers = List.of(
                answer(QuestionCategory.CARDIOLOGY, false),
                answer(QuestionCategory.CARDIOLOGY, false)
        );
        assertThat(engineFor(answers).calculateRawScore()).isEqualTo(0);
    }

    @Test
    @DisplayName("calculateRawScore: mixed correct and incorrect")
    void rawScore_mixed() {
        List<ExamAnswer> answers = List.of(
                answer(QuestionCategory.CARDIOLOGY, true),
                answer(QuestionCategory.CARDIOLOGY, false),
                answer(QuestionCategory.CARDIOLOGY, true),
                answer(QuestionCategory.CARDIOLOGY, false),
                answer(QuestionCategory.CARDIOLOGY, true)
        );
        assertThat(engineFor(answers).calculateRawScore()).isEqualTo(3);
    }

    // ---- calculateScorePercent ---------------------------------------------

    @Test
    @DisplayName("calculateScorePercent: 100% score")
    void scorePercent_perfect() {
        List<ExamAnswer> answers = List.of(
                answer(QuestionCategory.CARDIOLOGY, true),
                answer(QuestionCategory.CARDIOLOGY, true)
        );
        assertThat(engineFor(answers).calculateScorePercent()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("calculateScorePercent: 0% score")
    void scorePercent_zero() {
        List<ExamAnswer> answers = List.of(
                answer(QuestionCategory.CARDIOLOGY, false),
                answer(QuestionCategory.CARDIOLOGY, false)
        );
        assertThat(engineFor(answers).calculateScorePercent()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("calculateScorePercent: 60% score (3/5)")
    void scorePercent_sixty() {
        List<ExamAnswer> answers = List.of(
                answer(QuestionCategory.CARDIOLOGY, true),
                answer(QuestionCategory.CARDIOLOGY, true),
                answer(QuestionCategory.CARDIOLOGY, true),
                answer(QuestionCategory.CARDIOLOGY, false),
                answer(QuestionCategory.CARDIOLOGY, false)
        );
        assertThat(engineFor(answers).calculateScorePercent()).isEqualTo(60.0);
    }

    // ---- getPerformanceBand ------------------------------------------------

    @Test
    @DisplayName("getPerformanceBand: EXCELLENT at 80%+")
    void performanceBand_excellent() {
        List<ExamAnswer> answers = List.of(
                answer(QuestionCategory.CARDIOLOGY, true),
                answer(QuestionCategory.CARDIOLOGY, true),
                answer(QuestionCategory.CARDIOLOGY, true),
                answer(QuestionCategory.CARDIOLOGY, true),
                answer(QuestionCategory.CARDIOLOGY, false)  // 80%
        );
        assertThat(engineFor(answers).getPerformanceBand()).isEqualTo(PerformanceBand.EXCELLENT);
    }

    @Test
    @DisplayName("getPerformanceBand: GOOD at 70-79%")
    void performanceBand_good() {
        List<ExamAnswer> answers = List.of(
                answer(QuestionCategory.CARDIOLOGY, true),
                answer(QuestionCategory.CARDIOLOGY, true),
                answer(QuestionCategory.CARDIOLOGY, true),
                answer(QuestionCategory.CARDIOLOGY, true),
                answer(QuestionCategory.CARDIOLOGY, true),
                answer(QuestionCategory.CARDIOLOGY, true),
                answer(QuestionCategory.CARDIOLOGY, true),
                answer(QuestionCategory.CARDIOLOGY, false),
                answer(QuestionCategory.CARDIOLOGY, false),
                answer(QuestionCategory.CARDIOLOGY, false)  // 70%
        );
        assertThat(engineFor(answers).getPerformanceBand()).isEqualTo(PerformanceBand.GOOD);
    }

    @Test
    @DisplayName("getPerformanceBand: NEEDS_IMPROVEMENT below 60%")
    void performanceBand_needsImprovement() {
        List<ExamAnswer> answers = List.of(
                answer(QuestionCategory.CARDIOLOGY, true),
                answer(QuestionCategory.CARDIOLOGY, false),
                answer(QuestionCategory.CARDIOLOGY, false),
                answer(QuestionCategory.CARDIOLOGY, false),
                answer(QuestionCategory.CARDIOLOGY, false)  // 20%
        );
        assertThat(engineFor(answers).getPerformanceBand()).isEqualTo(PerformanceBand.NEEDS_IMPROVEMENT);
    }

    // ---- calculateCategoryBreakdown ----------------------------------------

    @Test
    @DisplayName("calculateCategoryBreakdown: returns per-category percentages")
    void categoryBreakdown_multipleCategories() {
        List<ExamAnswer> answers = List.of(
                answer(QuestionCategory.CARDIOLOGY, true),
                answer(QuestionCategory.CARDIOLOGY, true),
                answer(QuestionCategory.CARDIOLOGY, false),  // Cardiology: 2/3 = 66.7%
                answer(QuestionCategory.PHARMACOLOGY, true),
                answer(QuestionCategory.PHARMACOLOGY, true)  // Pharmacology: 2/2 = 100%
        );
        Map<String, Double> breakdown = engineFor(answers).calculateCategoryBreakdown();

        assertThat(breakdown).containsKey("CARDIOLOGY");
        assertThat(breakdown).containsKey("PHARMACOLOGY");
        assertThat(breakdown.get("PHARMACOLOGY")).isEqualTo(100.0);
        assertThat(breakdown.get("CARDIOLOGY")).isEqualTo(66.7);
    }

    // ---- calculateAvgTimePerQuestion ---------------------------------------

    @Test
    @DisplayName("calculateAvgTimePerQuestion: returns average seconds")
    void avgTimePerQuestion_correctAverage() {
        List<ExamAnswer> answers = List.of(
                answerWithTime(QuestionCategory.CARDIOLOGY, true, 30),
                answerWithTime(QuestionCategory.CARDIOLOGY, false, 60),
                answerWithTime(QuestionCategory.CARDIOLOGY, true, 90)  // avg = 60s
        );
        assertThat(engineFor(answers).calculateAvgTimePerQuestion()).isEqualTo(60.0);
    }

    // ---- calculate (full ScoreResult) ------------------------------------

    @Test
    @DisplayName("calculate: ScoreResult has correct total and incorrect counts")
    void calculate_scoreResultCounts() {
        List<ExamAnswer> answers = List.of(
                answer(QuestionCategory.CARDIOLOGY, true),
                answer(QuestionCategory.CARDIOLOGY, false),
                answer(QuestionCategory.CARDIOLOGY, false)
        );
        ScoreResult result = engineFor(answers).calculate();

        assertThat(result.rawScore()).isEqualTo(1);
        assertThat(result.totalQuestions()).isEqualTo(3);
        assertThat(result.incorrectCount()).isEqualTo(2);
        assertThat(result.scorePercent()).isEqualTo(33.3);
    }

    @Test
    @DisplayName("calculate: incorrectQuestionIds contains UUIDs of wrong answers only")
    void calculate_incorrectQuestionIds() {
        UUID wrongId1 = UUID.randomUUID();
        UUID wrongId2 = UUID.randomUUID();
        UUID correctId = UUID.randomUUID();

        Question correct = Question.builder().id(correctId).category(QuestionCategory.CARDIOLOGY)
                .difficulty(DifficultyLevel.EASY).stem("q").explanation("e").isActive(true).build();
        Question wrong1 = Question.builder().id(wrongId1).category(QuestionCategory.CARDIOLOGY)
                .difficulty(DifficultyLevel.EASY).stem("q").explanation("e").isActive(true).build();
        Question wrong2 = Question.builder().id(wrongId2).category(QuestionCategory.CARDIOLOGY)
                .difficulty(DifficultyLevel.EASY).stem("q").explanation("e").isActive(true).build();

        List<ExamAnswer> answers = List.of(
                ExamAnswer.builder().id(UUID.randomUUID()).question(correct).isCorrect(true).build(),
                ExamAnswer.builder().id(UUID.randomUUID()).question(wrong1).isCorrect(false).build(),
                ExamAnswer.builder().id(UUID.randomUUID()).question(wrong2).isCorrect(false).build()
        );

        ScoreResult result = engineFor(answers).calculate();
        assertThat(result.incorrectQuestionIds()).containsExactlyInAnyOrder(wrongId1, wrongId2);
        assertThat(result.incorrectQuestionIds()).doesNotContain(correctId);
    }
}
