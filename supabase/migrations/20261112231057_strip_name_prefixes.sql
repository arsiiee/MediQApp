-- Remove "Dr."/"Sec."/"Secretary " prefixes from person names so UI labels don't double-prefix.
-- Also update cancel_appointment notification text to avoid hardcoding "Dr. ".

BEGIN;

-- Strip common title prefixes from stored display names.
UPDATE public.users
SET full_name = regexp_replace(full_name, '^\s*(Dr\.?|Sec\.?|Secretary)\s+', '', 'i'),
    updated_at = NOW()
WHERE full_name ~* '^\s*(Dr\.?|Sec\.?|Secretary)\s+';

UPDATE public.profiles
SET full_name = regexp_replace(full_name, '^\s*(Dr\.?|Sec\.?|Secretary)\s+', '', 'i'),
    updated_at = NOW()
WHERE full_name ~* '^\s*(Dr\.?|Sec\.?|Secretary)\s+';

UPDATE public.doctors
SET full_name = regexp_replace(full_name, '^\s*(Dr\.?|Sec\.?|Secretary)\s+', '', 'i'),
    updated_at = NOW()
WHERE full_name ~* '^\s*(Dr\.?|Sec\.?|Secretary)\s+';

-- Clean demo/historical notification text that embedded title prefixes.
UPDATE public.notifications
SET title = regexp_replace(regexp_replace(regexp_replace(title, '\mSecretary\s+', '', 'gi'), '\mSec\.\s*', '', 'gi'), '\mDr\.\s*', '', 'gi'),
    body = regexp_replace(regexp_replace(regexp_replace(body, '\mSecretary\s+', '', 'gi'), '\mSec\.\s*', '', 'gi'), '\mDr\.\s*', '', 'gi')
WHERE title ~* '\m(Dr\.|Sec\.|Secretary)\M'
   OR body ~* '\m(Dr\.|Sec\.|Secretary)\M';

-- Clean demo/historical message bodies that embedded title prefixes.
DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = 'public' AND table_name = 'messages'
  ) THEN
    EXECUTE $sql$
      UPDATE public.messages
      SET body = regexp_replace(regexp_replace(regexp_replace(body, '\mSecretary\s+', '', 'gi'), '\mSec\.\s*', '', 'gi'), '\mDr\.\s*', '', 'gi')
      WHERE body ~* '\m(Dr\.|Sec\.|Secretary)\M'
    $sql$;
  END IF;
END $$;

-- Keep cancel notification behavior but do not hardcode "Dr. " before the doctor name.
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
END;
$$;

GRANT EXECUTE ON FUNCTION public.cancel_appointment(VARCHAR, TEXT) TO authenticated, anon;

COMMIT;
