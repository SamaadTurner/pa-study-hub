-- ============================================================
-- Count AI requests per user per day
-- ============================================================
-- The AI features run on a free plan with a daily request limit for the whole
-- key. Without a count of our own, one user (or a script) could use it all up
-- for everyone. AiUsageLimiter reads and updates this table before every call.

CREATE TABLE ai_usage (
    id             UUID NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id        UUID NOT NULL,
    usage_date     DATE NOT NULL,
    request_count  INT  NOT NULL DEFAULT 0
);

-- One row per user per day. This also makes the lookup for "today" fast.
CREATE UNIQUE INDEX idx_ai_usage_user_date ON ai_usage (user_id, usage_date);

-- For the site-wide total of a day
CREATE INDEX idx_ai_usage_date ON ai_usage (usage_date);
