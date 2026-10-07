-- MediQ server schema.
--
-- Read this file before changing it. It is the only place the data shape is
-- defined, and once real patient records exist, changing it means migrating
-- existing rows.
--
-- All timestamps are TIMESTAMP WITH TIME ZONE and are stored in UTC. The clinic
-- zone (Asia/Manila) is applied on display only, by the app.

-- ---------------------------------------------------------------------------
-- Accounts
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS users (
    id                VARCHAR(36) PRIMARY KEY,
    username          VARCHAR(64)  NOT NULL,
    -- PBKDF2 hash, never the password itself.
    password_hash     VARCHAR(512) NOT NULL,
    full_name         VARCHAR(160) NOT NULL,
    email             VARCHAR(254),
    -- E.164, e.g. +639175550142
    mobile_number     VARCHAR(20)  NOT NULL,
    date_of_birth     DATE         NOT NULL,
    sex               VARCHAR(24),
    address           VARCHAR(400),
    role              VARCHAR(24)  NOT NULL,
    -- Set when a doctor record is linked, so appointments can be traced back.
    doctor_id         VARCHAR(36),
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT users_username_unique UNIQUE (username),
    CONSTRAINT users_mobile_unique   UNIQUE (mobile_number)
);

-- Sessions exist so sign-out actually kills the token. A bare JWT cannot be
-- revoked before it expires, which means "sign out" would leave the token
-- usable for anyone who captured it. The session id is carried in the token
-- and checked on every authenticated request.
CREATE TABLE IF NOT EXISTS sessions (
    id            VARCHAR(36) PRIMARY KEY,
    user_id       VARCHAR(36) NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at    TIMESTAMP WITH TIME ZONE,
    CONSTRAINT sessions_user_fk FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX IF NOT EXISTS idx_sessions_user ON sessions (user_id);

-- One-time codes for the registration flow. `consumed_at` is what stops a
-- captured OTP from being replayed to register someone else's number.
CREATE TABLE IF NOT EXISTS otp_codes (
    id              VARCHAR(36) PRIMARY KEY,
    mobile_number   VARCHAR(20)  NOT NULL,
    code_hash       VARCHAR(512) NOT NULL,
    attempt_count   INT          NOT NULL DEFAULT 0,
    expires_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    consumed_at     TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_otp_mobile ON otp_codes (mobile_number);

-- ---------------------------------------------------------------------------
-- Doctors and availability
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS specialties (
    id              VARCHAR(24) PRIMARY KEY,
    display_name    VARCHAR(80) NOT NULL
);

CREATE TABLE IF NOT EXISTS doctors (
    id                  VARCHAR(36) PRIMARY KEY,
    full_name           VARCHAR(160) NOT NULL,
    specialty_id        VARCHAR(24)  NOT NULL,
    years_experience    INT          NOT NULL,
    fee_centavos        BIGINT       NOT NULL,
    license_number      VARCHAR(64)  NOT NULL,
    bio                 VARCHAR(2000) NOT NULL,
    building            VARCHAR(120) NOT NULL,
    floor               VARCHAR(40)  NOT NULL,
    room                VARCHAR(40)  NOT NULL,
    -- Comma-separated wire values, e.g. "en,fil".
    languages           VARCHAR(120) NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT doctors_specialty_fk FOREIGN KEY (specialty_id) REFERENCES specialties (id),
    CONSTRAINT doctors_fee_nonneg  CHECK (fee_centavos >= 0)
);

CREATE INDEX IF NOT EXISTS idx_doctors_specialty ON doctors (specialty_id);

-- One row per recurring clinic session, e.g. Monday 09:00-12:00.
CREATE TABLE IF NOT EXISTS clinic_hours (
    id           VARCHAR(36) PRIMARY KEY,
    doctor_id    VARCHAR(36) NOT NULL,
    day_of_week  INT         NOT NULL, -- 1 = Monday .. 7 = Sunday, java.time convention
    opens_at     TIME        NOT NULL,
    closes_at    TIME        NOT NULL,
    CONSTRAINT clinic_hours_doctor_fk FOREIGN KEY (doctor_id) REFERENCES doctors (id),
    CONSTRAINT clinic_hours_day_range CHECK (day_of_week BETWEEN 1 AND 7)
);

CREATE INDEX IF NOT EXISTS idx_clinic_hours_doctor ON clinic_hours (doctor_id, day_of_week);

-- Concrete bookable windows. Generated from clinic_hours, so a slot exists as a
-- row before anyone books it — which is what makes the unique constraint below
-- possible.
CREATE TABLE IF NOT EXISTS slots (
    id          VARCHAR(36) PRIMARY KEY,
    doctor_id   VARCHAR(36) NOT NULL,
    starts_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    ends_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    -- 'available' | 'blocked'. A slot that is taken is still 'available' here;
    -- the claim table is the source of truth for whether it is booked.
    status      VARCHAR(16) NOT NULL DEFAULT 'available',
    CONSTRAINT slots_doctor_fk  FOREIGN KEY (doctor_id) REFERENCES doctors (id),
    CONSTRAINT slots_status_chk CHECK (status IN ('available', 'blocked')),
    CONSTRAINT slots_window_chk CHECK (ends_at > starts_at)
);

-- This is the query the app actually runs: every slot for a doctor on a day.
CREATE INDEX IF NOT EXISTS idx_slots_doctor_time ON slots (doctor_id, starts_at);

-- ---------------------------------------------------------------------------
-- Appointments
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS appointments (
    id                  VARCHAR(36) PRIMARY KEY,
    user_id             VARCHAR(36) NOT NULL,
    doctor_id           VARCHAR(36) NOT NULL,
    slot_id             VARCHAR(36) NOT NULL,
    starts_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    ends_at             TIMESTAMP WITH TIME ZONE NOT NULL,
    status              VARCHAR(32)  NOT NULL,
    -- Free text from the patient. Read only for the appointment's owner and
    -- staff, never on list endpoints.
    reason_for_visit    VARCHAR(1000),
    confirmed_by_patient BOOLEAN      NOT NULL DEFAULT FALSE,
    requested_slot_id   VARCHAR(36),
    reschedule_note     VARCHAR(1000),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT appointments_user_fk   FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT appointments_doctor_fk FOREIGN KEY (doctor_id) REFERENCES doctors (id),
    CONSTRAINT appointments_slot_fk   FOREIGN KEY (slot_id) REFERENCES slots (id),
    CONSTRAINT appointments_status_chk CHECK (
        status IN ('pending_confirmation', 'confirmed', 'completed', 'cancelled', 'declined')
    )
);

CREATE INDEX IF NOT EXISTS idx_appointments_user ON appointments (user_id, starts_at);
CREATE INDEX IF NOT EXISTS idx_appointments_doctor ON appointments (doctor_id, starts_at);

-- ===========================================================================
-- DOUBLE-BOOKING PROTECTION
--
-- slot_claims holds at most one row per slot, enforced by the primary key.
-- Booking a slot means INSERTing here. Two patients tapping the same 9:30 AM
-- slot at the same instant produce two inserts; the database rejects the second
-- with a primary-key violation, and the transaction rolls back.
--
-- This is deliberately NOT a check-then-insert in application code. That
-- pattern has a window between the check and the write, and two requests can
-- pass the same check. The constraint has no window.
--
-- Cancelling deletes the claim, which frees the slot for rebooking while the
-- appointment row is kept for history.
-- ===========================================================================

CREATE TABLE IF NOT EXISTS slot_claims (
    slot_id         VARCHAR(36) PRIMARY KEY,
    appointment_id  VARCHAR(36) NOT NULL,
    claimed_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT slot_claims_appointment_fk FOREIGN KEY (appointment_id) REFERENCES appointments (id)
);

CREATE INDEX IF NOT EXISTS idx_slot_claims_appointment ON slot_claims (appointment_id);

-- ---------------------------------------------------------------------------
-- Notifications
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS notifications (
    id                      VARCHAR(36) PRIMARY KEY,
    user_id                 VARCHAR(36) NOT NULL,
    type                    VARCHAR(40) NOT NULL,
    title                   VARCHAR(160) NOT NULL,
    body                    VARCHAR(1000) NOT NULL,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL,
    read_at                 TIMESTAMP WITH TIME ZONE,
    related_appointment_id  VARCHAR(36),
    CONSTRAINT notifications_user_fk FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX IF NOT EXISTS idx_notifications_user ON notifications (user_id, created_at DESC);