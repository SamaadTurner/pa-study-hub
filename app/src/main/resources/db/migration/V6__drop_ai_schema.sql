-- ============================================================
-- Remove the AI assistant tables
-- ============================================================
-- The AI assistant was taken out of the app, so nothing reads or
-- writes these tables any more. V5 is left as it is because it may
-- already have run on an existing database.

-- chat_turns has a foreign key to chat_sessions, so it goes first
DROP TABLE IF EXISTS chat_turns;
DROP TABLE IF EXISTS chat_sessions;
