-- Ensure patient/staff date of birth is stored on profiles and available to web clients.
-- Idempotent: safe to re-run.

ALTER TABLE public.profiles
  ADD COLUMN IF NOT EXISTS date_of_birth VARCHAR(10);

-- Best-effort backfill from users table (covers rows where ids/auth linkage match).
UPDATE public.profiles p
SET date_of_birth = NULLIF(u.date_of_birth, '')
FROM public.users u
WHERE (p.date_of_birth IS NULL OR btrim(p.date_of_birth) = '')
  AND NULLIF(u.date_of_birth, '') IS NOT NULL
  AND (
    p.user_id::text = u.id::text
    OR EXISTS (
      SELECT 1
      FROM information_schema.columns c
      WHERE c.table_schema = 'public' AND c.table_name = 'users' AND c.column_name = 'auth_user_id'
    ) AND u.id IS NOT NULL AND p.user_id::text = (u.id)::text
  );

-- Backfill using auth_user_id linkage when that column exists.
DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'users' AND column_name = 'auth_user_id'
  ) THEN
    EXECUTE $q$
      UPDATE public.profiles p
      SET date_of_birth = NULLIF(u.date_of_birth, '')
      FROM public.users u
      WHERE (p.date_of_birth IS NULL OR btrim(p.date_of_birth) = '')
        AND NULLIF(u.date_of_birth, '') IS NOT NULL
        AND u.auth_user_id IS NOT NULL
        AND p.user_id = u.auth_user_id
    $q$;
  END IF;
END $$;

-- Keep trigger in sync so new self-registrations persist DOB/mobile in both tables.
CREATE OR REPLACE FUNCTION public.handle_new_auth_user()
RETURNS trigger AS $$
BEGIN
  INSERT INTO public.profiles (
    user_id, username, full_name, email, mobile_number, date_of_birth, role, status, created_at, updated_at
  )
  VALUES (
    NEW.id,
    COALESCE(NEW.raw_user_meta_data->>'username', split_part(NEW.email, '@', 1)),
    COALESCE(NEW.raw_user_meta_data->>'full_name', split_part(NEW.email, '@', 1)),
    NEW.email,
    COALESCE(NEW.raw_user_meta_data->>'mobile_number', ''),
    COALESCE(NEW.raw_user_meta_data->>'date_of_birth', ''),
    'PATIENT',
    'ACTIVE',
    NOW(),
    NOW()
  )
  ON CONFLICT (user_id) DO UPDATE SET
    username = EXCLUDED.username,
    full_name = EXCLUDED.full_name,
    email = EXCLUDED.email,
    mobile_number = COALESCE(NULLIF(EXCLUDED.mobile_number, ''), public.profiles.mobile_number),
    date_of_birth = COALESCE(NULLIF(EXCLUDED.date_of_birth, ''), public.profiles.date_of_birth),
    updated_at = NOW();

  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'users') THEN
    INSERT INTO public.users (
      id, username, full_name, email, mobile_number, date_of_birth, role, status, doctor_id, assigned_doctor_id, created_at, updated_at
    )
    VALUES (
      NEW.id,
      COALESCE(NEW.raw_user_meta_data->>'username', split_part(NEW.email, '@', 1)),
      COALESCE(NEW.raw_user_meta_data->>'full_name', split_part(NEW.email, '@', 1)),
      NEW.email,
      COALESCE(NEW.raw_user_meta_data->>'mobile_number', ''),
      COALESCE(NEW.raw_user_meta_data->>'date_of_birth', ''),
      'PATIENT',
      'ACTIVE',
      NULL,
      NULL,
      NOW(),
      NOW()
    )
    ON CONFLICT (id) DO UPDATE SET
      username = EXCLUDED.username,
      full_name = EXCLUDED.full_name,
      email = EXCLUDED.email,
      mobile_number = COALESCE(NULLIF(EXCLUDED.mobile_number, ''), public.users.mobile_number),
      date_of_birth = COALESCE(NULLIF(EXCLUDED.date_of_birth, ''), public.users.date_of_birth),
      updated_at = NOW();
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW
  EXECUTE FUNCTION public.handle_new_auth_user();