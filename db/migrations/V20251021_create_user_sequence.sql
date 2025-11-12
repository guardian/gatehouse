-- Migration: V20251021_create_user_sequence.sql
-- Description: Create a sequence for user IDs and set its starting value

CREATE SEQUENCE IF NOT EXISTS useridseq; 
SELECT setval('useridseq', (SELECT MAX(id::BIGINT) FROM users) + 100000); -- Set starting value to the latest user ID in Identity DB -- Add 100000 to avoid conflicts with existing IDs
GRANT USAGE, SELECT ON SEQUENCE useridseq TO identity_api;