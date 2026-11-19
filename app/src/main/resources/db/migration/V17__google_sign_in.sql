-- "Continue with Google". google_subject is Google's id for the account (the
-- "sub" claim), which never changes even if the user changes their email.
-- An account made with Google has no password, so password_hash can be empty.
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;
ALTER TABLE users ADD COLUMN google_subject VARCHAR(255);
CREATE UNIQUE INDEX idx_users_google_subject ON users (google_subject);
