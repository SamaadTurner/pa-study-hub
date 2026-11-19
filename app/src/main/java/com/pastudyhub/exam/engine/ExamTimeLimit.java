package com.pastudyhub.exam.engine;

import com.pastudyhub.exam.model.ExamSession;
import com.pastudyhub.exam.model.ExamStatus;

import java.time.LocalDateTime;

/**
 * When a timed exam has to end. Pure logic, no Spring, so it is easy to test.
 *
 * <p>The page counts down and hands the exam in at zero, but a page can be closed,
 * or a script can keep answering after the time is up. The server therefore checks
 * the limit itself. Answers are still accepted for a short grace period after the
 * deadline, so an answer sent in the last second is not lost on a slow connection.
 */
public final class ExamTimeLimit {

    public static final int GRACE_SECONDS = 30;

    private ExamTimeLimit() {
    }

    /** The moment the time runs out, or null for an untimed exam. */
    public static LocalDateTime deadline(ExamSession session) {
        if (session.getTimeLimitMinutes() <= 0 || session.getStartedAt() == null) {
            return null;
        }
        return session.getStartedAt().plusMinutes(session.getTimeLimitMinutes());
    }

    /** True when the exam is still running but its time and the grace period are over. */
    public static boolean isOver(ExamSession session, LocalDateTime now) {
        LocalDateTime deadline = deadline(session);
        return session.getStatus() == ExamStatus.IN_PROGRESS
                && deadline != null
                && now.isAfter(deadline.plusSeconds(GRACE_SECONDS));
    }

    /**
     * When the exam counts as finished: now, but never later than the deadline.
     * An exam handed in by the server an hour late still took only its time limit.
     */
    public static LocalDateTime finishTime(ExamSession session, LocalDateTime now) {
        LocalDateTime deadline = deadline(session);
        if (deadline != null && now.isAfter(deadline)) {
            return deadline;
        }
        return now;
    }
}
