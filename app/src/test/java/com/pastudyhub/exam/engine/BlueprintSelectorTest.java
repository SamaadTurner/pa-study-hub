package com.pastudyhub.exam.engine;

import com.pastudyhub.exam.model.DifficultyLevel;
import com.pastudyhub.exam.model.Question;
import com.pastudyhub.exam.model.QuestionCategory;
import com.pastudyhub.exam.model.TaskArea;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("BlueprintSelector")
class BlueprintSelectorTest {

    @Test
    @DisplayName("The 2025 blueprint percentages add up to 100")
    void percentagesAddUpTo100() {
        int total = 0;
        for (TaskArea area : TaskArea.values()) {
            total += area.getBlueprintPercent();
        }
        assertThat(total).isEqualTo(100);
    }

    @Test
    @DisplayName("A 100 question exam gets exactly the blueprint percentages")
    void quotasFor100_matchPercentages() {
        Map<TaskArea, Integer> quotas = BlueprintSelector.quotas(100);

        for (TaskArea area : TaskArea.values()) {
            assertThat(quotas.get(area)).isEqualTo(area.getBlueprintPercent());
        }
    }

    @Test
    @DisplayName("A 10 question exam hands the leftover places to the biggest fractions")
    void quotasFor10() {
        Map<TaskArea, Integer> quotas = BlueprintSelector.quotas(10);

        // Worked out by hand: everything gets the whole part of 10 * percent / 100,
        // then the four leftover places go to the fractions .8, .8, .6 and .6
        // (professional practice also has .6 but comes later in the list)
        assertThat(quotas).containsExactlyInAnyOrderEntriesOf(Map.of(
                TaskArea.HISTORY_AND_PHYSICAL, 2,
                TaskArea.DIAGNOSTIC_STUDIES, 1,
                TaskArea.MOST_LIKELY_DIAGNOSIS, 2,
                TaskArea.HEALTH_MAINTENANCE, 1,
                TaskArea.CLINICAL_INTERVENTION, 2,
                TaskArea.PHARMACEUTICAL_THERAPEUTICS, 1,
                TaskArea.BASIC_SCIENCE, 1,
                TaskArea.PROFESSIONAL_PRACTICE, 0));
    }

    @Test
    @DisplayName("A 5 question exam covers the five biggest shares")
    void quotasFor5() {
        Map<TaskArea, Integer> quotas = BlueprintSelector.quotas(5);

        assertThat(quotas.get(TaskArea.MOST_LIKELY_DIAGNOSIS)).isEqualTo(1);
        assertThat(quotas.get(TaskArea.HISTORY_AND_PHYSICAL)).isEqualTo(1);
        assertThat(quotas.get(TaskArea.CLINICAL_INTERVENTION)).isEqualTo(1);
        assertThat(quotas.get(TaskArea.PHARMACEUTICAL_THERAPEUTICS)).isEqualTo(1);
        assertThat(quotas.get(TaskArea.HEALTH_MAINTENANCE)).isEqualTo(1);
        assertThat(quotas.get(TaskArea.DIAGNOSTIC_STUDIES)).isZero();
    }

    @Test
    @DisplayName("The quotas always add up to the question count, from 1 to 120")
    void quotasAlwaysAddUp() {
        for (int count = 1; count <= 120; count++) {
            int total = BlueprintSelector.quotas(count).values().stream().mapToInt(Integer::intValue).sum();
            assertThat(total).as("total for %d questions", count).isEqualTo(count);
        }
    }

    @Test
    @DisplayName("With plenty of questions in every area, the exam follows the quotas exactly")
    void select_fullPool_followsQuotas() {
        List<Question> pool = new ArrayList<>();
        for (TaskArea area : TaskArea.values()) {
            pool.addAll(questions(area, 10));
        }

        List<Question> picked = new BlueprintSelector(new Random(42)).select(pool, 20);

        assertThat(picked).hasSize(20).doesNotHaveDuplicates();
        assertThat(countByArea(picked)).isEqualTo(BlueprintSelector.quotas(20));
    }

    @Test
    @DisplayName("When an area runs short, other questions fill its places")
    void select_shortArea_isFilledFromOthers() {
        // No professional practice or health maintenance questions at all,
        // and only one diagnosis question
        List<Question> pool = new ArrayList<>();
        pool.addAll(questions(TaskArea.MOST_LIKELY_DIAGNOSIS, 1));
        pool.addAll(questions(TaskArea.PHARMACEUTICAL_THERAPEUTICS, 10));
        pool.addAll(questions(TaskArea.CLINICAL_INTERVENTION, 10));

        List<Question> picked = new BlueprintSelector(new Random(7)).select(pool, 10);

        assertThat(picked).hasSize(10).doesNotHaveDuplicates();
        Map<TaskArea, Integer> counts = countByArea(picked);
        // the one diagnosis question is always used, the areas that have questions get at least their quota
        assertThat(counts.get(TaskArea.MOST_LIKELY_DIAGNOSIS)).isEqualTo(1);
        assertThat(counts.get(TaskArea.PHARMACEUTICAL_THERAPEUTICS)).isGreaterThanOrEqualTo(1);
        assertThat(counts.get(TaskArea.CLINICAL_INTERVENTION)).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("The questions come out mixed, not grouped by area")
    void select_mixesTheOrder() {
        List<Question> pool = new ArrayList<>();
        for (TaskArea area : TaskArea.values()) {
            pool.addAll(questions(area, 5));
        }

        List<Question> picked = new BlueprintSelector(new Random(1)).select(pool, 30);

        // Grouped by area, the task would change only 7 times from one question to the next
        int changes = 0;
        for (int i = 1; i < picked.size(); i++) {
            if (picked.get(i).getTaskArea() != picked.get(i - 1).getTaskArea()) {
                changes++;
            }
        }
        assertThat(changes).isGreaterThan(7);
    }

    @Test
    @DisplayName("Asking for more questions than the pool has fails")
    void select_poolTooSmall_throws() {
        List<Question> pool = questions(TaskArea.BASIC_SCIENCE, 3);

        assertThatThrownBy(() -> new BlueprintSelector(new Random()).select(pool, 4))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("requested 4 but only 3");
    }

    @Test
    @DisplayName("Every question in the pool can be used, nothing is picked twice")
    void select_wholePool() {
        List<Question> pool = new ArrayList<>();
        pool.addAll(questions(TaskArea.BASIC_SCIENCE, 4));
        pool.addAll(questions(TaskArea.HISTORY_AND_PHYSICAL, 2));

        List<Question> picked = new BlueprintSelector(new Random(3)).select(pool, 6);

        assertThat(new HashSet<>(picked)).isEqualTo(new HashSet<>(pool));
    }

    // ---- helpers -----------------------------------------------------------

    private List<Question> questions(TaskArea area, int howMany) {
        List<Question> questions = new ArrayList<>();
        for (int i = 0; i < howMany; i++) {
            questions.add(Question.builder()
                    .id(UUID.randomUUID())
                    .stem(area + " question " + i)
                    .category(QuestionCategory.CARDIOLOGY)
                    .taskArea(area)
                    .difficulty(DifficultyLevel.MEDIUM)
                    .explanation("Explanation")
                    .build());
        }
        return questions;
    }

    private Map<TaskArea, Integer> countByArea(List<Question> questions) {
        Map<TaskArea, Integer> counts = new EnumMap<>(TaskArea.class);
        for (TaskArea area : TaskArea.values()) {
            counts.put(area, 0);
        }
        for (Question question : questions) {
            counts.merge(question.getTaskArea(), 1, Integer::sum);
        }
        return counts;
    }
}
