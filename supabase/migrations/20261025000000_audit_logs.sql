-- Supabase migration: audit_logs table.
-- Mirrors server/src/main/resources/db/migration/V2__audit_logs.sql (Flyway)
-- so the Supabase schema stays in sync with the Ktor domain entities.
--
-- Required by: JdbcAuditRepository, AuditService, Routes.kt (GET /audit-logs),
-- DatabaseSeeder.kt (seedAuditLogs), and supabase/migrations/V2__seed_demo_data.sql.

CREATE TABLE IF NOT EXISTS audit_logs (
    id VARCHAR(255) PRIMARY KEY,
    actor VARCHAR(255) NOT NULL,
    action VARCHAR(255) NOT NULL,
    target VARCHAR(255) NOT NULL,
    at TIMESTAMP NOT NULL,
    severity VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_at ON audit_logs(at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_severity ON audit_logs(severity);
