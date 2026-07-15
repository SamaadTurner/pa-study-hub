package com.pastudyhub.exam.engine;

import com.pastudyhub.exam.model.DifficultyLevel;
import com.pastudyhub.exam.model.Question;
import com.pastudyhub.exam.model.QuestionCategory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Selects a randomized subset of questions for an exam session.
 *
 * <p>Applies optional category and difficulty filters, then shuffles
 * the result and returns up to the requested count.
 *
 * <p>Pure domain object — no Spring dependencies.
 */
public class QuestionRandomizer {

    private final List<Question> questionPool;

    public QuestionRandomizer(List<Question> questionPool) {
        this.questionPool = new ArrayList<>(questionPool);
    }

    /**
     * Returns up to {@code count} randomly selected questions, optionally
     * filtered by category and/or difficulty.
     *
     * @throws IllegalStateException if the filtered pool has fewer questions than requested
     */
    public List<Question> select(int count, QuestionCategory categoryFilter, DifficultyLevel difficultyFilter) {
        List<Question> filtered = questionPool.stream()
                .filter(q -> categoryFilter == null || q.getCategory() == categoryFilter)
                .filter(q -> difficultyFilter == null || q.getDifficulty() == difficultyFilter)
                .filter(Question::isActive)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));

        if (filtered.size() < count) {
            throw new IllegalStateException(
                    String.format("Not enough questions: requested %d but only %d available with the given filters",
                            count, filtered.size()));
        }

        Collections.shuffle(filtered);
        return filtered.subList(0, count);
    }

    /**
     * Returns all available questions matching the filters (no count limit).
     */
    public List<Question> selectAll(QuestionCategory categoryFilter, DifficultyLevel difficultyFilter) {
        return questionPool.stream()
                .filter(q -> categoryFilter == null || q.getCategory() == categoryFilter)
                .filter(q -> difficultyFilter == null || q.getDifficulty() == difficultyFilter)
                .filter(Question::isActive)
                .toList();
    }
}
