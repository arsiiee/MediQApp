-- Allow patients to cancel their own appointments (including confirmed ones).
-- DEV/PERMISSIVE: cancels any BOOKED/CONFIRMED appointment by id.
-- Keeps both 2-arg and 3-arg overloads for old/new clients.

BEGIN;

DROP FUNCTION IF EXISTS public.cancel_appointment(character varying, character varying, text);
DROP FUNCTION IF EXISTS public.cancel_appointment(character varying, text);

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
    v_patient_id VARCHAR;
    v_doctor_id VARCHAR;
    v_doctor_name VARCHAR;
    v_status VARCHAR;
BEGIN
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
    FROM users WHERE id = v_doctor_id
    LIMIT 1;

    UPDATE appointments
    SET status = 'CANCELLED',
        cancel_reason = NULLIF(btrim(COALESCE(p_cancel_reason, '')), ''),
        updated_at = NOW()
    WHERE id = p_appointment_id;

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
        NULL;
    WHEN OTHERS THEN
        NULL;
    END;
END;
$$;

CREATE OR REPLACE FUNCTION public.cancel_appointment(
    p_appointment_id VARCHAR,
    p_patient_id VARCHAR,
    p_cancel_reason TEXT DEFAULT NULL
)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
BEGIN
    PERFORM public.cancel_appointment(p_appointment_id, p_cancel_reason);
END;
$$;

GRANT EXECUTE ON FUNCTION public.cancel_appointment(VARCHAR, TEXT) TO authenticated, anon;
GRANT EXECUTE ON FUNCTION public.cancel_appointment(VARCHAR, VARCHAR, TEXT) TO authenticated, anon;

COMMIT;
