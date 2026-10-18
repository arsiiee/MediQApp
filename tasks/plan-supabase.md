# Implementation Plan: Supabase PostgreSQL Integration (Option A)

Following `SPEC-supabase.md`, this plan details the phases and tasks required to enable Supabase PostgreSQL while retaining H2 in-memory test compatibility.

**Status 2026-10-18: landed in `35250c3`, and reconciled against the tree here.**
Fourteen of the fifteen boxes are done and each is listed below with what
verifies it. **One is deliberately left open** (Phase 4, first box) — see the
note under it rather than a tick it has not earned.

## Tasks Breakdown

### Phase 1: Dependencies & Driver
- [x] Add `postgresql` version and library definition to `gradle/libs.versions.toml`. — `postgresql = "42.7.5"` and `server-postgresql = { group = "org.postgresql", … }`
- [x] Add `implementation(libs.server.postgresql)` to `server/build.gradle.kts`.
- [x] Verify build compiles (`.\gradlew.bat :server:compileKotlin`). — green

### Phase 2: Configuration & Database Engine Abstraction
- [x] Update `ServerConfig` in `server/.../Db.kt` to parse `MEDIQ_DB_USER` and `MEDIQ_DB_PASSWORD` if present. — `Db.kt:71-72`
- [x] Add `val isPostgres: Boolean = config.jdbcUrl.startsWith("jdbc:postgresql:")` to `Database`. — `Db.kt:105`
- [x] Configure HikariCP dataSource properties for PostgreSQL (driver class name, SSL parameters, pool size). — `Db.kt:107-122`: pool size 10, `driverClassName = "org.postgresql.Driver"` only on Postgres, and the H2 console property only on H2. **SSL is not set in code on purpose** — it rides on the URL's `sslmode` (README and `DatabaseDialectTest` both use `?sslmode=require`), because a code-level default would force TLS onto a local Postgres too.

### Phase 3: Dialect Handling in Stores
- [x] Update `DoctorStore.ensureSpecialties`: use `ON CONFLICT (id) DO UPDATE` for Postgres, keep `MERGE INTO` for H2. — `DoctorStore.kt:344-348`
- [x] Update `DoctorStore.ensureSlots`: use `ON CONFLICT (id) DO NOTHING` for Postgres, keep `MERGE INTO` for H2. — `DoctorStore.kt:268-271`
- [x] Update `DoctorStore.availableDates`: use `TO_CHAR(s.starts_at AT TIME ZONE '$zone', 'YYYY-MM-DD')` for Postgres, keep `FORMATDATETIME` for H2. — `DoctorStore.kt:174-177`
- [x] Update `AppointmentStore.kt` constraint violation handling: catch both `SQLIntegrityConstraintViolationException` and Postgres `PSQLException` with SQLState `23505`. — `AppointmentStore.kt:161-163`

### Phase 4: Schema Verification & Migration Support
- [ ] Review `server/src/main/resources/schema.sql` to guarantee valid execution under PostgreSQL.

  > **Still open, and it should stay visible.** The file was read line by line
  > against PostgreSQL syntax and `35250c3` changed nothing in it — which is a
  > review, not the guarantee this box asks for. It has **never been executed
  > against a real Postgres from this machine**: the Supabase host in `.env` does
  > not resolve here (`UnknownHostException: db.wdoyhtjpjwhmhsegdllvd.supabase.co`),
  > so the only engine the schema has actually booted on is H2. Closing this needs
  > one run against a reachable Postgres, not another read.
- [x] Write integration test verifying dialect branching and schema generation. — `DatabaseDialectTest` (1 test): `isPostgres` is false for an H2 URL and true for a Supabase-style one, and the H2 `Database(...)` construction bootstraps `schema.sql` into H2. **It does not connect to Postgres** — the Postgres half asserts config parsing, so it pins the branch, not Postgres execution.

### Phase 5: Verification & Documentation
- [x] Run `.\.mdfiles\check-boundaries.ps1` and `.\.mdfiles\check-contrast.ps1`. — re-run 2026-10-18: boundaries clean over 56 files, contrast clean (31 token pairs, 6 status chips, ratchet 4)
- [x] Run `.\gradlew.bat :server:test` (must stay green, $\ge 59$ tests). — green at **62**, 0 failures, counted from the JUnit XML. The floor has since moved 59 → 60 (`DatabaseDialectTest`) → 62 (`AppointmentLifecycleTest`)
- [x] Update `README.md` and `.mdfiles/AGENTS.md` with instructions on configuring Supabase (`MEDIQ_JDBC_URL`, `MEDIQ_DB_USER`, `MEDIQ_DB_PASSWORD`). — README had the section from `8e1380f`; `AGENTS.md` gained its own on 2026-10-18 under "Running the server", including the fact that the real environment wins over `.env`
