-- Seed data for MediQ application
-- Requires PostgreSQL with pgcrypto extension for gen_random_uuid()
-- Run this script after applying migrations

\c mediq_db; -- adjust database name as needed

-- Enable extension for UUID generation
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

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

-- 2. Insert doctors (30)
WITH docs AS (
  SELECT generate_series(1,30) AS seq,
         gen_random_uuid()::text AS user_id,
         gen_random_uuid()::text AS doctor_id,
         (SELECT id FROM specializations ORDER BY random() LIMIT 1) AS spec_id
)
INSERT INTO users (id, username, full_name, email, mobile_number, date_of_birth, role, status, doctor_id, created_at, updated_at)
SELECT user_id,
       'doc' || seq,
       'Dr. ' || md5(random()::text) AS full_name,
       'doc' || seq || '@mediq.com',
       '555-01' || lpad(seq::text,2,'0'),
       '1970-01-01',
       'DOCTOR',
       'ACTIVE',
       doctor_id,
       now(),
       now()
FROM docs;

INSERT INTO doctors (id, user_id, full_name, specialty, specialization_id, license_number, years_of_experience, fee_centavos, bio, building, floor, room, languages, status, created_at, updated_at)
SELECT doctor_id,
       user_id,
       'Dr. ' || md5(random()::text),
       (SELECT name FROM specializations WHERE id = spec_id),
       spec_id,
       'LIC' || lpad(seq::text,4,'0'),
       (floor(random()*30)+1)::int,
       (floor(random()*20000)+5000)::bigint,
       'Experienced professional.',
       'Main Building',
       (floor(random()*5)+1)::text,
       'Room ' || lpad(seq::text,2,'0'),
       'English,Spanish',
       'ACTIVE',
       now(),
       now()
FROM docs;

-- 3. Insert secretaries (20) and assign each to a random doctor
WITH secs AS (
  SELECT generate_series(1,20) AS seq,
         gen_random_uuid()::text AS user_id,
         (SELECT doctor_id FROM doctors ORDER BY random() LIMIT 1) AS assigned_doc_id
)
INSERT INTO users (id, username, full_name, email, mobile_number, date_of_birth, role, status, assigned_doctor_id, created_at, updated_at)
SELECT user_id,
       'sec' || seq,
       'Sec ' || md5(random()::text),
       'sec' || seq || '@mediq.com',
       '555-02' || lpad(seq::text,2,'0'),
       '1985-01-01',
       'SECRETARY',
       'ACTIVE',
       assigned_doc_id,
       now(),
       now()
FROM secs;

-- 4. Insert patients (150)
WITH pats AS (
  SELECT generate_series(1,150) AS seq,
         gen_random_uuid()::text AS user_id
)
INSERT INTO users (id, username, full_name, email, mobile_number, date_of_birth, role, status, created_at, updated_at)
SELECT user_id,
       'pat' || seq,
       'Patient ' || md5(random()::text),
       'pat' || seq || '@mediq.com',
       '555-03' || lpad(seq::text,2,'0'),
       (date '1990-01-01' + (seq % 30) * interval '1 year')::date,
       'PATIENT',
       'ACTIVE',
       now(),
       now()
FROM pats;

-- 5. Insert appointments (200) with varied status
WITH appts AS (
  SELECT generate_series(1,200) AS seq,
         (SELECT id FROM users WHERE role='PATIENT' ORDER BY random() LIMIT 1) AS patient_id,
         (SELECT id FROM users WHERE role='DOCTOR' ORDER BY random() LIMIT 1) AS doctor_id,
         (now() - (floor(random()*30)::int) * interval '1 day') + (floor(random()*8)::int * interval '1 hour') AS start_time,
         (floor(random()*3)+1)*15 AS duration,
         CASE
           WHEN random() < 0.1 THEN 'CANCELLED'
           WHEN random() < 0.15 THEN 'NO_SHOW'
           WHEN random() < 0.5 THEN 'BOOKED'
           ELSE 'COMPLETED'
         END AS status,
         'General consultation' AS reason
)
INSERT INTO appointments (id, patient_id, doctor_id, starts_at, duration_minutes, status, reason, created_at, updated_at, completed_at, outcome)
SELECT gen_random_uuid()::text,
       patient_id,
       doctor_id,
       start_time,
       duration,
       status,
       reason,
       now(),
       now(),
       CASE WHEN status='COMPLETED' THEN start_time + (duration || ' minutes')::interval ELSE NULL END,
       CASE WHEN status='COMPLETED' THEN 'SUCCESS' ELSE NULL END
FROM appts;

-- 6. Update specialization doctor_count
UPDATE specializations s SET doctor_count = sub.cnt
FROM (
  SELECT specialization_id AS spec_id, count(*) AS cnt
  FROM doctors
  GROUP BY specialization_id
) sub
WHERE s.id = sub.spec_id;

-- Seed complete
