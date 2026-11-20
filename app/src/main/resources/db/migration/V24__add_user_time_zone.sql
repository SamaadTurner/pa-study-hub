-- The user's time zone, as the browser reports it (for example America/Los_Angeles).
-- Streaks, the daily goal and due dates follow the user's calendar, not the server's
-- UTC clock. Null until the user's browser has sent it; then UTC is assumed.
ALTER TABLE users ADD COLUMN time_zone VARCHAR(64);
