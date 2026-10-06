# MediQ — Task List

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
- [ ] `git status` is clean afterwards, with no `.idea/` or `.artifacts/` staged
- [ ] `.gitignore` adds `/.idea/` (it currently lists only four specific files under `.idea/`, so the rest still surfaces as untracked) and `.artifacts/`; the `app-tree.txt` snapshot is either ignored or committed deliberately
- [ ] The commit message states that this is the networking + server backend work, not new work

**Verification:**
- [ ] `git status --short` returns nothing
- [ ] `.\gradlew.bat :app:compileDebugKotlin --console=plain` still green (a commit must not change the build)
- [ ] Manual check: read `git show --stat` and confirm no build output or IDE config was captured

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
- [ ] `kotlinx-coroutines-test` added to `gradle/libs.versions.toml` using the existing `coroutines` version ref (`1.10.2`), and wired as `testImplementation` in `app/build.gradle.kts`
- [ ] `ExampleUnitTest.kt` deleted and replaced with at least one genuine test that asserts a real value
- [ ] Hand-written fakes for `AuthRepository` and `AppointmentRepository` exist under `app/src/test/`, implementing the interfaces with settable results and no mocking framework
- [ ] `:app:testDebugUnitTest` reports executed tests, not "no tests found"

**Verification:**
- [ ] `.\gradlew.bat :app:testDebugUnitTest --console=plain` green with a non-zero test count
- [ ] Confirm the Gradle task name via `.\gradlew.bat :app:tasks --all` if `testDebugUnitTest` is not found — AGP 9 may name it differently
- [ ] Manual check: deliberately break one assertion, confirm the build fails, then restore it (a harness that cannot fail is not a harness)

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

- [ ] `:app:compileDebugKotlin` green
- [ ] `:app:testDebugUnitTest` green and actually running assertions
- [ ] `:server:test` still green
- [ ] Review with human before proceeding

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
- [ ] `RegisterViewModel` exposes one `RegisterUiState` covering all three steps, plus `requestOtp()`, `verifyOtp()`, and `register()` functions
- [ ] Blank full name, blank phone, or null date of birth sets an error and does **not** call the repository
- [ ] `registrationId` is stored by `verifyOtp()` and cleared when `register()` fails, so a retry re-uses it rather than re-verifying
- [ ] Unit tests cover: each validation branch, a successful three-step run, a wrong OTP, and a duplicate username

**Verification:**
- [ ] `.\gradlew.bat :app:testDebugUnitTest --console=plain` green
- [ ] `.\gradlew.bat :app:compileDebugKotlin --console=plain` green
- [ ] Manual check: confirm `domain/` gained no Android or Compose imports

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
- [ ] The four register routes live in a nested `navigation()` graph in `MediQNavHost`, and each screen obtains the ViewModel from the graph's `backStackEntry`
- [ ] The details screen collects full name, phone number, and date of birth, and calls `requestOtp()` — navigation to the OTP step happens **only** on success
- [ ] `Screen.RegisterOTP.route` stays argument-free, and no registration value is passed through a route string

**Verification:**
- [ ] `.\gradlew.bat :app:compileDebugKotlin --console=plain` green
- [ ] `.\gradlew.bat :app:testDebugUnitTest --console=plain` green
- [ ] Manual check: grep the register package for `mutableStateOf` — no local form state should remain on these screens

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
- [ ] Verify calls `verifyOtp()`, and a wrong code shows the server's message without clearing the typed code or advancing
- [ ] The lockout case (five wrong attempts, per `AuthServiceTest`) shows a distinct message rather than the generic failure
- [ ] The dev OTP is displayed only when non-empty, labelled as dev-only
- [ ] Tests cover the success and wrong-code paths against `FakeAuthRepository`

**Verification:**
- [ ] `.\gradlew.bat :app:testDebugUnitTest --console=plain` green
- [ ] `.\gradlew.bat :app:compileDebugKotlin --console=plain` green
- [ ] `.\gradlew.bat :server:test --console=plain` green — the lockout threshold must match `AuthServiceTest`

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
- [ ] Mismatched or under-8-character passwords set an error and never call `register`; a duplicate username shows the server's message
- [ ] On success the screen shows the real name from the returned `AuthSession`, and the hardcoded "Jesse" string is gone
- [ ] If `registrationId` is null on this step (e.g. the process was killed after OTP verification), navigate back to the details step instead of calling `register`
- [ ] Tests cover: successful registration, password mismatch, duplicate username, and the missing-`registrationId` guard

**Verification:**
- [ ] `.\gradlew.bat :app:testDebugUnitTest --console=plain` green
- [ ] `.\gradlew.bat :app:compileDebugKotlin --console=plain` green
- [ ] `.\gradlew.bat :server:test --console=plain` green
- [ ] Optional sub-item: `server/scripts/smoke.ps1` has no OTP or register coverage — add checks for `POST /auth/otp/request`, `/verify`, `/register` if HTTP-level proof is wanted
- [ ] Manual check: read `Logout`/`SignIn` navigation in `MediQNavHost` and confirm the back stack does not return a signed-in user to the wizard
- [ ] Emulator walkthrough: register a new account end-to-end. `AuthService.returnCodeToCaller` is `true`, so the OTP comes back in the HTTP response — read it from `adb logcat | Select-String okhttp` rather than guessing, then confirm the new account can sign in

**Dependencies:** Task 5

**Files likely touched:**
- `app/src/main/java/com/example/mediq/ui/feature/auth/register/RegisterCredentialsScreen.kt`
- `app/src/main/java/com/example/mediq/ui/feature/auth/register/RegisterSuccessScreen.kt`
- `app/src/test/java/com/example/mediq/ui/feature/auth/register/RegisterViewModelTest.kt`

**Estimated scope:** Medium (3 files)

---

## Checkpoint B: Registration

- [ ] All three wizard steps covered by unit tests against a fake repository
- [ ] Every registration failure mode in `AuthServiceTest` has a UI path
- [ ] `:app:compileDebugKotlin` and `:app:testDebugUnitTest` green
- [ ] Both "not connected to the backend" bullets in `.mdfiles/AGENTS.md` now describable as done

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
- [ ] `AppointmentDetailsViewModel` loads by the route's `appointmentId` and exposes `LoadState<Appointment>`; the screen `when`s over Loading, Success, and Error
- [ ] A null `appointmentId` renders a clear empty state and makes no network call
- [ ] The screen displays doctor, date, time, location, fee via `Money.format()`, status via `displayName`, and reason for visit
- [ ] Dates and times render through `toClinicDate()` / `toClinicTime()`, never device-local zone

**Verification:**
- [ ] `.\gradlew.bat :app:testDebugUnitTest --console=plain` green — tests cover success, error, and the null-id case
- [ ] `.\gradlew.bat :app:compileDebugKotlin --console=plain` green
- [ ] Manual check: confirm no `EmptyState("No appointment selected")` string remains as the main content
- [ ] Emulator walkthrough: open an appointment from the Appointments tab and confirm the details render with real server data

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
- [ ] Cancel and Reschedule render **only** when `status.isActionable`, and are absent for `COMPLETED`, `CANCELLED`, and `DECLINED`
- [ ] A successful cancel or reschedule request refreshes the displayed appointment; a failure shows the server message and leaves the appointment unchanged
- [ ] Reschedule requires a chosen slot and submits a `RescheduleRequest` with the real `appointmentId`
- [ ] Tests cover: cancel success, cancel failure, reschedule success, and the `isActionable` gate

**Verification:**
- [ ] `.\gradlew.bat :app:testDebugUnitTest --console=plain` green
- [ ] `.\gradlew.bat :app:compileDebugKotlin --console=plain` green
- [ ] `.\gradlew.bat :server:test --console=plain` green — `AppointmentLifecycleTest` guards the cancel path; it must not be weakened
- [ ] Manual check: `git diff --stat server/` is empty — this task must not touch the server
- [ ] Emulator walkthrough: cancel an appointment, confirm the status badge updates and the slot reappears as bookable in `server/scripts/smoke.ps1`'s rebooking check

**Dependencies:** Task 7

**Files likely touched:**
- `app/src/main/java/com/example/mediq/ui/feature/appointments/AppointmentDetailsViewModel.kt`
- `app/src/main/java/com/example/mediq/ui/feature/appointments/AppointmentDetailsScreen.kt`
- `app/src/test/java/com/example/mediq/ui/feature/appointments/AppointmentDetailsViewModelTest.kt`

**Estimated scope:** Medium (3 files)

---

## Checkpoint C: Appointment details

- [ ] Details screen renders a real appointment in all three `LoadState` cases
- [ ] Cancel and reschedule absent for completed, cancelled, and declined appointments
- [ ] `:server:test` still green
- [ ] Review with human before proceeding

---

## Task 9: Update `.mdfiles/AGENTS.md` and `.mdfiles/CHANGELOG.md`

**Description:** `.mdfiles/AGENTS.md` states it "must be updated in the same
change that alters the repo" and is "the only orientation a new session gets".
Both gaps it lists are now closed, and the two new facts a session would
otherwise waste a diagnostic pass rediscovering are the test commands and the
registration nav graph.

**Acceptance criteria:**
- [ ] Both "Known gaps" bullets about the registration flow and `AppointmentDetailsScreen` are **removed**, not reworded — per that file's own rule, an item leaves only once it is implemented
- [ ] The "Build environment" section documents `:app:testDebugUnitTest` alongside `:app:compileDebugKotlin`
- [ ] The "Architecture" section records that the four register screens share one graph-scoped `RegisterViewModel`
- [ ] The "Docs location" section names `tasks/` so it no longer contradicts the plan files
- [ ] `.mdfiles/CHANGELOG.md` gains entries under `[Unreleased]` for the wizard and the details screen

**Verification:**
- [ ] Manual check: re-read `AGENTS.md` top to bottom and confirm every claim is still true of the tree
- [ ] Manual check: confirm no gap bullet claims work that is not implemented
- [ ] `git diff .mdfiles/` shows only documentation edits

**Dependencies:** Tasks 6 and 8

**Files likely touched:**
- `.mdfiles/AGENTS.md`
- `.mdfiles/CHANGELOG.md`

**Estimated scope:** Small (2 files)

---

## Checkpoint D: Complete

- [ ] Both "Known gaps" bullets removed from `AGENTS.md`
- [ ] All acceptance criteria met across Tasks 1-9
- [ ] `:app:compileDebugKotlin`, `:app:testDebugUnitTest`, `:server:test` all green
- [ ] Ready for review
