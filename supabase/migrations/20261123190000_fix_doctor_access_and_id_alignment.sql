-- Fix doctor data access on hosted Supabase without resetting existing progress.
--
-- Additive/idempotent: replaces specific policies/functions only (no table/data drops).
-- Preserves: admin staff creation, patient signup, secretary/patient messaging,
-- secretary/patient/doctor appointment visibility and actions, patient booking/cancelling.
--
-- Addresses ID-mapping drift across auth.uid(), users.id/auth_user_id, profiles.user_id,
-- doctors.id/doctors.user_id, and doctor_id references in appointments, message_threads,
-- messages (context), clinic_hours, and availabilities.

-- ---------------------------------------------------------------------------
-- Helpers (SECURITY DEFINER so RLS/RPC checks can resolve identity safely)
-- ---------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION public.mediq_resolve_user_id()
RETURNS varchar
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
  SELECT COALESCE(
    (SELECT u.id FROM users u WHERE u.auth_user_id = auth.uid() LIMIT 1),
    (SELECT u.id FROM users u WHERE u.id = auth.uid()::text LIMIT 1),
    auth.uid()::text
  );
$$;

CREATE OR REPLACE FUNCTION public.mediq_is_doctor_owner(p_ref varchar)
RETURNS boolean
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
  v_uid uuid := auth.uid();
  v_user_id varchar;
BEGIN
  IF v_uid IS NULL OR p_ref IS NULL OR btrim(p_ref) = '' THEN
    RETURN false;
  END IF;

  -- Must be a doctor when role is known; in transitional sessions role may be null,
  -- but ownership still requires matching the signed-in identity/doctor mapping.
  IF EXISTS (SELECT 1 FROM profiles p WHERE p.user_id = v_uid)
     AND NOT EXISTS (SELECT 1 FROM profiles p WHERE p.user_id = v_uid AND upper(p.role) = 'DOCTOR') THEN
    RETURN false;
  END IF;

  IF p_ref = v_uid::text THEN
    RETURN true;
  END IF;

  SELECT u.id INTO v_user_id
  FROM users u
  WHERE u.auth_user_id = v_uid OR u.id = v_uid::text
  ORDER BY CASE WHEN u.auth_user_id = v_uid THEN 0 ELSE 1 END
  LIMIT 1;

  IF v_user_id IS NOT NULL AND p_ref = v_user_id THEN
    RETURN true;
  END IF;

  RETURN EXISTS (
    SELECT 1
    FROM doctors d
    WHERE
      (d.id = p_ref OR d.user_id = p_ref)
      AND (
        d.user_id = v_uid::text
        OR (v_user_id IS NOT NULL AND d.user_id = v_user_id)
        OR d.id = v_uid::text
        OR (v_user_id IS NOT NULL AND d.id = v_user_id)
      )
  ) OR EXISTS (
    SELECT 1
    FROM doctors d
    WHERE
      (d.id = v_uid::text OR d.user_id = v_uid::text OR (v_user_id IS NOT NULL AND (d.id = v_user_id OR d.user_id = v_user_id)))
      AND (d.id = p_ref OR d.user_id = p_ref)
  );
END;
$$;

CREATE OR REPLACE FUNCTION public.mediq_is_patient_owner(p_ref varchar)
RETURNS boolean
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
  v_uid uuid := auth.uid();
  v_user_id varchar;
BEGIN
  IF v_uid IS NULL OR p_ref IS NULL OR btrim(p_ref) = '' THEN
    RETURN false;
  END IF;

  IF p_ref = v_uid::text THEN
    RETURN true;
  END IF;

  SELECT u.id INTO v_user_id
  FROM users u
  WHERE u.auth_user_id = v_uid OR u.id = v_uid::text
  ORDER BY CASE WHEN u.auth_user_id = v_uid THEN 0 ELSE 1 END
  LIMIT 1;

  RETURN v_user_id IS NOT NULL AND p_ref = v_user_id;
END;
$$;

-- ---------------------------------------------------------------------------
-- appointments: preserve transitional broad reads, add tolerant doctor access
-- ---------------------------------------------------------------------------

-- Transitional reads (reconciled from untracked drafts; idempotent) so current
-- Ktor-JWT/anon-key flows keep working while Supabase Auth migration is in progress.
ALTER TABLE public.appointments ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "authenticated_read_all_appointments" ON appointments;
CREATE POLICY "authenticated_read_all_appointments" ON appointments
  FOR SELECT TO authenticated
  USING (true);

DROP POLICY IF EXISTS "anon_read_all_appointments" ON appointments;
CREATE POLICY "anon_read_all_appointments" ON appointments
  FOR SELECT TO anon
  USING (true);

DROP POLICY IF EXISTS "doctors_read_own_appointments" ON appointments;
CREATE POLICY "doctors_read_own_appointments" ON appointments
  FOR SELECT TO authenticated
  USING (
    (mediq_current_role() = 'DOCTOR' OR mediq_current_role() IS NULL)
    AND mediq_is_doctor_owner(doctor_id)
  );

DROP POLICY IF EXISTS "doctors_update_own_appointments" ON appointments;
CREATE POLICY "doctors_update_own_appointments" ON appointments
  FOR UPDATE TO authenticated
  USING (
    (mediq_current_role() = 'DOCTOR' OR mediq_current_role() IS NULL)
    AND mediq_is_doctor_owner(doctor_id)
  )
  WITH CHECK (
    (mediq_current_role() = 'DOCTOR' OR mediq_current_role() IS NULL)
    AND mediq_is_doctor_owner(doctor_id)
  );

-- ---------------------------------------------------------------------------
-- message_threads/messages: robust participant access + keep staff access
-- ---------------------------------------------------------------------------

DROP POLICY IF EXISTS "participants_read_own_threads" ON message_threads;
CREATE POLICY "participants_read_own_threads" ON message_threads
  FOR SELECT TO authenticated
  USING (
    mediq_is_patient_owner(patient_id)
    OR mediq_is_doctor_owner(doctor_id)
  );

DROP POLICY IF EXISTS "participants_create_own_threads" ON message_threads;
CREATE POLICY "participants_create_own_threads" ON message_threads
  FOR INSERT TO authenticated
  WITH CHECK (
    mediq_is_patient_owner(patient_id)
    OR mediq_is_doctor_owner(doctor_id)
  );

DROP POLICY IF EXISTS "participants_read_own_messages" ON messages;
CREATE POLICY "participants_read_own_messages" ON messages
  FOR SELECT TO authenticated
  USING (
    mediq_is_patient_owner(author_id)
    OR mediq_is_doctor_owner(author_id)
    OR conversation_id IN (
      SELECT mt.id
      FROM message_threads mt
      WHERE mediq_is_patient_owner(mt.patient_id)
         OR mediq_is_doctor_owner(mt.doctor_id)
    )
  );

DROP POLICY IF EXISTS "participants_create_own_messages" ON messages;
CREATE POLICY "participants_create_own_messages" ON messages
  FOR INSERT TO authenticated
  WITH CHECK (
    (mediq_is_patient_owner(author_id) OR mediq_is_doctor_owner(author_id))
    AND conversation_id IN (
      SELECT mt.id
      FROM message_threads mt
      WHERE mediq_is_patient_owner(mt.patient_id)
         OR mediq_is_doctor_owner(mt.doctor_id)
    )
  );

-- Ensure staff can update threads/read messages as needed by web clients (additive/idempotent)
DROP POLICY IF EXISTS "secretaries_admins_update_threads" ON message_threads;
CREATE POLICY "secretaries_admins_update_threads" ON message_threads
  FOR UPDATE TO authenticated
  USING (mediq_current_role() IN ('SECRETARY', 'ADMIN') OR mediq_current_role() IS NULL)
  WITH CHECK (mediq_current_role() IN ('SECRETARY', 'ADMIN') OR mediq_current_role() IS NULL);

DROP POLICY IF EXISTS "secretaries_admins_read_all_messages" ON messages;
CREATE POLICY "secretaries_admins_read_all_messages" ON messages
  FOR SELECT TO authenticated
  USING (mediq_current_role() IN ('SECRETARY', 'ADMIN') OR mediq_current_role() IS NULL);

-- ---------------------------------------------------------------------------
-- clinic_hours / availabilities: doctors manage their own (ID-mapping tolerant)
-- ---------------------------------------------------------------------------

DROP POLICY IF EXISTS "doctors_own_clinic_hours" ON clinic_hours;
CREATE POLICY "doctors_own_clinic_hours" ON clinic_hours
  FOR ALL TO authenticated
  USING (
    (mediq_current_role() = 'DOCTOR' OR mediq_current_role() IS NULL)
    AND mediq_is_doctor_owner(doctor_id)
  )
  WITH CHECK (
    (mediq_current_role() = 'DOCTOR' OR mediq_current_role() IS NULL)
    AND mediq_is_doctor_owner(doctor_id)
  );

DROP POLICY IF EXISTS "doctors_own_availabilities" ON availabilities;
CREATE POLICY "doctors_own_availabilities" ON availabilities
  FOR ALL TO authenticated
  USING (
    (mediq_current_role() = 'DOCTOR' OR mediq_current_role() IS NULL)
    AND mediq_is_doctor_owner(doctor_id)
  )
  WITH CHECK (
    (mediq_current_role() = 'DOCTOR' OR mediq_current_role() IS NULL)
    AND mediq_is_doctor_owner(doctor_id)
  );

-- ---------------------------------------------------------------------------
-- profiles/users: allow clinic staff to read records needed for desk/doctor UIs
-- (additive; does not remove existing own/admin/broad-read policies)
-- ---------------------------------------------------------------------------

DROP POLICY IF EXISTS "staff_read_profiles" ON profiles;
CREATE POLICY "staff_read_profiles" ON profiles
  FOR SELECT TO authenticated
  USING (mediq_current_role() IN ('DOCTOR', 'SECRETARY', 'ADMIN') OR mediq_current_role() IS NULL);

DROP POLICY IF EXISTS "staff_read_users" ON users;
CREATE POLICY "staff_read_users" ON users
  FOR SELECT TO authenticated
  USING (mediq_current_role() IN ('DOCTOR', 'SECRETARY', 'ADMIN') OR mediq_current_role() IS NULL);

-- ---------------------------------------------------------------------------
-- RPCs: doctor ownership checks tolerant of ID mappings (preserve semantics)
-- ---------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION public.confirm_appointment(
    p_appointment_id VARCHAR
)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
    v_role VARCHAR;
    v_patient_id VARCHAR;
    v_doctor_id VARCHAR;
    v_actor_user_id VARCHAR;
BEGIN
    v_role := mediq_current_role();
    v_actor_user_id := mediq_resolve_user_id();

    SELECT patient_id, doctor_id
      INTO v_patient_id, v_doctor_id
    FROM appointments
    WHERE id = p_appointment_id;

    IF v_patient_id IS NULL THEN
        RAISE EXCEPTION 'Appointment not found';
    END IF;

    IF v_role = 'PATIENT' THEN
        IF NOT mediq_is_patient_owner(v_patient_id) AND v_patient_id <> v_actor_user_id THEN
            RAISE EXCEPTION 'Not your appointment';
        END IF;
    ELSIF v_role = 'DOCTOR' THEN
        IF NOT mediq_is_doctor_owner(v_doctor_id) THEN
            RAISE EXCEPTION 'Not your appointment';
        END IF;
    ELSIF v_role IN ('SECRETARY', 'ADMIN') THEN
        -- allowed
    ELSE
        RAISE EXCEPTION 'Insufficient role';
    END IF;

    UPDATE appointments
    SET status = 'CONFIRMED', updated_at = NOW()
    WHERE id = p_appointment_id;
END;
$$;

GRANT EXECUTE ON FUNCTION public.confirm_appointment(VARCHAR) TO authenticated, anon;

CREATE OR REPLACE FUNCTION public.record_outcome(
    p_appointment_id VARCHAR,
    p_outcome VARCHAR
)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
    v_role VARCHAR;
    v_doctor_id VARCHAR;
BEGIN
    v_role := mediq_current_role();

    IF v_role <> 'DOCTOR' THEN
        RAISE EXCEPTION 'Only doctors can record outcomes';
    END IF;

    SELECT doctor_id INTO v_doctor_id
    FROM appointments
    WHERE id = p_appointment_id;

    IF v_doctor_id IS NULL THEN
        RAISE EXCEPTION 'Appointment not found';
    END IF;

    IF NOT mediq_is_doctor_owner(v_doctor_id) THEN
        RAISE EXCEPTION 'Not your appointment';
    END IF;

    UPDATE appointments
    SET outcome = p_outcome,
        status = 'COMPLETED',
        completed_at = NOW(),
        updated_at = NOW()
    WHERE id = p_appointment_id;
END;
$$;

GRANT EXECUTE ON FUNCTION public.record_outcome(VARCHAR, VARCHAR) TO authenticated, anon;

CREATE OR REPLACE FUNCTION public.cancel_appointment(
    p_appointment_id VARCHAR,
    p_cancel_reason TEXT DEFAULT NULL
)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
    v_role VARCHAR;
    v_patient_id VARCHAR;
    v_doctor_id VARCHAR;
    v_doctor_name VARCHAR;
    v_status VARCHAR;
    v_actor_user_id VARCHAR;
BEGIN
    v_role := mediq_current_role();
    v_actor_user_id := mediq_resolve_user_id();

    SELECT patient_id, doctor_id, status
      INTO v_patient_id, v_doctor_id, v_status
    FROM appointments
    WHERE id = p_appointment_id;

    IF v_patient_id IS NULL THEN
        RAISE EXCEPTION 'Appointment not found';
    END IF;

    IF v_status = 'CANCELLED' THEN
        RETURN;
    END IF;

    IF v_status NOT IN ('BOOKED', 'CONFIRMED') THEN
        RAISE EXCEPTION 'Appointment cannot be cancelled in its current status';
    END IF;

    SELECT full_name INTO v_doctor_name
    FROM users
    WHERE id = v_doctor_id
       OR id IN (SELECT d.user_id FROM doctors d WHERE d.id = v_doctor_id OR d.user_id = v_doctor_id)
    LIMIT 1;

    IF v_role = 'PATIENT' THEN
        IF NOT mediq_is_patient_owner(v_patient_id) AND v_patient_id <> v_actor_user_id THEN
            RAISE EXCEPTION 'Not your appointment';
        END IF;
    ELSIF v_role = 'DOCTOR' THEN
        IF NOT mediq_is_doctor_owner(v_doctor_id) THEN
            RAISE EXCEPTION 'Not your appointment';
        END IF;
    ELSIF v_role IN ('SECRETARY', 'ADMIN') THEN
        NULL;
    ELSE
        RAISE EXCEPTION 'Insufficient role';
    END IF;

    UPDATE appointments
    SET status = 'CANCELLED',
        cancel_reason = p_cancel_reason,
        updated_at = NOW()
    WHERE id = p_appointment_id;

    -- Best-effort patient notification (preserves existing behavior)
    BEGIN
        PERFORM public.send_notification(
            v_patient_id,
            'CANCELLED',
            'Appointment cancelled',
            'Your appointment with ' || COALESCE(v_doctor_name, 'your doctor') ||
            ' has been cancelled.' ||
            CASE
                WHEN p_cancel_reason IS NOT NULL AND btrim(p_cancel_reason) <> ''
                THEN ' Reason: ' || p_cancel_reason
                ELSE ''
            END,
            p_appointment_id
        );
    EXCEPTION WHEN undefined_function THEN
        -- send_notification may not exist in some environments; cancellation still succeeds
        NULL;
    END;
END;
$$;

GRANT EXECUTE ON FUNCTION public.cancel_appointment(VARCHAR, TEXT) TO authenticated, anon;

-- Grants for helpers (used by RPC/policies; execution context is definer-safe)
GRANT EXECUTE ON FUNCTION public.mediq_resolve_user_id() TO authenticated, anon;
GRANT EXECUTE ON FUNCTION public.mediq_is_doctor_owner(varchar) TO authenticated, anon;
GRANT EXECUTE ON FUNCTION public.mediq_is_patient_owner(varchar) TO authenticated, anon;

