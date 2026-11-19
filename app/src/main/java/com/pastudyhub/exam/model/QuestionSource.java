package com.pastudyhub.exam.model;

import java.util.UUID;

/** Who wrote a question, from the point of view of the user taking the exam. */
public enum QuestionSource {
    STARTER,
    YOU,
    ANOTHER_USER;

    public static QuestionSource of(Question question, UUID examTaker) {
        // A question whose author closed their account also has no owner
        if (question.getOwnerId() == null) {
            return question.isAuthorDeleted() ? ANOTHER_USER : STARTER;
        }
        if (question.isWrittenBy(examTaker)) {
            return YOU;
        }
        return ANOTHER_USER;
    }
}
