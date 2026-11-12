-- Canonicalize Dr. Smith (doc-001) clinic hours: remove duplicates/overlaps and
-- align with published availability (split-day aware, no duplicate rows).
-- Dr. Smith: doctors.id=doc-001, users/auth id=b5a53f20-9010-4728-8601-0ec8e4c4257b

BEGIN;

DELETE FROM public.clinic_hours
WHERE doctor_id = 'doc-001';

INSERT INTO public.clinic_hours (doctor_id, day_of_week, start_time, end_time, is_closed)
VALUES
  ('doc-001', 0, '13:00', '20:00', false), -- Sunday
  ('doc-001', 1, '09:00', '17:00', false), -- Monday
  ('doc-001', 2, '09:00', '17:00', false), -- Tuesday
  ('doc-001', 3, '09:00', '17:00', false), -- Wednesday
  ('doc-001', 4, '09:00', '17:00', false), -- Thursday
  ('doc-001', 5, '05:00', '12:00', false), -- Friday
  ('doc-001', 6, '05:00', '12:00', false); -- Saturday

COMMIT;
