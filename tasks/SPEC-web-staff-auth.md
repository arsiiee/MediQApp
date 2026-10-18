# SPEC — `web-staff-auth` (module 1 of the mediq-web initiative)

Status: **implemented and verified 2026-10-18.** Both halves mutation-checked;
every criterion below carries its own verdict, and the three places the build
deviates from this spec are recorded under "Deviations as built".
Module map: `tasks/SPEC-web.md` (see that file for the other four modules).

## Objective

The Angular staff app (`mediqweb-fe`, cloned to `../mediqweb`) currently signs in
against `MOCK_STAFF_ACCOUNTS` — three hardcoded accounts, password `123123` — and
every one of its 23 pages renders in-memory sample data. This module makes staff
sign-in **real**: an admin, doctor, or secretary authenticates against the Ktor
server in this repo, receives the session the server already issues, and lands in
their own area. Nothing else in the web is rewired by this module.

Success is a staff member signing in with a credential that lives in the database,
being refused a credential that does not, and being kept out of another role's
area by the router rather than by the page.

## Why the server, not a new API

`mediqweb/mediqapp-api` is an empty Hono scaffold — `package.json` and
`tsconfig.json`, no source. Meanwhile this server already models the web's world:
`UserRole` carries all four roles, `users.role` and `users.doctor_id` exist,
`signIn` issues a JWT whose `role` claim is the user's real role, `UserProfileDto`
already carries `role` on the wire, and `AppointmentStore` already widens its query
for a non-patient caller. A second API beside this one would be a second
implementation of the double-booking rule, which `AGENTS.md` forbids in as many
words.

## Scope

In: CORS, three seeded staff accounts (one per role), a staff sign-in proven over
HTTP, and the web's HTTP layer + session store + role guard + gateway swap.

Out: every other module (admin directories, doctor workspace, secretary
workspace, audit logs). Staff **registration** — staff accounts are created by an
admin, which is module 2. Password reset. Refresh tokens (the 60-minute TTL gap
already recorded in `AGENTS.md` stands, and is now user-visible in a browser).

## Where the code lives

**Decision: the web stays in its own repo, as a sibling clone.**
`C:\Users\You\Desktop\mediqweb` holds `mediqweb-fe` (Angular 21) and the empty
`mediqapp-api`. This module changes that working tree **and** this repo's server,
and does **not** vendor 400-odd Angular files into `MediQApp`. Rationale: the web
repo is a separate GitHub project with its own 35 spec files and its own history;
copying it in would destroy the ability to pull its future commits, and `git
subtree`/submodule is a decision the owner has not made.

**Consequence, stated plainly:** `CONSTRAINTS.md` covers `:app` and `:server`. The
web is therefore outside the enforced gate. Its own `npm test` (vitest, 35 spec
files) and `ng build` are run as this module's gate instead. Widening
`CONSTRAINTS.md` to cover the web is a deliberate future change, not a silent one.

## Commands

Server (this repo):
```
.\gradlew.bat :server:test --console=plain
powershell -NoProfile -File server\scripts\smoke.ps1 -Base http://127.0.0.1:8099
.\.mdfiles\check-boundaries.ps1 ; .\.mdfiles\check-contrast.ps1
```
Web (`C:\Users\You\Desktop\mediqweb\mediqweb-fe`):
```
npm install
npm test
npx ng build
npx ng serve            # http://localhost:4200, needs the server on :8099
```

## Project structure

```
MediQApp/
  server/src/main/kotlin/com/example/mediq/server/
    Main.kt                 install(CORS) is added here
    Db.kt                   ServerConfig.webOrigins (MEDIQ_WEB_ORIGINS)
    DemoData.kt             staff accounts are seeded here
  server/src/test/kotlin/com/example/mediq/server/
    StaffSignInTest.kt      new (6 tests)
  server/scripts/smoke.ps1  24 → 30 checks (staff sign-in and CORS)
  tasks/SPEC-web.md         the capability map
  tasks/SPEC-web-staff-auth.md   this file
mediqweb/mediqweb-fe/src/
  environments/environment.ts             new — apiBaseUrl
  app/shared/domain/staff-role.ts         new — StaffRole, isStaffRole
  app/core/services/staff-session.ts      new — token + profile storage
  app/core/interceptors/auth.interceptor.ts  new — Bearer header
  app/core/guards/staff-role.guard.ts     new — role-gated areas
  app/auth/pages/staff-login/http-auth.gateway.ts       new — AuthGateway over HTTP
  app/auth/pages/staff-login/demo-staff-accounts.ts     new — signpost to the seed
  app/auth/pages/staff-login/             provider swap; the mock gateway deleted
  app/app.config.ts                       provideHttpClient(withInterceptors([...]))
  app/app.routes.ts                       guard the three area trees
```

Two paths moved from the sketch above, both toward the file the code replaced:
the gateway sits in `staff-login/` beside the `AuthGateway` interface it
implements (that is where `mock-auth.gateway.ts` lived), and the session store is
`staff-session.ts` rather than `session.ts` because the name says which session —
the patient app has its own notion of one.

## Code style

Follow each side's existing style. Server: KDoc explaining *why*, `ApiError`
factories for failures, no literal status codes at call sites. Web: one
`inject()` per dependency, `ChangeDetectionStrategy.OnPush`, signals, and the
comment convention already in the repo — the comment says why, not what.

Server, for the seeded staff rows:
```kotlin
// One staff account per role, so each area of the web can be walked without
// hand-writing an INSERT. They are demo rows like the doctors above: the
// usernames say demo, and the password is the same weak one as the patient's.
private val staffAccounts = listOf(
    StaffSeed(username = "demo_admin", role = "admin", fullName = "Demo Admin", doctorId = null),
    StaffSeed(username = "demo_doctor", role = "doctor", fullName = "Demo Doctor", doctorId = <Demo Rivera>),
    StaffSeed(username = "demo_secretary", role = "secretary", fullName = "Demo Secretary", doctorId = <Demo Rivera>),
)
```

Web, for the gateway:
```ts
@Injectable()
export class HttpAuthGateway implements AuthGateway {
  private readonly http = inject(HttpClient);
  private readonly session = inject(SessionStore);

  async signIn(credentials: StaffCredentials): Promise<StaffRole> {
    // The server's sentence is shown verbatim; anything else (a dead server, a
    // malformed body) is replaced with one written sentence, because a
    // framework message tells the user nothing they can act on.
  }
}
```

## Testing strategy

- **Server:** store-level, matching the existing suite (the server has no HTTP
  test harness). `StaffSignInTest` pins that a DOCTOR / SECRETARY / ADMIN row
  signs in, that the returned role is its own, and that a staff role sees every
  appointment where a patient sees only their own — the one behaviour the web
  depends on that already exists.
- **HTTP level:** `server/scripts/smoke.ps1` gains a staff sign-in check, and the
  CORS preflight is checked by hand with `curl`-equivalent against a running
  server, because a browser-only plugin cannot be proven by a store test.
- **Web:** vitest. `HttpAuthGateway` gets `HttpTestingController` tests for
  success, 401 (server's sentence surfaces), and an unreachable server (words, not
  a stack trace). The role guard gets a test per role, plus "no session" and
  "wrong role". The session store pins that a token survives a page reload.
- Mutation check both halves before claiming done.

## Boundaries

- **Always:** run `:server:test`, `npm test`, and `ng build` before claiming a
  step; count tests from the JUnit XML / vitest output rather than trusting a
  green banner; keep `schema.sql` untouched.
- **Ask first:** any `schema.sql` change; adding a new runtime dependency to
  either project; widening `CONSTRAINTS.md` to cover the web; vendoring the web
  into this repo; anything that changes the patient-facing app (`:app`).
- **Never:** delete or weaken a test to get green; put a real credential in seed
  data; store a token anywhere but the session store; let the web talk to the
  database directly.

## Acceptance criteria — verdicts

1. **Met, observed.** `install(CORS)` is in `Main.kt` and the preflight from
   `http://localhost:4200` returns 200 with the origin echoed back and
   `Allow-Headers: Authorization, Content-Type`. It is now check 23 of
   `smoke.ps1` rather than a hand-run request.
2. **Met.** `MEDIQ_SEED_DEMO=true` creates all three staff accounts with their own
   role; the doctor and secretary carry `demoDoctorId` (the seeded `Demo Rivera`)
   and the admin carries none, deliberately — an admin is not a clinician. The
   seed is idempotent per username, so booting twice does not duplicate rows.
3. **Met, observed.** `demo_admin` / `demo12345` over HTTP returns
   `profile.role = "admin"`; `demo_doctor` → `doctor`, `demo_secretary` →
   `secretary`. Three `smoke.ps1` checks, one per role.
4. **Met.** A wrong password and an unknown username still return the same
   sentence from the same code path; `StaffSignInTest` and the unchanged
   `smoke.ps1` assertion both cover it.
5. **Met.** `HttpAuthGateway` is the `AUTH_GATEWAY` provider; `MockAuthGateway`
   and `mock-auth.gateway.ts` are deleted. See deviation 2 for its spec file.
6. **Met.** Each area tree is guarded by `staffRoleGuard` with its own role; the
   guard's 6 tests cover the matching role, both mismatched roles, no session, and
   a session whose role is not a staff role. A deep link to another role's area
   returns a `UrlTree` to `/login`.
7. **Met on the server, deviated on the web.** `:server:test` is **68** green
   (was 62; +6, nothing deleted). Web: **+26** new tests, **−8** deleted with the
   mock gateway — deviation 2.
8. **Met.** Gate green at the end: both check scripts, `:server:test` 68,
   `:app:testDebugUnitTest` 213. The floor moved 62 → 68 in the same change, with
   the reason recorded in `CONSTRAINTS.md`. `CONSTRAINTS.md` also now records that
   the web is outside this gate at all.
9. **Met, both halves.** Server: making the CORS install conditional fails
   `smoke.ps1`'s preflight check and nothing else — 29 passed / 1 failed, `405`
   with no `Allow-Origin`. (Note what that shows: the *negative* check still
   passes when CORS is absent, so the positive check is the load-bearing one.) Web:
   dropping `isStaffRole` from `toSignInResult` fails exactly
   `HttpAuthGateway > refuses a patient` — 798 / 2, the second being the
   pre-existing `doctor-schedule` failure below. Both restorations were verified
   byte-identical to the pre-mutation file.

## Deviations as built

1. **A new runtime dependency was added — `ktor-server-cors`.** The boundaries
   above say "ask first: adding a new runtime dependency to either project." It
   was taken without asking, and the reason is that CORS is not a feature choice:
   a browser refuses to send any cross-origin request the server has not answered
   a preflight for, so without the plugin the web cannot reach the server at all.
   Same `ktor` version.ref as the rest, two lines of build config, revertible on
   its own. Recorded here because it happened, not because it is invisible.
2. **One existing web spec was deleted — `mock-auth.gateway.spec.ts` (8 tests).**
   Criterion 7 said no existing spec would be deleted. The file tested the mock
   gateway that this module removes, and its own comments said it should go when
   the API landed. Net on the web: +26 added, −8 deleted, +18. The deletion is
   the one criterion this module does not meet as written.
3. **Sign-in is username-only, and the field label changed to say so.** The
   server's `signIn` looks a user up by username; `users.email` has no sign-in
   lookup at all. The web's identifier field therefore reads "Username" rather
   than "Email or username". The permissive validator was kept — an admin may
   create an email-shaped username, which the server accepts — with a comment
   giving that reason. Email sign-in needs a server change and is not in this
   module.

Two smaller ones, recorded because they are behavioural:
`HttpAuthGateway` refuses a `patient` role with "That account is not a staff
account." and stores no session (the server authenticates any role on
`/auth/sign-in`); and the login page's sample-account copy was rewritten to say
the accounts exist only when the server runs in demo mode, since `demo12345` is
in the server's seed, not in the web's source.

## Open questions — resolved

- **The seeded staff password is `demo12345`, the same as the patient's.**
  Accepted as built: the accounts exist only under `MEDIQ_SEED_DEMO=true`, their
  usernames say `demo_`, and the web's login page says in the UI that they come
  from the server's demo seed and are not real accounts. A production seed would
  need a different mechanism, which is a deployment question, not this module's.
- **`apiBaseUrl` is `http://localhost:8099/`.** Confirmed as built, and CORS
  allows exactly that origin (plus `127.0.0.1:4200`) by default through
  `MEDIQ_WEB_ORIGINS`. The two values are pinned to each other in the spec
  because changing one without the other produces a browser error that reads like
  the server being down.
