-- Supabase migration: profiles table for role-based access control.
--
-- Mirrors key fields from the Ktor `users` table so RLS policies can be
-- written against a Supabase-native profile once Supabase Auth is integrated
-- (Phase 2 of the Supabase migration per AGENTS.md).
--
-- `user_id` is a UUID that will reference `auth.users.id` after Supabase Auth
-- replaces Ktor auth. Until then it is a standalone identifier.
--
-- RLS is enabled with NO policies (deny-all for anon/authenticated), matching
-- the approach in 20261029153500_enable_row_level_security.sql. Policies are
-- added in a later phase once current_role() and Supabase Auth are in place.

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE,
    username VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    email VARCHAR(255),
    mobile_number VARCHAR(255),
    role VARCHAR(50) NOT NULL CHECK (role IN ('PATIENT', 'DOCTOR', 'SECRETARY', 'ADMIN')),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;

-- Index for current_role() lookups by auth.uid()
CREATE INDEX IF NOT EXISTS idx_profiles_user_id ON profiles(user_id);
