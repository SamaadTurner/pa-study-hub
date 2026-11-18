package com.pastudyhub.exam.repository;

import com.pastudyhub.exam.model.TaskArea;

/** One row of {@link ExamQuestionRepository#totalsByTaskArea}: questions asked and answered right. */
public record TaskAreaTotals(TaskArea taskArea, long total, long correct) {
}
