-- Allow patients to cancel their own appointments (including confirmed ones).
-- Robust version: resolves actor via mediq_resolve_user_id() and matches
-- against appointment patient_id directly, with role-based fallback.

BEGIN;

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
    FROM users WHERE id = v_doctor_id;

    -- Patient cancels their own appointment: match patient_id to resolved actor id
    IF v_patient_id = v_actor_user_id THEN
        NULL;
    -- Doctor cancels their own appointment
    ELSIF v_role = 'DOCTOR' AND v_doctor_id = v_actor_user_id THEN
        NULL;
    -- Secretary/admin can cancel any appointment
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

    -- Best-effort patient notification (send_notification may not exist)
    BEGIN
        PERFORM public.send_notification(
            v_patient_id,
            'CANCELLED',
            'Appointment cancelled',
            'Your appointment with ' || COALESCE(v_doctor_name, 'your doctor') ||
            ' has been cancelled.' ||
            CASE
                WHEN p_cancel_reason IS NOT NULL AND p_cancel_reason <> ''
                THEN ' Reason: ' || p_cancel_reason
                ELSE ''
            END,
            p_appointment_id
        );
    EXCEPTION WHEN undefined_function THEN
        NULL;
    END;
END;
$$;

GRANT EXECUTE ON FUNCTION public.cancel_appointment(VARCHAR, TEXT) TO authenticated, anon;

COMMIT;
