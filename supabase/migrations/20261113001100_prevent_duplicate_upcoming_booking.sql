-- Prevent patients from booking another appointment while they have an active upcoming one.
-- Applies to all book_appointment overloads used by clients.

BEGIN;

CREATE OR REPLACE FUNCTION public.book_appointment(
    p_doctor_id VARCHAR,
    p_starts_at TIMESTAMP,
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
BEGIN
    IF mediq_current_role() <> 'PATIENT' THEN
        RAISE EXCEPTION 'Only patients can book appointments';
    END IF;

    SELECT id INTO v_patient_id FROM users WHERE auth_user_id = auth.uid();
    IF v_patient_id IS NULL THEN
        RAISE EXCEPTION 'Authenticated user has no patient record';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM appointments
        WHERE patient_id = v_patient_id
          AND status IN ('BOOKED', 'CONFIRMED')
          AND starts_at > NOW()
    ) THEN
        RAISE EXCEPTION 'You already have an upcoming appointment. Cancel or complete it before booking another.';
    END IF;

    SELECT COUNT(*) INTO v_conflict
    FROM appointments
    WHERE doctor_id = p_doctor_id
      AND status <> 'CANCELLED'
      AND starts_at < p_starts_at + (p_duration_minutes || ' minutes')::interval
      AND starts_at + (p_duration_minutes || ' minutes')::interval > p_starts_at;

    IF v_conflict > 0 THEN
        RAISE EXCEPTION 'Doctor already has an appointment at that time';
    END IF;

    SELECT COUNT(*) INTO v_avail
    FROM availabilities
    WHERE doctor_id = p_doctor_id
      AND date = p_starts_at::date;

    IF v_avail = 0 THEN
        RAISE EXCEPTION 'Doctor is not available on that date';
    END IF;

    INSERT INTO appointments (id, patient_id, doctor_id, starts_at, duration_minutes, status, reason, created_at, updated_at)
    VALUES (v_new_id, v_patient_id, p_doctor_id, p_starts_at, p_duration_minutes, 'BOOKED', p_reason, NOW(), NOW());

    RETURN v_new_id;
END;
$$;

CREATE OR REPLACE FUNCTION public.book_appointment(
    p_patient_id VARCHAR,
    p_doctor_id VARCHAR,
    p_starts_at TIMESTAMP,
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
BEGIN
    v_role := mediq_current_role();

    IF v_role NOT IN ('PATIENT', 'DOCTOR', 'SECRETARY') THEN
        RAISE EXCEPTION 'Only patients, doctors, and secretaries can book appointments';
    END IF;

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
          AND starts_at > NOW()
    ) THEN
        RAISE EXCEPTION 'You already have an upcoming appointment. Cancel or complete it before booking another.';
    END IF;

    SELECT COUNT(*) INTO v_conflict
    FROM appointments
    WHERE doctor_id = p_doctor_id
      AND status <> 'CANCELLED'
      AND starts_at < p_starts_at + (p_duration_minutes || ' minutes')::interval
      AND starts_at + (p_duration_minutes || ' minutes')::interval > p_starts_at;

    IF v_conflict > 0 THEN
        RAISE EXCEPTION 'Doctor already has an appointment at that time';
    END IF;

    SELECT COUNT(*) INTO v_avail
    FROM availabilities
    WHERE doctor_id = p_doctor_id
      AND date = p_starts_at::date;

    IF v_avail = 0 THEN
        RAISE EXCEPTION 'Doctor is not available on that date';
    END IF;

    INSERT INTO appointments (id, patient_id, doctor_id, starts_at, duration_minutes, status, reason, created_at, updated_at)
    VALUES (v_new_id, v_patient_id, p_doctor_id, p_starts_at, p_duration_minutes, 'BOOKED', p_reason, NOW(), NOW());

    RETURN v_new_id;
END;
$$;

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
          AND starts_at > NOW()
    ) THEN
        RAISE EXCEPTION 'You already have an upcoming appointment. Cancel or complete it before booking another.';
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

    SELECT COUNT(*) INTO v_avail
    FROM availabilities
    WHERE doctor_id = p_doctor_id
      AND date = v_starts_at::date;

    IF v_avail = 0 THEN
        RAISE EXCEPTION 'Doctor is not available on that date';
    END IF;

    INSERT INTO appointments (id, patient_id, doctor_id, starts_at, duration_minutes, status, reason, created_at, updated_at)
    VALUES (v_new_id, v_patient_id, p_doctor_id, v_starts_at, p_duration_minutes, 'BOOKED', p_reason, NOW(), NOW());

    RETURN v_new_id;
END;
$$;

COMMIT;
