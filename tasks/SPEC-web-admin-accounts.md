# SPEC — `web-admin-accounts` (module 2 of the mediq-web initiative)

Status: **part 1 (the reads) implemented and verified 2026-10-18** — `:server:test`
68 → 73, `smoke.ps1` 30 → 38 checks, both check scripts clean, `:app` unchanged at
213. **Part 2 (the writes) is implemented and verified 2026-10-18** — `users.status`
and `specialties.description` added, disabling made a boundary at sign-in and on
every authenticated request, and `POST /admin/accounts` creating a staff account
(and, for a doctor, its `doctors` row in the same transaction). `:server:test`
73 → 101 and `smoke.ps1` 38 → 61 across the two halves. All four decisions are
recorded under "Decisions", including the fifth one the fourth opened: a doctor's
practice details go on the create form rather than the route refusing doctors. The
**whole server half is done**; what remains is the web half, which is a change of
its own. "Deviations as built" records why the server went first this time.

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
| The create-account form's inputs | `admin/components/staff-directory/staff-directory.ts` — name, email, username, password, status, specialization, licence, assigned doctor | **Two `NOT NULL` columns are never collected**: `users.mobile_number` and `users.date_of_birth`. A route built from that form cannot insert a row at all. |
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
  comment so the next reader does not add a plausible-looking default. **Since
  answered:** `status` and `description` are now columns and are on the wire, so
  `lastActiveOn` is the only field still absent. Its reason has not changed and
  will not: the only activity recorded is when a session was *created*.

## Part 2 — the writes, and the schema decisions behind them

All four questions were answered on 2026-10-18, and the answers are built —
including the fifth question the fourth one opened (a doctor account's practice
fields), which is answered and built below.

- **`users.status`** — `VARCHAR(16) NOT NULL DEFAULT 'active'`, wire values
  `active` / `inactive` to match the web's `AccountStatus`. A `UserStatus` enum
  lives in `server/.../db/`, **not** in `domain/model/` beside `UserRole`: that
  package is compiled into the app as `sharedDomain`, the patient app has no
  notion of a disabled account, and the server parses what it shares from
  untrusted input — so a value the app cannot mean would become one it accepts.
  Disabling is a boundary, not a label, and three places make it one:
  `AuthService.signIn` refuses after the password check, `configureAuth` refuses
  an already-issued token on its next request, and `PATCH
  /admin/accounts/{id}/status` is the write behind the web's Enable/Disable
  buttons. An unreadable status reads as **inactive** everywhere, so a hand-edited
  column refuses rather than hands out a working account.
- **`specialties.description`** — `VARCHAR(400)`, nullable, seeded from
  `ReferenceData` (which is where the six sentences are written down) and read
  back by `GET /admin/specializations`. The list itself still comes from the
  `Specialty` enum, so a description cannot introduce a specialty the patient
  app's filter does not offer, and the prose is the one part a future admin
  screen could edit without a code change.
- **Adding and editing specializations** — **read-only**, as recommended. The
  `Specialty` enum stays the source of the six; the admin area lists them with
  live counts and text and offers no create or edit.
- **Staff creation** — mobile number and date of birth go on the form and
  `POST /admin/accounts` is a real route. **Built.** The follow-up question the
  answer opened — where a `role = doctor` account's `doctors` row comes from — is
  answered the same way for the same reason: the create form grows a
  **practice-details section**, and the route writes both rows in one transaction.
  The two alternatives were rejected rather than deferred. Creating *doctor-less*
  staff only would have shipped a directory whose "Add doctor" button silently
  produced an account the doctor area cannot use. Wiring the web half first would
  have meant wiring a form to a route that still had to change, and the form is
  where the fields are collected, so the route's shape would have been decided by
  a screen rather than by the schema. The practice details the `doctors` row
  requires are all `NOT NULL` except the licence and bio sentences, so they are
  all **required** and **refused by name** when missing — none is defaulted,
  because a default fee or room is invented clinical data.
- **`POST /admin/accounts`** — admin-only through the same
  `Caller.requireRole` as the reads, answering 201 with the created account in
  the `AdminAccountDto` the directory already publishes, so a client can drop the
  new row into the list it holds instead of re-reading. `db/StaffAccountService.kt`
  owns the validation and the single `db.tx { }`. Three rules are worth naming:
  a `patient` role is **refused** (registration is what proves a mobile number,
  with an OTP, and this route cannot); the role-specific fields are **enforced
  rather than ignored** (a doctor needs the practice record, a secretary needs the
  doctor they work for, and each is refused if sent for the other role); and the
  assigned doctor is checked to **exist**, because `users.doctor_id` deliberately
  has no foreign key and nothing else would refuse a link to a doctor that is not
  there. `MIN_PASSWORD_LENGTH`, `normalizeUsername` and `normalizeMobile` moved
  out of `AuthService`'s private members into shared `internal` functions so the
  two write paths cannot enforce different password floors or store different
  forms of one username.

### Migrating an existing database

`schema.sql` is all `CREATE TABLE IF NOT EXISTS` and is applied at boot, so a
database that already has `users` would never gain the column while every user
SELECT names it. Idempotent `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` statements
therefore sit beside each table that changed — a no-op on a fresh database, and
the difference between an upgrade working and the first request after it failing
outright. This is the stopgap `AGENTS.md` already names; **move to Flyway before
the next column, not after.**

## Where the code lives

| File | Change |
|---|---|
| `server/src/main/resources/schema.sql` | `users.status` + `specialties.description`, each with the idempotent `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` beside its table |
| `server/.../db/UserStatus.kt` | new — `active` / `inactive`, and `fromWire` returning null for a value this build cannot read |
| `server/.../db/UserStore.kt` | `listByRole`; `doctor_id`, `created_at` **and `status`** in `SELECT_COLUMNS`, `UserRow` and `readUser`; `setStatus`; `create` takes a status and a `doctor_id`; `activeSession` → `liveSession`, which joins the account's status; **`createIn`**, the same insert taking the caller's `Connection` so it can join the doctor-row transaction |
| `server/.../db/AuthService.kt` | sign-in refuses an inactive account, after the password check; `MIN_PASSWORD_LENGTH`, `normalizeUsername` and `normalizeMobile` hoisted out of the private companion into shared top-level `internal` declarations |
| `server/.../db/StaffAccountService.kt` | **new** — `NewStaffAccount` / `NewDoctorPractice`, the role and field validation, and the one `db.tx { }` that writes a `users` row and, for a doctor, its `doctors` row |
| `server/.../db/DoctorStore.kt` | `specialtyCounts()`, `specialtyDescriptions()`; **`insert`** (the first `INSERT INTO doctors` outside the demo seed) and **`existsIn`**, both taking the caller's `Connection` |
| `server/.../ReferenceData.kt` | the six specialty descriptions, seeded and backfilled on existing rows |
| `server/.../http/Dtos.kt` | `AdminAccountDto` (+ `status`), `AdminAccountsDto`, `AdminSpecializationDto` (+ `description`), `UpdateAccountStatusRequestDto`, **`CreateAccountRequestDto` + `DoctorPracticeDto`** and their `toDomain()`, `UserRow.toAdminDto()` |
| `server/.../http/Routes.kt` | `Caller.requireRole`; the `/admin` routes; `PATCH /admin/accounts/{id}/status`; **`POST /admin/accounts`**; the status check in `configureAuth` |
| `server/.../Main.kt` | constructs `StaffAccountService` and passes it to `mediQRoutes` / `mediQModule` |
| `server/src/test/.../AdminAccountsTest.kt` | 11 tests, one database each |
| `server/src/test/.../StaffSignInTest.kt` | 9 tests, 3 of them for a disabled account |
| `server/src/test/.../StaffAccountCreateTest.kt` | **new** — 19 tests: the two-row write, the link, every refusal, the stored form, and the rollback |
| `server/scripts/smoke.ps1` | the `Admin` section (the 403s, the directory's contents, the disable/re-enable boundary) and the **`Create staff account`** section (the refusals over HTTP, the created doctor's practice record reading back through `GET /doctors/{id}`, the created account signing in, and both new rows reaching the directory) |

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
- `StaffAccountCreateTest` follows the same rule, for the same reason, and adds
  two kinds of assertion the reads never needed. **Row counts**, because a
  refusal has to be proved to have written nothing rather than only to have
  returned a sentence. And **one direct `db.tx` test**, because the atomicity
  claim is about the transaction and no public call path can produce a
  half-write: every application-level refusal happens before the insert. That
  test inserts a doctor, throws, and asserts the row is gone.
- The create route's HTTP half is `smoke.ps1`'s `Create staff account` section,
  and it is deliberately more than status codes: the created doctor's practice
  record is read back through the public `GET /doctors/{id}`, the created account
  signs in with the password the administrator chose, and both new rows are
  required to appear in the directory. A 201 that never reaches the list would
  leave an account that can be signed in as but never disabled, which no status
  code can show.

## Boundaries

- **No `schema.sql` change in part 1.** Part 1 needed none. Part 2's change is
  `users.status` and `specialties.description`, both additive and both approved
  as decisions before any code was written — the two `ALTER TABLE ... ADD COLUMN
  IF NOT EXISTS` statements are what keeps an existing database working. The create
  route needed **no further schema change**: every column it writes already
  existed, which is why the form's shape is derived from the `doctors` table
  rather than the table from the form.
- **One DTO change in part 1, additive.** Three new response types; no existing
  DTO moves. Part 2 adds one field to two of them (`status`, `description`) and
  one request type.
- **No existing test deleted or weakened.** `:server:test` goes 68 → 73 in part 1
  and 73 → 101 across the two halves of part 2, and the floor in `CONSTRAINTS.md`
  moves with it.
- **No new runtime dependency.**
- **The web repo is not vendored.** `mediqweb` stays a sibling clone.
- **No fabricated data.** No account value is invented to fill a field the form
  does not collect. That is why `POST /admin/accounts` **requires** every
  `doctors` column the schema makes `NOT NULL` and refuses by name rather than
  defaulting a fee, a room, or a speciality — and why `sex` and `address` are left
  `NULL` on a created account rather than filled with a guess: the admin form does
  not collect them and the columns allow it.
- **A doctor created by the route has no clinic hours, and that is deliberate.**
  Writing a default week would put invented sessions on the clinic's bookable
  calendar. The consequence — visible in the directory, can sign in, nothing
  bookable — is pinned by a test rather than left to be found by a patient.

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

### Part 2 — the schema and status half (verified 2026-10-18)

11. A new account is active, and `setStatus` flips it both ways. **Met.**
    `AdminAccountsTest > a new account is active and setStatus flips it both
    ways` asserts the disable *and* the re-enable, because an Enable button
    depends on the reverse.
12. A status this build cannot read reads as disabled, on both readers of the
    column. **Met, mutation-checked.** `AdminAccountsTest > a status this build
    cannot read reads as disabled` writes `'suspended'` and asserts `INACTIVE`
    from `findById` **and** from `liveSession`. Changing either
    `?: UserStatus.INACTIVE` fallback to `ACTIVE` failed that one test and
    nothing else (82 tests, 1 failed).
13. Signing in as a disabled account is refused, and only after the password
    matches. **Met.** `StaffSignInTest` asserts both messages from the same
    account: a wrong password still gets the shared "Those details don't match an
    account.", and the correct password gets the disabled sentence. The order is
    the assertion, not an implementation detail.
14. A token issued before a disable stops working. **Met, mutation-checked at the
    route layer.** `StaffSignInTest > disabling an account stops a token that was
    already issued` covers `authenticate`; `smoke.ps1` covers the route by
    disabling the demo secretary and calling `GET /profile` with the token it
    signed in with earlier in the same run. Replacing the status test in
    `configureAuth` with a bare `session != null` produced **49 passed / 1
    failed**, and the single failure was that check.
15. `PATCH /admin/accounts/{id}/status` is admin-only, validates its input, and
    refuses a self-disable. **Met, over HTTP.** `smoke.ps1` asserts 401 with no
    token, 403 for a doctor's token, 400 for an unknown status, 404 for an unknown
    account, 400 for an administrator disabling their own account, and 200 for
    both directions on a real account.
16. Every account in the directory reports a status, and every specialty reports
    its description. **Met.** Two content checks in `smoke.ps1` require the status
    on every patient, doctor and secretary, and a non-empty description on all six
    specialties.
17. An existing database gains the new columns rather than failing. **Met by
    construction, not by test.** The `ALTER TABLE ... ADD COLUMN IF NOT EXISTS`
    statements are what a fresh database no-ops and an older one executes; both
    engines were already exercised by `DatabaseDialectTest` for the *syntax* they
    sit in. There is no test that boots a pre-change database, and that is the
    honest limit of this criterion.
18. The whole existing gate stays green, and no test was weakened. **Met.**
    `check-boundaries.ps1` clean (56 files); `check-contrast.ps1` clean (31 pairs,
    6 status chips, ratchet 4); `:app:testDebugUnitTest` **213 / 0** (unchanged —
    this module added no app test); `:server:test` **82 / 0**; `smoke.ps1` **50
    passed / 0 failed** against a seeded server on `:8099`. Counts taken from the
    test-results XML after `--rerun-tasks`. The floor in `CONSTRAINTS.md` moved
    73 → 82 in the same change, and no gate script was edited.

### Part 2 — the create route (verified 2026-10-18)

19. `POST /admin/accounts` is admin-only, and answers 201 with the account it
    wrote, in the shape the directory read already publishes. **Met, over HTTP.**
    `smoke.ps1` asserts 401 with no token, 403 for a doctor's token, and reads the
    201 body's `role`, `status` and `doctorId`. That the response is the *same*
    `AdminAccountDto` is a design constraint rather than a checked one: the route
    calls `UserRow.toAdminDto()`, the function the reads use, so the two cannot
    drift into two shapes.
20. A doctor account and its `doctors` row are written together, and the practice
    record reads back where a doctor profile is read from. **Met.** The test
    asserts both rows and every practice field; `smoke.ps1` reads the record back
    through the public `GET /doctors/{id}`, which is the route the patient app
    uses, so the write is proved against a reader that existed before it.
21. A failure after the `doctors` insert rolls that row back. **Met, at the
    mechanism.** `StaffAccountCreateTest > a failure after the doctors row is
    written rolls that row back too` opens one `db.tx`, inserts a doctor, throws,
    and asserts the row is gone. This is what the connection-taking helpers are
    for, and it is the only way to reach the case: every application-level
    refusal happens *before* the insert, so no public call path can produce a
    half-write to observe.
22. The role decides which fields are required, and the wrong ones are refused
    rather than ignored. **Met.** A doctor with no practice record, a secretary
    with no assigned doctor, practice details sent for a secretary, and an
    assigned doctor sent for an administrator are each a 400 naming the field.
    Ignoring the extras would have let a client believe it had set something it
    had not, which is the failure this repo's "no silent drops" rule exists for.
23. A refusal writes nothing. **Met.** The refusal tests assert `userRowCount()`
    or `doctorRowCount()` is unchanged, not only the message — a message-only
    assertion would pass on a route that wrote half an account and then threw.
24. A patient account cannot be created here. **Met.** 400 over HTTP and in the
    service, with the reason stated: registration is what proves the number with
    an OTP and this route cannot.
25. A created account can sign in, active by default, and refused when created
    inactive. **Met.** One test signs in with the password the caller set (using a
    differently-cased username, so the create and sign-in normalizers are proved
    to agree); another creates the account `inactive`, asserts sign-in refuses,
    re-enables through `setStatus`, and asserts sign-in works again.
26. The username, email and mobile number are stored in one normal form, and the
    password floor is one number. **Met.** Normalization asserts the stored
    `ada.admin` / `ada@clinic.test` / E.164 forms and a spaced, upper-cased
    sign-in. The floor is now a single shared constant, so this is structural
    rather than asserted twice: there is no second number to drift.
27. The whole existing gate stays green, and no test was weakened. **Met.**
    `check-boundaries.ps1` clean (56 files); `check-contrast.ps1` clean (31 pairs,
    6 status chips, ratchet 4); `:app:testDebugUnitTest` **213 / 0** (unchanged —
    this half added no app test); `:server:test` **101 / 0**; `smoke.ps1` **61
    passed / 0 failed** against a seeded server on `:8099`, run twice in a row to
    prove the section's unique usernames do not collide with themselves. Counts
    taken from the test-results XML after `--rerun-tasks`. The floor in
    `CONSTRAINTS.md` moved 82 → 101 in the same change, and no gate script was
    edited.

## Deviations as built

1. **The web half is not wired yet, so this module is not yet an integration.**
   Module 1 went end to end on purpose, to prove the seam. Here the server half is
   complete and independently verifiable, and the schema decisions are in, so the
   blocker is gone — but the wiring is a separate change, and wiring the reads
   while the Enable/Disable buttons sat on the same rows would have put a screen
   that looks real next to buttons that quietly do nothing. That is now fixed at
   the server; the web half stays one change of its own.
2. **The web's create-account form is still not updated**, so it does not yet
   send a mobile number, a date of birth, or any practice detail — all of which
   the route now requires. The decision that unblocked this (mobile number, date
   of birth, and a practice-details section on the form) is recorded above and the
   route is built against it, but editing a reviewed form is part of the web half,
   which is one change of its own. The route went first because the form's shape
   is *derived* from the `doctors` table's `NOT NULL` columns, not the other way
   round.
3. **`admin-settings` has no module.** The capability map in `SPEC-web.md` names
   five modules and none of them is the settings screen; it needs a table and a
   decision about whether clinic settings are even server state.
4. **Part 2 landed as two commits' worth of work in one change** (the status
   plumbing and the `specialties.description` column), because both are "the
   answers to the four decisions" and splitting them would have left the docs
   describing a schema that was half-applied.

## Decisions (answered 2026-10-18)

The four questions below were each "add a column" or "drop the feature". All four
were answered on 2026-10-18, and the answers are what "Part 2" above records:

1. **Account status** → **add `users.status`** and refuse a disabled account.
   Built. Enable/Disable is a real feature rather than a pair of inert buttons.
2. **Specialization descriptions** → **add `specialties.description`**. Built,
   seeded from `ReferenceData` and read back per specialty.
3. **Adding and editing specializations** → **read-only list.** The `Specialty`
   enum stays the source of truth; the admin area lists and does not write.
4. **Staff creation** → **add mobile number and date of birth to the form and a
   real `POST /admin/accounts`. Built.** The question it opened — where a
   `role = doctor` account's `doctors` row comes from — was answered by adding a
   **practice-details section to the same form** and writing both rows in one
   transaction. The alternative ("create doctor-less staff only") was rejected:
   a directory whose "Add doctor" button produces an account the doctor area
   cannot use is worse than no button at all. Nothing is defaulted. Every
   `NOT NULL` column of the `doctors` table is required by the route and refused
   by name when it is missing, because a default fee or room is invented clinical
   data.

The one that mattered most was 1, for the reason the recommendation gave: it was
the only one gating a screen already built, and a status nothing checks would have
been worse than no status.
