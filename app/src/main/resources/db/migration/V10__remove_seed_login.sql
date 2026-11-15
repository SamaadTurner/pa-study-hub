-- V1 added a ready-made login to every database, and its password was printed in
-- the README. Anyone who read the README could sign in to any copy of the app.
--
-- This removes that account, but only if it still has the password from V1. If
-- someone changed the password, the account is theirs now and is left alone.
-- (Rows in refresh_tokens go with it through ON DELETE CASCADE.)
--
-- A demo account for local development is now created by the app itself, and only
-- when it runs with the "dev" profile. See DemoAccountLoader.
DELETE FROM users
WHERE email = 'jocelyn@pastudyhub.com'
  AND password_hash = '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQyCGkv0FyRdoJCw1gFhCVuZO';
