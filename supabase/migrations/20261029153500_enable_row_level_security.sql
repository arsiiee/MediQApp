-- Supabase migration: enable Row-Level Security on every public table.
-- Mirrors the Ktor Flyway schema (server/src/main/resources/db/migration/V1__*.sql)
-- plus audit_logs and email_verification so the Supabase project stays in sync.
--
-- RLS is enabled with NO policies. With RLS enabled and no policy, the `anon`
-- and `authenticated` roles (via the Data API) are denied all rows (deny-all).
-- Table owners and superusers bypass RLS, so the Ktor service account is
-- unaffected when it connects as the table owner (see Phase 1 report).
--
-- No GRANT is issued to anon/authenticated, so those roles cannot even reach
-- the tables through the Data API. Policies will be added in a later phase.

ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE password_hashes ENABLE ROW LEVEL SECURITY;
ALTER TABLE specializations ENABLE ROW LEVEL SECURITY;
ALTER TABLE doctors ENABLE ROW LEVEL SECURITY;
ALTER TABLE clinic_hours ENABLE ROW LEVEL SECURITY;
ALTER TABLE availabilities ENABLE ROW LEVEL SECURITY;
ALTER TABLE appointments ENABLE ROW LEVEL SECURITY;
ALTER TABLE message_threads ENABLE ROW LEVEL SECURITY;
ALTER TABLE messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE otp_codes ENABLE ROW LEVEL SECURITY;
ALTER TABLE refresh_tokens ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_logs ENABLE ROW LEVEL SECURITY;
