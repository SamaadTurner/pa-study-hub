package com.pastudyhub.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Deletes an account and everything that belongs to it, in one transaction.
 *
 * <p>The data is spread over the flashcard, exam and progress tables, which only
 * store the user's id (there are no foreign keys to users from those modules).
 * The statements run in an order where nothing still points at a row that is
 * being deleted, so it works the same on PostgreSQL and on H2 in the tests.
 *
 * <p>Two things are not simply deleted:
 * <ul>
 *   <li>Questions the user wrote that are part of other people's exams. Deleting
 *       them would break those results, so they lose their author (marked
 *       author_deleted) and are taken out of every future exam instead.</li>
 *   <li>Other people's own copies of the user's public decks. A copy belongs to the
 *       person who made it and is kept.</li>
 * </ul>
 * Other people's study progress on the user's public decks (not copies) goes away
 * with the decks.
 *
 * <p>When a new table gets a user_id column, it has to be added here.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountDeletionService {

    private static final String CARDS_OF_USER =
            "SELECT c.id FROM cards c JOIN decks d ON c.deck_id = d.id WHERE d.user_id = ?";
    private static final String EXAMS_OF_USER =
            "SELECT id FROM exam_sessions WHERE user_id = ?";

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void deleteEverything(UUID userId) {
        // Flashcards: the user's own reviews anywhere, and every review of the user's cards
        run("DELETE FROM review_logs WHERE user_id = ? OR card_id IN (" + CARDS_OF_USER + ")", userId, userId);
        run("DELETE FROM review_schedules WHERE user_id = ? OR card_id IN (" + CARDS_OF_USER + ")", userId, userId);
        run("DELETE FROM cards WHERE deck_id IN (SELECT id FROM decks WHERE user_id = ?)", userId);
        run("DELETE FROM decks WHERE user_id = ?", userId);

        // Exams the user took
        run("DELETE FROM exam_answers WHERE exam_session_id IN (" + EXAMS_OF_USER + ")", userId);
        run("DELETE FROM exam_questions WHERE exam_session_id IN (" + EXAMS_OF_USER + ")", userId);
        run("DELETE FROM exam_sessions WHERE user_id = ?", userId);

        // Questions the user wrote. The user's own exams are gone already, so any
        // exam still pointing at one of them belongs to somebody else.
        run("DELETE FROM question_reports WHERE reporter_id = ?", userId);
        run("UPDATE questions SET owner_id = NULL, is_active = FALSE, is_public = FALSE, author_deleted = TRUE "
                + "WHERE owner_id = ? AND ("
                + "EXISTS (SELECT 1 FROM exam_questions eq WHERE eq.question_id = questions.id) "
                + "OR EXISTS (SELECT 1 FROM exam_answers ea WHERE ea.question_id = questions.id))", userId);
        run("DELETE FROM question_reports WHERE question_id IN (SELECT id FROM questions WHERE owner_id = ?)", userId);
        run("DELETE FROM answer_options WHERE question_id IN (SELECT id FROM questions WHERE owner_id = ?)", userId);
        run("DELETE FROM questions WHERE owner_id = ?", userId);

        // Progress and usage
        run("DELETE FROM activity_logs WHERE user_id = ?", userId);
        run("DELETE FROM daily_goals WHERE user_id = ?", userId);
        run("DELETE FROM ai_usage WHERE user_id = ?", userId);

        // The account itself
        run("DELETE FROM refresh_tokens WHERE user_id = ?", userId);
        run("DELETE FROM users WHERE id = ?", userId);

        log.info("Account deleted with all its data: id={}", userId);
    }

    private void run(String sql, Object... params) {
        jdbcTemplate.update(sql, params);
    }
}
