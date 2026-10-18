# MediQ

A clinic appointment app for the Philippines. Patients browse doctors, see real availability, book slots, and manage appointments.

- **Client (`:app`):** Android / Jetpack Compose / Material 3 / MVVM
- **Backend (`:server`):** Ktor / Netty / HikariCP / PostgreSQL (Supabase) & H2
- **Contract:** Shared domain models (`domain/model/`), decoupled wire DTOs (`http/Dtos.kt`)

> [!WARNING]
> **Active Development — Not Finished:**
> Parts of the app are functional and covered by unit tests; other parts are stubs. **Do not put real patient data into this system.** OTP codes are currently returned in API responses, authentication tokens are stored in plaintext preferences, and SMS provider integration is pending. Read [Known Gaps & Debt](#known-gaps--debt) before assuming a feature is production-ready.

---

## Quick Navigation

- [Executive Dashboard](#executive-dashboard)
- [Quick Start](#quick-start)
- [Feature Status](#feature-status)
- [System Architecture](#system-architecture)
- [The Golden Rules (Never Break These)](#the-golden-rules-never-break-these)
- [Developer Guide: Adding a Feature](#developer-guide-adding-a-feature)
- [Configuration & Environment](#configuration--environment)
- [Testing & Quality Gates](#testing--quality-gates)
- [Known Gaps & Debt](#known-gaps--debt)
- [Repo Layout & References](#repo-layout--references)

---

## Executive Dashboard

Every number below is measured directly by automated checks, not claimed:

| Gate / Metric | Command | Status / Value | Notes |
|---|---|---|---|
| **App Compilation** | `.\gradlew.bat :app:compileDebugKotlin` |  **Pass** | 6 deprecation warnings (pre-existing) |
| **App Unit Tests** | `.\gradlew.bat :app:testDebugUnitTest` |  **213 pass** | All 10 ViewModels covered |
| **Server Tests** | `.\gradlew.bat :server:test` |  **101 pass** | Concurrency, auth, bookings, Supabase dialect, staff sign-in, admin directory, account status, staff creation |
| **Architecture Boundaries** | `.\.mdfiles\check-boundaries.ps1` |  **Clean (0 violations)** | 56 files checked |
| **Color Contrast (WCAG AA)** | `.\.mdfiles\check-contrast.ps1` |  **Clean** | 31 token pairs + 6 status chips; ratchet = 4 |
| **CI Automation** | `.github/workflows/ci.yml` |  **Enforced** | Runs gates, server tests, lint, unit tests |

---

## Quick Start

### 1. Start Backend Server (`:server`)

#### Option A: Zero-Config Local In-Memory (H2)
```powershell
$env:MEDIQ_SEED_DEMO="true"
$env:MEDIQ_PORT="8080"
.\gradlew.bat :server:run --console=plain
```

#### Option B: Persistent Cloud Database (Supabase PostgreSQL)
Create or edit `.env` in the project root (auto-loaded on boot; ignored by git):
```properties
MEDIQ_JDBC_URL=jdbc:postgresql://db.wdoyhtjpjhmhsegdllvd.supabase.co:5432/postgres?sslmode=require
MEDIQ_DB_USER=postgres
MEDIQ_DB_PASSWORD=your_supabase_password_here
MEDIQ_SEED_DEMO=true
MEDIQ_PORT=8080
```
Then start the server:
```powershell
.\gradlew.bat :server:run --console=plain
```

### 2. Verify Backend
```powershell
curl http://127.0.0.1:8080/health
# Returns: {"status":"ok"}
```

### 3. Run Android App (`:app`)
1. Set `RetrofitClient.BASE_URL` in `app/src/main/java/com/example/mediq/data/api/RetrofitClient.kt`:
   - Android Emulator: `http://10.0.2.2:8080/`
   - Physical Phone: LAN IP (e.g. `http://192.168.1.XX:8080/`)
2. Build and launch:
```powershell
.\gradlew.bat :app:installDebug
adb shell am start -n com.example.mediq/.MainActivity
```
*Demo Credentials:* `demo_patient` / `demo12345` (available when `MEDIQ_SEED_DEMO=true`).

---

## Feature Status

| Feature | Status | Details |
|---|:---:|---|
| **Sign-in & Sessions** |  Done | PBKDF2 hash, JWT + server-side revocation on sign-out, session signal & 401 expiry recovery |
| **Registration Flow** |  Done | 4-step wizard: Details $\rightarrow$ OTP $\rightarrow$ Password $\rightarrow$ Session |
| **Doctor Directory** |  Done | Search by name & specialty, debounced filter, "Next open" derived |
| **Real Availability** |  Done | Generated from clinic hours, month stepper, date/slot picker; claimed slots drawn greyed |
| **Atomic Booking** |  Done | `slot_claims` unique constraint prevents double-booking (HTTP 409) |
| **Appointment Management**|  Done | Upcoming & history lists, detail views, cancel & reschedule |
| **Notifications** |  Done | System alerts, mark-as-read |
| **Staff Web Sign-in** | ⚠️ Partial | The separate `mediqweb` staff app signs in against this server (real session, role-carried JWT, CORS allow-list). The account directories it displays have real server endpoints, **role-gated to administrators**, and the server can now **create** a staff account — a doctor's account and its practice record in one transaction — as well as **disable and re-enable** one, refused at sign-in with live tokens stopping immediately. The web is not wired to any of it yet: its pages still render sample data. See `tasks/SPEC-web.md` and `tasks/SPEC-web-admin-accounts.md`. |
| **Supabase PostgreSQL** |  Done | Dual-engine support (Postgres in prod, H2 in tests) |
| **Dark Theme** |  Done | 100% WCAG AA compliant contrast in both light & dark modes |
| **SMS OTP Dispatch** | ⚠️ Stub | Returns OTP in response body; SMS gateway not connected |
| **Profile Editing** | ⚠️ Display-only | Server API exists; UI edit screen not yet wired |
| **Messages Tab** | ❌ Placeholder | Renders placeholder screen |
| **Forgot Password** | ❌ Stub | Button click handler is empty |

---

## System Architecture

```
                       ┌──────────────────────────────┐
                       │  app/src/.../domain/model/   │
                       │    (Pure Kotlin Models)      │
                       └──────────────┬───────────────┘
                                      │
            ┌─────────────────────────┴─────────────────────────┐
            ▼                                                   ▼
┌──────────────────────────────┐                    ┌──────────────────────────────┐
│        Android Client        │                    │         Ktor Server          │
│            (:app)            │                    │          (:server)           │
├──────────────────────────────┤                    ├──────────────────────────────┤
│ UI: Jetpack Compose + MVVM   │                    │ Routes: Ktor Routing         │
│ UI State: LoadState<T>       │                    │ Auth: JWT + PBKDF2 + Sessions│
│ Repo: Retrofit + call { }    │                    │ DB: HikariCP + Raw JDBC      │
│ DI: AppContainer (Manual)    │                    │ Storage: Supabase PG / H2    │
└──────────────────────────────┘                    └──────────────────────────────┘
```

- **Domain is Shared Pure Kotlin:** Compiled directly into both `:app` and `:server` (`sharedDomain`). Contains zero Android or Compose dependencies.
- **Strict Decoupling:** Domain models and HTTP DTOs (`http/Dtos.kt` and `data/api/ApiDtos.kt`) are deliberately kept separate so backend API changes don't silently corrupt app domain contracts.

---

## The Golden Rules (Never Break These)

These rules are enforced by local scripts and CI gates. Violating any of them breaks the build:

1. 🚫 **`ui` must NEVER import `data/*`:** ViewModels and Screens only interact with `domain/*`.
2. 🚫 **`domain` must NEVER import Android or Compose:** Must stay portable plain Kotlin for JVM `:server`.
3. 🛡️ **Always wrap API calls:** Every `api.` call in `Retrofit*Repository` must be wrapped in `call { ... }` (`ApiErrors.kt`).
4. 🎭 **Masked fields require explicit keyboard options:** Any password or OTP field must set `KeyboardType.Password`/`NumberPassword` and `autoCorrectEnabled = false`.
5. 🎨 **No raw color literals:** UI must consume design tokens from `core/designsystem/theme/Color.kt` or theme color schemes.
6. 🔒 **Atomic concurrency:** Double-booking is stopped by the database constraint on `slot_claims`, never by "check-then-insert" application code.

---

## Developer Guide: Adding a Feature

Follow this standard 4-step path to add any full-stack feature:

```
[1. Domain Contract] ──► [2. Server API] ──► [3. Client Data] ──► [4. UI & ViewModel]
```

### Step 1: Define Domain Model & Repository (`domain/`)
Create pure Kotlin classes under `app/src/main/java/com/example/mediq/domain/`:
- `domain/model/Prescription.kt` (use `Instant` for timestamps, `Money` for currency)
- `domain/repository/PrescriptionRepository.kt` (all methods must be `suspend`)

### Step 2: Implement Server Endpoints (`server/`)
1. Add table statements to `server/src/main/resources/schema.sql` (`CREATE TABLE IF NOT EXISTS`).
2. Add store in `server/src/main/kotlin/com/example/mediq/server/db/`.
3. Add request/response DTOs to `http/Dtos.kt`.
4. Register routes in `http/Routes.kt`.
5. Write integration tests in `server/src/test/kotlin/`.

### Step 3: Implement Client Data Layer (`data/`)
1. Declare API DTOs in `data/api/ApiDtos.kt` and mapper extensions in `data/api/ApiMappers.kt`.
2. Add endpoints to `MediQApiService.kt`.
3. Implement `RetrofitPrescriptionRepository.kt` wrapping calls in `call { api.getPrescriptions() }`.
4. Register the repository in `di/AppContainer.kt`.

### Step 4: Implement UI & ViewModel (`ui/`)
1. Create ViewModel exposing `LoadState<T>` (`Loading`, `Success`, `Error`).
2. Handle errors by catching `ApiFailure` and displaying `e.message` (never framework messages).
3. Build Compose Screen using shared components: `MediQBrandAppBar`, `MediQInfoCard`, `MediQButton`, `EmptyState`.
4. Format currency via `money.format()` / `money.formatWithCentavos()` and clinic time via `.toClinicDate()` / `.toClinicTime()`.
5. Register screen in `ui/navigation/Routes.kt` and `ui/navigation/MediQNavHost.kt`.

### Step 5: Unit Test Harness
- Write fake repository in `app/src/test/java/.../fake/`.
- If ViewModel initializes data in `init { }`, instantiate it using **`by lazy { MyViewModel(fake) }`** (prevents eager completion before test fixtures are loaded).

---

## Configuration & Environment

Configuration is read from environment variables or a root `.env` file on startup:

| Variable | Default | Purpose |
|---|---|---|
| `MEDIQ_PORT` | `8080` | HTTP port the server listens on |
| `MEDIQ_JDBC_URL` | `jdbc:h2:mem:mediq;DB_CLOSE_DELAY=-1` | Database URL (H2 or Supabase PostgreSQL) |
| `MEDIQ_DB_USER` | `null` | Database user (e.g. `postgres`) |
| `MEDIQ_DB_PASSWORD` | `null` | Database password |
| `MEDIQ_SEED_DEMO` | `false` | Seeds mock doctors, demo patient, and three demo staff accounts (`demo_admin`, `demo_doctor`, `demo_secretary` — password `demo12345`) on startup |
| `MEDIQ_WEB_ORIGINS` | `http://localhost:4200,http://127.0.0.1:4200` | Comma-separated exact origins the browser frontend may call from (CORS allow-list; the port is part of the match) |
| `MEDIQ_ENV` | *unset* | Set to `production` to enforce security guards |
| `MEDIQ_JWT_SECRET` | `dev-only-...` | JWT signing secret ($\ge 32$ chars required in prod) |

---

## Testing & Quality Gates

Run the local quality gates before committing code:

```powershell
# 1. Architecture boundaries check (< 5s)
.\.mdfiles\check-boundaries.ps1

# 2. WCAG contrast and raw color ratchet (< 5s)
.\.mdfiles\check-contrast.ps1

# 3. Compile and run test suites (~90s)
.\gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest :server:test --console=plain
```

### Pre-commit Checklist
- [ ] `check-boundaries.ps1` reports 0 errors.
- [ ] `check-contrast.ps1` reports 0 errors; raw-color ratchet $\le 4$.
- [ ] `:server:test` passes ($\ge 101$ tests).
- [ ] `:app:testDebugUnitTest` passes ($\ge 213$ tests).
- [ ] Zero suppressions (`@Suppress`, `@SuppressLint`) or `TODO()` stubs.

---

## Known Gaps & Debt

- **SMS Integration:** `AuthService.returnCodeToCaller` is true; OTP is returned in the API body. Needs real SMS provider.
- **Client Token Storage:** Currently stored in plaintext `SharedPreferences`. Needs migration to `EncryptedSharedPreferences` (Tink).
- **Pagination in App:** Server supports cursor pagination, but client does not yet consume `nextCursor`.
- **Database Migrations:** Schema is applied via `CREATE TABLE IF NOT EXISTS` on boot. Migrate to Flyway once patient data is live.
- **Idempotency Keys:** `POST /appointments` lacks an idempotency key header for network disconnect retries.
- **Staff Web Covers Sign-in Only:** `mediqweb` (a sibling clone, not in this repo) authenticates for real; its admin, doctor, and secretary areas still render in-memory sample data. Its create-staff form collects neither a mobile number nor a date of birth — both `NOT NULL` columns the server now requires — and no practice details at all, so it cannot yet drive `POST /admin/accounts`.
- **The Web is Outside the Quality Gate:** `check-boundaries.ps1`, `check-contrast.ps1`, and both Gradle test tasks cover `:app` and `:server` only. The web's own `npm test` / `ng build` are its gate.
- **Only the Admin Routes Are Role-Gated:** `GET /admin/accounts`, `GET /admin/specializations`, `PATCH /admin/accounts/{id}/status` and `POST /admin/accounts` are the only routes that check the caller's role (403 for a doctor's token). Every other authenticated route still accepts any signed-in account, so the web's router guard is *still* not a security boundary outside `/admin`. A route added without `Caller.requireRole` is readable by every role.
- **A New Doctor Cannot Be Booked Until Someone Gives Them Hours:** `POST /admin/accounts` creates a doctor's account *and* its `doctors` row in one transaction, and deliberately writes **no clinic hours** — inventing a week of sessions would make the clinic's bookable calendar a guess. The doctor appears in the directory and can sign in, but `GET /doctors/{id}/availability` has nothing to offer until the doctor-workspace module adds a schedule editor. `lastActiveOn` stays absent for a different reason: the only activity recorded is when a session was created, which is a last sign-in, not a last activity.
- **A Staff Password Is Chosen by the Administrator, Not the Holder:** nothing forces a change on first use and there is no invitation email, so the password has to be passed on out of band. Every staff account works that way today.

---

## Repo Layout & References

```
MediQApp/
├── app/                  # Android Jetpack Compose client
├── server/               # Ktor backend (Netty, JDBC, Supabase/H2)
├── supabase/             # Supabase CLI project config & migrations
├── gradle/               # Wrapper & libs.versions.toml version catalog
├── tasks/                # Specifications (SPEC-*.md) and plans (plan.md, todo.md)
├── .mdfiles/             # Standing architectural guides
│   ├── AGENTS.md         # Full developer orientation and testing traps
│   ├── CONSTRAINTS.md    # Enforced numerical ratchets and quality bar
│   ├── check-boundaries.ps1 # Architecture boundary verification script
│   └── check-contrast.ps1   # WCAG contrast measurement script
├── .env.example          # Sample environment variables
└── README.md             # This document
```

The staff frontend (`mediqweb`) is a **separate repository**, cloned as a sibling
at `../mediqweb` rather than vendored in. Module 1 of the integration
(`web-staff-auth`) changed both trees; module 2 (`web-admin-accounts`) now has
its whole **server** half in this repo — the directories, the account status
column, and `POST /admin/accounts` — with its web half still to do, because
rewiring the pages is a separate slice. The map for the remaining modules is
`tasks/SPEC-web.md`.

### Key Documentation Links
- [AGENTS.md](file:///C:/Users/You/Desktop/MediQApp/.mdfiles/AGENTS.md) — Comprehensive architecture conventions and testing traps.
- [CONSTRAINTS.md](file:///C:/Users/You/Desktop/MediQApp/.mdfiles/CONSTRAINTS.md) — Exact numerical ratchets and test floors.
- [SPEC-supabase.md](file:///C:/Users/You/Desktop/MediQApp/tasks/SPEC-supabase.md) — Supabase PostgreSQL integration specification.
- [SPEC-web.md](file:///C:/Users/You/Desktop/MediQApp/tasks/SPEC-web.md) — Capability map for the five `mediq-web` integration modules.
- [SPEC-web-staff-auth.md](file:///C:/Users/You/Desktop/MediQApp/tasks/SPEC-web-staff-auth.md) — Module 1: real staff sign-in, with per-criterion verdicts.
- [SPEC-web-admin-accounts.md](file:///C:/Users/You/Desktop/MediQApp/tasks/SPEC-web-admin-accounts.md) — Module 2: the admin directories, the authorization mechanism, and the four decisions that block the rest.
