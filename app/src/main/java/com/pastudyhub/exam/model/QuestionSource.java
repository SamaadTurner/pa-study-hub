package com.pastudyhub.exam.model;

import java.util.UUID;

/** Who wrote a question, from the point of view of the user taking the exam. */
public enum QuestionSource {
    STARTER,
    YOU,
    ANOTHER_USER;

    public static QuestionSource of(Question question, UUID examTaker) {
        if (question.getOwnerId() == null) {
            return STARTER;
        }
        if (question.isWrittenBy(examTaker)) {
            return YOU;
        }
        return ANOTHER_USER;
    }
}
