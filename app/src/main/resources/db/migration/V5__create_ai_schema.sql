-- ============================================================
-- AI Assistant Service Schema
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- -------------------------------------------------------
-- Chat Sessions
-- -------------------------------------------------------
CREATE TABLE chat_sessions (
    id          UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id     UUID        NOT NULL,
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_chat_sessions_user_id ON chat_sessions (user_id, updated_at DESC);

-- -------------------------------------------------------
-- Chat Turns  (one row per user/assistant exchange)
-- -------------------------------------------------------
CREATE TABLE chat_turns (
    id              UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    session_id      UUID        NOT NULL REFERENCES chat_sessions (id) ON DELETE CASCADE,
    turn_number     INT         NOT NULL,
    user_message    TEXT        NOT NULL,
    assistant_reply TEXT        NOT NULL,
    input_tokens    INT,
    output_tokens   INT,
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_chat_turns_session ON chat_turns (session_id, turn_number ASC);
