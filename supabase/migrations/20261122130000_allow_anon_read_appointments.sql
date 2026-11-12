-- File: supabase/migrations/20261122130000_allow_anon_read_appointments.sql
-- Purpose: Allow anonymous/non-auth-Supabase requests to read all appointments.
--
-- The Angular app authenticates via Ktor JWTs, not Supabase Auth, so auth.uid()
-- returns NULL and mediq_current_role() returns NULL. Existing RLS policies
-- (secretaries_admins_all_appointments, authenticated_read_all_appointments) all
-- depend on mediq_current_role() or auth.uid() and match nothing.
--
-- Mirrors authenticated_read_all_profiles / authenticated_read_all_users which
-- already use USING (true) for the same transitional reason, extended to TO anon
-- because Ktor-authenticated requests arrive as the anon Postgres role.

ALTER TABLE public.appointments ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "anon_read_all_appointments" ON appointments;
CREATE POLICY "anon_read_all_appointments" ON appointments
  FOR SELECT
  TO anon
  USING (true);
