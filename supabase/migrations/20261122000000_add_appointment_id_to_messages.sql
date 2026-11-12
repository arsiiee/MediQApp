-- File: supabase/migrations/20261122000000_add_appointment_id_to_messages.sql
-- Purpose: Add appointment_id to messages so a message can reference the
--          appointment it is about. Also update send_message RPC to accept
--          an optional p_appointment_id parameter.
-- ========================================================================

ALTER TABLE public.messages
ADD COLUMN IF NOT EXISTS appointment_id VARCHAR(255) REFERENCES public.appointments(id);

CREATE INDEX IF NOT EXISTS idx_messages_appointment_id
ON public.messages(appointment_id);

DROP FUNCTION IF EXISTS public.send_message(VARCHAR, VARCHAR, VARCHAR);

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
    v_thread_patient VARCHAR;
    v_thread_doctor VARCHAR;
    v_new_id UUID := gen_random_uuid();
BEGIN
    v_role := mediq_current_role();

    SELECT id INTO v_author_id FROM users WHERE id = auth.uid()::VARCHAR;
    IF v_author_id IS NULL THEN
        RAISE EXCEPTION 'Authenticated user has no record';
    END IF;

    SELECT patient_id, doctor_id INTO v_thread_patient, v_thread_doctor
    FROM message_threads WHERE id = p_conversation_id;

    IF v_thread_patient IS NULL THEN
        RAISE EXCEPTION 'Conversation not found';
    END IF;

    IF v_author_id <> v_thread_patient AND v_author_id <> v_thread_doctor THEN
        RAISE EXCEPTION 'Not a participant in this conversation';
    END IF;

    IF p_appointment_id IS NOT NULL THEN
        IF NOT EXISTS (
            SELECT 1 FROM appointments
            WHERE id = p_appointment_id
              AND (doctor_id = v_thread_doctor OR patient_id = v_thread_patient)
        ) THEN
            RAISE EXCEPTION 'Appointment does not belong to this conversation';
        END IF;
    END IF;

    INSERT INTO messages (id, conversation_id, author_id, author_role, body, sent_at, appointment_id)
    VALUES (v_new_id::VARCHAR, p_conversation_id, v_author_id, v_role, p_body, NOW(), p_appointment_id);

    UPDATE message_threads
    SET last_message_at = NOW(),
        unread_count = unread_count + 1,
        awaiting_action = TRUE
    WHERE id = p_conversation_id;

    RETURN v_new_id;
END;
$$;
