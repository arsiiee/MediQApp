# MediQ app — screen captures

Captured on `mediq_api36` (1080x2400) against `:server:run`, H2 in-memory,
`MEDIQ_SEED_DEMO=true`. Signed in as `demo_patient` unless stated.

Regenerate with: run the server with H2 + seed, `:app:installDebug`, then
`adb exec-out screencap -p > <name>.png` (route it through `cmd /c` —
PowerShell's `>` corrupts the PNG).

| # | File | Screen |
| --- | --- | --- |
| 01 | `01-splash.png` | Splash / landing, signed out |
| 02 | `02-seeded-data.png` | Seeded data (long-press the wordmark) — temporary backend check |
| 03 | `03-sign-in.png` | Sign in |
| 04 | `04-home.png` | Home, first sign-in, nothing booked |
| 05 | `05-notifications-empty.png` | Notifications, empty |
| 06 | `06-doctors.png` | Doctors directory |
| 07 | `07-doctor-details.png` | Doctor details |
| 08 | `08-doctor-details-slots.png` | Doctor details, date chosen, slots listed |
| 09 | `09-doctor-details-slot-selected.png` | Doctor details, slot chosen, continue button |
| 10 | `10-confirm-booking.png` | Confirm booking |
| 11 | `11-booking-success.png` | Booking success |
| 12 | `12-appointments.png` | Appointments (upcoming) |
| 13 | `13-appointment-details.png` | Appointment details, action buttons |
| 14 | `14-messages.png` | Messages (placeholder) |
| 15 | `15-profile.png` | Profile |
| 16 | `16-edit-profile.png` | Edit profile (new) |
| 17 | `17-edit-profile-sex-menu.png` | Edit profile, sex menu open |
| 18 | `18-profile-after-save.png` | Profile after a save — sex updated in place |
| 19 | `19-home-cold-start.png` | Home after force-stop + relaunch — session and booking persisted |
| 20 | `20-sign-out-dialog.png` | Sign-out confirmation |
| 22 | `22-register-details.png` | Register 1/4 — details |
| 23 | `23-dob-picker.png` | Register — date-of-birth picker |
| 24 | `24-register-details-filled.png` | Register 1/4 — filled |
| 25 | `25-register-otp.png` | Register 2/4 — OTP, dev code shown |
| 26 | `26-register-otp-filled.png` | Register 2/4 — code entered |
| 27 | `27-register-credentials.png` | Register 3/4 — credentials |
| 28 | `28-register-credentials-filled.png` | Register 3/4 — filled |
| 29 | `29-register-success.png` | Register 4/4 — success |
| 30 | `30-notifications-with-content.png` | Notifications with a booking request |
| 31 | `31-reschedule-sheet.png` | Reschedule, opened |
| 32 | `32-reschedule-picker.png` | Reschedule, slot picker |
| 33 | `33-appointment-cancelled.png` | Appointment details after cancelling — actions withdrawn |

No `21`: signing out returns to 01.

## What these confirm

- The new edit-profile path works end to end: 16 → 17 → 18, and 19 shows the
  saved value surviving a cold start (so it reached the server, not just the cache).
- Cancelling withdraws every action, matching the "status permits a change" rule.
- Register does collect a date of birth, via a picker.

Not a screen list: the Messages tab and the notifications empty state are
deliberate placeholders.
