-- ============================================================
-- One row per flashcard review
-- ============================================================
-- review_schedules only keeps the latest state of each card (the next date, the
-- last rating). That is enough for SM-2, but not for newer schedulers like FSRS,
-- which learn from the whole history of a card, and not for checking which
-- scheduler predicts forgetting better. This table keeps every review.
--
-- Nothing is backfilled: the old schedules only know the last review of each
-- card, not the history, so the log starts with this version.

CREATE TABLE review_logs (
    id                     UUID      NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id                UUID      NOT NULL,
    card_id                UUID      NOT NULL REFERENCES cards(id) ON DELETE CASCADE,
    -- The button the user pressed: 1 Again, 2 Hard, 4 Good, 5 Easy (see ReviewRequest)
    quality                INT       NOT NULL,
    reviewed_at            TIMESTAMP NOT NULL,
    -- Calendar days since this user last reviewed this card. NULL on the first review.
    days_since_last_review INT,
    -- The interval the scheduler had set before this review, in days (0 for a new card)
    scheduled_days         INT       NOT NULL,
    -- Seconds spent on the card, capped like the study time (NULL if the page sent none)
    time_spent_seconds     INT
);

-- The history of one card for one user, in order. This is how both FSRS and the
-- evaluation read the log.
CREATE INDEX idx_review_logs_user_card_time ON review_logs (user_id, card_id, reviewed_at);
