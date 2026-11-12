-- File: supabase/migrations/20261122120000_allow_secretary_read_appointments.sql
-- Purpose: Allow authenticated users (secretaries) to read all appointments via
--          the Supabase JS client (anon key), since mediq_current_role() returns
--          NULL until Supabase Auth replaces Ktor auth.
--
-- Mirrors authenticated_read_all_profiles and authenticated_read_all_users which
-- both use USING (true) for the same transitional reason.

ALTER TABLE public.appointments ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "authenticated_read_all_appointments" ON appointments;
CREATE POLICY "authenticated_read_all_appointments" ON appointments
  FOR SELECT
  TO authenticated
  USING (true);
