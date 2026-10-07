# Implementation Plan: Temporary Seeded-Data Screen (backend verification)

> **Status: active.** The previous plan (token lifecycle) is spec'd and planned
> but **unstarted** — verified, not assumed — and is archived below. Plan v1
> (registration + appointment details) is complete and archived at the bottom.
> Task lists for all three live in `tasks/todo.md`.
>
> Spec: `tasks/SPEC-seeded-data-screen.md`

## Overview

One question has no single answer today: **is the backend actually working?**
The seed prints to a console `:server:run` scrolls away, the Doctors tab renders
an empty list whether the seed ran or the server is down, and "Couldn't reach the
clinic" reads the same whether Wi-Fi dropped or the base URL points at the wrong
host.

This adds one temporary screen that calls the live backend through the normal
repository and reports what came back: the rows that exist, or the specific
reason there are none. Every value is read over HTTP when the screen opens.
Nothing is hardcoded — not a doctor, not a count, not a placeholder row.

**This is a temporary verification tool, not a feature.** It has no entry in the
bottom nav, and Task 4 proves it deletes in one commit.

## Current State (verified against the filesystem, 2026-10-07)

| Fact | Evidence |
|---|---|
| The seed works and is invisible | `DemoData.kt` inserts 3 doctors + `demo_patient`; `Main.kt:42-44` gates it on `MEDIQ_SEED_DEMO`; the only evidence is `println` at `DemoData.kt:36,155` |
| Seeding is already idempotent | `DemoData.kt:26-34` skips when `doctors` has rows, so a restart does not duplicate |
| `GET /doctors` is public and needs no token | `README.md:406-407`; `Routes.kt` has no auth block on it. Reachable pre-sign-in, which is where the screen is entered |
| No screen imports `data/` | `check-boundaries.ps1:108` bans it; ratchet `ui-imports-data` = 0. So the base-URL string must be passed in, not imported |
| `SplashScreen` is the colour-ratchet exemption | `CONSTRAINTS.md:98` — its 4 raw literals measure 6.63:1 and are permitted. Adding a gesture there adds **zero** new literals |
| `BuildConfig` does not exist | `app/build.gradle.kts:37-39` declares only `compose = true`; `rg BuildConfig` over the tree returns nothing. A debug gate needs a build-file change |
| Unreachable → empty list is already the contract | `AGENTS.md` Conventions; `RetrofitDoctorRepository` returns an empty `Paged`. Unreachable and empty are **indistinguishable** at this layer |
| `DoctorQuery` has all-default params | `Requests.kt:11-15` — `DoctorQuery()` is a valid unfiltered query |
| `:app` baseline is 166 tests | counted at `HEAD` (166 `@Test` across the 11 committed test classes), matching `SPEC-token-lifecycle.md:61`. `CONSTRAINTS.md` floor said 165 — **corrected to 166 on 2026-10-07**; the docs were stale by one, not the tests. With this plan's Task 1 (+8) the worktree measures 174 |
| `:server` baseline is 50 tests | counted from source; matches `CONSTRAINTS.md:36` |

## Architecture Decisions

Full reasoning is in the spec (D1-D4). The sequencing consequences:

**D1 — no hardcoded rows, anywhere.** The screen is an ordinary consumer of
`DoctorRepository.getDoctors()`. A hardcoded demo list would render identically
with the backend dead, so it would report "working" while proving nothing — a
green light wired to nothing. It would also break `AGENTS.md`'s *"No
fake/invented data"* rule that `DemoData.kt:19` was written to respect. Task 1's
seventh test exists solely to catch a fallback list appearing behind the
repository call.

**D2 — long-press the splash wordmark, 2 seconds.** Zero permanent UI, so
deletion is 4 lines rather than a hunt. A bottom-nav tab would edit a shared
component and survive into a release build.

**D3 — no `BuildConfig.DEBUG` gate.** Enabling it costs a build-file change, and
a gated screen is one nobody tests on release and nobody removes. Removal is
verified by a command in Task 4 instead.

**D4 — two distinguishable outcomes, and the third is not available.** Unreachable,
reachable-but-empty, and server-errored: the first two arrive at this layer as the
*same* `Success(emptyList())`, because `RetrofitDoctorRepository.kt:38-40` absorbs a
network failure and returns an empty `Paged` — correctly, and as documented in
`AGENTS.md` for every other screen. So the empty state names both causes and says it
cannot separate them, instead of guessing one. A wrong guess about a dead backend
costs a diagnostic pass, which is the trap the base-URL subtitle exists to reduce.

A real third state is available cheaply if wanted: `getDoctor` is a single-entity
read that propagates `ApiFailure`, and an unknown id 404s, so one extra call is a
genuine reachability probe. Not done — it is **Ask first** in the spec's Boundaries,
and this is a temporary screen. Recorded in full under Checkpoint B in `tasks/todo.md`.

### Two traps the tasks must route around

**An eager dispatcher settles an `init` load before the fixture is set.**
`SeededDataViewModel` loads in `init`, so the test must build it with `by lazy`
rather than assigning in `setUp`. With `UnconfinedTestDispatcher` the read
completes first and every assertion reads a settled empty state — a green suite
asserting nothing. `AGENTS.md`, "Two traps in the ViewModel tests" §1.

**No real suspension point means `LoadState.Loading` never exists observably.**
The in-flight test needs `FakeDoctorRepository.getDoctorsGate`, a
`CompletableDeferred` the test completes, per the same section.

## Task List

> **Re-planned 2026-10-07** after a gate run. Tasks 1-3 were already written and
> green — Task 3 in particular is in the worktree and compiles, which the original
> plan did not know. What remains is **commit, prove, then document**, in that
> order. The ordering is load-bearing: the ratchet floor cannot move to 174 until
> `SeededDataViewModelTest` is committed, and no doc claim gets written before the
> behaviour it describes has been observed.

### Phase 1: The read path
- [x] Task 1: `SeededDataViewModel` + 8 tests — done, 174 green, two mutations caught
- [x] Checkpoint A: read path

### Phase 2: The screen
- [x] Task 2: `SeededDataScreen` — done, 174 green, contrast ratchet still 4

### Phase 3: The entry point
- [x] Task 3: route + nav host + splash long-press — done, and **run on a device in Task 5**
- [x] Task 4: **commit the v3 code** — done as `30d69c7`. 7 paths, no `.md`. `HEAD` then measured 174
- [x] Task 5: **Checkpoint B + C on the emulator** — **done 2026-10-07.** Server up → 3 doctors with `DEMO-PRC-0001..0003`; server stopped → rows gone, empty card; server back → rows return. The stopped-server line held first try
- [x] Task 6: **the removal proof** — **done 2026-10-07.** Green at 174 before, `BUILD SUCCESSFUL` at 166 after, zero `SeededData` references, ratchet still 4, branch discarded. **It found that `AppContainer.baseUrl` dies with the screen** — see Task 7
- [ ] Task 7: **the remaining docs** — floor 166 → 174, `AGENTS.md` entry, `README.md` line, `CHANGELOG.md`, **plus adding `AppContainer.baseUrl` to the removal list**
- [ ] Checkpoint D: complete

### Why this order, and not "docs then commit"

`CONSTRAINTS.md` is explicit that a ratchet records **what the committed tree
guarantees**. The floor says 166 because `HEAD` measures 166; the worktree measures
174 only because `SeededDataViewModelTest` is untracked. Raising the floor first
would put the ratchet above the committed count and fail CI on a fresh checkout —
the exact failure the earlier 166 correction was made to avoid.

And the docs come **last** because three of the four remaining doc claims are
claims about observed behaviour: that the screen renders rows, that stopping the
server empties it, and that it deletes in one commit. Writing those before the
observations is how a "Known gaps" bullet turns into a claim that work was done —
`AGENTS.md`'s own maintenance rule forbids it.

## Risks and Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Nobody deletes it, and a demo screen reaches a real patient showing invented doctors as real | High | The exact hazard `AGENTS.md` names. Zero nav-bar presence, a KDoc naming the deletion, and Task 4 proves removal by doing it |
| A hardcoded fallback list sneaks in behind the repository call | High | Task 1's seventh test asserts the rendered set equals the repository's verbatim. The screen would then report "working" with the backend dead |
| The screen reads as proof the whole backend is healthy | Medium | The "what this does not prove" table ships as an on-screen footnote — it only exercises `GET /doctors`, pre-auth, no writes |
| A wrong LAN IP answers from another machine — green screen, wrong server | Medium | The resolved base URL is shown as a subtitle so the host is visible, not assumed (`10.0.2.2` on a physical phone is the recorded trap) |
| `demo12345` is a credential in the APK | Medium | Already true of `README.md` and stdout. Weak password on a demo-only account, never a real database. Stated in the spec's Boundaries, not hidden |
| The `:app` ratchet collides with the archived v2 plan, which targets ≥172 | Low | v2 is unstarted, so there is no conflict to resolve — whichever lands second re-measures. This plan targets 174 from a 166 baseline |

## Open Questions

1. **Skip the composable test?** Recommended yes, and do not start that project
   here — `CONSTRAINTS.md:116` records that no Compose test exists and
   `ExampleInstrumentedTest` is still a template. The ViewModel carries all the
   logic; seven ViewModel tests beat one brittle instrumentation test on a screen
   scheduled for deletion.
2. **Ratchet numbering — resolved.** This plan targets 174 from a measured 166,
   which moves `CONSTRAINTS.md:37` off its documented 165. **Done on 2026-10-07,
   ahead of Task 4:** the stale-165 correction landed at **166**, not 174,
   because a ratchet records what the committed tree guarantees and
   `SeededDataViewModelTest` is still uncommitted — a 174 floor would have failed
   CI on a fresh checkout. The 166 -> 174 step rides along in Task 4's removal
   proof, when this plan's code is committed.

---

# Archived: Plan v2 — Token Lifecycle (Expiry, 401 Handling, Refresh)

> **Status: planned, never started.** Kept for the decision record; its 93
> task-list boxes are untouched in `tasks/todo.md`. Archived here on 2026-10-07
> when the seeded-data screen plan became active — **not because it was
> abandoned.** Verified unstarted rather than assumed:
> `SessionSignal`, `isAuthFailure`, and `Authenticator` return **0 hits** across
> `app/src/main`. It should be picked up after the seeded-data screen, and it is
> the higher-severity of the two: five screens render an unactionable "Please
> sign in again", and `POST /appointments` has no idempotency key.
>
> Status when active: re-auth, not refresh — D2 resolved by the user, so no
> `POST /auth/refresh` route and no `refresh_token` column.
>
> Spec: `tasks/SPEC-token-lifecycle.md`

## Overview

A patient whose token expires mid-session is told to sign in again by five
screens and given no way to act on it — the only sign-in affordance in the app
lives on Profile, which they have to find first. Separately, a stored session
with a corrupt `expiresAt` permanently wedges the Profile screen, because
`Instant.parse` throws before the expiry check runs and the token is never
cleared.

Both share a cause: expiry is tested in exactly one place, and that place is not
on the request path. The fix is one seam — an OkHttp `Authenticator` that clears
the token and raises a domain-owned signal, observed once in the nav host.

## Current State (verified against the filesystem)

| Fact | Evidence |
|---|---|
| `getAccessToken()` never tests expiry | `TokenStore.kt:32` is a bare field read; the only `isExpired` call is `RetrofitAuthRepository.kt:56` |
| `currentSession()` has one caller | `ProfileViewModel.kt:53`; nothing on the request path consults it |
| No screen branches on a 401 | no `status ==` comparison anywhere in `ui/`; every 401 becomes `LoadState.Error(e.message)` |
| `LoadState.Error` carries only a message | `LoadState.kt:18` — no status, no code. The 401 prompt **cannot** ride on `LoadState` without changing 10 ViewModels |
| `toDomain()` parses persisted bytes unguarded | `ApiMappers.kt:52` `Instant.parse(expiresAt)`; `LocalDate.parse` at `:62` is the same hazard |
| `TokenStore.getSession()` guards Gson only | `TokenStore.kt:29` — `runCatching` wraps the parse, not the map, so a malformed timestamp survives it |
| No OkHttp `Authenticator` exists | zero hits in `app/src/main`; `RetrofitClient.kt:46-49` sets only two interceptors |
| `okhttp3.Authenticator` is already on the classpath | `app/build.gradle.kts:58` — no dependency change needed |
| Every 401 already has a readable body | `Routes.kt:334-336` challenge sends `ErrorDto("unauthorized", …)`, so `ApiErrors.kt:65` uses the server's code |
| `SessionSignal` must exist before the client | `AppContainer.kt:41` builds the Retrofit client first; a signal handed to `create()` has to be created above it |
| `ui/` may import `di/` but not `data/` | `check-boundaries.ps1:108` bans `com.example.mediq.data.` in `ui/` — so the signal lives in `domain/` or `di/` |
| Booking state already survives a failed submit | `BookingViewModel.kt:74-80` sets `error` and leaves `reasonForVisit` alone; `BookingSelection` is a process-wide singleton cleared only on success (`:68`) |
| `:app` test floor is 165 | `CONSTRAINTS.md:37`; 165 `@Test` methods present. `:server` floor 50 |
| No mocking framework, no Robolectric | `app/build.gradle.kts:62` — JUnit 4 + `kotlinx-coroutines-test` only |

### Two traps the plan must route around

**`TokenStore` is untestable as written.** Its constructor takes
`android.content.Context` (`:18`) and there is no Robolectric, so the spec's
planned `TokenStoreTest.kt` cannot construct one. The validation logic moves to a
pure function that takes bytes and returns a session or null; `TokenStore` keeps
only the `SharedPreferences` call. Testable, no new dependency.

**A re-issue guard is mandatory, not a nicety.** OkHttp's `Authenticator`
re-sends whatever request it was handed. `MediQApiService` has five
non-idempotent routes — `POST /appointments`, `DELETE /appointments/{id}`,
`PATCH`, `PUT`, `POST /…/reschedule-request`. Without an explicit method check,
a 401 on a booking re-issues the `POST` and the patient is double-booked. D3's
"never auto-retry a write" lands as a method allow-list inside the
`Authenticator`, and as a mutation-checked test.

## Architecture Decisions

**D1 — `Authenticator` plus a domain-owned `SessionSignal`.** An interceptor
sees the response; retrying is the `Authenticator`'s job. The signal is a
`MutableStateFlow<Boolean>` in `domain/`, created in `AppContainer` before the
Retrofit client and handed to both the OkHttp stack and the nav host, so `ui/`
never imports `data/`. Rejected: handling 401 per-ViewModel — six places to get
wrong, and the sixth gets written by someone who hasn't read this file.

**D2 — re-auth, not a refresh token.** Decided. Closes every acceptance
criterion with no server route, no schema column, and no second long-lived
credential in plaintext prefs. The client seam is identical either way, so
refresh can be added later without redoing this work.

**D3 — abort writes, retry nothing.** With D2 = re-auth there is nothing to
refresh with, so the retry path is closed entirely. The write guard is the
point: reads may be re-issued once, writes never. The patient's booking form
already survives a failed submit (`BookingViewModel.kt:74-80`), so this decision
is about the navigation, not the ViewModel.

**D4 — validate at the store, not at each read site.** `TokenStore.getSession()`
returns null for a blob that does not map, and clears the key when one was
present. `currentSession()` then has one contract: null means "no usable
session". Bug 2 dies at the boundary instead of at five read sites.

**The prompt is a nav-host concern, not a `LoadState` concern.**
`LoadState.Error` carries a `String` and nothing else (`LoadState.kt:18`);
threading a 401 through it would change 10 ViewModels and their tests. The
signal already carries it, so the prompt listens to the signal.

**Sign-in lands on Home.** Open Question 2 in the spec, decided as recommended.
Returning to the screen the patient was on re-issues a read immediately, which
is the request path this work exists to make predictable.

## Task List

### Phase 1: The signal and the failure flag

- [ ] Task 1: `ApiFailure.isAuthFailure`
- [ ] Task 2: `SessionSignal` in `domain/`
- [ ] Task 3: Pure session validation + `TokenStore` validate-on-read

### Checkpoint A: Signal and validation

- [ ] `:app:compileDebugKotlin` green
- [ ] `:app:testDebugUnitTest` green, above the 165 floor
- [ ] `check-boundaries.ps1` reports `domain-imports-android` = 0
- [ ] Review with human before proceeding

### Phase 2: The request path

- [ ] Task 4: `SessionAuthenticator` — clear, signal once, retry reads only
- [ ] Task 5: `AppContainer` owns the signal; hand it to the OkHttp stack
- [ ] Task 6: `currentSession()` contract simplification

### Checkpoint B: Request path

- [ ] Five concurrent 401s produce exactly one signal — proven by test
- [ ] A 401 on `POST /appointments` issues zero additional requests
- [ ] `:app:testDebugUnitTest` green

### Phase 3: The patient-facing prompt

- [ ] Task 7: `MediQNavHost` observes the signal, one-tap sign-in
- [ ] Task 8: Clear the signal on sign-out
- [ ] Task 9: `smoke.ps1` — a booking across an expiry produces exactly one `POST`

### Checkpoint C: Prompt

- [ ] `MEDIQ_TOKEN_TTL_MINUTES=2` verified on the emulator — one prompt per screen
- [ ] `:server:test` green at its floor of 50

### Phase 4: Documentation

- [ ] Task 10: `AGENTS.md`, `CHANGELOG.md`, `README.md`, `CONSTRAINTS.md`

### Checkpoint D: Complete

- [ ] `:app:testDebugUnitTest` green at ≥172; `:server:test` green at 50
- [ ] `ui-imports-data` = 0 and `domain-imports-android` = 0
- [ ] `README.md:146-147` and the `AGENTS.md` known-gap bullet updated in the same commit

## Risks and Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| The `Authenticator` re-issues a write and double-books a patient | High | Method allow-list, GET/HEAD only. Mutation-checked: enable write retry and the suite must fail. |
| `TokenStore` takes a `Context`, so its new validation is untestable | High | Validation is a pure function over bytes; `TokenStore` keeps only the prefs call. No new test dependency. |
| The signal leaks the last user's state across sign-out | High | Cleared in `signOut()` alongside `TokenStore.clearSession()`. One test. |
| Navigating to sign-in pops the in-progress booking form | Medium | `popUpTo` keeps the booking entry rather than clearing the stack. `BookingSelection` is process-wide and survives regardless. |
| `toDomain()` is still the unguarded parse point | Medium | Fixing `getSession()` to validate before mapping is what makes the unguarded parse unreachable from persisted bytes. Documented, not left as a trap. |
| `getSession()` clearing on a bad read means a transient write race signs the patient out | Low | `apply()` is async; the risk is one extra sign-in. Noted, not mitigated. |
| `:server` floor of 50 does not move | Low | D2 = re-auth adds no route and no test. The floor stays; that is a real outcome, not a skipped task. |

## Open Questions

1. **Does the expired-session prompt replace the screen or sit above it?**
   Decided at Task 7: a modal over the current content. It satisfies acceptance
   criterion 1 (one tap, from that screen) without a seventh sign-in entry point.
2. **Sign-in lands on Home.** Decided. See Architecture Decisions.
3. **`refresh` later.** If it is ever added, the seam does not change — only the
   `Authenticator` gains a body. Worth a note in `AGENTS.md` Auth so the next
   session knows the seam was built for it.

---

# Archived: Plan v1 — Registration and Appointment Details

Complete. Commits `c9df562` through `24d9ef2`. Kept for the decision record;
its task list is checked off in `tasks/todo.md`.

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

> **As of before this plan was executed.** This is the record of what was true
> when the plan was written — every unchecked box below is preserved on purpose.
> For the state of the tree today, read `README.md` "Current condition" and
> `.mdfiles/AGENTS.md`. Two items in this table were the point of the plan and
> are no longer true: the registration wizard now reaches the backend, and
> `AppointmentDetailsScreen` loads, cancels, and reschedules.

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
