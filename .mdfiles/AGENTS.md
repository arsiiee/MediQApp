# AGENTS.md

Two Gradle modules: `:app` (Android/Jetpack Compose client) and `:server` (Ktor JVM backend). Kotlin 2.2, AGP 9.3, Compose BOM 2026.02, `minSdk` 24, `compileSdk`/`targetSdk` 37.

**Read `CONSTRAINTS.md` at the repo root before writing code.** It carries the
enforced quality bar as numbers with a command per rule. Do not weaken it to
make a change pass — change it explicitly, in its own change, where the diff
shows the bar moving.

## Build environment

`JAVA_HOME` is set at **user** level to `C:\Program Files\Android\Android Studio\jbr` (OpenJDK 21.0.10) and Gradle runs on it. New terminals and Android Studio pick this up with no per-shell setup:

```powershell
.\gradlew.bat :app:compileDebugKotlin --console=plain
```

Two caveats. The **machine**-level `JAVA_HOME` still points at `C:\Users\You\Downloads\javafx-sdk-21.0.9`, which does not exist; user-level shadows it, so it is inert, but clearing it needs an admin shell. And a terminal opened *before* the fix still holds the old value in its process environment — reopen it rather than re-exporting.

`gradle/gradle-daemon-jvm.properties` requests a toolchain JVM 25 via foojay; the bundled JBR 21 is what actually runs. The `toolchainUrl.*` lines there are cached download URLs, not a requirement — leave them alone.

Verify the app with `:app:compileDebugKotlin` (~2s warm, ~3min cold). Full APK: `:app:assembleDebug` (~80s).

`:app` has one real unit test — `data/api/ApiErrorsTest.kt` (11 tests over error-body parsing) — runnable with `.\gradlew.bat :app:testDebugUnitTest`. That is the whole harness: `ExampleUnitTest`/`ExampleInstrumentedTest` are still untouched Android Studio templates, there is no coroutine test dependency, and there are no fake repositories, so **ViewModel logic cannot be unit tested yet**. Any ViewModel calls `viewModelScope` and needs `Dispatchers.Main`, which throws on the JVM unless `kotlinx-coroutines-test` swaps it. The substantial test suite is `:server:test` (36 tests, see below).

## Running on a device

There **is** a working run target: AVD `mediq_api36` (API 36 / Android 16,
`default;x86_64`, Pixel 6 profile). Start it from Android Studio's Device
Manager, or headless:

```powershell
$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe -avd mediq_api36 -no-window -no-audio -no-boot-anim
```

Hardware acceleration is available via WHPX — `emulator -accel-check` reports
it as usable — but it is only partly effective here; see below. Confirm the
device is up with `adb devices` before assuming a build problem.

**Expect it to be slow, and do not fight it.** WHPX is working but the CPU is
missing some modern x86 virtualisation features, so on boot the emulator logs
"Not all modern X86 virtualization features supported ... Setting AVD to run
with 1 vCPU core only" and overrides `hw.cpu.ncore` and `hw.ramSize` in
`config.ini` regardless of what they are set to. A cold boot took **86
seconds**; Gradle's `installDebug` took under two minutes on top of that. Budget
several minutes for install-plus-launch, and treat a slow first launch as the
environment rather than a code problem. The `config.ini` values (4 cores, 4G)
are recorded as intent but are not what actually runs.

This AVD exists because `cmdline-tools` and the system image were both missing
from the SDK. `cmdline-tools` (19.0) is now installed at
`Sdk/cmdline-tools/latest`, so `sdkmanager` and `avdmanager` are available from
the shell. To recreate the AVD or add another API level:

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"   # sdkmanager needs this explicitly
& "$env:LOCALAPPDATA\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat" "system-images;android-36;default;x86_64"
& "$env:LOCALAPPDATA\Android\Sdk\cmdline-tools\latest\bin\avdmanager.bat" create avd -n mediq_api36 -k "system-images;android-36;default;x86_64" -d pixel_6
```

Two things worth knowing. `sdkmanager` prints a warning that it "only
understands SDK XML versions up to 3 but an SDK XML file of version 4 was
encountered" — it is cosmetic and installs still work. And PowerShell's `>` is
text, so `adb exec-out screencap -p > file.png` writes a corrupt image; route it
through `cmd /c` instead.

`:app:connectedAndroidTest` and on-device visual checks are now possible. The
app has still never been run on a **low** API: `app/build.gradle.kts` enables
core library desugaring because the domain models use `java.time`, which ships
natively only from API 26, so API 24/25 remain untested and would need a second
AVD.

## Docs location

Repo markdown lives in two places by purpose. `.mdfiles/` at the repo root holds
the standing documentation — this file, `README.md`, and `CHANGELOG.md`. The dot
prefix keeps those docs out of the way of the Gradle source tree. `tasks/` at
the repo root holds per-work planning: `plan.md` is the design record and
`todo.md` is the task checklist with acceptance criteria. A new feature gets a
plan here; environment and architecture facts belong in this file instead.

## Maintaining this file

**This file must be updated in the same change that alters the repo.** It is the only orientation a new session gets, so a stale line costs someone a full diagnostic pass. Specifically:

- Fix a broken instruction rather than leaving it with a workaround attached — if a command needs a special incantation, say why and remove it once the cause is gone.
- When you change behaviour, a test's meaning, or a machine/SDK fact, edit the section that describes it in the same commit. Do not append a dated changelog at the bottom; the file is organised by topic and belongs that way.
- Record *why* a rule exists where the rule is stated, and keep that reasoning when the surrounding code changes.
- If you fixed a bug that a test failed to catch, note the trap in the relevant section so it is not reintroduced.
- Do not let a "Known gaps" bullet become a claim that work was done. Move an item out only once it is actually implemented.

## Testing traps

Seeding a fixture through a data-class `.copy()` writes nothing. `users.create(...).copy(address = "…")` mutates the returned `UserRow` and leaves the column `NULL`, so any assertion about it compares against nothing while looking like a real failure. Pass optional columns to the store method instead. `ProfileUpdateTest` had exactly this bug — the COALESCE logic it guards was correct all along.

In PowerShell, reading an error body needs `$_.ErrorDetails.Message`. `$_.Exception.Response.GetResponseStream()` is already drained by `Invoke-RestMethod` and returns an empty string, which makes every error-handling assertion compare against `''`. `ErrorDetails.Message` arrives as a decoded array, so take `[0]` before `ConvertFrom-Json`. `server/scripts/smoke.ps1` does this correctly.

## Running the server

```powershell
$env:MEDIQ_SEED_DEMO="true"
$env:MEDIQ_PORT="8099"   # 8080 is often already taken on this machine
.\gradlew.bat :server:run --console=plain
```

`MEDIQ_SEED_DEMO=true` inserts three invented doctors plus `demo_patient` / `demo12345`. That data is demo-only and clearly labelled (`DEMO-PRC-0001` licence numbers) — never seed a database that holds real patients.

Two ways to verify, and they cover different things:

- `.\gradlew.bat :server:test` — in-process. `BookingConcurrencyTest` is the one that cannot be checked by hand.
- `.\server\scripts\smoke.ps1 -Base http://127.0.0.1:8099` — over HTTP against a running server. 24 checks covering the status of every route, the double-booking refusal, and that signing out kills the token mid-flight. Needs the server up first.

Use `installDist` plus `server/build/install/server/bin/server.bat` when you want a server that survives the Gradle daemon. `:server:run` dies with the shell that started it.

All config is env vars (`ServerConfig.fromEnv`), read in `server/.../Db.kt`. `MEDIQ_ENV=production` makes `validate()` refuse to boot on the dev JWT secret or an H2 URL, which is the backstop against shipping a signing key that is printed in a source file.

## Architecture

App side — strict three-layer split, wired by hand (no Hilt, no kapt/ksp):

```
app/src/main/java/com/example/mediq/
  MediQApp.kt         Application subclass; calls AppContainer.init(this) in onCreate
  domain/model/       plain Kotlin — no Compose, no Android imports.
                      Holds ApiFailure and BackendNotConnectedException, which
                      ViewModels catch and so cannot live under data/
  domain/repository/  interfaces only
  data/api/           ApiDtos.kt, ApiMappers.kt, MediQApiService.kt (Retrofit interface),
                      RetrofitClient.kt (OkHttp + AuthInterceptor), TokenStore.kt,
                      ApiErrors.kt (call {} — maps HttpException to ApiFailure)
  data/repository/    Retrofit*Repository implementations (one per aggregate)
  di/AppContainer     object; init(context) wires TokenStore + all Retrofit repos
  ui/feature/<area>/  Screen + ViewModel pairs
  ui/navigation/      Routes.kt (sealed class Screen) + MediQNavHost
  core/designsystem/  theme + EmptyState, MediQBottomBar
```

Server side — `server/src/main/kotlin/com/example/mediq/server/`:

```
Db.kt               Hikari pool, schema.sql bootstrap, tx() helper
ServerConfig        env-var config + production guard
db/*Store.kt        SQL, one class per aggregate
db/AuthService.kt   sign-in, OTP, registration
http/Dtos.kt        the wire contract, separate from domain models
http/Routes.kt      every endpoint, all in one file
auth/               Passwords (PBKDF2), Tokens (JWT)
schema.sql          the only place the data shape is defined
```

`:server` compiles `app/src/main/java/com/example/mediq/domain` as an extra source dir (`sharedDomain` in `server/build.gradle.kts`). The two modules share the models but **not** the wire format: `http/Dtos.kt` maps domain to JSON deliberately, so a model refactor cannot silently break the API contract.

Rules that are easy to break:

- **`domain/` must not import Compose or Android.** That is what keeps the schema portable to the server.
- **Repositories are the only place that knows the data source.** `AppContainer` is the single file to edit when swapping implementations. Screens and ViewModels must not reference `data/` directly.
- **`LoadState<T>`** (in `domain/model/LoadState.kt`) is `Loading` / `Success` / `Error` and is what every ViewModel exposes. Screens `when` over all three.
- **`suspend` on every repository function.** Adding one later means touching every caller.

## Conventions

- **Money is `Money` (centavos, `Long`)** — never `Int` pesos, never a formatted string. Formatting to `₱` happens in the UI (`Money.format()` in `DoctorsScreen.kt` is the pattern).
- **Instants are `Instant`, displayed via `toClinicDate()` / `toClinicTime()`** from `domain/model/ClinicTime.kt`. Clinic zone is pinned to `Asia/Manila` via `CLINIC_ZONE` — do not use device-local zone.
- **Enum wire values are explicit** (`AppointmentStatus(wireValue = "confirmed")`). Sending the enum name leaks the constant name into the API contract.
- **No fake/invented data.** All hardcoded mock data was deliberately removed. `EmptyRepositories.kt` returns empty lists and throws `BackendNotConnectedException` for writes. Do not reintroduce placeholder doctors, appointments, or personal details — this is a real-patient app and fabricated records with plausible licence numbers get mistaken for real ones.
- **`BackendNotConnectedException` lives in `domain/model/`, not `data/repository/`.** ViewModels catch it, and they may not import from `data/`. It was in `data/repository/` until 2026-10-06, which seven ViewModels were breaking the rule for. The compiler cannot catch that violation — the import compiles fine — so `check-boundaries.ps1` does.
- `BackendNotConnectedException` must map to `LoadState.Success(emptyList())` on reads, **not** `Error` — an unconnected backend is the expected state, not a failure. The Retrofit repositories follow the same contract: list reads catch an unreachable-server `ApiFailure` and return an empty `Paged`; single-entity reads and writes propagate. Note the branch is on `isNetworkFailure`, **not** on a bare exception type — catching `IOException` (or `Exception`) also swallowed HTTP failures, so a 500 on a list read rendered as "you have no appointments." Only a genuinely unreachable server may become an empty state.
- **Every call to `api.` in a `Retrofit*Repository` must sit inside `call { }`** (`data/api/ApiErrors.kt`). It maps `HttpException` into `ApiFailure`, reading the server's `ErrorDto` body. An unwrapped call returns Retrofit's raw `"HTTP 401 "` as its message, which is what a patient sees. `check-boundaries.ps1` enforces this; it was unenforced when that bug shipped with a green build.
- **Never surface a framework exception's `message`.** Catch `ApiFailure` and show `e.message` — the server writes those for a patient to read. Retrofit, Gson, and OkHttp messages leak internals or are useless to a user.
- Screens take a `NavController` (not `NavHostController`) and navigate imperatively. `MediQNavHost` uses explicit imports, not a wildcard.

## Networking

The app uses Retrofit 2.11.0 + OkHttp 4.12.0 + Gson 2.11.0. The single `MediQApiService` interface in `data/api/` covers all endpoints. An `AuthInterceptor` (in `RetrofitClient.kt`) injects `Authorization: Bearer <token>` automatically — no `@Header` per-method needed.

**Base URL** is the constant `RetrofitClient.BASE_URL`, currently `http://10.0.2.2:8099/` (Android Emulator host loopback). Change this one constant when switching test targets:
- Emulator: `http://10.0.2.2:8099/`
- Physical device (ADB port-forward): `http://127.0.0.1:8099/`
- Physical device on LAN: `http://<host-IP>:8099/`

**Cleartext HTTP** is allowed only to `10.0.2.2` and `127.0.0.1` via `res/xml/network_security_config.xml`. All other hosts still require HTTPS.

**Token storage** uses `SharedPreferences` (`TokenStore.kt`). The full `AuthSessionDto` JSON is stored so `currentSession()` can reconstruct the complete `AuthSession` — including the nested `UserProfile` — without a network call. The token is stored in plaintext in the app's private storage; `EncryptedSharedPreferences` (Tink) or Android Keystore should replace this before production.

**DTOs** in `data/api/ApiDtos.kt` mirror the server wire format with `String` fields for all dates, times, and enums. `data/api/ApiMappers.kt` converts them to domain types. Enum lookups use `firstOrNull` with safe fallbacks so unknown server values don't crash the app.

## Error contract

The server publishes one error shape for every non-2xx response:
`ErrorDto(error, message)` from `configureStatusPages` in `Routes.kt`, with the
copy written by `ApiError` for a patient to read. It is a good contract —
`AuthService` in particular gives an unknown username and a wrong password the
*same* message so neither can be probed, and takes a dummy hash path so the two
also take the same time.

The client half is `data/api/ApiErrors.kt`. `call { }` reads that body and
throws `ApiFailure(status, code, message)`; `ApiFailure` lives in `domain/model/`
because `AGENTS.md` forbids ViewModels importing from `data/`, and `:server`
compiles `domain/` as `sharedDomain` so it must stay plain Kotlin.

**This mapping was missing until 2026-10-06, and the whole test suite stayed
green throughout.** Retrofit throws `HttpException`, whose `message` is
`"HTTP <code> <reason phrase>"`. Every ViewModel that surfaced `e.message`
therefore showed a patient their own HTTP status instead of the sentence the
server wrote for them — and `BookingViewModel` discarded the 409 entirely,
telling someone to "try again" on a booking that could never succeed because the
slot was gone. A rule nobody checked, failing silently. `ApiErrorsTest` and
`check-boundaries.ps1` are the two guards so it cannot recur.

## Booking and double-booking

This is the part that must not be vibecoded, so read `schema.sql` before changing any of it.

`slot_claims` has **one row per slot, enforced by its primary key**. Booking means INSERTing there inside `db.tx { }`. Two patients tapping the same 9:30 AM at the same instant produce two inserts and the database rejects the second with a constraint violation → `ApiError.conflict` → HTTP 409. A "SELECT then INSERT" check in application code has a window between the two statements and both requests would pass.

Three consequences worth knowing before you touch this code:

- **Cancelling deletes the claim row**, which frees the slot. The `appointments` row is kept for history. So you cannot "un-book" by updating status alone.
- **Slots are materialised as rows** (generated from `clinic_hours`) rather than computed on read. That is what makes the unique constraint possible — there has to be a row to collide with. Slot ids are derived from `UUID.nameUUIDFromBytes("$doctorId|$startsAt")` so regenerating a date is idempotent under concurrent requests; a random UUID would create duplicates.
- **Booking runs in one transaction** with the appointment insert and the notification insert. Without it, a notification failure would leave a slot marked taken for an appointment the patient never received.

`BookingConcurrencyTest` is the proof — 12 simultaneous bookings at one slot, asserting exactly one winner and exactly one row in each table. If that test is deleted or made sequential, the protection is unverified.

## Auth

Deliberately server-side session + JWT rather than a bare stateless token. The token carries a session id and `configureAuth` checks the session row on **every** authenticated request, so sign-out kills the token immediately instead of leaving it valid until expiry. Passwords are PBKDF2-HMAC-SHA256 at 210k iterations via the JDK, with the iteration count stored alongside the hash so it can be raised later.

Three things to fix before real patients:

- **`AuthService.returnCodeToCaller` is `true`**, which prints the OTP to stdout and returns it in the HTTP response. There is no SMS provider. Wire one up and set it false.
- **PBKDF2 is a reasonable choice that needs no new dependency, not the current OWASP first recommendation.** Argon2id is preferred where memory hardness matters. Confirm against current OWASP guidance before shipping.
- **Token storage on the Android side uses `SharedPreferences` (plaintext).** The token is in the app's private storage — safe enough for development, but `EncryptedSharedPreferences` (Tink library) should be used before shipping to real patients.

## Known gaps

### API contract review (2026-10-06)

A review of the client/server contract found eleven issues. #1 is fixed and
documented under "Error contract" above. The remaining ten are **unstarted** —
they are observations, not planned work, so they are not in `tasks/todo.md`.

- **`ignoreUnknownKeys = false` makes every response field a breaking change.**
  Sound for inbound bodies, inverted for responses: adding a field to `DoctorDto`
  breaks every installed build. Needs asymmetric config — strict in, lenient out.
- **Pagination is fully specified server-side and entirely unused.** `Routes.kt`
  has cursors and `MAX_PAGE_SIZE`; `MediQApiService` sends no `limit` or `cursor`,
  and `AppointmentsViewModel` drops `nextCursor` by taking `.items`. A patient
  with 25 appointments sees 20, with no indication.
- **`POST /appointments` has no idempotency key.** Book, lose the response, tap
  again → 409 "that time was just taken" for a slot the patient holds. Derive the
  key from the slot id and claim it against a unique constraint, the mechanism
  already proven by `slot_claims`.
- **`DELETE /appointments/{id}` is not idempotent.** A second cancel throws 400.
  A retry after a lost response reports failure for an action that succeeded.
- **`PUT /profile` is a PATCH in disguise, and fields cannot be cleared.**
  `explicitNulls = false` plus `COALESCE` means omission and clearing are the
  same operation, so clearing is impossible — `ifBlank { null }` in the repository
  silently restores the old value.
- **Client enum resolution silently substitutes values the server would reject.**
  An unknown appointment status becomes `PENDING_CONFIRMATION`. A cancelled
  appointment rendered as pending, in a medical app, unlogged.
- **`verifyOtp` is untyped on both sides** (`Map<String, String>`), so a missing
  key threw a developer string that reached the UI.
- **Status codes are overloaded.** Duplicate username and already-cancelled are
  400, not 409; missing date of birth is 400, not 422. `ApiError.conflict`
  hardcodes `code = "slot_taken"` despite being generically named.
- **Token expiry mid-session is unhandled and there is no refresh.** After the
  60-minute TTL every request 401s with no route back to sign-in.
- **`getDoctors(@QueryMap)` is an untyped string bag.** A typo'd key is ignored
  by the server and silently drops the filter.
- **`confirmedByPatient` is validated then discarded** — the insert hardcodes
  `true`. It reads as a server guarantee and provides none.

### Application

- **Registration flow screens are not connected to the backend.** `RegisterDetails`, `RegisterOTP`, `RegisterCredentials`, and `RegisterSuccess` have no ViewModels and navigate between themselves via static local state. The server-side OTP + registration flow works, but the app screens don't call it yet.
- **`AppointmentDetailsScreen` is still a static empty state.** No ViewModel exists for it yet.
- **H2 in-memory by default.** Fine for development, wrong for real patients. `MEDIQ_JDBC_URL` must point at Postgres, and `ServerConfig.validate()` refuses H2 when `MEDIQ_ENV=production`.
- **`schema.sql` is applied at boot, not migrated.** It is all `CREATE TABLE IF NOT EXISTS`, safe on an existing database but not a migration tool. Once real data exists, move to Flyway before changing a column.
- **Release build has R8/minification disabled** (`optimization { enable = false }`).
- `res/values/colors.xml` and the TODO in `res/xml/data_extraction_rules.xml` are template leftovers.

## Toolchain notes

- Gradle 9.8, configuration cache on, parallel off. The wrapper was bumped from 9.5.0 and the regenerated `gradle-wrapper.properties` dropped `distributionSha256Sum` and `validateDistributionUrl`, so the downloaded distribution is no longer checksum-verified.
- Icons: prefer `Icons.AutoMirrored.*` for `ArrowBack`, `Chat`, `Logout` (the non-mirrored ones are deprecated and warn).
- `TabRowDefaults.Indicator` is deprecated → use `PrimaryTabRow`. Note `SecondaryIndicator` does **not** exist in this BOM version even though the deprecation message suggests it.
- `Theme.kt` uses the deprecated `statusBarColor`; this is the last remaining compile warning.
- Wildcard imports (`layout.*`, `material3.*`) are the existing style, but scripts that rewrite import blocks tend to duplicate `import ...material.icons.Icons` and trip "Conflicting import". Verify counts after scripted import edits.