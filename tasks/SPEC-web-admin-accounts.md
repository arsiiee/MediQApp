# SPEC — `web-admin-accounts` (module 2 of the mediq-web initiative)

Status: **part 1 (the reads) implemented and verified 2026-10-18** — `:server:test`
68 → 73, `smoke.ps1` 30 → 38 checks, both check scripts clean, `:app` unchanged at
213. Part 2 (the writes) is not started and is blocked on one decision — see
"Open questions". The web half of part 1 is not started either; "Deviations as
built" records why the server went first this time.

Prerequisite: module 1 (`SPEC-web-staff-auth.md`), implemented 2026-10-18.

## Objective

The admin area's directories — patients, doctors, secretaries, specializations —
read real rows from the Ktor server, and the accounts it creates are real
accounts. Staff account creation lives here, because module 1 deliberately left
it out and a sign-in is only as real as the accounts it can sign in as.

## What the recon found: the screens ask for fields the schema does not have

Before any code, each admin screen was read against `server/schema.sql`. Five of
the fields they display cannot be produced by the server as it stands:

| The screen wants | Where it is modelled | What the server has |
|---|---|---|
| `StaffAccount.status`, `PatientAccount.status` — the Active/Inactive badge, plus the Enable and Disable buttons | `admin.models.ts:34`, `:62` | **Nothing.** `users` has no status column (`schema.sql:14-32`). The server has no notion of a disabled account, so there is no correct value to send — and the buttons cannot do anything. |
| `StaffAccount.lastActiveOn` | `admin.models.ts:52` | **Nothing.** No column. It is *derivable* from the newest `sessions.created_at`, but that is the last sign-**in**, not the last activity, and mislabelling one as the other is worse than showing neither. |
| `Specialization.description` | `admin.models.ts:70` | **Nothing.** `specialties` is `(id, display_name)` (`schema.sql`), and the list itself is projected from the `Specialty` enum the patient app filters by. |
| The create-account form's inputs | `staff-directory/staff-directory.ts:147-162` — name, email, username, password, status, specialization, licence, assigned doctor | **Two `NOT NULL` columns are never collected**: `users.mobile_number` and `users.date_of_birth`. A route built from that form cannot insert a row at all. |
| The audit log and the settings form | `admin-audit-logs`, `admin-settings` | **No table for either.** `admin-settings` already reports honestly that it cannot save (`admin-settings.ts:83-98`). |

Two more findings, which are the good news:

- `users.created_at` and `users.doctor_id` **do** exist. `UserRow` simply never
  selected them (`UserStore.kt` `SELECT_COLUMNS`), so "joined on" and "which
  doctor a secretary sits behind" were real columns nobody could read.
- `specializationId` and `licenseNumber` are *not* account fields. They belong to
  the `doctors` row the account is linked to, and `GET /doctors` already
  publishes both. The link is what a client joins on.

## Why this module is split in two

The reads need no schema change and no product decision: the accounts exist, the
columns exist, they were just never selected. They can be built, tested and
proved over HTTP today.

The writes need both, and each is a decision rather than a task:

- Enable/Disable needs a `users.status` column, and once accounts can be
  disabled the authentication path has to refuse them — a status nothing checks
  is worse than no status.
- Creating a staff account needs two more fields on a form that has already been
  designed and reviewed, and a way to decide the account's role (the patient
  register route does not take one).
- "Add a specialization" contradicts the model: specialties are the `Specialty`
  enum. A row added through the admin screen would be accepted by the server and
  then not offered by the patient app's filter, which is a silent disagreement
  between two screens about what the clinic practises.

## Part 1 — the reads (built)

Server only:

- `UserStore.listByRole(role)`, ordered by name so two screens cannot disagree
  about the order of one list.
- `UserRow.doctorId` and `UserRow.createdAt`, selected and mapped.
- `DoctorStore.specialtyCounts()` — one `GROUP BY`, omitting specialties with no
  doctors so the route fills the zero from the enum (a specialty nobody practises
  must show as `0`, not disappear and read as "not offered").
- `GET /admin/accounts` — the three directories in one response.
- `GET /admin/specializations` — the six specialties with live doctor counts.

### The authorization mechanism (decided here, as module 2 was meant to)

Module 1's role guard is a **UX** gate. `configureAuth` proves a token is valid
and its session is live, and says nothing about who may call what — so until
these routes, a doctor's token was accepted on any authenticated route the
server had. Module 2 owns that decision because it is the first module that adds
one.

The mechanism is `Caller.requireRole(vararg allowed, message: String)` in
`Routes.kt`, and the rules that go with it:

- **403, not 404.** The caller is signed in and the route exists; a 404 would
  send them looking for a typo in the path.
- **The message has no default.** Every route names the sentence its caller will
  read; a shared default would put the same useless sentence on every refusal.
- **An unrecognised role is refused, not rejected as a bad request.** The value
  came out of a token this server signed.
- **It is per route, not per area.** `authenticate(AUTH_JWT)` is not a
  substitute, and a route added later that forgets the check is readable by
  every role.

`GET /admin/accounts` is deliberately one response with three lists rather than a
`?role=` parameter: the admin screens hold all three at once, the dashboard
counts across them, and it is the shape the fixture it replaces had.

### What is knowingly left out of part 1

- **No web consumer yet.** The endpoints are proved by `smoke.ps1` and by hand;
  the admin pages still read `AdminSession`'s fixtures. Wiring them is the next
  step and it cannot be finished without the decision below, because the status
  badge and the Enable/Disable buttons are on the same tables.
- **No paging.** The directory is returned whole. Wrong at real volume; recorded
  in `SPEC-web.md` as the initiative's pagination gap.
- **No `status`, no `lastActiveOn`, no specialization `description`** on the
  wire. Their absence is the honest answer, and `AdminAccountDto` says so in a
  comment so the next reader does not add a plausible-looking default.

## Where the code lives

| File | Change |
|---|---|
| `server/.../db/UserStore.kt` | `listByRole`; `doctor_id` + `created_at` in `SELECT_COLUMNS`, `UserRow` and `readUser` |
| `server/.../db/DoctorStore.kt` | `specialtyCounts()` |
| `server/.../http/Dtos.kt` | `AdminAccountDto`, `AdminAccountsDto`, `AdminSpecializationDto`, `UserRow.toAdminDto()` |
| `server/.../http/Routes.kt` | `Caller.requireRole`; the `/admin` routes |
| `server/src/test/.../AdminAccountsTest.kt` | 5 tests, one database each |
| `server/scripts/smoke.ps1` | the `Admin` section: the 403, and the directory's contents |

## Commands

```powershell
# The server test task (store level)
.\gradlew.bat :server:test --console=plain

# HTTP behaviour, against a seeded server
$env:MEDIQ_SEED_DEMO="true"; $env:MEDIQ_PORT="8099"
$env:MEDIQ_JDBC_URL="jdbc:h2:mem:mediq;DB_CLOSE_DELAY=-1"
.\gradlew.bat :server:run --console=plain      # then, in another shell:
.\server\scripts\smoke.ps1
```

## Testing strategy

Server tests are store level and there is no HTTP harness, so the split is the
one module 1 established: **the store answers what the data is, `smoke.ps1`
answers what the route does.**

- `AdminAccountsTest` covers the three things that were actually missing: that
  `listByRole` filters and orders (asserting a role nobody holds comes back
  *empty*, which is what a dropped `WHERE` would break), that `created_at` and
  `doctor_id` round-trip, and that `specialtyCounts` omits the empty specialties.
  It also asserts on the DTO itself, not only the row: with
  `explicitNulls = false`, a null or mismapped field leaves the response
  silently rather than failing anything.
- `smoke.ps1` carries the role check, because a JWT claim read by the route
  layer is not visible from a store. The 403 is the load-bearing check — a route
  that forgot `requireRole` answers 200 there and every other check still passes.
- Every `AdminAccountsTest` test gets its own database. They create accounts of
  overlapping roles and assert on whole lists, so a shared one would make each
  assertion depend on which test ran first.

## Boundaries

- **No `schema.sql` change.** Part 1 does not need one; part 2 does, and that is
  the open question rather than a thing to slip in.
- **One DTO change, additive.** Three new response types; no existing DTO moves.
- **No existing test deleted or weakened.** `:server:test` goes 68 → 73, and the
  floor in `CONSTRAINTS.md` moves with it.
- **No new runtime dependency.**
- **The web repo is not vendored.** `mediqweb` stays a sibling clone.
- **No fabricated data.** No account value is invented to fill a field the schema
  cannot supply.

## Acceptance criteria — verdicts (verified 2026-10-18)

Every number below came from a command, not from the diff.

1. `GET /admin/accounts` answers 200 to an admin and 403 to a doctor's token.
   **Met, over HTTP.** `smoke.ps1` "the account directory answers an
   administrator" → 200, "a doctor's token is refused on the account directory" →
   403. The server log for the same run shows the 403 and not a 200.
2. `GET /admin/accounts` answers 401 to no token at all. **Met.** `smoke.ps1` "the
   account directory needs a token" → 401.
3. The directory carries the seeded staff accounts with their role, their
   `createdAt`, and the `doctorId` of the doctor they sit behind. **Met.**
   `smoke.ps1` asserts `demo_doctor` comes back with `role`, a non-null `doctorId`
   and a `createdAt`.
4. Each account appears in exactly one list, and a patient never appears in a
   staff list. **Met.** `smoke.ps1` compares the three lists by username and
   `AdminAccountsTest` asserts a whole-list answer per role.
5. `GET /admin/specializations` lists all six specialties with live counts, and a
   specialty with no doctors is listed with `0` rather than omitted. **Met.**
   `smoke.ps1` requires exactly six entries with at least one non-zero and at
   least one zero count; `AdminAccountsTest` pins that the store returns the
   absent key rather than a fabricated `0`, which is what the route fills.
6. `listByRole` returns one role's accounts only, ordered by name, and an empty
   role returns empty. **Met.** `AdminAccountsTest > listByRole returns the role
   asked for and nothing else`, including the empty-`ADMIN` assertion.
7. `users.created_at` and `users.doctor_id` reach the wire. **Met.**
   `AdminAccountsTest` reads both back off the row and asserts the DTO carries
   them, because `explicitNulls = false` means a dropped field would leave the
   response silently.
8. No `schema.sql` change, no dependency added, no test weakened. **Met.**
   `git status` shows no `schema.sql`, no `libs.versions.toml` and no
   `build.gradle.kts`; the suite went 68 → 73 by adding `AdminAccountsTest`, and
   the other nine test classes kept their exact counts. No gate script edited.
9. The whole existing gate stays green. **Met.** `check-boundaries.ps1` clean (56
   files); `check-contrast.ps1` clean (31 pairs, 6 status chips, ratchet 4);
   `:app:testDebugUnitTest` **213 / 0**; `:server:test` **73 / 0**; `smoke.ps1`
   **38 passed / 0 failed** against a seeded server on `:8099`. Counts taken from
   the test-results XML after `--rerun-tasks`, not from `UP-TO-DATE`.
10. Mutation check: forcing `requireRole` to pass shows the smoke 403 failing and
    nothing else. **Met, with one failed attempt recorded.** The first mutation —
    `mine == null && mine !in allowed` — did not compile: the `&&` removes the
    smart cast the `!in` needs, so it proved nothing and is not counted as
    evidence. The valid mutation replaced the membership test with
    `allowed.isEmpty()`, which refuses only unrecognised roles: `smoke.ps1` then
    read **37 passed / 1 failed**, and the single failure was the admin 403. The
    diff was restored and `git diff --stat` confirms only the intended files
    remain.

## Deviations as built

1. **The web half is not wired yet, so this module is not yet an integration.**
   Module 1 went end to end on purpose, to prove the seam. Here the server half
   is complete and independently verifiable, and the web half cannot be finished
   without the schema decision: the status badge and the Enable/Disable buttons
   sit on the same tables as the rows that would be wired. Wiring the reads and
   leaving the writes mocked would put a screen that looks real next to buttons
   that quietly do nothing.
2. **`users.mobile_number` and `users.date_of_birth` are not collected by the
   create-account form.** Recorded rather than fixed: adding inputs to a reviewed
   form is a product decision, and it belongs with the write endpoints that would
   need them.
3. **`admin-settings` has no module.** The capability map in `SPEC-web.md` names
   five modules and none of them is the settings screen; it needs a table and a
   decision about whether clinic settings are even server state.

## Open questions

**One decision blocks the rest of module 2.** Each part below is either "add a
column" or "drop the feature", and they are independent:

1. **Account status** — add `users.status` (or `disabled_at`) plus a check in
   `configureAuth` that refuses a disabled account, and then Enable/Disable is a
   real feature; **or** drop the status badge and both buttons from the three
   directory screens. Until this is answered the admin reads cannot be wired,
   because the badge is in the same table.
2. **Specialization descriptions** — add a `specialties.description` column, or
   drop the field from the admin screen.
3. **Adding and editing specializations** — accept that the admin screen may only
   *list* them (they are the `Specialty` enum the patient app filters by), or
   decide the enum is no longer the source of truth and the specialties table
   becomes the only one.
4. **Staff creation** — add mobile number and date of birth to the form and a
   `POST /admin/accounts`, or leave staff creation to the seed and the database.

The recommendation, cheapest first: answer 1 alone. It is the only one that
gates a screen already built, it is a small additive column plus one check in
`configureAuth`, and it turns the whole patient/doctor/secretary read path into
something the web can be wired to without any screen losing a feature.
