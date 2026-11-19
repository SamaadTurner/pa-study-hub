package com.pastudyhub.exam.dto;

import java.util.Map;

/**
 * How many questions an exam for this user can pick from, in total and per
 * category (categories with none are left out).
 */
public record QuestionCountsResponse(long total, Map<String, Long> byCategory) {
}
