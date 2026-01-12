-- Migration: V202601121111__create_user_permissions.sql
-- Description: Creates tables for user permissions.

CREATE TABLE IF NOT EXISTS user_permissions (
    user_id VARCHAR NOT NULL,
    actor VARCHAR NOT NULL,
    permission VARCHAR NOT NULL,
    consented BOOLEAN NOT NULL,
    last_changed TIMESTAMP NOT NULL,
    CONSTRAINT fk_user
        FOREIGN KEY(user_id) 
        REFERENCES users(id)
        ON DELETE CASCADE,
    PRIMARY KEY (user_id, permission)
);

