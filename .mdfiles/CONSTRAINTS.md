# Constraints

Last reviewed: 2026-10-06

The quality bar for this repo, as numbers rather than prose. `.mdfiles/AGENTS.md`
states the same architectural rules in words; this file states them with a
command that produces a verdict, so a violation is caught by running something
rather than by someone remembering.

Read this before writing code. **Do not weaken it to make a change pass.** If a
rule is wrong, change it here explicitly, in its own change, where the diff
shows the bar moving.

## Floor (always enforced, no setup required)

- No new suppression comments: `@Suppress`, `@SuppressLint`, `noinspection`, `// nosemgrep`
- No unimplemented stubs: `TODO(`, `throw NotImplementedError`, `NotImplementedException`
- No skipped or deleted tests without a reason in the commit message
- No secrets in source. The dev JWT secret in `Db.kt` is deliberate and guarded
  by `ServerConfig.validate()`; adding a *second* one is not
- This file does not get weakened to make a change pass

## Enforced with numbers

Every row names the command that produces the verdict. A rule with a number and
no command is an aspiration, not a constraint.

| Dimension | Rule | Checked by | Runs at |
|-----------|------|-----------|---------|
| Types | Zero compile errors in `:app` and `:server` | `.\gradlew.bat :app:compileDebugKotlin :server:compileKotlin` | every edit |
| Architecture | `ui-imports-data` = 0 | `.\.mdfiles\check-boundaries.ps1` | every edit, < 5s; **and every CI run** |
| Architecture | `domain-imports-android` = 0 | `.\.mdfiles\check-boundaries.ps1` | every edit, < 5s; **and every CI run** |
| Architecture | `api-call-not-wrapped` = 0 | `.\.mdfiles\check-boundaries.ps1` | every edit, < 5s; **and every CI run** |
| Architecture | `secret-field-no-keyboard-options` = 0 | `.\.mdfiles\check-boundaries.ps1` | every edit, < 5s; **and every CI run** |
| Architecture | `secret-field-not-password-keytype` = 0 | `.\.mdfiles\check-boundaries.ps1` | every edit, < 5s; **and every CI run** |
| Tests | `:server:test` green, 101 tests minimum | `.\gradlew.bat :server:test` | task end |
| Tests | `:app:testDebugUnitTest` green, 238 tests minimum | `.\gradlew.bat :app:testDebugUnitTest --rerun-tasks` | task end |
| Contrast | Every token pair clears WCAG AA; 0 hardcoded page backgrounds; raw-colour ratchet = 4 (over `ui/` and the shared components); 6 status-chip pairs | `.\.mdfiles\check-contrast.ps1` | every edit, < 5s; **and every CI run** |

Both check scripts resolve paths relative to the working directory, so they must
be run from the repo root even though they live in `.mdfiles/`.

Combined task-end gate, ~90s budget:

```powershell
.\.mdfiles\check-boundaries.ps1
.\.mdfiles\check-contrast.ps1
.\gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest :server:test --console=plain
```

**Both check scripts also run in CI**, in a `gates` job in
`.github/workflows/ci.yml`, so the rows above are no longer manual. Wired
2026-10-07; before that they were described here as enforced but only ever ran
by hand, and a violation of either merged green. The job needs no JVM or Android
SDK (`pwsh` is preinstalled on `ubuntu-latest`), takes ~10s, and runs in parallel
with the build jobs. It is **not** a step inside the `android` job — parking a
10-second check behind a 3-minute SDK download is the cost that teaches people to
ignore a red pipeline — and it carries **no `paths:` filter**, because these
scripts check the current state of the tree rather than a diff.

## Why architecture boundaries are the enforced dimension

Not coverage, not security scanning — those still need tooling this repo does not
have. Architecture boundaries won because they needed **no new dependency**: two
PowerShell scripts over the source tree, which is why they were enforceable
immediately even while nothing ran automatically here. The rules already existed
in `AGENTS.md` under a heading reading *"Rules that are easy to break"*, and
**nothing checked any of them**. Three independent failures followed from that,
all with fully green builds:

1. The server published an error contract (`ErrorDto`) that the client never
   read. Patients saw `"HTTP 401"` instead of the message written for them. The
   whole test suite passed throughout.
2. Seven ViewModels imported `BackendNotConnectedException` from
   `data/repository/`, directly violating *"Screens and ViewModels must not
   reference `data/` directly"*. Found by the first run of `check-boundaries.ps1`
   and fixed the same day — the exception moved to `domain/model/`, where it
   belongs, and `ui-imports-data` went from 7 to 0.
3. Not one of the ten `OutlinedTextField`s in the app set `keyboardOptions`.
   Compose therefore used `KeyboardOptions.Default` — `autoCorrect = true`, no
   `KeyboardType` — so Android kept autocorrect and suggestions live on the
   masked fields and rewrote the characters as they were typed. A correct
   password reached the server as a different string and returned the same
   `"Those details don't match an account."` as a wrong one, so nothing on
   screen distinguished a typo from a bad password. Five masked fields were
   affected, sign-in included, and the build was green throughout.

All three are the same failure: a rule stated in prose, never checked. That is
what this file and `check-boundaries.ps1` exist to stop recurring. The third is
the one to read twice — it is invisible in review, invisible to the compiler,
and invisible to the entire test suite, and it shipped in the most safety-
critical place in the app.

The fourth was the scripts themselves. They were described here as enforced
while running only when a human remembered to type them, so a boundary or
contrast violation merged green. Both now run in CI (see above) — which is the
same lesson one level up: a rule with no command producing its verdict is a rule
nobody is checking.

## Ratchets (measured 2026-10-07, must not worsen)

Set at today's value rather than at an aspiration, so the gate is green on day
one. Move a number only when the code moves it.

| Metric | Today | Direction | Note |
|--------|-------|-----------|------|
| `ui-imports-data` | 0 | must not grow | Was **7** until 2026-10-06 — every one `BackendNotConnectedException`, caught by ViewModels from `data/repository/`. Moved to `domain/model/`, where it belongs, and the ratchet dropped from 7 to 0. |
| `domain-imports-android` | 0 | must not grow | Would break `:server`, which compiles `domain/` as `sharedDomain`. |
| `api-call-not-wrapped` | 0 | must not grow | An unwrapped call is how the `HTTP 401` bug returns. |
| `secret-field-no-keyboard-options` | 0 | must not grow | Was **5** on 2026-10-06 — every masked field in the app, sign-in included. See failure 3 above. |
| `secret-field-not-password-keytype` | 0 | must not grow | Was **0**, and stays 0. Added at the same time as the rule above; nothing set the wrong key type because nothing set one at all. |
| `:server:test` count | 101 | must not fall | `BookingConcurrencyTest` is 4 of those and cannot be checked by hand. Raised from 36 on 2026-10-06 — `AppointmentLifecycleTest` (13), `AvailableDatesTest` (4), `OtpLockoutTest` (3) and the 7 username-normalisation cases in `AuthServiceTest` were added, so the floor moves with the code. Raised 50 → 59 on 2026-10-07 by `DoctorSearchTest` (9). **Raised 59 → 60 on 2026-10-16** by `DatabaseDialectTest` (1) pinning PostgreSQL configuration and dialect detection for Supabase. **Raised 60 → 62 on 2026-10-18** by `AppointmentLifecycleTest` (2) pinning `appointments.created_at` — one test per SELECT, because the list and the single read are separate queries and fixing only one is how the field would have shipped half-wired. **Raised 62 → 68 on 2026-10-18** by `StaffSignInTest` (6) for the `web-staff-auth` module: all three seeded staff roles sign in and get their own role back, the seeded doctor and secretary are linked to a real doctor row, a patient role is *not* refused by this endpoint (the web's gateway owns that refusal, so the server's permissiveness is pinned rather than assumed), and the seed is idempotent per username. Mutation-checked: `allowOrigins { it == origin }` replaced by a conditional install fails the CORS preflight check in `smoke.ps1` and nothing else. **Raised 68 → 73 on 2026-10-18** by `AdminAccountsTest` (5) for the `web-admin-accounts` module: `listByRole` filters and orders, with a role nobody holds asserted **empty** rather than the whole table (which is what a dropped `WHERE` breaks while every other assertion still passes); `users.created_at` and `users.doctor_id` reach the wire; `specialtyCounts` omits the specialties nobody practises rather than inventing a zero; and the admin DTO is asserted directly, because `explicitNulls = false` means a null or mismapped field leaves the response silently. Mutation-checked: the `mine !in allowed` clause in `Caller.requireRole` replaced by `allowed.isEmpty()` fails exactly the new admin 403 check in `smoke.ps1` and nothing else. **The "Today" cell also read 60 while the enforced floor was already 68** — module 1's raise moved the rule on line 36 and left this column behind; both say 73 now, verified by counting the test-results XML rather than by arithmetic on the diff. **Raised 73 → 82 on 2026-10-18** by the write half of `web-admin-accounts`: `AdminAccountsTest` (6 more) and `StaffSignInTest` (3 more) pin that a new account is active and that `setStatus` flips both ways (a disable has to be reversible, which is what the web's Enable button relies on); that a status this build cannot read reads as **inactive** on both readers of the column, so a hand-edited row can never hand out a working account (mutation-checked: changing either `?: UserStatus.INACTIVE` fallback to `ACTIVE` fails that one test and nothing else); that `liveSession` carries the account's status in the same query that proves the session is live and still returns null for a revoked session; that the admin DTO carries the status, asserted on the DTO because `explicitNulls = false` means a field that never reached the response would leave it silently and the badge would fall back to whatever the web defaults to; that sign-in refuses a disabled account **only after the password matches** (the order is the point — checking status first would tell anyone who typed a username that the account exists, which is what the shared sign-in message exists to hide); that a token issued *before* a disable stops working immediately; that re-enabling lets the account back in; and that every `Specialty` entry has a description, with a null one reported as absent rather than blank. The route-level half is only reachable over HTTP and is proved by `smoke.ps1`: 38 → **50** checks, including that a doctor's token gets 403 on `PATCH /admin/accounts/{id}/status`, that a disabled account cannot sign in again and its live token stops working, and that an administrator cannot disable their own account. Mutation-checked at the route layer: replacing the status test in `configureAuth` with a bare `session != null` fails exactly the "existing token stops working" check (49 passed / 1 failed) and nothing else. **Raised 82 → 101 on 2026-10-18** by `StaffAccountCreateTest` (19) for `POST /admin/accounts`, the module's only write that adds rows and the only place the server writes a `doctors` row at all: that a doctor account and its `doctors` row are written together and the practice record reads back through `GET /doctors/{id}`; that a secretary is linked to the doctor they are assigned to and adds no `doctors` row; that an administrator gets neither link; that a refusal writes **nothing**, asserted on row counts rather than only on the message; that a patient account, practice details for a non-doctor, an assigned doctor for a non-doctor, a doctor with no practice details, and a secretary pointing at a doctor that is not there are each a 400 naming the field; that a negative fee or a negative year count is a 400 rather than the column's `CHECK` answering 500; that a short password is refused against the same floor registration enforces, which is now one shared constant; that the username, email, and mobile number are stored in the normal form `register` and `signIn` already agree on; and that a failure *after* the `doctors` insert rolls that row back, which is the whole reason the stores gained connection-taking helpers instead of the service opening a second transaction. **The value cell read 73 in the same commit while the enforced floor on line 36 said 82** — the same drift the previous paragraph records, left behind by the status/description slice and found only because this raise needed the number to move. Both lines say 101 now, counted from the test-results XML after `--rerun-tasks`, not by arithmetic on the diff. `smoke.ps1` went 50 → **61** in the same change, the added checks being the four role/validation refusals and the duplicate-username 400 over HTTP (a route-level answer no store test can give), that a created doctor's practice record reads back from the public doctor-profile route, that the created account can sign in with the password the administrator chose, and that the created accounts reach the directory they will be managed from — because a 201 that never appears in the list is an account that can be signed in as but never disabled. |
| `:app:testDebugUnitTest` count | 213 | must not fall | **Raised 210 → 213 on 2026-10-18** by the `ui-appointment` restyle (figure 6): `AppointmentCreatedAtTest` (3) pins `createdAt` reaching the domain, an absent field mapping to null (so the `BOOKED ON` row hides instead of the appointment failing to render), and a malformed timestamp doing the same — all through Gson, because constructing the DTO in Kotlin never exercises the path that fails. Mutation-checked: dropping the mapping in `ApiMappers.kt` fails the first test exactly. **Raised 195 → 210 on 2026-10-17** by the `ui-scheduling` restyle (figures 4 and 5): four tests in `DoctorDetailsViewModelTest` pin `visibleSlots` - an available slot and a claimed (`RESERVED`) one are both drawn, a clinic-`BLOCKED` one is not, an `UNKNOWN`-status one is not, and the raw `slots` field is left untouched (the filter is a view, not a rewrite). Mutation-checked: reverting `visibleSlots` to `isBookable` fails the claimed-slot test exactly. **The floor was also corrected 195 → 206 in the same change** - the tree had held 206 since `8c986b0` (the doctor-details month stepper and the `MoneyFormat` extraction) and the ratchet never moved with it, so 195 had been stale for two commits. **Raised 189 → 195 on 2026-10-15** by the doctors-list restyle (`ui-doctors`): `NextClinicSessionTest` (6) pins the `Next open` line, which is derived from `Doctor.clinicHours` rather than requested per row, so no existing test had to be rewritten to make room. **Corrected 2026-10-15 from 182:** the measured count with `--rerun-tasks` is 185. `DoctorDetailsViewModelTest` (3 tests) was added in `2d2343d` on 2026-10-07 and never added to this floor or to the inventory in `AGENTS.md`, so both read 182 while the tree held 185. No test was added or changed here — the number was verified, not moved. Added 2026-10-06 at the current value, when ViewModels first became testable: 23 in `data/api/`, plus `RegisterViewModelTest` (22) and `AppointmentDetailsViewModelTest` (27). The 23 were the whole suite until then — every ViewModel was untested because the harness did not exist. Raised from 51 when the appointment cancel/reschedule actions were wired: 10 more cover the `isActionable` gate, a cancel that must reload, a refused cancel, and a reschedule that must carry the real id and a chosen slot. Raised again from 61 when the reschedule slot picker was built: 10 more cover the availability reads, the bookable-only filter, discarding a slot when the date changes, and that a picker cannot be opened on an appointment that cannot be changed. Raised from 71 to **166** when the six remaining untested ViewModels were covered: `DoctorsViewModel` (22), `BookingViewModel` (19), `SignInViewModel` (16), `ProfileViewModel` (14), `HomeViewModel` (14), `NotificationsViewModel` (10). Every one of those 95 was mutation-checked — the ViewModel was deliberately broken and the suite had to fail — so the count is 166 tests that can fail, not 166 that pass. **Corrected 2026-10-07 from 165:** the six listed classes sum to 166, and `HomeViewModelTest` holds 14, not the 13 recorded here — the floor was one low from the day it was written. **Then 166 → 174 the same day**, once `SeededDataViewModelTest` (8) reached `HEAD`. The floor tracks the committed tree, and the screen that test covers is a temporary backend-verification tool whose removal is already proven on a scratch branch (`tasks/todo.md`, v3 Task 4). Raised again **174 → 180** for six `MediQBottomBarLabelsTest` cases guarding visible and accessible names, then **180 → 182** for two delayed-response regressions in `DoctorsViewModelTest`: an opening read must not replace newer filtered results or flash them while a search is debouncing. When the temporary seeded-data screen and its 8 tests are deleted, lower the floor by 8 in the same commit and remeasure. **Then 185 to 189** on 2026-10-15, when the specialty filter replaced the fake `PEDIATRICS` chip in `DoctorsScreen.kt`: four tests in `DoctorsViewModelTest` pin that every `Specialty.entries` value is forwarded verbatim, that a specialty-only query carries no other filter, that toggling off and on re-queries, and that a specialty with no doctors is an empty `Success`. `DoctorsViewModelTest` goes 24 to 28, and the mutant that sends `PEDIATRICS` for every chip was confirmed to fail exactly those four. |
| Suppressions | 0 | must not grow | |
| Stubs (`TODO(`) | 0 | must not grow | |
| Raw colour literals in `ui/` and the shared components | 4 | must not grow | The 4 are `SplashScreen`'s white-on-brand-green, which measures 6.63:1. Was **25** on 2026-10-06 — 22 `Color.Gray` at 3.95:1 and friends, all invisible to a green build because nothing measured them. **The ratchet now matches every `Color(0x...)` literal and scans `core/designsystem/component/` too** — see "Not enforced, and why". |
| Hardcoded page backgrounds | 0 | must not grow | Was **10**. Caused the dark scheme's white-on-white 1.00:1. |

## Known debt

| Item | Why it is still open |
|------|---------------------|
| `window.statusBarColor` is deprecated | Pre-existing warning in `Theme.kt`. Replacing it means `enableEdgeToEdge()`, which changes how insets are handled app-wide — its own task, not a contrast fix. |
| Dark mode is contrast-correct but not tonally tuned | Status chips keep fixed light pastel containers, so they stay correct in both modes but read as bright blocks on a dark surface. Needs a dark status palette, which is a design decision. |
| **Six compiler warnings, not one** | Added 2026-10-07. `README.md` claimed `statusBarColor` was "the one remaining compile warning". `.\gradlew.bat --warning-mode all :app:compileDebugKotlin --rerun-tasks` reports **6**: that one, plus 5 uses of the deprecated `KeyboardOptions(capitalization, autoCorrect, …)` constructor at `RegisterCredentialsScreen.kt:91,115,143` and `SignInScreen.kt:109,133`. All pre-existing and committed, none introduced by the doc work. The constructor wants the new `autoCorrectEnabled` parameter. Checked with `--warning-mode all`, because the default mode collapses all six into "Deprecated Gradle features were used" with no detail. |
| **Gradle 10 will not build this** | `.\gradlew.bat --warning-mode all :server:compileKotlin` reports `Configuration.setVisible(boolean) has been deprecated. This is scheduled to be removed in Gradle 11`. Not ours — grepping all four build scripts for `visible` returns 0 hits, so it comes from a plugin (AGP 9.3.3 or KGP 2.2.10). Nothing in this repo can fix it short of a plugin bump, so it is recorded rather than enforced. Worth re-checking on the next AGP upgrade. |
| **Three JVM versions in one build** | The Gradle daemon runs JBR 21, `server/build.gradle.kts` pins `JavaLanguageVersion.of(17)`, and `gradle-daemon-jvm.properties` requests `toolchainVersion=25`. So `:server:run` launches on Adoptium **17** while `:app` compiles on 21 — visible in a `:server:run` failure, which names the toolchain JVM rather than the daemon's. Not a bug today (17 satisfies the `JavaVersion.VERSION_11` target in `app/build.gradle.kts:34-35`), but the docs describing a single "JDK 21+" are describing the daemon only, and a `--warning-mode all` build or a JVM-sensitive server bug would be diagnosed against the wrong number. |

## Exceptions

None.

## Not enforced, and why

| Dimension | Why not |
|-----------|---------|
| Test coverage | Needs JaCoCo or Kover in both modules. `:app` has 213 unit tests and all ten ViewModels are covered. One on-device Compose test (`MediQBottomBarSemanticsTest`) now checks the bottom bar's merged accessible name; the unit test alone missed Material3 dropping an icon description from the clickable parent. Other composables, including the seeded-data splash gesture, still have no rendered regression tests. `ExampleInstrumentedTest` remains an untouched template. |
| **The `mediqweb` staff frontend is outside this gate entirely** | Added 2026-10-18 with `web-staff-auth`. The Angular app is a **sibling clone** at `../mediqweb`, not a subdirectory of this repo, so `check-boundaries.ps1` and `check-contrast.ps1` never see it and neither do the two Gradle test tasks. Its own gate is `npm test` (Angular's `@angular/build:unit-test`, vitest + jsdom) plus `npx ng build` for the strict-template compile, and as of 2026-10-18 that suite was **800 tests, 799 green** — the one failure is a pre-existing date-rot bug in `doctor-schedule.spec.ts`, reproduced on the pristine clone. Nothing in this file's numbers covers the web, and a change there cannot move them. Widening the gate to include it is a deliberate future change, not an oversight. |
| Security scanning | Semgrep/osv-scanner have no workflow. CI exists (`.github/workflows/ci.yml`, added in `ef106ef`) so there is somewhere to add them, but neither has been wired. Genuinely relevant: the repo has a dev JWT secret in source, guarded only by `MEDIQ_ENV=production`. |
| Lint | `:app:lintDebug` runs in CI on the stock Android rules, so lint is a gate — but the repo carries no lint **config** (no detekt, ktlint, or `.editorconfig`), so those stock defaults are the whole bar. |
| The raw-colour ratchet was narrower than it reads — **fixed 2026-10-15** | Before that date, `check-contrast.ps1:177` matched `Color.Gray\|LightGray\|DarkGray\|White\|Black` plus the single hex `0xFFD32F2F`, so an arbitrary literal such as `Color(0xFF00FF00)` passed it — proved by mutation on 2026-10-07, not inferred. **It no longer does:** the pattern now matches every `Color(0x…)` literal, and the thirteen sites it reaches are tokens in the same change — the twelve status chips in `AppointmentsScreen.kt` and `NotificationsScreen.kt:122` — so the floor stayed 4 while the bar widened. What it still does **not** catch, and must not be claimed to: `Color(red =, green =, blue =)`, indirect construction, `.copy(alpha = …)`, and a literal on a line that is entirely a comment (`:178` drops whole-line comments). **Widened again on 2026-10-15 to scan `core/designsystem/component/` too** — the shared components are where the design system's colours now live, so a card that hardcodes its container is the same defect wherever the file sits; proved by mutation (`Color.White` in `MediQInfoCard.kt` failed the ratchet at 5, and before the widening that file was not scanned at all). The status-chip check also asserts the expected pair count — **6, one per `AppointmentStatus` member** — instead of only "at least one", so a pair that is dropped (rewritten to a raw literal) or swapped for another token is loud; both mutations were run. |
| Secrets scanning | Would flag the intentional dev JWT secret in `Db.kt` on every run until it is allowlisted. |
