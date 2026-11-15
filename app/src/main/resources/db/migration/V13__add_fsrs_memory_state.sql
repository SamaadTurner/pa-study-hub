-- ============================================================
-- FSRS memory state next to SM-2
-- ============================================================
-- Every review now runs both schedulers. SM-2 keeps its own columns (ease_factor,
-- review_interval, repetitions). FSRS keeps its two numbers here, so switching the
-- scheduler (setting scheduling.algorithm) does not throw away any card's history.
-- Cards reviewed before this version have no FSRS state yet; FSRS starts them
-- fresh at their next review.

ALTER TABLE review_schedules ADD COLUMN fsrs_stability  DOUBLE PRECISION;
ALTER TABLE review_schedules ADD COLUMN fsrs_difficulty DOUBLE PRECISION;

-- The interval the active scheduler chose at the last review. review_interval is
-- SM-2's own number and is used in its math, so it can't double as this.
ALTER TABLE review_schedules ADD COLUMN planned_interval_days INT NOT NULL DEFAULT 0;

-- Until now SM-2 was the only scheduler, so its interval was the planned one
UPDATE review_schedules SET planned_interval_days = review_interval;
