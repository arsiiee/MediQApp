-- DEV: allow appointment status updates via REST/RPC in transitional (Ktor/anon) sessions.
-- Needed because cancel fallback does PATCH /appointments and some clients lack auth.uid().

ALTER TABLE public.appointments ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "anon_update_appointments_dev" ON public.appointments;
CREATE POLICY "anon_update_appointments_dev" ON public.appointments
  FOR UPDATE
  TO anon, authenticated
  USING (true)
  WITH CHECK (true);

DROP POLICY IF EXISTS "anon_insert_appointments_dev" ON public.appointments;
CREATE POLICY "anon_insert_appointments_dev" ON public.appointments
  FOR INSERT
  TO anon, authenticated
  WITH CHECK (true);
