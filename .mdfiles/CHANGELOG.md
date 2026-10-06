# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Added
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
- **Project Rules**: Updated `AGENTS.md` to reflect the new networking architecture, note the token storage decision, and clear out resolved "Known gaps".

### Fixed
- **Booking State Bug**: Fixed an issue in `BookingViewModel` where the selected slot (`BookingSelection`) was not being cleared after a successful booking. This prevents users from accidentally re-submitting a stale booking.
