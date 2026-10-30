-- Supabase migration: current_role() helper for RLS policies.
--
-- Returns the role of the currently authenticated user by looking up their
-- profile via auth.uid(). Used by RLS policies to enforce role-based access
-- (patient, doctor, secretary, admin) once Supabase Auth is integrated.
--
-- Until Supabase Auth replaces Ktor auth, auth.uid() returns NULL and this
-- function returns NULL — RLS policies referencing it will match no rows,
-- which is the safe default (deny-all) until policies are explicitly added.
--
-- SECURITY INVOKER: runs with the caller's privileges, so RLS still applies.
-- Do NOT use SECURITY DEFINER — it would bypass RLS.

CREATE OR REPLACE FUNCTION current_role()
RETURNS VARCHAR
LANGUAGE sql
STABLE
SECURITY INVOKER
AS $$
    SELECT role FROM profiles WHERE user_id = auth.uid();
$$;
