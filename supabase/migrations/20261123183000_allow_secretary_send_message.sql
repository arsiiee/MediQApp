-- Allow secretaries and admins to send/read/update messages as part of desk work.
-- Fixes: "Not a participant in this conversation" when a secretary replies via
-- POST /rest/v1/rpc/send_message. Secretaries/admins are not thread
-- patient/doctor participants, but they operate the clinic messaging desk.

-- 1) Authorize secretary/admin in send_message RPC
DROP FUNCTION IF EXISTS public.send_message(VARCHAR, TEXT, VARCHAR);

CREATE OR REPLACE FUNCTION public.send_message(
    p_conversation_id VARCHAR,
    p_body TEXT,
    p_appointment_id VARCHAR DEFAULT NULL
)
RETURNS UUID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
    v_role VARCHAR;
    v_author_id VARCHAR;
    v_author_auth_user_id UUID;
    v_thread_patient VARCHAR;
    v_thread_doctor VARCHAR;
    v_new_id UUID := gen_random_uuid();
    v_allowed BOOLEAN := FALSE;
BEGIN
    -- Resolve the authenticated user's domain record across auth bridge variants.
    SELECT u.id, u.auth_user_id
      INTO v_author_id, v_author_auth_user_id
    FROM users u
    WHERE u.auth_user_id = auth.uid()
       OR u.id = auth.uid()::VARCHAR
    LIMIT 1;

    IF v_author_id IS NULL THEN
        RAISE EXCEPTION 'Authenticated user has no record';
    END IF;

    -- Resolve role (profile is Supabase-auth source of truth; fall back safely).
    SELECT p.role INTO v_role
    FROM profiles p
    WHERE p.user_id = auth.uid()
       OR (v_author_auth_user_id IS NOT NULL AND p.user_id = v_author_auth_user_id)
    LIMIT 1;

    IF v_role IS NULL THEN
        v_role := mediq_current_role();
    END IF;
    IF v_role IS NULL THEN
        SELECT u.role INTO v_role FROM users u WHERE u.id = v_author_id;
    END IF;

    v_role := upper(COALESCE(v_role, ''));

    SELECT patient_id, doctor_id
      INTO v_thread_patient, v_thread_doctor
    FROM message_threads
    WHERE id = p_conversation_id;

    IF v_thread_patient IS NULL OR v_thread_doctor IS NULL THEN
        RAISE EXCEPTION 'Conversation not found';
    END IF;

    -- Direct thread participants can send.
    IF v_author_id = v_thread_patient OR v_author_id = v_thread_doctor THEN
        v_allowed := TRUE;
    END IF;

    -- Secretaries and admins do the desk work and may participate in clinic
    -- message threads (RLS already exposes threads/messages to them).
    IF NOT v_allowed AND v_role IN ('SECRETARY', 'ADMIN') THEN
        v_allowed := TRUE;
    END IF;

    IF NOT v_allowed THEN
        RAISE EXCEPTION 'Not a participant in this conversation';
    END IF;

    -- If a message references an appointment, keep it consistent with the thread.
    -- Accept doctor-id variants (users/auth id vs doctors.id) seen in existing data.
    IF p_appointment_id IS NOT NULL THEN
        IF NOT EXISTS (
            SELECT 1
            FROM appointments a
            WHERE a.id = p_appointment_id
              AND a.patient_id = v_thread_patient
              AND (
                  a.doctor_id = v_thread_doctor
                  OR EXISTS (
                      SELECT 1 FROM doctors d
                      WHERE d.user_id = v_thread_doctor
                        AND (d.user_id = a.doctor_id OR d.id = a.doctor_id)
                  )
                  OR EXISTS (
                      SELECT 1 FROM doctors d
                      WHERE d.id = v_thread_doctor
                        AND (d.user_id = a.doctor_id OR d.id = a.doctor_id)
                  )
              )
        ) THEN
            RAISE EXCEPTION 'Appointment does not belong to this conversation';
        END IF;
    END IF;

    INSERT INTO messages (id, conversation_id, author_id, author_role, body, sent_at, appointment_id)
    VALUES (
        v_new_id::VARCHAR,
        p_conversation_id,
        v_author_id,
        CASE WHEN v_role = '' THEN 'USER' ELSE v_role END,
        p_body,
        NOW(),
        p_appointment_id
    );

    UPDATE message_threads
    SET last_message_at = NOW(),
        unread_count = unread_count + 1,
        awaiting_action = TRUE
    WHERE id = p_conversation_id;

    RETURN v_new_id;
END;
$$;

GRANT EXECUTE ON FUNCTION public.send_message(VARCHAR, TEXT, VARCHAR) TO authenticated, anon;

-- 2) RLS for secretary/admin desk messaging via Supabase client.
-- message_threads SELECT for secretary/admin already exists; allow updates used
-- by mark-as-read and allow reading message rows in all threads.

DROP POLICY IF EXISTS "secretaries_admins_read_all_messages" ON messages;
CREATE POLICY "secretaries_admins_read_all_messages" ON messages
  FOR SELECT TO authenticated
  USING (mediq_current_role() IN ('SECRETARY', 'ADMIN'));

DROP POLICY IF EXISTS "secretaries_admins_update_threads" ON message_threads;
CREATE POLICY "secretaries_admins_update_threads" ON message_threads
  FOR UPDATE TO authenticated
  USING (mediq_current_role() IN ('SECRETARY', 'ADMIN'))
  WITH CHECK (mediq_current_role() IN ('SECRETARY', 'ADMIN'));