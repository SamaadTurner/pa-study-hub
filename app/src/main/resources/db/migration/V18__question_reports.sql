-- Reports on shared questions. A user who had a shared question in a finished
-- exam can report it once. With 3 reports from different users the question is
-- left out of other people's exams (the author still sees it, with the reports).
CREATE TABLE question_reports (
    id           UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    question_id  UUID         NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    reporter_id  UUID         NOT NULL,
    reason       VARCHAR(20)  NOT NULL,
    note         VARCHAR(300),
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_question_reports_question_reporter UNIQUE (question_id, reporter_id)
);

CREATE INDEX idx_question_reports_question_id ON question_reports (question_id);

-- True for a question whose author closed their account. It has no owner then,
-- like a starter question, and this keeps the two apart (results still say the
-- question was written by another user).
ALTER TABLE questions ADD COLUMN author_deleted BOOLEAN NOT NULL DEFAULT FALSE;
