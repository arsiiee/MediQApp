# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Fixed
- **Registration could not create an account from the app.** All four
  `ui/feature/auth/register/` screens held their values in local
  `remember { mutableStateOf(...) }` and navigated between each other on a button
  press. Nothing called `AuthRepository`, so the full name and phone number were
  discarded on Continue and **no account could be created through the UI at all**,
  while `:app:compileDebugKotlin` stayed green — not calling the API is not a
  compile error, and no test covered it. The server-side OTP and registration
  flow was already complete and tested.
  - `RegisterViewModel` now owns all three steps and all three calls
    (`requestOtp`, `verifyOtp`, `register`) in one `RegisterUiState`, and the
    four routes are nested in a `navigation()` graph so they share it. Step 3
    cannot succeed without step 2, so a per-screen ViewModel would have had to
    pass values forward by hand.
  - Each screen now advances **only on server success**, not on a tap. A wrong
    OTP keeps the typed code and shows the server's sentence, including the
    lockout's own message rather than a generic failure.
  - **The details step gained a date-of-birth field**, which is not optional:
    `AuthService` answers a null one with 400 "A date of birth is required."
    *after* its registration-id, username, email, and mobile checks, and
    `RegisterRequest.dateOfBirth` is nullable, so the compiler could not catch the
    omission. Confirmed against a running server. Collected with a date picker
    rather than a text field, because the server only accepts `YYYY-MM-DD`.
  - `registrationId` lives in the ViewModel and nowhere else. `otpVerified` is the
    UI's view of it, which doubles as the guard for the credentials step: process
    death between steps 2 and 3 loses the id, and the user is sent back rather
    than submitting a request the server can only refuse.
  - **The success screen greeted patients as "Jesse"** — a hardcoded personal name
    that no real patient shares. It now comes from the returned `AuthSession`.
  - That screen also navigated to `Screen.SignIn` after registering.
    `RetrofitAuthRepository.register` had already stored the session via
    `TokenStore`, so the user was signed in and was being asked to authenticate
    again. It now goes straight to Home and pops the auth stack.
  - `RegisterDetailsScreen`'s phone field gained `KeyboardType.Phone` and a
    `+63917…` placeholder; the old `+63 917 555 0142` placeholder taught a spaced
    format `AuthService.normalizeMobile` rejects. `RegisterViewModel` also strips
    separators before sending, mirroring the server's `^\+?[0-9]{10,15}$`.
  - Guard: `RegisterViewModelTest`, 22 tests, all against a fake
    `AuthRepository`. Each asserts **whether the repository was reached**, not just
    the resulting state — a state-only assertion would still pass against a
    ViewModel that ignored its inputs.
- **`AppointmentDetailsScreen` ignored the `appointmentId` from its route.** The
  composable took the id and rendered a fixed "No appointment selected"
  `EmptyState` for every appointment, so tapping a real appointment led to a
  screen claiming none existed. The signature asked for the value and the body
  discarded it, which neither the compiler nor any existing test could see.
  `AppointmentRepository.getAppointment` and the Retrofit call already existed and
  were unwired from any UI.
  - `AppointmentDetailsViewModel` loads by that id and exposes
    `LoadState<Appointment>`; the screen `when`s over all three cases and renders
    doctor, clinic-zone date and time, location, fee, status, and reason for
    visit.
  - A null or blank id renders a clear empty state and **makes no network call** —
    the assertion that distinguishes "loaded the appointment the user tapped" from
    "loaded something, somehow".
  - `BackendNotConnectedException` maps to `LoadState.Error` here, **not** to
    `Success(emptyList())` as on the list screens. That mapping is for reads where
    "no results" and "no backend" look the same; a single-entity success carrying
    nothing would tell a patient with a real appointment that it does not exist.
  - Guard: `AppointmentDetailsViewModelTest`, 7 tests against a fake
    `AppointmentRepository` that records the ids it was asked for.
- **Sign-in rejected correct credentials.** `MessagesScreen` aside, the cause was
  that no `OutlinedTextField` in the app set `keyboardOptions`, so Compose used
  `KeyboardOptions.Default` — `autoCorrect = true`, no `KeyboardType`. Android
  kept autocorrect and suggestions live on the masked fields and rewrote the
  characters as they were typed, so `demo12345` reached the server as a different
  string. The server answered with `"Those details don't match an account."` —
  the same message as a genuinely wrong password, so nothing on screen
  distinguished a keyboard typo from a bad credential. The build was green and
  the server was healthy the whole time; the credentials really were correct.
  `PasswordVisualTransformation` only masks what is *displayed* and says nothing
  about what the keyboard may do to the value.
  - Client: `keyboardOptions` with `KeyboardType.Password` (password, confirm
    password) and `KeyboardType.NumberPassword` (OTP), plus `autoCorrect = false`
    and `KeyboardCapitalization.None` on every identifier field.
  - Server: `AuthService.normalizeUsername` trims and lowercases at `register`
    *and* `signIn`, so the two cannot disagree. Also closes the case where
    `Case` and `case` registered as two accounts indistinguishable at sign-in.
  - Guard: `check-boundaries.ps1` Rule 4, keyed on `PasswordVisualTransformation`
    so a new secret input is covered without editing the script. Both rules were
    verified to fail on a deliberately reverted field before being accepted.
- **The sign-in message could not distinguish a typo from a wrong password.**
  Server-side normalization above means a stray space no longer produces a dead
  end, but the two cases remain deliberately indistinguishable to a caller for
  anti-enumeration reasons.

- **A patient could not cancel an appointment from the app.**
  `AppointmentRepository.cancel` existed, `DELETE /appointments/{id}` existed, and
  `AppointmentStatus.isActionable` already encoded which statuses permit a change —
  but no screen called any of them, so the actions were unreachable and
  `isActionable` gated nothing. The repository method being present is not the
  same as a patient having a button.
  - Cancel and Reschedule render only when
    `LoadState.Success.data.status.isActionable`, so they are absent for
    `COMPLETED`, `CANCELLED`, `DECLINED`, and `UNKNOWN`.
  - A successful cancel **reloads** the appointment. Cancelling deletes the
    `slot_claims` row to free the slot and only the server can change the status;
    without the reload the badge still read "Confirmed" and the screen kept
    offering a second cancel the server would refuse.
  - A refused or undeliverable change shows the server's sentence and leaves the
    displayed appointment untouched — no refresh on failure, because the server
    refused and what is on screen is still the truth. An unreachable backend says
    explicitly that the change **was not made**, since a generic "couldn't reach"
    leaves a patient unsure whether they still hold the booking.
  - Guard: `AppointmentDetailsViewModelTest`, 10 new tests. The fake repository
    records the cancelled ids and the `RescheduleRequest`s it was handed, so a
    cancel that reported success without reaching the repository cannot pass.

### Known gaps
- None outstanding from the registration work. `RegisterDetailsScreen`'s phone
  field gap listed here is fixed above, along with the unwired wizard.
- **Reschedule has no slot picker.** The endpoint, the repository call, and the
  `requestedSlotId` are all correct, but the screen collects that id as a typed
  string — and nobody can guess a slot id. It needs the date-and-slot picker
  `DoctorDetailsScreen` already uses. `AppointmentDetailsViewModel.requestReschedule`
  takes a `String?` specifically so swapping the picker does not change the
  ViewModel or its tests.

### Changed
- **Step completion is now a consumable event, not a sticky flag.** The wizard's
  screens `LaunchedEffect` on the flag saying their step succeeded
  (`otpRequested`, `otpVerified`, `registeredName`). A flag stays true for the
  life of the ViewModel, so the effect fired again every time the user navigated
  *back* to that step — system back from the OTP screen would bounce them
  straight forward to it, trapping them in the wizard with no way out. Replaced
  with `pendingStep: RegisterStep?`, which the screen clears via `onStepHandled()`
  once it has navigated. `otpVerified` stays as real state, because the
  credentials step reads it as a guard.
- **`NavBackStackEntry.parent` is not public in navigation 2.10.1.** It is the
  obvious way to get a nested graph's back stack entry — which is how a
  graph-scoped ViewModel is shared between its destinations — and it fails to
  compile. The register graph therefore carries an explicit route and its screens
  use `navController.getBackStackEntry(Screen.RegisterFlow.route)`. Worth knowing
  before anyone copies that snippet from a tutorial written against navigation
  2.7.

### Added
- **A ViewModel test harness for `:app`.** ViewModel logic could not be unit
  tested at all before this: every ViewModel calls `viewModelScope`, which posts
  to `Dispatchers.Main`, and that dispatcher throws on the JVM unless
  `kotlinx-coroutines-test` swaps it. Added that dependency plus hand-written
  fakes (`FakeAuthRepository`, `FakeAppointmentRepository`, `TestFixtures`) and a
  `MainDispatcherRule` — no mocking framework, matching the hand-built-fake
  convention the `data/api` tests already set. A fake that records what it was
  asked for says more about a state machine than a verify-count does.
  The rule installs an `UnconfinedTestDispatcher` so a launch runs to completion
  eagerly; without it a test that never awaits its coroutine passes while proving
  nothing.
  - `ExampleUnitTest` was an untouched Android Studio template asserting
    `2 + 2 == 4` and was deleted in favour of real tests. `:app` went from 23
    tests, all in `data/api/`, to 51.
  - The harness was verified by deliberately reverting the phone sanitisation and
    confirming the test failed, then restoring it — a harness that cannot fail is
    not a harness.
- **`check-boundaries.ps1` Rule 4** — `secret-field-no-keyboard-options` and
  `secret-field-not-password-keytype`. Both ratcheted at 0; the first was **5**
  before the fix above. `CONSTRAINTS.md` records this as the third instance of
  the repo's recurring failure mode: a rule stated in prose, never checked, and
  invisible to the compiler and the full test suite.

- **README at the repo root**: A user-facing description of what MediQ is, what
  works today, and an explicit list of what does not — including that
  registration cannot create an account from the app, that `AppointmentDetails`
  and Messages are stubs, and the three security items that must close before
  any real patient. It leads with the fact that this is unfinished, because the
  app has 15 screens and a working backend and the gap between those two numbers
  is the first thing a new reader needs to know. `.mdfiles/README.md` is now an
  index of that folder rather than a second copy of the README.
- **Contrast enforcement**: `check-contrast.ps1`, which parses the real tokens out
  of `Color.kt` and the real status chip pairs out of `AppointmentsScreen.kt`,
  measures them with the WCAG 2.1 relative-luminance formula, and fails on any
  pair below its threshold or any hardcoded page background. It keeps no copy of
  the hex values, because a checker holding a stale list passes clean while the
  token is reverted to a failing one — which is exactly what happened to
  `MediQTextSecondary` before this existed.
- **Mode-aware colour roles**: `MediQExtendedColors` (accent, secondary text,
  outline) provided through `LocalMediQColors`. Material's `ColorScheme` has no
  slot for an accent drawn on the themed background, and `primary` must stay the
  dark brand green in both schemes — filled buttons take their label from
  `onPrimary`, so a light `primary` in dark mode puts white-on-pale-green labels
  on every button at 1.79:1.
- **Quality bar**: Added `CONSTRAINTS.md` and `check-boundaries.ps1`, which mechanically enforce the architecture rules `AGENTS.md` previously stated in prose only. The rules are: `domain/` imports no Android or Compose, `ui/` never imports `data/`, and every `api.` call sits inside `call { }`. All three had already been violated or ignored with fully green builds.
- **API test suite**: `ApiErrorsTest` (11 tests) covering error-body parsing, malformed bodies, and network failures. `:app` previously had no runnable unit tests.
- **Network Layer**: Added Retrofit, OkHttp, and Gson dependencies to the Android app to enable HTTP communication with the Ktor server.
- **API Models**: Created data transfer objects (DTOs) and mappers (`ApiDtos.kt`, `ApiMappers.kt`) to translate JSON data from the server into domain models safely.
- **Token Storage**: Implemented `TokenStore.kt` using `SharedPreferences` to persist user authentication sessions across app restarts.
- **Application Class**: Added `MediQApp.kt` to initialize the app's dependency graph (`AppContainer`) at startup.
- **Live Repositories**: Created real server-backed repositories (`RetrofitAuthRepository`, `RetrofitDoctorRepository`, `RetrofitAppointmentRepository`, `RetrofitNotificationRepository`, `RetrofitProfileRepository`) to replace the static `EmptyRepositories`.

### Changed
- **Every screen now follows the theme, so dark mode is legible**: `AppointmentsScreen`,
  `HomeScreen`, `DoctorsScreen`, `NotificationsScreen`, `DoctorDetailsScreen`,
  `BookingFlowScreen`, `AppointmentDetailsScreen`, the four registration screens,
  `SignInScreen`, `EmptyState` and `MediQBottomBar` all hardcoded
  `Color.White` backgrounds, `Color.Black`/`Color.Gray` text, or raw `Color(0x…)`
  status chips while `Theme.kt` follows `isSystemInDarkTheme()`. In dark mode that
  text rendered light-on-light and was invisible. All of them now read
  `MaterialTheme.colorScheme.*` or `LocalMediQColors.current.*`. Three measured
  fixes came out of it: `MediQTextSecondary` `#757575` → `#5F5F5F` (3.95:1 →
  6.39:1 on white), the pending-appointment chip label `#F9A825` → `#8F5000`
  (1.85:1 → 6.28:1 on its `#FFF8E1` container), and the notification timestamp
  off `Color.LightGray`, which was 1.50:1 and effectively invisible at 12sp.
- **Profile Screen follows the Material theme**: `ProfileScreen` hardcoded `Color.White` backgrounds, `Color.Black` text, and raw `Color(0x…)` values, while `Theme.kt` follows `isSystemInDarkTheme()`. In dark mode its text rendered light-on-light and was effectively invisible. Surfaces, labels, the role badge, the avatar, and the sign-out button now use `colorScheme.background` / `surfaceVariant` / `primaryContainer` / `error` and their `on*` pairs. This was the first of the four screens converted; the rest are in the entry above.
- **Dev Base URL**: `RetrofitClient.BASE_URL` switched from `http://10.0.2.2:8099/` to this PC's LAN address, `http://192.168.100.14:8099/`. `10.0.2.2` is an alias that exists only inside the emulator's virtual network, so on a physical phone it is unroutable, every call times out, and the app reports "Couldn't reach the clinic" while the server is answering `127.0.0.1` perfectly well — which is what made sign-in fail on 2026-10-06 with the server up and healthy. The LAN address is the one value that works on both targets. `network_security_config.xml` was updated in step to permit cleartext to it, and both must change together when the DHCP address moves.
- **Sign-In Flow**: Wired `SignInScreen` to `SignInViewModel`. The app now securely verifies credentials against the server instead of navigating blindly to the home screen.
- **Doctor Details**: Wired `DoctorDetailsScreen` to `DoctorDetailsViewModel` to fetch and display live doctor profiles, clinic hours, and available booking time slots.
- **Appointments & Notifications**: Wired `AppointmentsScreen` and `NotificationsScreen` to their respective ViewModels to pull real user data from the API instead of showing static empty states.
- **Profile Screen**: Wired `ProfileScreen` to display live user details and integrated a fully working "Sign Out" flow that clears the session.
- **App Configuration**: Updated `AndroidManifest.xml` to include the `INTERNET` permission and allow local cleartext traffic (`10.0.2.2`, `127.0.0.1`, and the LAN host) for development testing.
- **Architecture boundaries enforced**: Added `CONSTRAINTS.md` and `check-boundaries.ps1`. The rules `AGENTS.md` stated in prose — `domain/` imports no Android, `ui/` never imports `data/`, every `api.` call sits inside `call { }` — are now checked by a command rather than remembered. The first run found 7 violations of the second rule, which were fixed rather than ratcheted in.
- **`BackendNotConnectedException` moved to `domain/model/`**: Seven ViewModels imported it from `data/repository/`, violating "Screens and ViewModels must not reference `data/` directly". The compiler cannot catch this — the import compiles — so the type now lives with the other things ViewModels catch.
- **Project Rules**: Updated `AGENTS.md` to reflect the new networking architecture, note the token storage decision, and clear out resolved "Known gaps".

### Fixed
- **Correct credentials could be rejected as a wrong password**: usernames were
  stored and looked up raw, so a phone keyboard that autocorrected or
  autocompleted — `Demo_patient ` instead of `demo_patient` — produced the same
  opaque "Those details don't match an account." as a genuinely wrong password,
  leaving a patient with no way to tell a typo from a bad password. `register`
  now stores a trimmed, lowercased username and `signIn` looks up the same form,
  so the two can never disagree. The uniqueness check uses the same normalized
  value, so `Case_Collide` and `case_collide` can no longer become two accounts
  that are indistinguishable at sign-in. An empty username is rejected outright
  rather than appearing to be tried. Seven tests in `AuthServiceTest` pin this,
  including one that normalization must not weaken the password check.
- **Dark-mode text was invisible on four screens**: `AppointmentsScreen`,
  `HomeScreen`, `DoctorsScreen` and `NotificationsScreen` set `Color.White`
  page backgrounds while `Theme.kt` follows `isSystemInDarkTheme()`, so in dark
  mode theme-coloured text landed on a hardcoded white surface at 1.00:1. The
  conversion to `colorScheme` and `LocalMediQColors` is in "Changed" above; what
  is fixed here is that the defect can no longer come back silently, because
  `check-contrast.ps1` now fails on any hardcoded page background.
- **Four colour tokens failed WCAG AA and three measured fixes followed**:
  `MediQSuccess` `#388E3C` → `#2E7D32` (4.12:1 → 5.13:1 on white),
  `MediQWarning` `#FBC02D` → `#B25E00` (1.66:1 → 4.67:1), and the dark-scheme
  `error` red, which measured 1.96:1 on the dark raised surface and now lightens
  to `#FF6B6B` at 5.17:1. Each token now carries its measured ratio in a comment,
  and `check-contrast.ps1` re-measures rather than comparing against a stored
  list.
- **Status bar icons were dark on a dark bar in dark mode**:
  `isAppearanceLightStatusBars` was keyed off `darkTheme`, but `statusBarColor` is
  `colorScheme.primary`, which stays the dark brand green in both schemes. Dark
  mode therefore drew dark icons on a dark green bar at 3.17:1. The flag is now
  unconditionally `false`, which matches the bar that is actually drawn.
- **A wrong claim in `AGENTS.md`, corrected with a test**: the "Known gaps" list asserted that the server's `ignoreUnknownKeys = false` makes every *added* response field a breaking change for installed builds. It does not — that setting governs how the **server** parses inbound request bodies, while the client parses responses with Gson, whose default ignores unknown fields. `GsonLeniencyTest` (3 tests) pins the actual behaviour. The real risk is the mirror one and is now stated instead: Gson cannot tell "the server never sent this" from "the field was renamed", so a rename arrives as `null` with no error and a non-null Kotlin DTO field reads as null at runtime. Renames need a contract test.
- **Unrecognised wire values no longer resolve to a confident wrong answer**: `ApiMappers` fell back to the first enum entry, so an appointment status this build did not recognise became `PENDING_CONFIRMATION` — a cancelled appointment shown to a patient as awaiting confirmation — and a slot status fell back to `BLOCKED`, having previously been `AVAILABLE`, i.e. bookable on a guess. Nothing was logged, so no trail pointed at the rename that caused it. `AppointmentStatus` and `SlotStatus` now carry an explicit `UNKNOWN` member that is neither actionable nor upcoming; the status chip renders grey rather than borrow a status colour. `UserRole` and `Specialty` deliberately keep positional fallbacks, because `:server` compiles `domain/model/` and parses both from untrusted input — `UnknownWireValueTest` (8 tests) pins the distinction.
- **Patient-facing error messages**: The client never read the server's error contract. Every non-2xx response carries `ErrorDto(error, message)` with copy written for a patient, but nothing consumed it — ViewModels surfaced Retrofit's raw `HttpException.message` instead, so a wrong password displayed `"HTTP 401"` rather than "Those details don't match an account." `BookingViewModel` was worse: it discarded the 409 `slot_taken` message and told the patient to try again on a booking that could never succeed. Added `ApiFailure` (`domain/model/ApiFailure.kt`) and `data/api/ApiErrors.kt`, whose `call { }` wrapper reads the error body; all 5 Retrofit repositories and 8 ViewModels now route through it.
- **HTTP failures disguised as empty states**: The `ui-imports-data` review turned up a related defect — list reads caught `IOException` (and generic `Exception` in ViewModels), which cannot distinguish an unreachable server from an HTTP failure. A 500 or 409 on a list read rendered as "you have no appointments". Repositories now branch on `ApiFailure.isNetworkFailure`, so only a genuinely unreachable server becomes an empty state.
- **Booking State Bug**: Fixed an issue in `BookingViewModel` where the selected slot (`BookingSelection`) was not being cleared after a successful booking. This prevents users from accidentally re-submitting a stale booking.
