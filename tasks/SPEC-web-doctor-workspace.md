# SPEC — `web-doctor-workspace` (module 3 of the mediq-web initiative)

Status: **Drafting specification**.
Prerequisites: module 1 (`SPEC-web-staff-auth.md`, complete), module 2 (`SPEC-web-admin-accounts.md`, complete).

## Objective

Connect the Doctor area (`/doctor/*`) in `mediqweb-fe` to the real Ktor `:server`.
A signed-in doctor views their own appointments (dashboard, open queue, visit history),
sees their patients and consult history, manages their weekly consulting schedule,
and records consultation visit outcomes (`completed` / `no-show`).

---

## 1. Reconnaissance: What the Doctor Screens Ask For

The Doctor area consists of 6 primary pages plus a layout shell:
1. **`DoctorDashboard`** (`/doctor/dashboard`):
   - Working list of appointments for today
   - Counts: today's open appointments, awaiting confirmation, completed this week
   - Hero for next appointment
   - Today's schedule summary
2. **`DoctorAppointments`** (`/doctor/appointments`):
   - Working queue of `booked` and `confirmed` appointments soonest first
   - Actions: `recordOutcome(id, 'completed' | 'no-show')` on confirmed visits
3. **`DoctorHistory`** (`/doctor/history`):
   - Past visits: `completed` and `no-show` (excludes `cancelled`)
   - Filter by outcome and search by patient name
4. **`DoctorPatients`** (`/doctor/patients`):
   - List of patients who have booked or completed an appointment with this doctor
   - Summary: last visit date, next visit date, total visit count
5. **`DoctorPatientDetails`** (`/doctor/patients/:id`):
   - Contact info (phone, email, date of birth, address)
   - Relationship check: only accessible if patient has appointments with this doctor
   - History of visits with this doctor
6. **`DoctorSchedule`** (`/doctor/schedule`):
   - Weekly repeating schedule (days of week 0–6, multiple sessions per day)
   - Save weekly schedule (`saveSchedule`)
   - Dated availability overrides / exceptions (`saveDatedAvailability`, `clearDatedAvailability`)
7. **`DoctorProfile`** (`/doctor/profile`):
   - Doctor's specialty, PRC license number, joined date
   - Editable fields: full name, email, bio (`updateProfile`)

---

## 2. Server Gap Analysis

Comparing what the screens need with the server schema (`schema.sql`) and routes (`Routes.kt`):

| Screen / Feature | What Frontend Expects | What Server Currently Has | Gap / Solution |
|---|---|---|---|
| **Doctor Scoping** | Doctor only sees their own appointments and patients | `users.doctor_id` links a staff account (`role = 'doctor'`) to a `doctors` table row. `AppointmentStore.list` currently returns clinic-wide appointments for any staff. | Need route `/doctor/appointments` or role-aware filtering in `AppointmentStore.list` scoped by `caller.doctorId`. |
| **Visit Outcome** | Doctor records `completed` or `no-show` on confirmed visits | Server has `completed` status; does not have `no_show` in `AppointmentStatus`. No endpoint to record consultation outcome. | Add `NO_SHOW("no_show", "No show")` to `AppointmentStatus` on server, add `POST /doctor/appointments/{id}/outcome` (or `PATCH /appointments/{id}/status`). |
| **Patients List** | Patients who have a relationship with this doctor + summaries | Patients exist in `users (role = 'patient')`. Appointments join `user_id` and `doctor_id`. | `GET /doctor/patients` returning the cohort of patients associated with the authenticated doctor. |
| **Weekly Schedule** | Repeating weekly hours (multiple session blocks per day) | `clinic_hours` table: `(doctor_id, day_of_week INT 1..7, opens_at TIME, closes_at TIME)`. Supports multiple rows per day. | Add `GET /doctor/schedule` and `PUT /doctor/schedule` validating and writing `clinic_hours` in a transaction. |
| **Dated Overrides** | One-off calendar date exceptions (replacing week for that date) | `slots` exists for concrete windows, but there is no exceptions table for dated closures or custom hours. | Evaluate adding `doctor_schedule_exceptions` table or handling exceptions in gateway. |
| **Doctor Profile** | Full name, email, specialty, PRC license, bio | `users` has name/email; `doctors` has specialty, license, bio. `GET /profile` + `GET /doctors/{id}`. | `GET /doctor/profile` combining `users` and `doctors` rows, and `PUT /doctor/profile` updating user name/email and doctor bio. |

---

## 3. Architecture & Gateway Design

Following the established pattern in Module 1 (`HttpAuthGateway`) and Module 2 (`HttpAdminAccountsGateway`):

1. **`DoctorWorkspaceGateway`** (abstract class in `src/app/doctor/services/doctor-workspace.gateway.ts`):
   - `getWorkspaceData(): Promise<DoctorWorkspaceSnapshot>`
   - `recordOutcome(appointmentId: string, outcome: VisitOutcome): Promise<void>`
   - `saveSchedule(days: readonly ScheduleDay[]): Promise<void>`
   - `saveDatedAvailability?(entry: DatedAvailability): Promise<void>`
   - `updateProfile(draft: ProfileDraft): Promise<DoctorProfile>`

2. **`DoctorSession` Integration**:
   - Optionally inject `DoctorWorkspaceGateway`.
   - On construction, if gateway is present, invoke `loadFromGateway()`.
   - Provide async methods: `recordOutcomeAsync()`, `saveScheduleAsync()`, `updateProfileAsync()`.
   - Keep synchronous methods as fallbacks for in-memory unit tests.

3. **`DoctorShell` Integration**:
   - Provide `{ provide: DoctorWorkspaceGateway, useClass: HttpDoctorWorkspaceGateway }` alongside `DoctorSession`.

---

## 4. Work Breakdown

### Phase 1: Server Endpoints (Kotlin / Ktor)
- [ ] Add `UserRole.DOCTOR` role-gated routes under `/doctor`:
  - `GET /doctor/profile`
  - `PUT /doctor/profile`
  - `GET /doctor/appointments`
  - `POST /doctor/appointments/{id}/outcome`
  - `GET /doctor/patients`
  - `GET /doctor/schedule`
  - `PUT /doctor/schedule`
- [ ] Support `no_show` status in `AppointmentStatus` on server.
- [ ] Write server unit tests in `DoctorWorkspaceTest.kt`.
- [ ] Add smoke tests in `smoke.ps1`.

### Phase 2: Web Gateway & Integration (Angular)
- [ ] Create `DoctorWorkspaceGateway` interface and data models.
- [ ] Implement `HttpDoctorWorkspaceGateway` with error handling.
- [ ] Implement `FakeDoctorWorkspaceGateway` for unit testing.
- [ ] Update `DoctorSession` to load from gateway and perform async mutations.
- [ ] Update `DoctorShell` providers.
- [ ] Verify all 163+ doctor workspace tests pass.
- [ ] Run full frontend test suite and `npx ng build`.
