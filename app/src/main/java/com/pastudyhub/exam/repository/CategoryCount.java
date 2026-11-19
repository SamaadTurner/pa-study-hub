package com.pastudyhub.exam.repository;

import com.pastudyhub.exam.model.QuestionCategory;

/** How many questions one category has, as a grouped query returns it. */
public record CategoryCount(QuestionCategory category, long count) {
}
