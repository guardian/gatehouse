--
-- add temporary index to search for usernames in case insensitive way
--
CREATE INDEX CONCURRENTLY non_unique_users_lower_username ON users(lower(username));
