-- MediQ initial schema for the Supabase project.
-- Mirrors server/src/main/resources/db/migration/V1__initial_schema.sql (Flyway) so the
-- Supabase project schema stays in sync with the Ktor domain entities (com.example.mediq.domain.model).
--
-- The Ktor application runs Flyway against this same schema at startup when configured
-- with a Postgres/Supabase `db.url` (H2 remains the dev default). All DDL below is
-- dialect-neutral standard SQL and applies cleanly on both H2 and PostgreSQL/Supabase.

CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(255) PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    mobile_number VARCHAR(255) NOT NULL,
    date_of_birth VARCHAR(10) NOT NULL,
    role VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    doctor_id VARCHAR(255),
    assigned_doctor_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS password_hashes (
    user_id VARCHAR(255) PRIMARY KEY REFERENCES users(id),
    hash VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS specializations (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    doctor_count INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS doctors (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL UNIQUE REFERENCES users(id),
    full_name VARCHAR(255) NOT NULL,
    specialty VARCHAR(255) NOT NULL,
    specialization_id VARCHAR(255) NOT NULL REFERENCES specializations(id),
    license_number VARCHAR(255) NOT NULL,
    years_of_experience INTEGER NOT NULL,
    fee_centavos BIGINT NOT NULL,
    bio TEXT NOT NULL,
    building VARCHAR(255) NOT NULL,
    floor VARCHAR(255) NOT NULL,
    room VARCHAR(255) NOT NULL,
    languages TEXT NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS clinic_hours (
    id SERIAL PRIMARY KEY,
    doctor_id VARCHAR(255) NOT NULL REFERENCES doctors(id),
    day_of_week INTEGER NOT NULL,
    start_time VARCHAR(10) NOT NULL,
    end_time VARCHAR(10) NOT NULL,
    is_closed BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS availabilities (
    id SERIAL PRIMARY KEY,
    doctor_id VARCHAR(255) NOT NULL REFERENCES doctors(id),
    date VARCHAR(10) NOT NULL,
    sessions TEXT NOT NULL,
    is_override BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE(doctor_id, date)
);

CREATE TABLE IF NOT EXISTS appointments (
    id VARCHAR(255) PRIMARY KEY,
    patient_id VARCHAR(255) NOT NULL REFERENCES users(id),
    doctor_id VARCHAR(255) NOT NULL REFERENCES users(id),
    starts_at TIMESTAMP NOT NULL,
    duration_minutes INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL,
    reason TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    outcome VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS message_threads (
    id VARCHAR(255) PRIMARY KEY,
    patient_id VARCHAR(255) NOT NULL REFERENCES users(id),
    doctor_id VARCHAR(255) NOT NULL REFERENCES users(id),
    subject VARCHAR(255) NOT NULL,
    last_message_at TIMESTAMP NOT NULL,
    unread_count INTEGER NOT NULL DEFAULT 0,
    awaiting_action BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS messages (
    id VARCHAR(255) PRIMARY KEY,
    conversation_id VARCHAR(255) NOT NULL REFERENCES message_threads(id),
    author_id VARCHAR(255) NOT NULL REFERENCES users(id),
    author_role VARCHAR(50) NOT NULL,
    body TEXT NOT NULL,
    sent_at TIMESTAMP NOT NULL,
    read_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS notifications (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL REFERENCES users(id),
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    data TEXT,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    read_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS otp_codes (
    otp_key VARCHAR(255) PRIMARY KEY,
    code VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    token VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL REFERENCES users(id),
    expires_at TIMESTAMP NOT NULL
);
