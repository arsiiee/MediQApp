-- V3: email verification support
-- Adds a UNIQUE constraint on users.email and a per-account verification status.
-- Existing phone-only accounts keep their rows unchanged: their email is NULL,
-- so the UNIQUE index does not affect them, and they continue to sign in with
-- username and password. New email registrations start unverified.

CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email_unique ON users(email);

ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verification_status VARCHAR(50) NOT NULL DEFAULT 'unverified';
