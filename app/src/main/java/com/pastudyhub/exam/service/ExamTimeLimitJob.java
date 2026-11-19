package com.pastudyhub.exam.service;

import com.pastudyhub.exam.repository.ExamSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Hands in timed exams whose time ran out while nobody had them open, so they are
 * scored and count toward the streak on the day they ended.
 *
 * <p>The service also checks the time whenever an exam is opened or answered. This
 * job is still needed for exams nobody comes back to. On a free server that sleeps
 * when idle the job does not run during the sleep; the check on open covers that.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExamTimeLimitJob {

    private final ExamSessionRepository examSessionRepository;
    private final ExamService examService;

    @Scheduled(fixedDelayString = "${exam.time-limit-check-ms:60000}", initialDelay = 60000)
    public void handInExpiredExams() {
        int handedIn = 0;
        for (UUID sessionId : examSessionRepository.findIdsOfTimedExamsInProgress()) {
            // Each exam in its own transaction (inside the service), so one failure
            // does not stop the others
            try {
                if (examService.finishExamIfTimeUp(sessionId)) {
                    handedIn++;
                }
            } catch (RuntimeException ex) {
                // Most likely the user finished it at the same moment (version check)
                log.warn("Could not hand in exam {}: {}", sessionId, ex.getMessage());
            }
        }
        if (handedIn > 0) {
            log.info("Handed in {} exam(s) whose time ran out", handedIn);
        }
    }
}
