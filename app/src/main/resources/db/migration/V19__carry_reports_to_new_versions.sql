-- Editing a question that is already in an exam saves a new version. Reports used
-- to stay behind on the old version, so an author could wipe them by editing
-- without fixing anything. The new version now carries how many reports its earlier
-- versions had, and that number counts toward leaving shared exams.
ALTER TABLE questions ADD COLUMN earlier_report_count INT NOT NULL DEFAULT 0;
