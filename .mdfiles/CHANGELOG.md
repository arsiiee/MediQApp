# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Added
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
