# AGENTS.md

Two Gradle modules: `:app` (Android/Jetpack Compose client) and `:server` (Ktor JVM backend). Kotlin 2.2, AGP 9.3, Compose BOM 2026.02, `minSdk` 24, `compileSdk`/`targetSdk` 37.

**Read `CONSTRAINTS.md` in this folder before writing code**, and run its gate. It carries the
enforced quality bar as numbers with a command per rule. Do not weaken it to
make a change pass — change it explicitly, in its own change, where the diff
shows the bar moving.

**Read `README.md` at the repo root for what the project is and what it does not
do yet.** It is the user-facing description; this file is the orientation one.

## Build environment

`JAVA_HOME` is set at **user** level to `C:\Program Files\Android\Android Studio\jbr` (OpenJDK 21.0.10) and Gradle runs on it. New terminals and Android Studio pick this up with no per-shell setup:

```powershell
.\gradlew.bat :app:compileDebugKotlin --console=plain
```

Two caveats. The **machine**-level `JAVA_HOME` still points at `C:\Users\You\Downloads\javafx-sdk-21.0.9`, which does not exist; user-level shadows it, so it is inert, but clearing it needs an admin shell. And a terminal opened *before* the fix still holds the old value in its process environment — reopen it rather than re-exporting.

`gradle/gradle-daemon-jvm.properties` requests a toolchain JVM 25 via foojay; the bundled JBR 21 is what actually runs. The `toolchainUrl.*` lines there are cached download URLs, not a requirement — leave them alone.

Verify the app with `:app:compileDebugKotlin` (~2s warm, ~3min cold). Full APK: `:app:assembleDebug` (~80s).

`:app` unit tests — 61, runnable with `.\gradlew.bat :app:testDebugUnitTest`:

- `data/api/` — `ApiErrorsTest.kt` (11 tests over error-body parsing),
  `UnknownWireValueTest.kt` (8 pinning how unrecognised wire values resolve), and
  `GsonLeniencyTest.kt` (3 pinning JSON parsing behaviour).
- `ui/feature/auth/register/RegisterViewModelTest.kt` (22) — the registration
  wizard's validation branches, both failure paths per step, that each step
  actually reaches `AuthRepository`, and that a handled navigation event does not
  fire again when the user steps back.
- `ui/feature/appointments/AppointmentDetailsViewModelTest.kt` (17) — loading:
  success, server error, the null-id case, and the `LoadState` mapping. Mutating:
  a cancel reaches the repository *and* reloads, a refused cancel surfaces the
  server's sentence without refreshing, an unreachable backend says the change was
  **not** made, the `isActionable` gate per status, and that a reschedule carries
  the real appointment id and a chosen slot — no slot means no request at all.

**ViewModels are unit testable now.** They were not, and the reason is worth
keeping: every one calls `viewModelScope`, which posts to `Dispatchers.Main`, and
that dispatcher throws on the JVM unless `kotlinx-coroutines-test` swaps it. That
dependency plus the hand-written fakes in `app/src/test/java/.../fake/` are the
harness, and there is no mocking framework — a fake that records what it was asked
for says more about a state machine than a verify-count does. `MainDispatcherRule`
swaps in an `UnconfinedTestDispatcher` so a launch runs to completion eagerly;
without that, a test that never awaits its coroutine passes while proving
nothing. The substantial suite is still `:server:test` (50 tests, see below).

`ExampleInstrumentedTest` remains an untouched Android Studio template.

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

Repo markdown lives in three places by purpose. `README.md` at the repo root is
the user-facing description of the project, and it is at the root because that is
the only place GitHub renders it. `.mdfiles/` holds the standing documentation —
this file, `CONSTRAINTS.md`, `CHANGELOG.md`, the two check scripts, and the
original product spec PDF. The dot prefix keeps those out of the way of the
Gradle source tree. `tasks/` holds per-work planning: `plan.md` is the design
record and `todo.md` is the task checklist with acceptance criteria. A new
feature gets a plan there; environment and architecture facts belong in this
file instead.

The check scripts resolve paths relative to the working directory, so run them
from the repo root as `.\.mdfiles\check-boundaries.ps1`, not from inside
`.mdfiles/`.

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
- `.\server\scripts\smoke.ps1 -Base http://127.0.0.1:8099` — over HTTP against a running server. 19 checks covering the status of every route, the double-booking refusal, and that signing out kills the token mid-flight. Needs the server up first.

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
- **The four register screens share one `RegisterViewModel`, scoped to a nested
  navigation graph.** They are a single transaction — the name and number typed on
  step 1 are still needed on step 3 — so a per-screen ViewModel would mean passing
  values forward by hand. `MediQNavHost` nests them under
  `navigation(startDestination = RegisterDetails, route = Screen.RegisterFlow.route)`
  and each screen takes the ViewModel from the *graph's* back stack entry.
  **`NavBackStackEntry.parent`, the obvious way to get that entry, is not public in
  navigation 2.10.1** — it fails to compile, and the fix is
  `navController.getBackStackEntry(Screen.RegisterFlow.route)`, which is why the
  graph has an explicit route rather than a generated one.
- **`registrationId` lives in the `RegisterViewModel` and nowhere else.** It is the
  server's proof that the number was verified, and `/auth/register` cannot succeed
  without it. It is deliberately absent from `RegisterUiState`; `otpVerified` is
  what the UI sees instead, which is also the guard for the credentials step — that
  flag can only be true while the ViewModel still holds the id, so process death
  between steps 2 and 3 is detectable and the user is sent back rather than
  submitting a request the server can only refuse.
- **`RegisterRequest.dateOfBirth` is nullable and the server requires it.**
  `AuthService` answers a null date of birth with 400 "A date of birth is
  required." — but only *after* its registration-id, username, email, and mobile
  checks, so it surfaces as a mid-transaction rejection rather than a validation
  error at the edge. The type cannot catch the omission, so
  `RegisterViewModel.requestOtp` refuses to run without one, and the details step
  collects it with a date picker. This was confirmed against a running server.

## Conventions

- **Money is `Money` (centavos, `Long`)** — never `Int` pesos, never a formatted string. Formatting to `₱` happens in the UI (`Money.format()` in `DoctorsScreen.kt` is the pattern).
- **Instants are `Instant`, displayed via `toClinicDate()` / `toClinicTime()`** from `domain/model/ClinicTime.kt`. Clinic zone is pinned to `Asia/Manila` via `CLINIC_ZONE` — do not use device-local zone.
- **Enum wire values are explicit** (`AppointmentStatus(wireValue = "confirmed")`). Sending the enum name leaks the constant name into the API contract.
- **No fake/invented data.** All hardcoded mock data was deliberately removed. `EmptyRepositories.kt` returns empty lists and throws `BackendNotConnectedException` for writes. Do not reintroduce placeholder doctors, appointments, or personal details — this is a real-patient app and fabricated records with plausible licence numbers get mistaken for real ones.
- **`BackendNotConnectedException` lives in `domain/model/`, not `data/repository/`.** ViewModels catch it, and they may not import from `data/`. It was in `data/repository/` until 2026-10-06, which seven ViewModels were breaking the rule for. The compiler cannot catch that violation — the import compiles fine — so `check-boundaries.ps1` does.
- `BackendNotConnectedException` must map to `LoadState.Success(emptyList())` on reads, **not** `Error` — an unconnected backend is the expected state, not a failure. The Retrofit repositories follow the same contract: list reads catch an unreachable-server `ApiFailure` and return an empty `Paged`; single-entity reads and writes propagate. Note the branch is on `isNetworkFailure`, **not** on a bare exception type — catching `IOException` (or `Exception`) also swallowed HTTP failures, so a 500 on a list read rendered as "you have no appointments." Only a genuinely unreachable server may become an empty state.
- **Every call to `api.` in a `Retrofit*Repository` must sit inside `call { }`** (`data/api/ApiErrors.kt`). It maps `HttpException` into `ApiFailure`, reading the server's `ErrorDto` body. An unwrapped call returns Retrofit's raw `"HTTP 401 "` as its message, which is what a patient sees. `check-boundaries.ps1` enforces this; it was unenforced when that bug shipped with a green build.
- **A masked text field must set `keyboardOptions` with a password key type.** `KeyboardType.Password` on a password, `KeyboardType.NumberPassword` on the OTP, plus `autoCorrect = false` and `KeyboardCapitalization.None`. **This was the sign-in bug on 2026-10-06**: no field in the app set `keyboardOptions`, so Compose used `KeyboardOptions.Default` — `autoCorrect = true`, no `KeyboardType` — and Android rewrote the credentials as they were typed. `demo12345` reached the server as something else and came back as `"Those details don't match an account."`, indistinguishable from a wrong password, while the server was healthy and the credentials were correct. `check-boundaries.ps1` Rule 4 enforces it. Note `PasswordVisualTransformation` only masks what is *displayed*; it says nothing about what the keyboard is allowed to *do* to the value.
- **Never surface a framework exception's `message`.** Catch `ApiFailure` and show `e.message` — the server writes those for a patient to read. Retrofit, Gson, and OkHttp messages leak internals or are useless to a user.
- **Usernames are normalized on both sides.** `AuthService.normalizeUsername` trims and lowercases at `register` *and* at `signIn`, so the stored form and the lookup form cannot disagree. Matching raw meant correct credentials failed on a stray space, and `Case` / `case` could register as two accounts that were indistinguishable at sign-in. Mobile numbers were already normalized; usernames were the one identifier that was not.
- Screens take a `NavController` (not `NavHostController`) and navigate imperatively. `MediQNavHost` uses explicit imports, not a wildcard.

## Networking

The app uses Retrofit 2.11.0 + OkHttp 4.12.0 + Gson 2.11.0. The single `MediQApiService` interface in `data/api/` covers all endpoints. An `AuthInterceptor` (in `RetrofitClient.kt`) injects `Authorization: Bearer <token>` automatically — no `@Header` per-method needed.

**Base URL** is the constant `RetrofitClient.BASE_URL`, currently `http://192.168.100.14:8099/` — this PC's LAN address. It is the LAN address rather than `10.0.2.2` because that is the one value that works on *every* target here, emulator and physical phone alike. Change this one constant when switching test targets:
- Emulator or phone on this Wi-Fi: `http://192.168.100.14:8099/` (current)
- Emulator only: `http://10.0.2.2:8099/`
- Physical device over ADB, no Wi-Fi: `http://127.0.0.1:8099/`, which additionally needs `adb reverse tcp:8099 tcp:8099`

**`10.0.2.2` is the trap here.** It is an alias that exists only inside the emulator's virtual network. On a physical phone it is unroutable, every call times out, and `call {}` reports "Couldn't reach the clinic" — which reads like the server is down while the server is answering `127.0.0.1` perfectly well. This exact mismatch made sign-in fail on 2026-10-06 with the server up and healthy. Check what `adb devices` actually lists before trusting a network error: an emulator and a phone are both just "a device", and moving between them changes nothing else.

The LAN address is DHCP-assigned and changes when this PC joins a different network. When it does, update `RetrofitClient.BASE_URL` **and** `res/xml/network_security_config.xml` in step.

**Cleartext HTTP** is allowed only to `10.0.2.2`, `127.0.0.1`, and that LAN address via `res/xml/network_security_config.xml`. Any host missing from that file has its cleartext refused, and the app fails with the same misleading "couldn't reach the clinic" message. All other hosts still require HTTPS.

**Token storage** uses `SharedPreferences` (`TokenStore.kt`). The full `AuthSessionDto` JSON is stored so `currentSession()` can reconstruct the complete `AuthSession` — including the nested `UserProfile` — without a network call. The token is stored in plaintext in the app's private storage; `EncryptedSharedPreferences` (Tink) or Android Keystore should replace this before production.

**DTOs** in `data/api/ApiDtos.kt` mirror the server wire format with `String` fields for all dates, times, and enums. `data/api/ApiMappers.kt` converts them to domain types.

**Enum lookups fall back to `UNKNOWN`, never to a positional guess.** The mapper used to do `firstOrNull { … } ?: <first enum entry>`, which resolved a wire value this build did not recognise into a confident wrong answer: an unrecognised appointment status became `PENDING_CONFIRMATION` (a cancelled appointment shown to a patient as awaiting confirmation) and an unrecognised slot became `BLOCKED`, having previously been `AVAILABLE` — bookable, on a guess. `AppointmentStatus` and `SlotStatus` now carry an explicit `UNKNOWN` member. **`UserRole` and `Specialty` must not.** They live in `domain/model/`, which `:server` compiles as `sharedDomain`, so the server parses them from untrusted input and reads them back with `.first {}`; an `UNKNOWN` member would widen what the server accepts and then fail on the row lookup. Those two fall back to `PATIENT` and `INTERNAL_MEDICINE` with the reason stated at the call site. `UnknownWireValueTest` fails if a positional fallback returns.

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
documented under "Error contract" above, and the enum-resolution issue is fixed
and documented under "Networking". The remaining nine are **unstarted** —
they are observations, not planned work, so they are not in `tasks/todo.md`.

- **A renamed response field fails silently, and nothing detects it.** *This
  replaced an earlier, wrong claim: that `ignoreUnknownKeys = false` makes every
  added response field a breaking change.* It does not. That setting is on the
  server (`Main.kt`) and governs how the **server** parses an inbound request
  body; the client parses responses with Gson through
  `GsonConverterFactory.create()`, whose default ignores unknown fields, so
  adding a field is safe for installed builds. `GsonLeniencyTest` pins this,
  because the claim looked plausible and was wrong. The real risk is the mirror
  one: Gson cannot distinguish "the server never sent this" from "the field was
  renamed" — both arrive as `null`, with no error either way, and a Kotlin
  non-null field declared on the DTO will read as null at runtime without
  throwing. Renames need a contract test; added fields need nothing.
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

- **Reschedule takes a typed slot id.** Cancel is wired end to end and gated on
  `AppointmentStatus.isActionable`; reschedule reaches the right endpoint but the
  screen collects `requestedSlotId` as a text field, because `RescheduleRequest`
  names a slot id and nothing yet offers a picker. `SchedulePicker.kt` in
  `ui/feature/booking/` already does exactly this for booking — reuse it rather
  than writing a second one, and read availability through `DoctorRepository`.
- **A cancel reloads the appointment; a reschedule request deliberately does
  not.** Cancelling deletes the `slot_claims` row to free the slot and only the
  server can change the status, so `AppointmentDetailsViewModel.cancel()` calls
  `load()` afterwards — without it the badge still reads "Confirmed" and
  `canCancelOrReschedule` offers a second cancel the server will refuse. A
  reschedule is a *request*: nothing changes until the clinic answers, so
  re-reading would show an unchanged appointment that looks like a no-op.
- **`canCancelOrReschedule` is read off `AppointmentStatus.isActionable`, not
  re-decided in the state.** One definition of which statuses permit a change.
  `UNKNOWN` is deliberately not actionable — offering a mutation for a status the
  app cannot read means guessing which endpoint the appointment would accept.
- **Colour literals are still spread across `ui/`, and only `SplashScreen` is
  exempt.** Every screen has been converted off `Color.White` backgrounds,
  `Color.Black` text, and `Color.Gray`/`Color.LightGray` metadata, so dark mode
  is legible — that conversion finished on 2026-10-06, after `ProfileScreen`
  went first. `check-contrast.ps1` now enforces it rather than trusting anyone
  to remember: it fails on any page background or app bar hardcoded to
  `Color.White`, on any token pair below its WCAG threshold, and on more than 4
  raw colour literals in `ui/`. The 4 it permits are `SplashScreen`'s
  white-on-brand-green, which measures 6.63:1 in both modes. Prefer
  `MaterialTheme.colorScheme.*` and `LocalMediQColors.current.*` over literals;
  a single green cannot clear 4.5:1 on both `#FFFFFF` and `#121212`, which is
  why the accent is a separate role from `primary`.
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