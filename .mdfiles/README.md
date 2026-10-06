# MediQ

A clinic appointment app for the Philippines: patients browse doctors, see real
availability, book a slot, and manage appointments. Android/Compose client, Ktor
backend, shared domain model.

**Status: working end to end in development, not production-ready.** See
[Known gaps](#known-gaps) — three of them are security or data-integrity items
that must be fixed before real patients are involved.

---

## Current condition

Verified on this machine by running the commands below, not inferred.

| Check | Command | Result |
|---|---|---|
| App compiles | `:app:compileDebugKotlin` | Passes, 1 deprecation warning |
| Server tests | `:server:test` | 6/6 pass |
| App tests | `:app:testDebugUnitTest` | None — templates only |
| Runs on a device | `:app:connectedAndroidTest` | Cannot run, no emulator/AVD |
| Server | `:server:run` | Starts, H2 in-memory |

`76` Kotlin files. `19` HTTP endpoints. One remaining compile warning is the
deprecated `statusBarColor` in `core/designsystem/theme/Theme.kt:53`.

The Android test suite is effectively empty — `ExampleUnitTest` and
`ExampleInstrumentedTest` are untouched Android Studio templates. **All real
test coverage is on the server.** Do not read "the build is green" as "the app
is tested."

---

## Architecture

Two Gradle modules.

`:server` compiles `app/src/main/java/com/example/mediq/domain` as an extra
source dir, so both modules share the domain models. They deliberately do
**not** share the wire format: `http/Dtos.kt` maps domain to JSON, so a model
refactor cannot silently break the API contract.

```
app/src/main/java/com/example/mediq/
  MediQApp.kt            Application subclass; calls AppContainer.init()
  domain/model/          plain Kotlin — no Compose, no Android imports
  domain/repository/     interfaces only
  data/api/              ApiDtos, ApiMappers, MediQApiService, RetrofitClient, TokenStore
  data/repository/       Retrofit*Repository, one per aggregate
  di/AppContainer        object; the single place wiring is edited
  ui/feature/<area>/     Screen + ViewModel pairs
  ui/navigation/         Routes.kt, MediQNavHost
  core/designsystem/     theme, EmptyState, MediQBottomBar

server/src/main/kotlin/com/example/mediq/server/
  Db.kt                  Hikari pool, schema.sql bootstrap, tx() helper, ServerConfig
  db/*Store.kt           SQL, one class per aggregate
  db/AuthService.kt      sign-in, OTP, registration
  http/Routes.kt         every endpoint, in one file
  auth/                  Passwords (PBKDF2), Tokens (JWT)
  schema.sql             the only definition of the data shape
```

### Rules that are easy to break

- **`domain/` must not import Compose or Android.** That constraint is what
  keeps the schema portable to the server.
- **Repositories are the only place that knows the data source.** Screens and
  ViewModels never reference `data/` directly.
- **`LoadState<T>`** is `Loading` / `Success` / `Error` and is what every
  ViewModel exposes.
- **`suspend` on every repository function.**
- **Money is `Money`** (centavos, `Long`) — never `Int` pesos, never a formatted
  string.
- **Instants are `Instant`**, displayed via `toClinicDate()` / `toClinicTime()`.
  The clinic zone is pinned to `Asia/Manila`; never use device-local zone.
- **Enum wire values are explicit** (`wireValue = "confirmed"`). Sending the enum
  name leaks the constant name into the API contract.
- **No fabricated data.** This is a real-patient app; invented doctors with
  plausible licence numbers get mistaken for real ones.

### Booking and double-booking

Read `schema.sql` before changing this.

`slot_claims` holds **one row per slot, enforced by its primary key**. Booking
is an `INSERT` inside `db.tx { }`. A "SELECT then INSERT" check in application
code has a window between the two statements and both requests would pass; the
second insert hits the constraint and becomes HTTP 409.

Three consequences:

- **Cancelling deletes the claim row**, freeing the slot. You cannot un-book by
  updating status alone.
- **Slots are materialised as rows**, generated from `clinic_hours`, not
  computed on read — there has to be a row to collide with. Slot ids derive from
  `UUID.nameUUIDFromBytes("$doctorId|$startsAt")` so regenerating a date is
  idempotent under concurrency.
- **Booking is one transaction** covering the appointment and the notification,
  so a notification failure cannot leave a slot marked taken for an appointment
  the patient never received.

`BookingConcurrencyTest` fires 12 simultaneous bookings at one slot and asserts
exactly one winner. If that test is deleted or made sequential, the protection
is unverified.

---

## Tech stack

| Layer | Choice |
|---|---|
| Language | Kotlin 2.2.10 |
| Build | Gradle 9.5.0, AGP 9.3.3, version catalog |
| UI | Jetpack Compose, BOM 2026.02.01, Material 3 |
| Navigation | navigation-compose 2.10.1 |
| Architecture | MVVM, manual DI (`AppContainer`) |
| App SDK | minSdk 24, target/compile 37, Java 11 |
| Networking | Retrofit 2.11.0, OkHttp 4.12.0, Gson 2.11.0 |
| Server | Ktor 3.1.3 on Netty, kotlinx.serialization |
| Database | H2 2.3.232 (dev), HikariCP 6.3.0, hand-written JDBC |
| Auth | java-jwt 4.5.0, PBKDF2-HMAC-SHA256 at 210k iterations |
| Logging | Logback 1.5.18 |

Core library desugaring is on because the domain models use `java.time`, which
is native only from API 26 while `minSdk` is 24.

**Absent on purpose:** Hilt/Dagger (DI is hand-wired), Room and kapt/ksp (SQL is
hand-written), Flyway (`schema.sql` runs at boot), Tink (token storage is
plaintext), an SMS provider, Argon2id.

---

## Running it

Requires JDK 21+ for Gradle. `JAVA_HOME` should point at a real JDK — on
Windows, Android Studio's bundled `jbr` works.

### Server

```powershell
$env:MEDIQ_SEED_DEMO="true"
$env:MEDIQ_PORT="8099"   # 8080 is often already taken
.\gradlew.bat :server:run --console=plain
```

`MEDIQ_SEED_DEMO=true` inserts three invented doctors plus `demo_patient` /
`demo12345`. That data is demo-only and labelled (`DEMO-PRC-0001` licence
numbers). **Never seed a database that holds real patients.**

For a server that outlives the shell:

```powershell
.\gradlew.bat :server:installDist
.\server\build\install\server\bin\server.bat
```

`:server:run` dies with the shell that started it.

### App

The base URL is the single constant `RetrofitClient.BASE_URL`, currently
`http://10.0.2.2:8099/` (emulator host loopback). Change that one constant to
switch targets:

| Target | URL |
|---|---|
| Emulator | `http://10.0.2.2:8099/` |
| Physical device, ADB forward | `http://127.0.0.1:8099/` |
| Physical device on LAN | `http://<host-IP>:8099/` |

Cleartext HTTP is permitted **only** to `10.0.2.2` and `127.0.0.1` via
`res/xml/network_security_config.xml`. Every other host still requires HTTPS.

```powershell
.\gradlew.bat :app:assembleDebug
```

There is currently no run target on this machine: `emulator -list-avds` is
empty, no system images are installed, `adb devices` shows nothing, and the SDK
has no `cmdline-tools`. Creating an AVD needs Android Studio's SDK Manager, or a
physical device over USB. A missing emulator is not a code problem.

---

## API

All routes are in `server/.../http/Routes.kt`. Auth is a bearer JWT, injected
automatically by `AuthInterceptor` on the client.

| Method | Path | Auth |
|---|---|---|
| GET | `/health` | no |
| POST | `/auth/sign-in` | no |
| POST | `/auth/sign-out` | yes |
| POST | `/auth/otp/request` | no |
| POST | `/auth/otp/verify` | no |
| POST | `/auth/register` | no |
| GET | `/doctors` | no |
| GET | `/doctors/{doctorId}` | no |
| GET | `/doctors/{doctorId}/availability` | no |
| GET | `/doctors/{doctorId}/slots` | no |
| GET | `/appointments` | yes |
| GET | `/appointments/{appointmentId}` | yes |
| POST | `/appointments` | yes |
| DELETE | `/appointments/{appointmentId}` | yes |
| POST | `/appointments/{appointmentId}/reschedule-request` | yes |
| GET | `/notifications` | yes |
| PATCH | `/notifications/{notificationId}/read` | yes |
| GET | `/profile` | yes |
| PUT | `/profile` | yes |

`GET /doctors` accepts `search`, `specialty`, `building`, `limit`, and a
Base64 `cursor`. Listing is cursor-paginated, default 20, max 100.
`reason_for_visit` is returned by `GET /appointments/{id}` only, not by the list.

### Auth model

Server-side session plus JWT rather than a bare stateless token. The token
carries a session id, and `configureAuth` checks the session row on **every**
authenticated request — so sign-out kills the token immediately instead of
leaving it valid until expiry. That costs a database read per request, which is
the deliberate price.

Passwords are PBKDF2-HMAC-SHA256 at 210k iterations via the JDK, with the
iteration count stored alongside the hash so it can be raised later.

---

## Configuration

All server config is environment variables, read by `ServerConfig.fromEnv()`.

| Variable | Default |
|---|---|
| `MEDIQ_PORT` | `8080` |
| `MEDIQ_JDBC_URL` | `jdbc:h2:mem:mediq;DB_CLOSE_DELAY=-1` |
| `MEDIQ_JWT_SECRET` | `dev-only-insecure-secret-change-me` |
| `MEDIQ_JWT_ISSUER` | `mediq` |
| `MEDIQ_TOKEN_TTL_MINUTES` | `60` |
| `MEDIQ_SLOT_MINUTES` | `30` |
| `MEDIQ_OTP_TTL_MINUTES` | `5` |
| `MEDIQ_SEED_DEMO` | `false` |
| `MEDIQ_ENV` | unset; `production` enables the guard below |

`MEDIQ_ENV=production` makes `validate()` refuse to boot on the dev JWT secret
or an H2 URL. That is the backstop against shipping the signing key that is
currently printed in a source file.

---

## Testing

Two commands, covering different things:

```powershell
.\gradlew.bat :server:test
.\server\scripts\smoke.ps1 -Base http://127.0.0.1:8099
```

- **`:server:test`** — in-process, no port. 6 tests: 4 in
  `BookingConcurrencyTest` (one winner under 12 concurrent bookings, cancel
  frees the slot, cross-patient access refused, sign-out invalidates the token
  before expiry) and 2 in `ProfileUpdateTest` (partial profile updates preserve
  untouched fields).
- **`smoke.ps1`** — over HTTP against a running server. 19 checks covering the
  status of every route, the double-booking refusal, and that signing out kills
  the token mid-flight.

### Testing traps

Seeding a fixture through a data-class `.copy()` writes nothing.
`users.create(...).copy(address = "…")` mutates the returned `UserRow` and
leaves the column `NULL`, so the assertion compares against nothing while
looking like a real failure. Pass optional columns to the store method instead.

In PowerShell, reading an error body needs `$_.ErrorDetails.Message`;
`$_.Exception.Response.GetResponseStream()` is already drained by
`Invoke-RestMethod` and returns an empty string. `ErrorDetails.Message` arrives
as a decoded array, so take `[0]` before `ConvertFrom-Json`.

---

## Known gaps

### Fix before real patients

- **`AuthService.returnCodeToCaller` is `true`.** The OTP is printed to stdout
  and returned in the HTTP response. There is no SMS provider. Wire one up and
  set it to `false`.
- **H2 in-memory by default.** Data is lost on restart. `MEDIQ_JDBC_URL` must
  point at Postgres for real use; `validate()` refuses H2 when
  `MEDIQ_ENV=production`.
- **Token storage is plaintext `SharedPreferences`.** The token sits in the
  app's private storage, which is acceptable for development. Use
  `EncryptedSharedPreferences` (Tink) or Android Keystore before shipping.

### Other gaps

- **The registration flow screens are not connected to the backend.**
  `RegisterDetails`, `RegisterOTPScreen`, `RegisterCredentials`, and
  `RegisterSuccess` have no ViewModels and navigate between themselves via
  static local state. The server-side OTP and registration flow works; the app
  screens do not call it yet.
- **`AppointmentDetailsScreen` is a static empty state.** No ViewModel exists.
- **`schema.sql` is applied at boot, not migrated.** All
  `CREATE TABLE IF NOT EXISTS`, which is safe on an existing database but is not
  a migration tool. Move to Flyway before changing a column once real data
  exists.
- **PBKDF2 is a reasonable choice that needs no new dependency, not the current
  OWASP first recommendation.** Argon2id is preferred where memory hardness
  matters — confirm against current guidance before shipping.
- **Release build has R8/minification disabled** (`optimization { enable = false }`).
- **No app-side test coverage.** See [Current condition](#current-condition).
- `res/values/colors.xml` and the TODO in `res/xml/data_extraction_rules.xml`
  are template leftovers.

---

## Further reading

`AGENTS.md` (same folder) is the orientation file for new contributors and
agents — build environment quirks, the layered architecture, conventions, and
the reasoning behind the rules above. `CHANGELOG.md` tracks what changed.

All three files live in `.mdfiles/` at the repo root, which keeps them out of the
way of the Gradle source tree.