-- ============================================================
-- Remember which questions each exam picked
-- ============================================================
-- An exam picks its questions at random when it starts, but the
-- picks were only sent back in that first response and never saved.
-- Loading the exam again could only show questions that already had
-- an answer, so a new exam came back with no questions at all, and a
-- question nobody answered was missing from the score.

CREATE TABLE exam_questions (
    id               UUID NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    exam_session_id  UUID NOT NULL REFERENCES exam_sessions(id) ON DELETE CASCADE,
    question_id      UUID NOT NULL REFERENCES questions(id),
    order_index      INT  NOT NULL
);

-- A question can only be in the same exam once
CREATE UNIQUE INDEX idx_exam_questions_session_question ON exam_questions (exam_session_id, question_id);
CREATE INDEX idx_exam_questions_session_order ON exam_questions (exam_session_id, order_index);

-- Exams taken before this table existed: the answered questions are the only
-- ones we still know about, so keep those, in the order they were answered.
INSERT INTO exam_questions (exam_session_id, question_id, order_index)
SELECT exam_session_id,
       question_id,
       ROW_NUMBER() OVER (PARTITION BY exam_session_id ORDER BY answered_at) - 1
FROM exam_answers;
