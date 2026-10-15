# Implementation Plan: Supabase PostgreSQL Integration (Option A)

Following `SPEC-supabase.md`, this plan details the phases and tasks required to enable Supabase PostgreSQL while retaining H2 in-memory test compatibility.

## Tasks Breakdown

### Phase 1: Dependencies & Driver
- [ ] Add `postgresql` version and library definition to `gradle/libs.versions.toml`.
- [ ] Add `implementation(libs.server.postgresql)` to `server/build.gradle.kts`.
- [ ] Verify build compiles (`.\gradlew.bat :server:compileKotlin`).

### Phase 2: Configuration & Database Engine Abstraction
- [ ] Update `ServerConfig` in `server/.../Db.kt` to parse `MEDIQ_DB_USER` and `MEDIQ_DB_PASSWORD` if present.
- [ ] Add `val isPostgres: Boolean = config.jdbcUrl.startsWith("jdbc:postgresql:")` to `Database`.
- [ ] Configure HikariCP dataSource properties for PostgreSQL (driver class name, SSL parameters, pool size).

### Phase 3: Dialect Handling in Stores
- [ ] Update `DoctorStore.ensureSpecialties`: use `ON CONFLICT (id) DO UPDATE` for Postgres, keep `MERGE INTO` for H2.
- [ ] Update `DoctorStore.ensureSlots`: use `ON CONFLICT (id) DO NOTHING` for Postgres, keep `MERGE INTO` for H2.
- [ ] Update `DoctorStore.availableDates`: use `TO_CHAR(s.starts_at AT TIME ZONE '$zone', 'YYYY-MM-DD')` for Postgres, keep `FORMATDATETIME` for H2.
- [ ] Update `AppointmentStore.kt` constraint violation handling: catch both `SQLIntegrityConstraintViolationException` and Postgres `PSQLException` with SQLState `23505`.

### Phase 4: Schema Verification & Migration Support
- [ ] Review `server/src/main/resources/schema.sql` to guarantee valid execution under PostgreSQL.
- [ ] Write integration test verifying dialect branching and schema generation.

### Phase 5: Verification & Documentation
- [ ] Run `.\.mdfiles\check-boundaries.ps1` and `.\.mdfiles\check-contrast.ps1`.
- [ ] Run `.\gradlew.bat :server:test` (must stay green, $\ge 59$ tests).
- [ ] Update `README.md` and `.mdfiles/AGENTS.md` with instructions on configuring Supabase (`MEDIQ_JDBC_URL`, `MEDIQ_DB_USER`, `MEDIQ_DB_PASSWORD`).
