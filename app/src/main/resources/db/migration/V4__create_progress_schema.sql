-- ============================================================
-- Study Progress Service Schema
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- -------------------------------------------------------
-- Activity Logs
-- -------------------------------------------------------
CREATE TABLE activity_logs (
    id               UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id          UUID         NOT NULL,
    activity_type    VARCHAR(30)  NOT NULL,
    category         VARCHAR(50)  NOT NULL,
    duration_minutes INT          NOT NULL DEFAULT 0,
    cards_reviewed   INT          NOT NULL DEFAULT 0,
    correct_count    INT          NOT NULL DEFAULT 0,
    total_count      INT          NOT NULL DEFAULT 0,
    activity_date    DATE         NOT NULL DEFAULT CURRENT_DATE,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_activity_user_date     ON activity_logs (user_id, activity_date DESC);
CREATE INDEX idx_activity_type          ON activity_logs (activity_type);
CREATE INDEX idx_activity_category      ON activity_logs (category);

-- -------------------------------------------------------
-- Daily Goals  (one row per user, upserted)
-- -------------------------------------------------------
CREATE TABLE daily_goals (
    id                    UUID    NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id               UUID    NOT NULL UNIQUE,
    target_cards_per_day  INT     NOT NULL DEFAULT 20,
    target_minutes_per_day INT   NOT NULL DEFAULT 30,
    created_at            TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_daily_goals_user_id ON daily_goals (user_id);
