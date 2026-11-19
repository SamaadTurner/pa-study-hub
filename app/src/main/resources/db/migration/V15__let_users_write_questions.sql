-- Users can write their own practice exam questions.
--
-- owner_id is the user who wrote the question. The starter questions from V3 have
-- no owner, and every user's exams can use them. A user's own question is used in
-- their own exams, and in other users' exams only when it is shared (is_public)
-- and the other user chose to include shared questions.
ALTER TABLE questions ADD COLUMN owner_id UUID;
ALTER TABLE questions ADD COLUMN is_public BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_questions_owner_id ON questions (owner_id) WHERE is_active = TRUE;
