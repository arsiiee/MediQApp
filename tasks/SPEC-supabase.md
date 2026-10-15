# Spec: Supabase PostgreSQL Integration (Option A)

## Objective
Migrate MediQ's persistent storage backend from an ephemeral in-memory H2 database to Supabase-managed PostgreSQL while maintaining 100% backward compatibility with existing Ktor server logic and H2 unit testing.

### Why
Currently, the MediQ server runs on in-memory H2 by default (`jdbc:h2:mem:mediq`), meaning all patient, doctor, and appointment data is wiped whenever the server restarts. In production, `ServerConfig.validate()` explicitly refuses to boot on H2. Integrating Supabase via JDBC provides persistent storage, ACID transactions, and production-grade concurrency out of the box with zero client changes.

## Tech Stack
- **Database:** Supabase PostgreSQL (via direct or pooled connection string)
- **Driver:** PostgreSQL JDBC Driver `org.postgresql:postgresql:42.7.5`
- **Connection Pool:** HikariCP `6.3.0`
- **Compatibility Target:** Dual compatibility: PostgreSQL in production/staging (Supabase), H2 in-memory for fast zero-network unit tests (`:server:test`).

## Core Areas & Requirements

### 1. Database Driver & Dependencies
- Add `postgresql = "42.7.5"` to `gradle/libs.versions.toml`.
- Add `implementation(libs.server.postgresql)` to `server/build.gradle.kts`.

### 2. Dual-Engine SQL Dialect Abstraction
Two SQL constructs currently in `DoctorStore.kt` are H2-specific and fail on PostgreSQL:
1. `MERGE INTO ... KEY (id) VALUES (...)`:
   - H2 syntax. PostgreSQL uses `INSERT INTO ... ON CONFLICT (id) DO UPDATE / NOTHING` or `MERGE INTO` (PG 15+ has standard SQL MERGE, but `KEY (id)` clause is H2-specific).
   - Solution: Detect engine in `Database` (`isPostgres: Boolean = config.jdbcUrl.startsWith("jdbc:postgresql:")`).
   - For slot generation and specialties ensure, use SQL that is either portable or branched cleanly:
     - For `specialties`: `INSERT INTO specialties (id, display_name) VALUES (?, ?) ON CONFLICT (id) DO UPDATE SET display_name = EXCLUDED.display_name` (PostgreSQL) vs `MERGE INTO specialties (id, display_name) KEY (id) VALUES (?, ?)` (H2).
     - For `slots`: `INSERT INTO slots (id, doctor_id, starts_at, ends_at, status) VALUES (?, ?, ?, ?, 'available') ON CONFLICT (id) DO NOTHING` (PostgreSQL) vs `MERGE INTO slots ... KEY (id)` (H2).
2. `FORMATDATETIME(s.starts_at, 'yyyy-MM-dd', 'en', '$zone')`:
   - `FORMATDATETIME` is an H2-specific function.
   - On PostgreSQL, formatting an instant to clinic date is: `TO_CHAR(s.starts_at AT TIME ZONE '$zone', 'YYYY-MM-DD')`.
   - Solution: In `DoctorStore.availableDates()`, select `formatDateExpr`:
     - If Postgres: `TO_CHAR(s.starts_at AT TIME ZONE '$zone', 'YYYY-MM-DD')`
     - If H2: `FORMATDATETIME(s.starts_at, 'yyyy-MM-dd', 'en', '$zone')`

### 3. Constraint Violation Detection in `AppointmentStore.kt`
- Concurrency booking conflict is caught via `SQLIntegrityConstraintViolationException` in `AppointmentStore.kt:150`.
- Standard PostgreSQL JDBC driver throws `org.postgresql.util.PSQLException` with SQLState `23505` (unique_violation) or `SQLIntegrityConstraintViolationException` (subclass of `SQLException`).
- Ensure the catch block catches `SQLException` and checks for unique violation:
  `(e is java.sql.SQLIntegrityConstraintViolationException || (e is org.postgresql.util.PSQLException && e.sqlState == "23505"))` -> translates to `ApiError.conflict("That time was just taken. Please pick another.")`.

### 4. Configuration & Environment Variables
- `MEDIQ_JDBC_URL`:
  - Default: `jdbc:h2:mem:mediq;DB_CLOSE_DELAY=-1` (unchanged, keeping zero-dependency local dev and CI green).
  - Production / Supabase: `jdbc:postgresql://db.<PROJECT-REF>.supabase.co:5432/postgres?sslmode=require` or port 6543 (transaction pooler).
- `MEDIQ_DB_USER` and `MEDIQ_DB_PASSWORD`:
  - Support setting user/password either inside JDBC URL or via separate environment variables `MEDIQ_DB_USER` / `MEDIQ_DB_PASSWORD`.
- In `Db.kt`:
  - If `jdbc:postgresql:`, configure Hikari with Postgres driver class `org.postgresql.Driver`.
  - Set session time zone: `connectionInitSql = "SET TIME ZONE '${config.sessionTimeZone}'"`. Note: in PostgreSQL, `SET TIME ZONE 'Asia/Manila'` is valid syntax.

### 5. Schema Compatibility (`schema.sql`)
- Verify all table declarations in `server/src/main/resources/schema.sql` are valid PostgreSQL:
  - `VARCHAR(n)`, `BIGINT`, `INT`, `BOOLEAN`, `DATE`, `TIME`, `TIMESTAMP WITH TIME ZONE` are all standard PostgreSQL types.
  - `CREATE TABLE IF NOT EXISTS` and `CREATE INDEX IF NOT EXISTS` are standard PostgreSQL.
  - Check constraints (`CHECK (fee_centavos >= 0)`, etc.) are standard PostgreSQL.

## Boundaries
- **Always do:**
  - Keep H2 unit tests passing 100% (all 59 server tests, including `BookingConcurrencyTest` and `AvailableDatesTest`).
  - Maintain the `:server` floor of $\ge 59$ tests in `CONSTRAINTS.md`.
  - Keep architecture boundaries clean (`check-boundaries.ps1` and `check-contrast.ps1`).
- **Ask first:**
  - Dropping any existing tables or columns.
  - Introducing ORMs (Exposed, Hibernate) or heavy frameworks.
- **Never do:**
  - Commit live Supabase database passwords or connection strings to git.
  - Break in-memory H2 development setup.

## Success Criteria
1. PostgreSQL JDBC driver is integrated into `:server`.
2. Database initialization and migration run cleanly on both PostgreSQL and H2.
3. Dialect-sensitive queries (`specialties`, slot generation, and `availableDates` formatting) execute correctly without syntax errors.
4. `:server:test` passes all 59+ unit tests on H2 with zero regressions.
5. `smoke.ps1` and manual instructions allow connecting to any Supabase PostgreSQL instance simply by providing `MEDIQ_JDBC_URL`.
