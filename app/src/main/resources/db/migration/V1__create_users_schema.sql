-- PA Study Hub — User Service Schema
-- V1: Initial schema — users and refresh tokens tables

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(72) NOT NULL,
    first_name      VARCHAR(50) NOT NULL,
    last_name       VARCHAR(50) NOT NULL,
    role            VARCHAR(20) NOT NULL DEFAULT 'STUDENT',
    pa_school_name  VARCHAR(200),
    graduation_year INTEGER,
    study_reminders_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    preferred_study_time    VARCHAR(5),
    avatar_url      VARCHAR(500),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    last_login_at   TIMESTAMP,

    CONSTRAINT chk_role CHECK (role IN ('STUDENT', 'ADMIN')),
    CONSTRAINT chk_graduation_year CHECK (graduation_year IS NULL OR (graduation_year >= 2020 AND graduation_year <= 2040))
);

-- Index for fast email lookups during login
CREATE INDEX IF NOT EXISTS idx_users_email ON users (email);

-- Refresh tokens table
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       VARCHAR(36) NOT NULL UNIQUE,
    expires_at  TIMESTAMP NOT NULL,
    is_revoked  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Index for fast token lookups during refresh
CREATE UNIQUE INDEX IF NOT EXISTS idx_refresh_tokens_token ON refresh_tokens (token);
-- Index for revoking all tokens for a user on logout-all
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens (user_id);

-- Seed the default study user (jocelyn) for initial demo
-- Password: PAStudent2026! (BCrypt hash with strength 12)
-- NOTE: This seed user is only for local development — production uses real registration
INSERT INTO users (email, password_hash, first_name, last_name, role, pa_school_name, graduation_year, is_active)
VALUES (
    'jocelyn@pastudyhub.com',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQyCGkv0FyRdoJCw1gFhCVuZO',
    'Jocelyn',
    'Turner',
    'STUDENT',
    'Your PA School',
    2028,
    TRUE
) ON CONFLICT (email) DO NOTHING;
