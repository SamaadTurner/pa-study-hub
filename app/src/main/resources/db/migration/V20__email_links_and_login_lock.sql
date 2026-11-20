-- Email confirmation, password reset by email, and a login lock that survives a restart.

-- Whether the user has clicked the confirmation link. Google has already checked
-- the address of accounts that use Google sign-in.
ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE users SET email_verified = TRUE WHERE google_subject IS NOT NULL;

-- One row per link sent by email (reset a password, confirm the email). Only a
-- SHA-256 hash of the token is stored, so the table alone can't be used to reset
-- anyone's password. A link works once and until it expires.
CREATE TABLE email_tokens (
    id          UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    purpose     VARCHAR(20)  NOT NULL,
    token_hash  VARCHAR(64)  NOT NULL,
    expires_at  TIMESTAMP    NOT NULL,
    used_at     TIMESTAMP,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_email_tokens_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_email_tokens_user_purpose ON email_tokens (user_id, purpose, created_at);

-- Failed logins per email (see LoginAttemptLimiter). Used to be kept in memory,
-- so a restart (every wake-up on the free plan) cleared the lock.
CREATE TABLE login_failures (
    email          VARCHAR(255) NOT NULL PRIMARY KEY,
    failure_count  INT          NOT NULL,
    window_end     TIMESTAMP    NOT NULL
);
