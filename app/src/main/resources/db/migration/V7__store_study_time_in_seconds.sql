-- ============================================================
-- Store study time in seconds instead of minutes
-- ============================================================
-- One flashcard usually takes well under a minute, so a column of
-- whole minutes could only ever hold 0 for a card review. That is
-- why the daily minutes goal never moved. Seconds can be added up
-- and turned into minutes when they are shown.

ALTER TABLE activity_logs ADD COLUMN duration_seconds INT NOT NULL DEFAULT 0;

-- Keep whatever was already saved
UPDATE activity_logs SET duration_seconds = duration_minutes * 60;

ALTER TABLE activity_logs DROP COLUMN duration_minutes;
