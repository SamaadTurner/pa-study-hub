package com.pastudyhub.exam.engine;

import com.pastudyhub.exam.model.ExamSession;
import com.pastudyhub.exam.model.ExamStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Exam time limit")
class ExamTimeLimitTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 11, 19, 10, 0, 0);

    private ExamSession exam(int minutes, ExamStatus status) {
        ExamSession session = ExamSession.builder()
                .timeLimitMinutes(minutes)
                .status(status)
                .build();
        session.setStartedAt(START);
        return session;
    }

    @Test
    @DisplayName("An untimed exam has no deadline and never runs out")
    void untimed_neverOver() {
        ExamSession session = exam(0, ExamStatus.IN_PROGRESS);
        assertThat(ExamTimeLimit.deadline(session)).isNull();
        assertThat(ExamTimeLimit.isOver(session, START.plusDays(3))).isFalse();
        assertThat(ExamTimeLimit.finishTime(session, START.plusDays(3))).isEqualTo(START.plusDays(3));
    }

    @Test
    @DisplayName("A timed exam is only over once the grace period after the deadline has passed")
    void timed_overAfterGrace() {
        ExamSession session = exam(15, ExamStatus.IN_PROGRESS);
        LocalDateTime deadline = START.plusMinutes(15);

        assertThat(ExamTimeLimit.deadline(session)).isEqualTo(deadline);
        assertThat(ExamTimeLimit.isOver(session, deadline.minusSeconds(1))).isFalse();
        assertThat(ExamTimeLimit.isOver(session, deadline.plusSeconds(ExamTimeLimit.GRACE_SECONDS))).isFalse();
        assertThat(ExamTimeLimit.isOver(session, deadline.plusSeconds(ExamTimeLimit.GRACE_SECONDS + 1))).isTrue();
    }

    @Test
    @DisplayName("A finished or abandoned exam is never over again")
    void notInProgress_neverOver() {
        LocalDateTime muchLater = START.plusHours(5);
        assertThat(ExamTimeLimit.isOver(exam(15, ExamStatus.COMPLETED), muchLater)).isFalse();
        assertThat(ExamTimeLimit.isOver(exam(15, ExamStatus.ABANDONED), muchLater)).isFalse();
    }

    @Test
    @DisplayName("The finish time is now, but never later than the deadline")
    void finishTime_cappedAtDeadline() {
        ExamSession session = exam(15, ExamStatus.IN_PROGRESS);
        assertThat(ExamTimeLimit.finishTime(session, START.plusMinutes(10))).isEqualTo(START.plusMinutes(10));
        assertThat(ExamTimeLimit.finishTime(session, START.plusHours(2))).isEqualTo(START.plusMinutes(15));
    }
}
