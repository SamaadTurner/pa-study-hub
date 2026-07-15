package com.pastudyhub.exam.repository;

import com.pastudyhub.exam.model.AnswerOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AnswerOptionRepository extends JpaRepository<AnswerOption, UUID> {
    List<AnswerOption> findByQuestionId(UUID questionId);
}
