-- Fix in-app notifications staying unread: tolerant ownership, read/update access,
-- and mark-read helpers. Additive/idempotent (no data reset).
--
-- Background:
-- - 20261108213000 created notifications SELECT policy only (strict users.id via auth_user_id).
-- - send_notification stores p_user_id as provided (appointments/users ids vary across flows),
--   so strict matching and missing UPDATE policy prevent clients from persisting read state.
-- - Android/web call table UPDATE/PATCH for is_read; RLS denied updates -> unread keeps popping up.

ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notifications ADD COLUMN IF NOT EXISTS read_at timestamptz;

-- Tolerant "own notification" predicate across auth.uid()/users.id/profile-linked ids.
-- mediq_is_patient_owner()/mediq_resolve_user_id() are identity-tolerant helpers added earlier.

DROP POLICY IF EXISTS "users_read_own_notifications" ON notifications;
CREATE POLICY "users_read_own_notifications" ON notifications
  FOR SELECT TO authenticated
  USING (
    user_id = auth.uid()::text
    OR user_id = mediq_resolve_user_id()
    OR mediq_is_patient_owner(user_id)
    OR user_id IN (SELECT p.user_id::text FROM profiles p WHERE p.user_id = auth.uid())
  );

DROP POLICY IF EXISTS "users_update_own_notifications" ON notifications;
CREATE POLICY "users_update_own_notifications" ON notifications
  FOR UPDATE TO authenticated
  USING (
    user_id = auth.uid()::text
    OR user_id = mediq_resolve_user_id()
    OR mediq_is_patient_owner(user_id)
    OR user_id IN (SELECT p.user_id::text FROM profiles p WHERE p.user_id = auth.uid())
  )
  WITH CHECK (
    user_id = auth.uid()::text
    OR user_id = mediq_resolve_user_id()
    OR mediq_is_patient_owner(user_id)
    OR user_id IN (SELECT p.user_id::text FROM profiles p WHERE p.user_id = auth.uid())
  );

-- Mark a single notification read for the signed-in owner (idempotent).
CREATE OR REPLACE FUNCTION public.mark_notification_read(p_notification_id varchar)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
BEGIN
  IF auth.uid() IS NULL THEN
    RAISE EXCEPTION 'Not authenticated';
  END IF;

  UPDATE notifications
  SET
    is_read = TRUE,
    read_at = COALESCE(read_at, NOW())
  WHERE id = p_notification_id
    AND (
      user_id = auth.uid()::text
      OR user_id = mediq_resolve_user_id()
      OR mediq_is_patient_owner(user_id)
      OR user_id IN (SELECT p.user_id::text FROM profiles p WHERE p.user_id = auth.uid())
    );
END;
$$;

-- Mark all notifications read for the signed-in owner (idempotent).
CREATE OR REPLACE FUNCTION public.mark_all_notifications_read()
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
BEGIN
  IF auth.uid() IS NULL THEN
    RAISE EXCEPTION 'Not authenticated';
  END IF;

  UPDATE notifications
  SET
    is_read = TRUE,
    read_at = COALESCE(read_at, NOW())
  WHERE COALESCE(is_read, FALSE) = FALSE
    AND (
      user_id = auth.uid()::text
      OR user_id = mediq_resolve_user_id()
      OR mediq_is_patient_owner(user_id)
      OR user_id IN (SELECT p.user_id::text FROM profiles p WHERE p.user_id = auth.uid())
    );
END;
$$;

GRANT EXECUTE ON FUNCTION public.mark_notification_read(varchar) TO authenticated;
GRANT EXECUTE ON FUNCTION public.mark_all_notifications_read() TO authenticated;
