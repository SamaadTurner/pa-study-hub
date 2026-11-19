-- Version number for optimistic locking. The time limit job and a user's own
-- request can both try to finish the same exam at the same moment; with this
-- column the second one fails instead of scoring the exam twice.
ALTER TABLE exam_sessions ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
