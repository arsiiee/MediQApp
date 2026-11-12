-- Purpose: Remove ambiguous book_appointment overloads (causing PostgREST HTTP 300)
--          and restore a single canonical RPC with same-day duplicate guard,
--          doctor-conflict checks, role-aware patient resolution, and
--          availability validation compatible with clinic_hours/availabilities.
--          Safe to apply via `supabase db query --linked --file ...`.

BEGIN;

-- Remove stale overloads that make PostgREST unable to choose a function.
DROP FUNCTION IF EXISTS public.book_appointment(
    character varying,
    timestamp without time zone,
    integer,
    text
);

DROP FUNCTION IF EXISTS public.book_appointment(
    character varying,
    character varying,
    timestamp without time zone,
    integer,
    text
);

CREATE OR REPLACE FUNCTION public.book_appointment(
    p_patient_id VARCHAR,
    p_doctor_id VARCHAR,
    p_starts_at VARCHAR,
    p_duration_minutes INTEGER,
    p_reason TEXT
)
RETURNS UUID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
    v_patient_id VARCHAR;
    v_conflict INTEGER;
    v_avail INTEGER;
    v_new_id UUID := gen_random_uuid();
    v_role VARCHAR;
    v_starts_at TIMESTAMP;
    v_duration_interval INTERVAL;
    v_dow INTEGER;
BEGIN
    v_role := mediq_current_role();

    IF v_role NOT IN ('PATIENT', 'DOCTOR', 'SECRETARY') THEN
        RAISE EXCEPTION 'Only patients, doctors, and secretaries can book appointments';
    END IF;

    BEGIN
        v_starts_at := p_starts_at::timestamp;
    EXCEPTION WHEN OTHERS THEN
        RAISE EXCEPTION 'Invalid starts_at timestamp: %', p_starts_at;
    END;

    v_duration_interval := (p_duration_minutes || ' minutes')::interval;

    IF v_role = 'PATIENT' THEN
        SELECT id INTO v_patient_id
        FROM users
        WHERE auth_user_id = auth.uid();
        IF v_patient_id IS NULL THEN
            RAISE EXCEPTION 'Authenticated user has no patient record';
        END IF;
    ELSE
        v_patient_id := p_patient_id;
        IF v_patient_id IS NULL THEN
            RAISE EXCEPTION 'Patient id is required';
        END IF;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM appointments
        WHERE patient_id = v_patient_id
          AND status IN ('BOOKED', 'CONFIRMED')
          AND DATE(starts_at) = DATE(v_starts_at)
    ) THEN
        RAISE EXCEPTION 'You already have an appointment on this day.';
    END IF;

    SELECT COUNT(*) INTO v_conflict
    FROM appointments
    WHERE doctor_id = p_doctor_id
      AND status <> 'CANCELLED'
      AND starts_at < v_starts_at + v_duration_interval
      AND starts_at + v_duration_interval > v_starts_at;

    IF v_conflict > 0 THEN
        RAISE EXCEPTION 'Doctor already has an appointment at that time';
    END IF;

    v_dow := EXTRACT(DOW FROM v_starts_at)::integer;

    SELECT COUNT(*) INTO v_avail
    FROM availabilities
    WHERE doctor_id = p_doctor_id
      AND date = v_starts_at::date;

    IF v_avail = 0 THEN
        SELECT COUNT(*) INTO v_avail
        FROM clinic_hours
        WHERE doctor_id = p_doctor_id
          AND day_of_week = v_dow
          AND COALESCE(is_closed, false) = false;
    END IF;

    IF v_avail = 0 THEN
        RAISE EXCEPTION 'Doctor is not available on that date';
    END IF;

    INSERT INTO appointments (id, patient_id, doctor_id, starts_at, duration_minutes, status, reason, created_at, updated_at)
    VALUES (v_new_id, v_patient_id, p_doctor_id, v_starts_at, p_duration_minutes, 'BOOKED', p_reason, NOW(), NOW());

    RETURN v_new_id;
END;
$$;

GRANT EXECUTE ON FUNCTION public.book_appointment(VARCHAR, VARCHAR, VARCHAR, INTEGER, TEXT)
    TO authenticated, anon;

COMMIT;
