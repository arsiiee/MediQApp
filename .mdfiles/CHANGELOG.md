# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Added
- **Quality bar**: Added `CONSTRAINTS.md` and `check-boundaries.ps1`, which mechanically enforce the architecture rules `AGENTS.md` previously stated in prose only. The rules are: `domain/` imports no Android or Compose, `ui/` never imports `data/`, and every `api.` call sits inside `call { }`. All three had already been violated or ignored with fully green builds.
- **API test suite**: `ApiErrorsTest` (11 tests) covering error-body parsing, malformed bodies, and network failures. `:app` previously had no runnable unit tests.
- **Network Layer**: Added Retrofit, OkHttp, and Gson dependencies to the Android app to enable HTTP communication with the Ktor server.
- **API Models**: Created data transfer objects (DTOs) and mappers (`ApiDtos.kt`, `ApiMappers.kt`) to translate JSON data from the server into domain models safely.
- **Token Storage**: Implemented `TokenStore.kt` using `SharedPreferences` to persist user authentication sessions across app restarts.
- **Application Class**: Added `MediQApp.kt` to initialize the app's dependency graph (`AppContainer`) at startup.
- **Live Repositories**: Created real server-backed repositories (`RetrofitAuthRepository`, `RetrofitDoctorRepository`, `RetrofitAppointmentRepository`, `RetrofitNotificationRepository`, `RetrofitProfileRepository`) to replace the static `EmptyRepositories`.

### Changed
- **Dev Base URL**: `RetrofitClient.BASE_URL` switched from `http://127.0.0.1:8099/` to `http://10.0.2.2:8099/`. The old value assumed a physical device over `adb reverse`; the project now has an emulator AVD, and `10.0.2.2` reaches the host with no extra command. Switch back to `127.0.0.1` (plus `adb reverse tcp:8099 tcp:8099`) to test on a physical device.
- **Sign-In Flow**: Wired `SignInScreen` to `SignInViewModel`. The app now securely verifies credentials against the server instead of navigating blindly to the home screen.
- **Doctor Details**: Wired `DoctorDetailsScreen` to `DoctorDetailsViewModel` to fetch and display live doctor profiles, clinic hours, and available booking time slots.
- **Appointments & Notifications**: Wired `AppointmentsScreen` and `NotificationsScreen` to their respective ViewModels to pull real user data from the API instead of showing static empty states.
- **Profile Screen**: Wired `ProfileScreen` to display live user details and integrated a fully working "Sign Out" flow that clears the session.
- **App Configuration**: Updated `AndroidManifest.xml` to include the `INTERNET` permission and allow local cleartext traffic (`10.0.2.2`, `127.0.0.1`) for development testing.
- **Architecture boundaries enforced**: Added `CONSTRAINTS.md` and `check-boundaries.ps1`. The rules `AGENTS.md` stated in prose — `domain/` imports no Android, `ui/` never imports `data/`, every `api.` call sits inside `call { }` — are now checked by a command rather than remembered. The first run found 7 violations of the second rule, which were fixed rather than ratcheted in.
- **`BackendNotConnectedException` moved to `domain/model/`**: Seven ViewModels imported it from `data/repository/`, violating "Screens and ViewModels must not reference `data/` directly". The compiler cannot catch this — the import compiles — so the type now lives with the other things ViewModels catch.
- **Project Rules**: Updated `AGENTS.md` to reflect the new networking architecture, note the token storage decision, and clear out resolved "Known gaps".

### Fixed
- **Patient-facing error messages**: The client never read the server's error contract. Every non-2xx response carries `ErrorDto(error, message)` with copy written for a patient, but nothing consumed it — ViewModels surfaced Retrofit's raw `HttpException.message` instead, so a wrong password displayed `"HTTP 401"` rather than "Those details don't match an account." `BookingViewModel` was worse: it discarded the 409 `slot_taken` message and told the patient to try again on a booking that could never succeed. Added `ApiFailure` (`domain/model/ApiFailure.kt`) and `data/api/ApiErrors.kt`, whose `call { }` wrapper reads the error body; all 5 Retrofit repositories and 8 ViewModels now route through it.
- **HTTP failures disguised as empty states**: The `ui-imports-data` review turned up a related defect — list reads caught `IOException` (and generic `Exception` in ViewModels), which cannot distinguish an unreachable server from an HTTP failure. A 500 or 409 on a list read rendered as "you have no appointments". Repositories now branch on `ApiFailure.isNetworkFailure`, so only a genuinely unreachable server becomes an empty state.
- **Booking State Bug**: Fixed an issue in `BookingViewModel` where the selected slot (`BookingSelection`) was not being cleared after a successful booking. This prevents users from accidentally re-submitting a stale booking.
