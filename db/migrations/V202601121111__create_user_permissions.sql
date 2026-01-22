-- Migration: V202601121111__create_user_permissions.sql
-- Description: Creates tables for user permissions.

CREATE TABLE IF NOT EXISTS user_permissions (
    user_id VARCHAR NOT NULL,
    permission_id VARCHAR NOT NULL,
    enabled BOOLEAN NOT NULL,
    last_modified TIMESTAMP NOT NULL,
    actor VARCHAR NOT NULL,
    CONSTRAINT fk_user
        FOREIGN KEY(user_id) 
        REFERENCES users(id)
        ON DELETE CASCADE,
    PRIMARY KEY (user_id, permission_id)
);

GRANT INSERT, SELECT, UPDATE, DELETE ON user_permissions TO identity_api;
GRANT INSERT, SELECT, UPDATE, DELETE ON user_permissions TO identity_okta_tools;
