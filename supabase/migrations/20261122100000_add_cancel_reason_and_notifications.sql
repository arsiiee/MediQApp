-- File: supabase/migrations/20261122100000_add_cancel_reason_and_notifications.sql
-- Purpose: Add cancel_reason to appointments, create send_notification RPC,
--          and update cancel_appointment to accept a reason + notify patient.
-- ========================================================================

-- Allow storing why an appointment was cancelled
ALTER TABLE public.appointments
ADD COLUMN IF NOT EXISTS cancel_reason TEXT;

-- Create a notification row so the patient sees why their appointment changed
CREATE OR REPLACE FUNCTION public.send_notification(
    p_user_id VARCHAR,
    p_type VARCHAR,
    p_title VARCHAR,
    p_body TEXT,
    p_data TEXT DEFAULT NULL
)
RETURNS UUID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
    v_new_id UUID := gen_random_uuid();
BEGIN
    INSERT INTO notifications (id, user_id, type, title, body, data, is_read, created_at)
    VALUES (v_new_id::VARCHAR, p_user_id, p_type, p_title, p_body, p_data, FALSE, NOW());
    RETURN v_new_id;
END;
$$;

-- Update cancel_appointment to accept a cancellation reason and notify the patient
DROP FUNCTION IF EXISTS public.cancel_appointment(VARCHAR);

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
BEGIN
    v_role := mediq_current_role();

    SELECT patient_id, doctor_id INTO v_patient_id, v_doctor_id
    FROM appointments WHERE id = p_appointment_id;

    IF v_patient_id IS NULL THEN
        RAISE EXCEPTION 'Appointment not found';
    END IF;

    SELECT full_name INTO v_doctor_name
    FROM users WHERE id = v_doctor_id;

    IF v_role = 'PATIENT' THEN
        IF v_patient_id <> (SELECT id FROM users WHERE auth_user_id = auth.uid()) THEN
            RAISE EXCEPTION 'Not your appointment';
        END IF;
    ELSIF v_role = 'DOCTOR' THEN
        IF v_doctor_id <> (SELECT id FROM users WHERE auth_user_id = auth.uid()) THEN
            RAISE EXCEPTION 'Not your appointment';
        END IF;
    ELSIF v_role IN ('SECRETARY', 'ADMIN') THEN
        NULL; -- allowed
    ELSE
        RAISE EXCEPTION 'Insufficient role';
    END IF;

    UPDATE appointments
    SET status = 'CANCELLED',
        cancel_reason = p_cancel_reason,
        updated_at = NOW()
    WHERE id = p_appointment_id;

    -- Notify the patient about the cancellation, including the reason if given
    PERFORM public.send_notification(
        v_patient_id,
        'CANCELLED',
        'Appointment cancelled',
        'Your appointment with Dr. ' || COALESCE(v_doctor_name, 'your doctor') ||
        ' has been cancelled.' ||
        CASE
            WHEN p_cancel_reason IS NOT NULL AND p_cancel_reason <> ''
            THEN ' Reason: ' || p_cancel_reason
            ELSE ''
        END,
        p_appointment_id
    );
END;
$$;

GRANT EXECUTE ON FUNCTION public.cancel_appointment(VARCHAR, TEXT) TO authenticated, anon;
GRANT EXECUTE ON FUNCTION public.send_notification(VARCHAR, VARCHAR, VARCHAR, TEXT, TEXT) TO authenticated, anon;
