# Implementation Plan: Close the Two Functional Gaps in MediQ

## Overview

Two features are listed as "Known gaps" in `.mdfiles/AGENTS.md` and neither is
reachable from the running app:

1. **The registration wizard is not connected to the backend.** All four
   `ui/feature/auth/register/` screens navigate between each other on local
   `remember` state with no arguments, so the full name and phone number are
   discarded the moment the user taps Continue. No account can actually be
   created from the app.
2. **`AppointmentDetailsScreen` is a static empty state.** It receives an
   `appointmentId` from its route and ignores it, always rendering
   `EmptyState`. There is no ViewModel, and no place to cancel or request a
   reschedule.

This plan closes both. Everything needed already exists on the server side, so
**this is UI-layer work only** — no new endpoints, no DTO changes, no schema
changes.

## Current State (verified against the filesystem, not assumed)

| Fact | Evidence |
|---|---|
| Build is green before any change | `:app:compileDebugKotlin` → `BUILD SUCCESSFUL`, 1 known `statusBarColor` warning |
| Repository interfaces are already complete | `AuthRepository` has `requestOtp`, `verifyOtp`, `register`; `AppointmentRepository` has `getAppointment`, `cancel`, `requestReschedule` |
| Retrofit implementations already exist | `RetrofitAuthRepository` implements all three auth calls and persists the session via `TokenStore` |
| Server routes already exist | `http/Routes.kt`: `POST /auth/otp/request`, `POST /auth/otp/verify`, `POST /auth/register`, `GET /appointments/{id}`, `DELETE /appointments/{id}`, `POST /appointments/{id}/reschedule-request` |
| Server-side wizard is already tested | `AuthServiceTest` has 17 tests covering OTP request/verify/lockout and registration |
| The app has no runnable test suite | `:app` has only `testImplementation(libs.junit)`; `ExampleUnitTest` is an untouched template |
| **There is now a working emulator** | AVD `mediq_api36` (API 36) created and verified end-to-end: installs, launches, and signs in against the live server via `10.0.2.2` |
| The working tree is large and uncommitted | 24 modified/deleted files plus 25 untracked entries, including all of `data/`, `di/`, and every ViewModel |

### The blocking discovery

`AuthServiceTest.kt:221` — `register rejects registration without a date of
birth`. The server **requires** `dateOfBirth`. But:

- `RegisterRequest.dateOfBirth` is `LocalDate?` — nullable, so the client
  compiler will not catch a missing value.
- `RegisterDetailsScreen` collects only full name and phone number.

Wiring the credentials step to `register` without collecting a date of birth
would compile, pass `:app:compileDebugKotlin`, and then fail at runtime against
a real server. Adding date of birth to the details step is therefore **in
scope**, not scope creep. `email` and `sex` are genuinely optional (the tests
pass `null` for both on the success path) and stay out of scope.

## Architecture Decisions

**One ViewModel for the whole wizard, scoped to a nested navigation graph.**
The four register screens are argument-free siblings in the root `NavHost`, so
there is nowhere for step 2 to read what step 1 collected. The fix is to nest
them in a `navigation(startDestination = RegisterDetails)` graph and obtain a
single `RegisterViewModel` with `viewModel(viewModelStoreOwner = graphBackStackEntry)`.

Rejected alternatives:

- *Route arguments* (`register_otp/{phoneNumber}/{fullName}`) — pollutes the route
  table with strings that need URL-encoding, and puts a secret-adjacent value in
  a navigation route.
- *One ViewModel per screen, passing results forward* — spreads a single
  three-step state machine across four classes and re-implements parameter
  passing by hand.

**`registrationId` lives in the ViewModel only, never in the UI layer.** It is
the proof that the phone number was verified; keeping it in one place makes the
"no id → send them back to step 1" guard in Task 6 a one-line check.

**Follow the existing ViewModel pattern exactly.** `SignInViewModel` and
`AppointmentsViewModel` both use a `MutableStateFlow` + `StateFlow` + a
`companion object { val Factory = viewModelFactory { initializer { X(AppContainer.y) } } }`.
No Hilt, no new DI mechanism — `AGENTS.md` states the app is wired by hand on
purpose.

**`LoadState` for reads, a plain `isLoading`/`error` pair for form submits.**
`AGENTS.md` requires every ViewModel to expose `LoadState<T>` and screens to
`when` over all three cases. Form submission is not a read, and `SignInViewModel`
already models it with booleans; the credentials and details screens follow that
precedent instead of inventing a second convention.

**Client-side validation mirrors the server, it does not replace it.** The server
enforces password length >= 8, unique username, and required date of birth. The
client checks the same three so the user gets an answer without a round trip,
and the server remains the authority.

## Task List

### Phase 1: Baseline and verification harness

- [ ] Task 1: Commit the uncommitted working tree as a reviewed baseline
- [ ] Task 2: Give `:app` a real unit test suite

### Checkpoint A: Baseline and harness

- [ ] `:app:compileDebugKotlin` green
- [ ] `:app:testDebugUnitTest` green and actually running assertions
- [ ] `:server:test` still green (17+ tests)

### Phase 2: Registration wizard

- [ ] Task 3: `RegisterViewModel` wizard state machine, with unit tests
- [ ] Task 4: Registration nav graph + details screen wired to `requestOtp`
- [ ] Task 5: OTP screen wired to `verifyOtp`
- [ ] Task 6: Credentials and success screens wired to `register`

### Checkpoint B: Registration

- [ ] All three wizard steps covered by unit tests against a fake repository
- [ ] Every registration failure mode from `AuthServiceTest` has a UI path
- [ ] `:app:compileDebugKotlin` and `:app:testDebugUnitTest` green

### Phase 3: Appointment details

- [ ] Task 7: `AppointmentDetailsViewModel` and a real details screen
- [ ] Task 8: Cancel and reschedule actions gated on `AppointmentStatus.isActionable`

### Checkpoint C: Appointment details

- [ ] Details screen renders a real appointment in all three `LoadState` cases
- [ ] Cancel and reschedule are absent for `COMPLETED`, `CANCELLED`, `DECLINED`
- [ ] `:server:test` still green

### Phase 4: Documentation

- [ ] Task 9: Update `.mdfiles/AGENTS.md` and `.mdfiles/CHANGELOG.md`

### Checkpoint D: Complete

- [ ] Both "Known gaps" bullets in `AGENTS.md` removed — moved only after the
      work is real, per that file's own maintenance rule
- [ ] All acceptance criteria met
- [ ] Ready for review

## Risks and Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| **The emulator is slow and single-threaded.** WHPX works, but the CPU lacks some modern x86 virtualisation features, so the emulator forces 1 vCPU; a cold boot takes 86s and `installDebug` ~2min | Medium | Budget minutes per install-and-launch cycle. Do not "fix" it by editing `config.ini` — `hw.cpu.ncore` and `hw.ramSize` are overridden on boot. Recorded in `AGENTS.md`. |
| **The emulator exists, so UI bugs can now be seen but not caught by CI.** Compose regressions pass a manual look | Medium | Task 2's unit tests stay the repeatable gate; the emulator is for confirming behaviour end-to-end, not for proving correctness. |
| **`smoke.ps1` has no OTP or register coverage** — it only exercises `DELETE /appointments/{id}` | Medium | The registration HTTP contract is proven in-process by `AuthServiceTest`. Task 6 carries an optional sub-item to add `smoke.ps1` checks if HTTP-level coverage is wanted. |
| **Process death mid-wizard loses `registrationId`**, stranding the user on the credentials step with nothing to submit | Medium | Task 6 requires a guard: if `registrationId` is null on the credentials step, navigate back to details instead of calling `register`. |
| **Date picker to `LocalDate` on minSdk 24.** `DatePicker` yields epoch millis | Low | `isCoreLibraryDesugaringEnabled = true` is already on in `app/build.gradle.kts`, so `java.time` is safe. Convert in the ViewModel, not the composable. |
| **A required-field regression slips through because `RegisterRequest` fields are nullable** | Medium | `RegisterRequest.dateOfBirth` stays nullable (it mirrors the domain, and `:server` shares it), but Task 4 makes the ViewModel reject a blank date of birth and Task 6's tests assert `register` is never called without one. |
| **`register` signs the user in but the success screen says "sign in"** | Medium | Raised as Open Question 1. Default chosen so implementation is not blocked. |
| **A giant uncommitted diff makes the new work unreviewable or unrevertable** | High | Task 1 commits the existing tree first, so every task after it is a small isolated diff. |

## Open Questions

1. **Where does the wizard go after `register` succeeds?** `RetrofitAuthRepository.register`
   calls `tokenStore.saveSession(dto)`, so the user is **already signed in** when
   `RegisterSuccessScreen` renders — but that screen's copy says "sign in to
   search doctors" and its button navigates to `Screen.SignIn`. Sending a signed-in
   user to the sign-in form is a bug, not a style choice.
   *Recommended default:* navigate straight to `Screen.Home` and pop the whole
   auth stack. *Alternative:* keep the current behaviour for spec fidelity if the
   design document mandates the extra step. Needs a human answer.
2. **Should the success screen show a real name?** It currently hardcodes
   "Welcome to MediQ, Jesse." That string should come from the returned
   `AuthSession`. Assumed yes — it is a one-line fix inside Task 6 and leaving a
   hardcoded personal name in a real-patient app contradicts the "no invented
   data" rule.
3. **Should `sex` and `email` be collected?** The server accepts `null` for both.
   Out of scope here. Flagged so it is a decision rather than an oversight.
4. **`tasks/` conflicts with the docs-location rule.** `.mdfiles/AGENTS.md`
   states all repo markdown lives in `.mdfiles/`. The user chose `tasks/`
   explicitly, so this plan uses `tasks/plan.md` and `tasks/todo.md`.
   Task 9 amends `AGENTS.md` to name `tasks/` as well, so the two documents
   agree instead of contradicting each other.
