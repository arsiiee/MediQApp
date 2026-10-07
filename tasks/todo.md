# MediQ — Task List

## Plan history

| Plan | Status | Scope |
|---|---|---|
| v1 — registration + appointment details | **Complete**, commits `c9df562`..`24d9ef2` | Close the two "Known gaps" in `.mdfiles/AGENTS.md` |
| v2 — token lifecycle | **Active**, see below | 401 handling, session expiry, corrupt-session recovery |

**Active plan:** `tasks/plan.md` → Token Lifecycle (Expiry, 401 Handling, Refresh),
spec at `tasks/SPEC-token-lifecycle.md`. D2 resolved as **re-auth, no refresh
endpoint** — no server route, no `refresh_token` column.

---

## v1 — Registration and Appointment Details (complete)

Plan: `tasks/plan.md`. Scope: close the two "Known gaps" in `.mdfiles/AGENTS.md`
(registration wizard not wired to the backend; `AppointmentDetailsScreen` is a
static empty state).

**Verification commands used throughout** (all must stay green):

```powershell
.\gradlew.bat :app:compileDebugKotlin --console=plain   # ~2s warm, ~3min cold
.\gradlew.bat :app:testDebugUnitTest --console=plain    # new in Task 2
.\gradlew.bat :server:test --console=plain              # existing, 17 tests
```

**An emulator is available**: AVD `mediq_api36` (API 36). Boot it headless with
`-no-window -no-audio -no-boot-anim`, expect an ~86s cold boot, then
`:app:installDebug` and `adb shell am start -n com.example.mediq/.MainActivity`.
Sign in with `demo_patient` / `demo12345` while the server runs on port 8099.

Because of that, tasks below may now include an on-device check — but only as a
*manual confirmation of behaviour*, never as the gate. Unit tests remain the
repeatable check; one manual pass cannot catch a regression later.
`adb shell uiautomator dump /sdcard/ui.xml` reads on-screen positions reliably;
guessing tap coordinates from a screenshot is not.

---

## Task 1: Commit the uncommitted working tree as a reviewed baseline

**Description:** The working tree has 24 modified/deleted files and 25 untracked
entries, including the whole `data/` and `di/` layers and every ViewModel — none
of it committed. Committing it first makes Tasks 2-9 reviewable as small diffs
and keeps them revertable. This task changes no behaviour; it only fixes the
state the work starts from.

**Acceptance criteria:**
- [x] `git status` is clean afterwards, with no `.idea/` or `.artifacts/` staged
- [x] `.gitignore` adds `/.idea/` (it currently lists only four specific files under `.idea/`, so the rest still surfaces as untracked) and `.artifacts/`; the `app-tree.txt` snapshot is either ignored or committed deliberately
- [x] The commit message states that this is the networking + server backend work, not new work

**Verification:**
- [x] `git status --short` returns nothing
- [x] `.\gradlew.bat :app:compileDebugKotlin --console=plain` still green (a commit must not change the build)
- [x] Manual check: read `git show --stat` and confirm no build output or IDE config was captured

**Dependencies:** None

**Files likely touched:**
- `.gitignore`
- `app/src/main/java/com/example/mediq/**` (staged only, not edited)
- `server/src/**` (staged only, not edited)

**Estimated scope:** Small (no code edits — staging and one commit)

---

## Task 2: Give `:app` a real unit test suite

**Description:** There is no way to verify any new logic: `:app` depends only on
`testImplementation(libs.junit)`, `ExampleUnitTest` is an Android Studio
template, and there is no emulator. This task adds the minimum harness — a
coroutines test dependency, hand-written fake repositories, and one real test to
prove the harness works. Every later task depends on this.

ViewModels call `viewModelScope`, which needs `Dispatchers.Main`; on the JVM that
throws unless it is swapped, so `kotlinx-coroutines-test` is required rather than
optional.

**Acceptance criteria:**
- [x] `kotlinx-coroutines-test` added to `gradle/libs.versions.toml` using the existing `coroutines` version ref (`1.10.2`), and wired as `testImplementation` in `app/build.gradle.kts`
- [x] `ExampleUnitTest.kt` deleted and replaced with at least one genuine test that asserts a real value
- [x] Hand-written fakes for `AuthRepository` and `AppointmentRepository` exist under `app/src/test/`, implementing the interfaces with settable results and no mocking framework
- [x] `:app:testDebugUnitTest` reports executed tests, not "no tests found"

**Verification:**
- [x] `.\gradlew.bat :app:testDebugUnitTest --console=plain` green with a non-zero test count
- [x] Confirm the Gradle task name via `.\gradlew.bat :app:tasks --all` if `testDebugUnitTest` is not found — AGP 9 may name it differently
- [x] Manual check: deliberately break one assertion, confirm the build fails, then restore it (a harness that cannot fail is not a harness)

**Dependencies:** Task 1

**Files likely touched:**
- `gradle/libs.versions.toml`
- `app/build.gradle.kts`
- `app/src/test/java/com/example/mediq/ExampleUnitTest.kt` (deleted)
- `app/src/test/java/com/example/mediq/fake/FakeAuthRepository.kt`
- `app/src/test/java/com/example/mediq/fake/FakeAppointmentRepository.kt`

**Estimated scope:** Medium (4-5 files)

---

## Checkpoint A: Baseline and harness

- [x] `:app:compileDebugKotlin` green
- [x] `:app:testDebugUnitTest` green and actually running assertions
- [x] `:server:test` still green
- [x] Review with human before proceeding

---

## Task 3: `RegisterViewModel` wizard state machine, with unit tests

**Description:** Creates the single owner of all three registration steps' state
and all three API calls, with no UI wired yet. Written before the screens so the
logic is testable in isolation — the screen work in Tasks 4-6 then only renders
what already exists. Follows the `SignInViewModel` pattern exactly: private
constructor param, `MutableStateFlow` + `StateFlow`, `companion object` with
`viewModelFactory { initializer { RegisterViewModel(AppContainer.authRepository) } }`.

Owns: full name, phone number, date of birth, OTP code, `registrationId`,
per-step loading and error flags, and `devOtpHint`.

**Acceptance criteria:**
- [x] `RegisterViewModel` exposes one `RegisterUiState` covering all three steps, plus `requestOtp()`, `verifyOtp()`, and `register()` functions
- [x] Blank full name, blank phone, or null date of birth sets an error and does **not** call the repository
- [x] `registrationId` is stored by `verifyOtp()` and cleared when `register()` fails, so a retry re-uses it rather than re-verifying
- [x] Unit tests cover: each validation branch, a successful three-step run, a wrong OTP, and a duplicate username

**Verification:**
- [x] `.\gradlew.bat :app:testDebugUnitTest --console=plain` green
- [x] `.\gradlew.bat :app:compileDebugKotlin --console=plain` green
- [x] Manual check: confirm `domain/` gained no Android or Compose imports

**Dependencies:** Task 2

**Files likely touched:**
- `app/src/main/java/com/example/mediq/ui/feature/auth/register/RegisterViewModel.kt`
- `app/src/test/java/com/example/mediq/ui/feature/auth/register/RegisterViewModelTest.kt`

**Estimated scope:** Medium (2 files)

---

## Task 4: Registration nav graph + details screen wired to `requestOtp`

**Description:** Nests the four register destinations in a
`navigation(startDestination = Screen.RegisterDetails)` graph so all four screens
share one `ViewModelStoreOwner`, then wires the details screen to it. The screen
today collects full name and phone only, and navigates on a button press that
discards both — it must gain a **date of birth field**, because the server
rejects registration without one (`AuthServiceTest.kt:221`) and
`RegisterRequest.dateOfBirth` is nullable so the compiler will not catch it.

Also remove the local `remember` state from the screen in favour of the
ViewModel.

**Acceptance criteria:**
- [x] The four register routes live in a nested `navigation()` graph in `MediQNavHost`, and each screen obtains the ViewModel from the graph's `backStackEntry`
- [x] The details screen collects full name, phone number, and date of birth, and calls `requestOtp()` — navigation to the OTP step happens **only** on success
- [x] `Screen.RegisterOTP.route` stays argument-free, and no registration value is passed through a route string

**Verification:**
- [x] `.\gradlew.bat :app:compileDebugKotlin --console=plain` green
- [x] `.\gradlew.bat :app:testDebugUnitTest --console=plain` green
- [x] Manual check: grep the register package for `mutableStateOf` — no local form state should remain on these screens

**Dependencies:** Task 3

**Files likely touched:**
- `app/src/main/java/com/example/mediq/ui/navigation/MediQNavHost.kt`
- `app/src/main/java/com/example/mediq/ui/navigation/Routes.kt`
- `app/src/main/java/com/example/mediq/ui/feature/auth/register/RegisterDetailsScreen.kt`

**Estimated scope:** Medium (3 files)

---

## Task 5: OTP screen wired to `verifyOtp`

**Description:** Replaces the OTP screen's local `var otp` and unconditional
navigation. Verify now calls `verifyOtp()` and stores the `registrationId` in the
ViewModel; navigation to the credentials step happens only on success.

`RetrofitAuthRepository.requestOtp` returns the OTP string because the server has
`returnCodeToCaller = true`. Show it as a clearly-labelled dev-only hint, and
never require it — the screen must work when the server returns no code.

**Acceptance criteria:**
- [x] Verify calls `verifyOtp()`, and a wrong code shows the server's message without clearing the typed code or advancing
- [x] The lockout case (five wrong attempts, per `AuthServiceTest`) shows a distinct message rather than the generic failure
- [x] The dev OTP is displayed only when non-empty, labelled as dev-only
- [x] Tests cover the success and wrong-code paths against `FakeAuthRepository`

**Verification:**
- [x] `.\gradlew.bat :app:testDebugUnitTest --console=plain` green
- [x] `.\gradlew.bat :app:compileDebugKotlin --console=plain` green
- [x] `.\gradlew.bat :server:test --console=plain` green — the lockout threshold must match `AuthServiceTest`

**Dependencies:** Task 4

**Files likely touched:**
- `app/src/main/java/com/example/mediq/ui/feature/auth/register/RegisterOTPScreen.kt`
- `app/src/test/java/com/example/mediq/ui/feature/auth/register/RegisterViewModelTest.kt`

**Estimated scope:** Small (2 files)

---

## Task 6: Credentials and success screens wired to `register`

**Description:** The last step. `register()` returns an `AuthSession` and
`RetrofitAuthRepository.register` persists it via `TokenStore`, so the user is
**already signed in** when the success screen renders. The success screen also
hardcodes "Welcome to MediQ, Jesse.", which must come from the session instead.

Client-side checks (password >= 8, passwords match) mirror the server so the user
gets an immediate answer; the server stays the authority.

See Open Question 1 in the plan — recommended default is to navigate to
`Screen.Home` and pop the auth stack rather than sending a signed-in user to the
sign-in form.

**Acceptance criteria:**
- [x] Mismatched or under-8-character passwords set an error and never call `register`; a duplicate username shows the server's message
- [x] On success the screen shows the real name from the returned `AuthSession`, and the hardcoded "Jesse" string is gone
- [x] If `registrationId` is null on this step (e.g. the process was killed after OTP verification), navigate back to the details step instead of calling `register`
- [x] Tests cover: successful registration, password mismatch, duplicate username, and the missing-`registrationId` guard

**Verification:**
- [x] `.\gradlew.bat :app:testDebugUnitTest --console=plain` green
- [x] `.\gradlew.bat :app:compileDebugKotlin --console=plain` green
- [x] `.\gradlew.bat :server:test --console=plain` green
- [x] Optional sub-item: `server/scripts/smoke.ps1` has no OTP or register coverage — add checks for `POST /auth/otp/request`, `/verify`, `/register` if HTTP-level proof is wanted
- [x] Manual check: read `Logout`/`SignIn` navigation in `MediQNavHost` and confirm the back stack does not return a signed-in user to the wizard
- [x] Emulator walkthrough: register a new account end-to-end. `AuthService.returnCodeToCaller` is `true`, so the OTP comes back in the HTTP response — read it from `adb logcat | Select-String okhttp` rather than guessing, then confirm the new account can sign in

**Dependencies:** Task 5

**Files likely touched:**
- `app/src/main/java/com/example/mediq/ui/feature/auth/register/RegisterCredentialsScreen.kt`
- `app/src/main/java/com/example/mediq/ui/feature/auth/register/RegisterSuccessScreen.kt`
- `app/src/test/java/com/example/mediq/ui/feature/auth/register/RegisterViewModelTest.kt`

**Estimated scope:** Medium (3 files)

---

## Checkpoint B: Registration

- [x] All three wizard steps covered by unit tests against a fake repository
- [x] Every registration failure mode in `AuthServiceTest` has a UI path
- [x] `:app:compileDebugKotlin` and `:app:testDebugUnitTest` green
- [x] Both "not connected to the backend" bullets in `.mdfiles/AGENTS.md` now describable as done

---

## Task 7: `AppointmentDetailsViewModel` and a real details screen

**Description:** The screen already receives `appointmentId` from its route and
ignores it, always rendering `EmptyState`. Add a ViewModel that loads the
appointment and render it.

Must follow the `AGENTS.md` rule strictly: expose `LoadState<Appointment>` and
`when` over all three cases in the UI.

**Trap to avoid:** `AGENTS.md` says `BackendNotConnectedException` maps to
`LoadState.Success(emptyList())` on **list** reads only. This is a single-entity
read, so it maps to `LoadState.Error` — copying `AppointmentsViewModel`'s catch
block here would be wrong.

**Acceptance criteria:**
- [x] `AppointmentDetailsViewModel` loads by the route's `appointmentId` and exposes `LoadState<Appointment>`; the screen `when`s over Loading, Success, and Error
- [x] A null `appointmentId` renders a clear empty state and makes no network call
- [x] The screen displays doctor, date, time, location, fee via `Money.format()`, status via `displayName`, and reason for visit
- [x] Dates and times render through `toClinicDate()` / `toClinicTime()`, never device-local zone

**Verification:**
- [x] `.\gradlew.bat :app:testDebugUnitTest --console=plain` green — tests cover success, error, and the null-id case
- [x] `.\gradlew.bat :app:compileDebugKotlin --console=plain` green
- [x] Manual check: confirm no `EmptyState("No appointment selected")` string remains as the main content
- [x] Emulator walkthrough: open an appointment from the Appointments tab and confirm the details render with real server data

**Dependencies:** Task 2 (fakes) — independent of Tasks 3-6, so it can run in parallel

**Files likely touched:**
- `app/src/main/java/com/example/mediq/ui/feature/appointments/AppointmentDetailsViewModel.kt`
- `app/src/main/java/com/example/mediq/ui/feature/appointments/AppointmentDetailsScreen.kt`
- `app/src/test/java/com/example/mediq/ui/feature/appointments/AppointmentDetailsViewModelTest.kt`

**Estimated scope:** Medium (3 files)

---

## Task 8: Cancel and reschedule actions gated on `AppointmentStatus.isActionable`

**Description:** `AppointmentRepository` already exposes `cancel` and
`requestReschedule`, and `AppointmentStatus.isActionable` already encodes which
statuses permit a change. Wire both actions, and refresh the appointment after a
successful mutation so the status badge updates.

Note the server-side rule from `AGENTS.md`: cancelling deletes the `slot_claims`
row to free the slot, so this cannot be done by updating status alone. Do not
modify `schema.sql` or the booking transaction.

**Acceptance criteria:**
- [x] Cancel and Reschedule render **only** when `status.isActionable`, and are absent for `COMPLETED`, `CANCELLED`, and `DECLINED`
- [x] A successful cancel or reschedule request refreshes the displayed appointment; a failure shows the server message and leaves the appointment unchanged
- [x] Reschedule requires a chosen slot and submits a `RescheduleRequest` with the real `appointmentId`
- [x] Tests cover: cancel success, cancel failure, reschedule success, and the `isActionable` gate

**Verification:**
- [x] `.\gradlew.bat :app:testDebugUnitTest --console=plain` green
- [x] `.\gradlew.bat :app:compileDebugKotlin --console=plain` green
- [x] `.\gradlew.bat :server:test --console=plain` green — `AppointmentLifecycleTest` guards the cancel path; it must not be weakened
- [x] Manual check: `git diff --stat server/` is empty — this task must not touch the server
- [x] Emulator walkthrough: cancel an appointment, confirm the status badge updates and the slot reappears as bookable in `server/scripts/smoke.ps1`'s rebooking check

**Dependencies:** Task 7

**Files likely touched:**
- `app/src/main/java/com/example/mediq/ui/feature/appointments/AppointmentDetailsViewModel.kt`
- `app/src/main/java/com/example/mediq/ui/feature/appointments/AppointmentDetailsScreen.kt`
- `app/src/test/java/com/example/mediq/ui/feature/appointments/AppointmentDetailsViewModelTest.kt`

**Estimated scope:** Medium (3 files)

---

## Checkpoint C: Appointment details

- [x] Details screen renders a real appointment in all three `LoadState` cases
- [x] Cancel and reschedule absent for completed, cancelled, and declined appointments
- [x] `:server:test` still green
- [x] Review with human before proceeding

---

## Task 9: Update `.mdfiles/AGENTS.md` and `.mdfiles/CHANGELOG.md`

**Description:** `.mdfiles/AGENTS.md` states it "must be updated in the same
change that alters the repo" and is "the only orientation a new session gets".
Both gaps it lists are now closed, and the two new facts a session would
otherwise waste a diagnostic pass rediscovering are the test commands and the
registration nav graph.

**Acceptance criteria:**
- [x] Both "Known gaps" bullets about the registration flow and `AppointmentDetailsScreen` are **removed**, not reworded — per that file's own rule, an item leaves only once it is implemented
- [x] The "Build environment" section documents `:app:testDebugUnitTest` alongside `:app:compileDebugKotlin`
- [x] The "Architecture" section records that the four register screens share one graph-scoped `RegisterViewModel`
- [x] The "Docs location" section names `tasks/` so it no longer contradicts the plan files
- [x] `.mdfiles/CHANGELOG.md` gains entries under `[Unreleased]` for the wizard and the details screen

**Verification:**
- [x] Manual check: re-read `AGENTS.md` top to bottom and confirm every claim is still true of the tree
- [x] Manual check: confirm no gap bullet claims work that is not implemented
- [x] `git diff .mdfiles/` shows only documentation edits

**Dependencies:** Tasks 6 and 8

**Files likely touched:**
- `.mdfiles/AGENTS.md`
- `.mdfiles/CHANGELOG.md`

**Estimated scope:** Small (2 files)

---

## Checkpoint D: Complete

- [x] Both "Known gaps" bullets removed from `AGENTS.md`
- [x] All acceptance criteria met across Tasks 1-9
- [x] `:app:compileDebugKotlin`, `:app:testDebugUnitTest`, `:server:test` all green
- [x] Ready for review

---

# v2 — Token Lifecycle (Expiry, 401 Handling, Refresh)

Plan: `tasks/plan.md` → Token Lifecycle. Spec:
`tasks/SPEC-token-lifecycle.md`. **D2 resolved as re-auth** — no
`POST /auth/refresh`, no `refresh_token` column, `:server` floor stays at 50.

**Verification commands used throughout** (all must stay green):

```powershell
.\gradlew.bat :app:compileDebugKotlin --console=plain
.\gradlew.bat :app:testDebugUnitTest --console=plain     # floor 165, target >= 172
.\gradlew.bat :server:test --console=plain               # floor 50, unchanged
.\.mdfiles\check-boundaries.ps1                          # ui-imports-data = 0
.\.mdfiles\check-contrast.ps1
.\server\scripts\smoke.ps1 -Base http://127.0.0.1:8099   # needs :server:run up
```

Two traps to carry into the tasks:

- **`TokenStore` takes a `android.content.Context` and there is no Robolectric**,
  so a `TokenStoreTest` cannot construct one. Validation has to be a pure
  function over bytes.
- **OkHttp's `Authenticator` re-sends whatever it is handed**, and
  `MediQApiService` has five non-idempotent routes. Without an explicit method
  check a 401 on a booking re-issues the `POST`.

---

## Task 1: `ApiFailure.isAuthFailure`

**Description:** One flag beside the existing `isNetworkFailure`, keyed on
`status == 401`. This is what lets the prompt and the tests talk about "the
token was rejected" without pattern-matching on a message string. Pure
additive change to a file that is already compiled into `:server` via
`sharedDomain`, so it must not import `android` or `androidx.compose`.

**Acceptance criteria:**
- [ ] `isAuthFailure` is a `val` property on `ApiFailure`, `get() = status == 401`, declared immediately after `isNetworkFailure` (`ApiFailure.kt:38`)
- [ ] KDoc states the contrast with `isNetworkFailure`: one means "never reached the server", the other "reached it and was told who you are is no longer valid"
- [ ] `ApiErrorsTest` gains cases for a 401, a 403, and a network failure, asserting `isAuthFailure` and `isNetworkFailure` are independent

**Verification:**
- [ ] `.\gradlew.bat :app:testDebugUnitTest --console=plain` green
- [ ] `.\gradlew.bat :server:test --console=plain` green — `ApiFailure` is in `sharedDomain`, so a bad import breaks the server build
- [ ] `.\.mdfiles\check-boundaries.ps1` green

**Dependencies:** None

**Files likely touched:**
- `app/src/main/java/com/example/mediq/domain/model/ApiFailure.kt`
- `app/src/test/java/com/example/mediq/data/api/ApiErrorsTest.kt`

**Estimated scope:** XS (2 files)

---

## Task 2: `SessionSignal` in `domain/`

**Description:** The `ui/`↔`data/` seam. A `MutableStateFlow<Boolean>` owned by
`AppContainer`, created before the Retrofit client and handed to both the OkHttp
stack and the nav host. It lives in `domain/` so `ui/` can read it without
importing `data/`, and it must not import `android` or Compose.

**Acceptance criteria:**
- [ ] `domain/model/SessionSignal.kt` owns a `MutableStateFlow<Boolean>` exposed as `StateFlow<Boolean>`, plus `fun markExpired()` and `fun clear()`
- [ ] It starts `false`; a fresh install and a signed-out patient are indistinguishable to the nav host
- [ ] KDoc names the bug in the past tense — five screens each rendering an unactionable "Please sign in again."
- [ ] `MediQNavHost` importing `AppContainer.sessionSignal` is not a boundary violation; `check-boundaries.ps1` still reports `ui-imports-data` = 0

**Verification:**
- [ ] `.\gradlew.bat :app:testDebugUnitTest --console=plain` green
- [ ] Tests cover: starts false, `markExpired()` flips it, `clear()` flips it back
- [ ] `.\.mdfiles\check-boundaries.ps1` green — `domain-imports-android` = 0
- [ ] Manual check: `Select-String -Path domain/model/SessionSignal.kt -Pattern 'import (android|androidx)'` returns nothing

**Dependencies:** Task 1

**Files likely touched:**
- `app/src/main/java/com/example/mediq/domain/model/SessionSignal.kt` (new)
- `app/src/test/java/com/example/mediq/domain/model/SessionSignalTest.kt` (new)

**Estimated scope:** S (2 files)

---

## Task 3: Pure session validation + `TokenStore` validate-on-read

**Description:** Kills Bug 2 at the boundary. Today `getSession()` wraps only
the Gson parse (`TokenStore.kt:29`), so a blob whose `expiresAt` will not parse
survives the read and throws later at `ApiMappers.kt:52`, outside every
`runCatching` — leaving the Profile screen wedged in `LoadState.Error` with the
token still stored.

**The constraint:** `TokenStore`'s constructor takes a `Context` and there is no
Robolectric, so validation must be a pure function over bytes that tests can
call directly. `TokenStore` keeps only the `SharedPreferences` call.

**Acceptance criteria:**
- [ ] A pure function maps a session blob to a domain session or null, and calls `toDomain()` **inside** its own guard so a malformed `expiresAt` yields null rather than throwing
- [ ] `TokenStore.getSession()` uses that function; when a stored blob exists but will not map, the key is removed and null is returned
- [ ] `currentSession()` returns null for a corrupt blob without throwing, and the store is empty afterwards
- [ ] The null-`getSession()` case (no key at all) does not call `clearSession()`
- [ ] KDoc records why the parse was moved inside the guard, not that it "throws"

**Verification:**
- [ ] `.\gradlew.bat :app:testDebugUnitTest --console=plain` green
- [ ] Tests cover: valid blob, malformed `expiresAt`, malformed `dateOfBirth`, empty string, absent key
- [ ] Tests assert no exception escapes — `@Test(expected = ...)` is the wrong tool here, assert the returned null
- [ ] Manual check: `.\gradlew.bat :app:compileDebugKotlin --console=plain` green

**Dependencies:** Task 1

**Files likely touched:**
- `app/src/main/java/com/example/mediq/data/api/TokenStore.kt`
- `app/src/main/java/com/example/mediq/data/api/SessionMapper.kt` (new — the pure function)
- `app/src/test/java/com/example/mediq/data/api/SessionMapperTest.kt` (new)
- `app/src/test/java/com/example/mediq/data/repository/RetrofitAuthRepositoryTest.kt` (new — first test in `data/repository/`)

**Estimated scope:** M (4 files)

---

## Checkpoint A: Signal and validation

- [ ] `:app:compileDebugKotlin` green
- [ ] `:app:testDebugUnitTest` green, above the 165 floor
- [ ] `:server:test` green at 50
- [ ] `check-boundaries.ps1` reports `domain-imports-android` = 0
- [ ] Review with human before proceeding

---

## Task 4: `SessionAuthenticator` — clear, signal once, retry reads only

**Description:** The core of D1. OkHttp's `Authenticator` is what re-issues a
request; `AuthInterceptor` cannot retry because an interceptor only sees the
response. Three responsibilities, in order: refuse to re-issue a write, clear
the dead token, raise the signal.

**The write guard is the load-bearing part.** `Authenticator` re-sends whatever
request it was handed, and `MediQApiService` declares five non-idempotent
routes: `POST /appointments`, `DELETE /appointments/{id}`, `PATCH`,
`PUT /…`, `POST /…/reschedule-request`. `POST /appointments` has no idempotency
key, so a retried write is how a patient gets double-booked — the first attempt
may have succeeded server-side and lost its response.

**Acceptance criteria:**
- [ ] Returns null for any method outside the read allow-list, so no non-idempotent request is ever re-issued
- [ ] Clears `TokenStore` on every 401 it handles, so a dead token is not re-sent on the next request
- [ ] Calls `sessionSignal.markExpired()` — and the signal is **idempotent**, so five concurrent 401s produce one state change, not five
- [ ] Caps retries per request: a response carrying more than one prior 401 attempt returns null rather than looping
- [ ] KDoc names the double-booking hazard in the past tense

**Verification:**
- [ ] `.\gradlew.bat :app:testDebugUnitTest --console=plain` green
- [ ] Tests cover: 401 on a `GET` re-issues exactly once; 401 on `POST` / `DELETE` / `PATCH` / `PUT` re-issues **zero** times; five concurrent 401s → one signal transition; the store is empty afterwards
- [ ] **Mutation check:** temporarily allow `POST` in the allow-list, confirm the write test fails, restore. A guard nobody has broken is not known to work.
- [ ] Manual check: `.\gradlew.bat :app:compileDebugKotlin --console=plain` green

**Dependencies:** Tasks 2, 3

**Files likely touched:**
- `app/src/main/java/com/example/mediq/data/api/RetrofitClient.kt` — `SessionAuthenticator` beside `AuthInterceptor`
- `app/src/test/java/com/example/mediq/data/api/SessionAuthenticatorTest.kt` (new)

**Estimated scope:** M (2 files)

---

## Task 5: `AppContainer` owns the signal; hand it to the OkHttp stack

**Description:** Wiring only. Ordering is the constraint:
`AppContainer.kt:41` builds the Retrofit client before any repository, so the
signal has to be constructed above line 41 and threaded through
`RetrofitClient.create(tokenStore, sessionSignal)`.

**Acceptance criteria:**
- [ ] `AppContainer` creates the `SessionSignal` first and exposes it as a `lateinit` alongside the existing ones
- [ ] `RetrofitClient.create` takes the signal and registers the `Authenticator` on the `OkHttpClient.Builder` alongside the two interceptors
- [ ] Every existing repository is still constructed from the same `api` instance — no second Retrofit client
- [ ] The `debug`/`release` `buildLoggingInterceptor` pair is untouched

**Verification:**
- [ ] `.\gradlew.bat :app:compileDebugKotlin --console=plain` green
- [ ] Manual check: `git diff app/src/main/java/com/example/mediq/di/AppContainer.kt` is wiring only, no logic
- [ ] Emulator: sign in, browse Doctors (public route, no token), confirm no crash and no spurious prompt

**Dependencies:** Task 4

**Files likely touched:**
- `app/src/main/java/com/example/mediq/di/AppContainer.kt`
- `app/src/main/java/com/example/mediq/data/api/RetrofitClient.kt`

**Estimated scope:** S (2 files)

---

## Task 6: `currentSession()` contract simplification

**Description:** Once `getSession()` validates on read, `currentSession()`'s
own expiry branch is redundant: null already means "no usable session". Leaving
both means two places decide what a dead session is, and they will disagree.

**Acceptance criteria:**
- [ ] `currentSession()` returns `tokenStore.getSession()`'s domain result directly, with the `isExpired` branch removed
- [ ] An expired session still returns null and leaves the store empty — the expiry test moves into the store, it does not disappear
- [ ] `ProfileViewModel.kt:58`'s generic `catch (e: Exception)` path is no longer reachable from a corrupt blob; if a guard is still wanted there, it is documented as defence-in-depth
- [ ] Tests cover: expired → null + store cleared; corrupt → null + store cleared + no throw; valid → the session

**Verification:**
- [ ] `.\gradlew.bat :app:testDebugUnitTest --console=plain` green
- [ ] `.\gradlew.bat :server:test --console=plain` green
- [ ] Manual check: `rg isExpired app/src/main` — every remaining call site is one that can actually see an unexpired-but-stored blob

**Dependencies:** Task 3

**Files likely touched:**
- `app/src/main/java/com/example/mediq/data/repository/RetrofitAuthRepository.kt`
- `app/src/test/java/com/example/mediq/data/repository/RetrofitAuthRepositoryTest.kt`

**Estimated scope:** S (2 files)

---

## Checkpoint B: Request path

- [ ] Five concurrent 401s produce exactly one signal transition — proven by test, not by inspection
- [ ] A 401 on `POST /appointments` issues zero additional requests
- [ ] A corrupt `expiresAt` yields null and an empty store, with no throw
- [ ] `:app:testDebugUnitTest` green
- [ ] Review with human before proceeding

---

## Task 7: `MediQNavHost` observes the signal, one-tap sign-in

**Description:** Closes acceptance criteria 1-3. Today the signal has nowhere to
land; the nav host is the only place that can redirect from any screen without
adding a seventh sign-in entry point.

**The prompt is a modal, not a screen replacement** (Open Question 1 in the
spec). A modal satisfies "one tap, from that screen" and stops the patient
tapping cancel/reschedule against a token the server has already rejected.

**`LoadState.Error` is not an option here.** It carries a `String` and nothing
else (`LoadState.kt:18`), so threading a 401 through it would change 10
ViewModels and their tests. The signal already carries the fact.

**Acceptance criteria:**
- [ ] A single `LaunchedEffect` at the nav host collects the signal; no screen-level 401 handling is added
- [ ] On `true`, one modal with a sign-in action appears, dismissable only by signing in
- [ ] The sign-in action navigates to `Screen.SignIn` with `popUpTo` that **keeps** the booking back stack entry rather than clearing the whole stack
- [ ] After a successful sign-in the patient lands on Home and the prompt is gone — Open Question 2, decided
- [ ] `SignInScreen`'s existing success navigation is reused, not duplicated
- [ ] The signal is cleared when the modal resolves, so a rotate does not re-prompt

**Verification:**
- [ ] `.\gradlew.bat :app:testDebugUnitTest --console=plain` green
- [ ] `.\gradlew.bat :app:compileDebugKotlin --console=plain` green
- [ ] `.\.mdfiles\check-boundaries.ps1` green — `ui-imports-data` = 0
- [ ] Manual check: `rg "status ==" app/src/main/java/com/example/mediq/ui` returns nothing; no screen branches on 401 itself
- [ ] Emulator: `$env:MEDIQ_TOKEN_TTL_MINUTES="2"`, sign in, wait, confirm **one** prompt per screen on Home, Doctors, Appointments and Profile

**Dependencies:** Task 5

**Files likely touched:**
- `app/src/main/java/com/example/mediq/ui/navigation/MediQNavHost.kt`
- `app/src/main/java/com/example/mediq/ui/navigation/Routes.kt`

**Estimated scope:** M (2 files)

---

## Task 8: Clear the signal on sign-out

**Description:** The one High-severity risk in the spec. `signOut()` clears the
token but nothing else, so the signal would keep its last value and the next
patient to sign in on that device could inherit a stale prompt. Small task,
carried on its own so it is not forgotten inside Task 5's wiring.

**Acceptance criteria:**
- [ ] `signOut()` clears the signal alongside `TokenStore.clearSession()`
- [ ] A sign-out followed by a sign-in shows no prompt
- [ ] Tests cover the ordering — signal cleared even when the server call fails (`ProfileViewModel.kt:44` already `runCatching`s it)

**Verification:**
- [ ] `.\gradlew.bat :app:testDebugUnitTest --console=plain` green
- [ ] Manual check: `ProfileViewModelTest` has a sign-out-then-sign-in case
- [ ] Emulator: sign out, sign back in, confirm no prompt appears

**Dependencies:** Tasks 5, 6

**Files likely touched:**
- `app/src/main/java/com/example/mediq/data/repository/RetrofitAuthRepository.kt`
- `app/src/test/java/com/example/mediq/data/repository/RetrofitAuthRepositoryTest.kt`

**Estimated scope:** XS (2 files)

---

## Task 9: `smoke.ps1` — a booking across an expiry produces exactly one `POST`

**Description:** Success criterion 4 is an HTTP-level claim, and a unit test
cannot make it. The script currently exercises `DELETE /appointments/{id}` and
no auth-expiry path, so the guarantee that matters most — no double-booking —
has no repeatable check above the emulator.

**Acceptance criteria:**
- [ ] `smoke.ps1` signs in, waits past the TTL, then submits a booking and asserts exactly **one** `POST /appointments` reached the server
- [ ] The assertion fails loudly on two POSTs; it does not merely warn
- [ ] Existing `smoke.ps1` checks are unchanged and still pass

**Verification:**
- [ ] `.\server\scripts\smoke.ps1 -Base http://127.0.0.1:8099` green against `:server:run` with `MEDIQ_TOKEN_TTL_MINUTES=2`
- [ ] `.\gradlew.bat :server:test --console=plain` green at 50
- [ ] Manual check: `git diff server/` shows only `scripts/smoke.ps1` — no route, no schema change (D2 = re-auth means none is needed)

**Dependencies:** Tasks 4, 7

**Files likely touched:**
- `server/scripts/smoke.ps1`

**Estimated scope:** S (1 file)

---

## Checkpoint C: Prompt

- [ ] `MEDIQ_TOKEN_TTL_MINUTES=2` verified on the emulator — one prompt per screen, never three
- [ ] A booking submitted across an expiry produces exactly one `POST`, checked in `smoke.ps1`
- [ ] A corrupt `expiresAt` recovers on its own, with no app-data clear
- [ ] `:server:test` green at 50
- [ ] Review with human before proceeding

---

## Task 10: `AGENTS.md`, `CHANGELOG.md`, `README.md`, `CONSTRAINTS.md`

**Description:** `AGENTS.md` states it must be updated in the same change that
alters the repo, and is the only orientation a new session gets. The gap
bullet this closes is named there, and success criterion 5 says the fix does not
count as done without it.

**Acceptance criteria:**
- [ ] The token-expiry gap bullet is **removed**, not reworded — per that file's own rule an item leaves only once it is implemented
- [ ] Auth section records the `SessionSignal` seam, so the next session does not re-derive it
- [ ] Auth section notes that a refresh endpoint can be added later without changing the seam, and that D2 chose re-auth
- [ ] `CONSTRAINTS.md` ratchets updated **in the same commit**: `:app` floor 165 → the new count, `:server` floor stays 50 (D2 added no route, so there is nothing to move)
- [ ] `README.md:146-147` and the `CHANGELOG.md` `[Unreleased]` section both updated

**Verification:**
- [ ] Manual check: re-read `AGENTS.md` top to bottom and confirm every claim is true of the tree
- [ ] Manual check: no gap bullet claims work that is not implemented
- [ ] `git diff .mdfiles/` shows only documentation edits
- [ ] `.\.mdfiles\check-boundaries.ps1` and `.\.mdfiles\check-contrast.ps1` green

**Dependencies:** Tasks 7, 8, 9

**Files likely touched:**
- `.mdfiles/AGENTS.md`
- `.mdfiles/CHANGELOG.md`
- `.mdfiles/CONSTRAINTS.md`
- `README.md`

**Estimated scope:** Small (4 files, docs only)

---

## Checkpoint D: Complete

- [ ] `:app:testDebugUnitTest` green at ≥172; `:server:test` green at 50
- [ ] `check-boundaries.ps1` reports `ui-imports-data` = 0 and `domain-imports-android` = 0
- [ ] `README.md:146-147` and the `AGENTS.md` known-gap bullet updated in the same commit
- [ ] Every new test mutation-checked: break the code, watch it fail, restore
- [ ] Ready for review
