package com.pastudyhub.exam.engine;

import com.pastudyhub.exam.model.Question;
import com.pastudyhub.exam.model.TaskArea;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Picks exam questions so the mix of task areas follows the PANCE blueprint
 * percentages (for example about 18% "most likely diagnosis" questions).
 *
 * <p>Pure domain object, no Spring dependencies. The caller filters the pool
 * (category, difficulty, active) before handing it over.
 */
public class BlueprintSelector {

    private final Random random;

    public BlueprintSelector(Random random) {
        this.random = random;
    }

    /**
     * How many questions of each task area an exam of {@code count} questions should have.
     *
     * <p>Uses the largest remainder method: every area first gets the whole part of
     * its share, and the questions left over go to the areas with the biggest
     * fractions. A plain round() would not always add up to {@code count}.
     * Ties go to the area listed first in {@link TaskArea}.
     */
    public static Map<TaskArea, Integer> quotas(int count) {
        Map<TaskArea, Integer> quotas = new EnumMap<>(TaskArea.class);
        int handedOut = 0;
        for (TaskArea area : TaskArea.values()) {
            int whole = count * area.getBlueprintPercent() / 100;
            quotas.put(area, whole);
            handedOut += whole;
        }

        // The remainder of count * percent / 100, as a number out of 100
        List<TaskArea> byFraction = new ArrayList<>(List.of(TaskArea.values()));
        byFraction.sort(Comparator.comparingInt(
                (TaskArea area) -> count * area.getBlueprintPercent() % 100).reversed());

        for (int i = 0; i < count - handedOut; i++) {
            TaskArea area = byFraction.get(i);
            quotas.put(area, quotas.get(area) + 1);
        }
        return quotas;
    }

    /**
     * Returns {@code count} questions in random order, as close to the blueprint
     * mix as the pool allows.
     *
     * <p>When an area has fewer questions than its quota, the missing places are
     * filled with random questions from the other areas. The question bank is
     * small, so without this most blueprint exams could not start at all.
     *
     * @throws IllegalStateException if the pool has fewer than {@code count} questions
     */
    public List<Question> select(List<Question> pool, int count) {
        if (pool.size() < count) {
            throw new IllegalStateException(String.format(
                    "Not enough questions: requested %d but only %d available", count, pool.size()));
        }

        Map<TaskArea, List<Question>> byArea = new EnumMap<>(TaskArea.class);
        for (TaskArea area : TaskArea.values()) {
            byArea.put(area, new ArrayList<>());
        }
        for (Question question : pool) {
            byArea.get(question.getTaskArea()).add(question);
        }

        List<Question> picked = new ArrayList<>();
        List<Question> leftOver = new ArrayList<>();
        Map<TaskArea, Integer> quotas = quotas(count);

        for (TaskArea area : TaskArea.values()) {
            List<Question> questions = byArea.get(area);
            Collections.shuffle(questions, random);
            int take = Math.min(quotas.get(area), questions.size());
            picked.addAll(questions.subList(0, take));
            leftOver.addAll(questions.subList(take, questions.size()));
        }

        Collections.shuffle(leftOver, random);
        int missing = count - picked.size();
        picked.addAll(leftOver.subList(0, missing));

        // Mix the areas up, otherwise all the diagnosis questions would come in a row
        Collections.shuffle(picked, random);
        return picked;
    }
}
