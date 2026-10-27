-- Seed data for MediQ application
-- Requires PostgreSQL with pgcrypto extension for gen_random_uuid()
-- Run this script after applying migrations. It is idempotent: it deletes the
-- seeded tables first, then re-inserts demo data, all in a single transaction.

\c mediq_db; -- adjust database name as needed

-- Enable extension for UUID generation
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

BEGIN;

-- Clear previously seeded data (FK-safe order: children before parents).
DELETE FROM appointments;
DELETE FROM doctors;
DELETE FROM refresh_tokens;
DELETE FROM users;
DELETE FROM specializations;

-- 1. Insert specializations
INSERT INTO specializations (id, name, description, doctor_count)
SELECT gen_random_uuid()::text, 
       spec.name,
       spec.description,
       0
FROM (VALUES
    ('Cardiology', 'Heart specialist'),
    ('Dermatology', 'Skin specialist'),
    ('Pediatrics', 'Children health'),
    ('Orthopedics', 'Musculoskeletal system'),
    ('Psychiatry', 'Mental health'),
    ('Neurology', 'Nervous system'),
    ('Gynecology', 'Women health'),
    ('Ophthalmology', 'Eye specialist'),
    ('Radiology', 'Imaging'),
    ('General Practice', 'Primary care')
) AS spec(name, description);

-- 2. Insert users (doctors/secretaries/patients), then doctors, then link users
--    to their doctor row. doctor_id is set NULL on the user insert to avoid any
--    circular FK, then populated by the UPDATE below.
--
--    `docs` is a temp table (not a CTE) so the same user_id/doctor_id pairs are
--    reused across the users, doctors, and UPDATE statements below; a CTE would
--    be re-evaluated per statement and produce different random UUIDs each time.
CREATE TEMP TABLE docs (
    seq int,
    user_id text,
    doctor_id text,
    spec_id text
) ON COMMIT DROP;

INSERT INTO docs (seq, user_id, doctor_id, spec_id)
SELECT generate_series(1,30) AS seq,
       gen_random_uuid()::text,
       gen_random_uuid()::text,
       (SELECT id FROM specializations ORDER BY random() LIMIT 1)
FROM generate_series(1,30);

INSERT INTO users (id, username, full_name, email, mobile_number, date_of_birth, role, status, doctor_id, created_at, updated_at)
SELECT user_id,
       'doc' || seq,
       'Dr. ' || md5(random()::text) AS full_name,
       'doc' || seq || '@mediq.com',
       '555-01' || lpad(seq::text,2,'0'),
       '1970-01-01',
       'DOCTOR',
       'ACTIVE',
       NULL,
       now(),
       now()
FROM docs;

INSERT INTO users (id, username, full_name, email, mobile_number, date_of_birth, role, status, assigned_doctor_id, created_at, updated_at)
SELECT gen_random_uuid()::text,
       'sec' || seq,
       'Sec ' || md5(random()::text),
       'sec' || seq || '@mediq.com',
       '555-02' || lpad(seq::text,2,'0'),
       '1985-01-01',
       'SECRETARY',
       'ACTIVE',
       NULL,
       now(),
       now()
FROM generate_series(1,20) AS seq;

INSERT INTO users (id, username, full_name, email, mobile_number, date_of_birth, role, status, created_at, updated_at)
SELECT gen_random_uuid()::text,
       'pat' || seq,
       'Patient ' || md5(random()::text),
       'pat' || seq || '@mediq.com',
       '555-03' || lpad(seq::text,2,'0'),
       (date '1990-01-01' + (seq % 30) * interval '1 year')::date,
       'PATIENT',
       'ACTIVE',
       now(),
       now()
FROM generate_series(1,150) AS seq;

INSERT INTO doctors (id, user_id, full_name, specialty, specialization_id, license_number, years_of_experience, fee_centavos, bio, building, floor, room, languages, status, created_at, updated_at)
SELECT d.doctor_id,
       d.user_id,
       'Dr. ' || md5(random()::text),
       (SELECT name FROM specializations WHERE id = d.spec_id),
       d.spec_id,
       'LIC' || lpad(d.seq::text,4,'0'),
       (floor(random()*30)+1)::int,
       (floor(random()*20000)+5000)::bigint,
       'Experienced professional.',
       'Main Building',
       (floor(random()*5)+1)::text,
       'Room ' || lpad(d.seq::text,2,'0'),
       'English,Spanish',
       'ACTIVE',
       now(),
       now()
FROM docs d;

-- Link each doctor user to its doctor row (after both sides exist, no circular FK).
UPDATE users u SET doctor_id = d.doctor_id
FROM docs d
WHERE u.id = d.user_id AND u.role = 'DOCTOR';

-- Assign each secretary to a random doctor (after doctors exist).
UPDATE users u SET assigned_doctor_id = (
  SELECT doctor_id FROM docs ORDER BY random() LIMIT 1
)
WHERE u.role = 'SECRETARY';

-- 3. Insert appointments (200) with varied status.
--    random() is computed exactly once per row (in the inner subquery) so the
--    status distribution is not skewed by multiple independent calls.
WITH appts AS (
  SELECT generate_series(1,200) AS seq,
         (SELECT id FROM users WHERE role='PATIENT' ORDER BY random() LIMIT 1) AS patient_id,
         (SELECT id FROM users WHERE role='DOCTOR' ORDER BY random() LIMIT 1) AS doctor_id,
         (now() - (floor(random()*30)::int) * interval '1 day') + (floor(random()*8)::int * interval '1 hour') AS start_time,
         (floor(random()*3)+1)*15 AS duration,
         r AS rand,
         'General consultation' AS reason
  FROM generate_series(1,200), LATERAL (SELECT random() AS r) AS rnd
)
INSERT INTO appointments (id, patient_id, doctor_id, starts_at, duration_minutes, status, reason, created_at, updated_at, completed_at, outcome)
SELECT gen_random_uuid()::text,
       patient_id,
       doctor_id,
       start_time,
       duration,
       CASE
         WHEN rand < 0.1 THEN 'CANCELLED'
         WHEN rand < 0.25 THEN 'NO_SHOW'
         WHEN rand < 0.75 THEN 'BOOKED'
         ELSE 'COMPLETED'
       END AS status,
       reason,
       now(),
       now(),
       CASE WHEN rand >= 0.75 THEN start_time + (duration || ' minutes')::interval ELSE NULL END,
       CASE WHEN rand >= 0.75 THEN 'COMPLETED' ELSE NULL END
FROM appts;

-- 4. Update specialization doctor_count
UPDATE specializations s SET doctor_count = sub.cnt
FROM (
  SELECT specialization_id AS spec_id, count(*) AS cnt
  FROM doctors
  GROUP BY specialization_id
) sub
WHERE s.id = sub.spec_id;

COMMIT;

-- Seed complete
