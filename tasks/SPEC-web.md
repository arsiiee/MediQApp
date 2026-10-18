# SPEC — the `mediq-web` initiative (capability map)

Status: **module 1 implemented 2026-10-18.** Modules 2–5 are scoped here to
capability level only; each gets its own spec before any of it is built.
Module 1's file: `tasks/SPEC-web-staff-auth.md`.

The request that started this: *"clone this
https://github.com/Christian-Carl-C-Cabalde/mediqweb/tree/development integrate
web via backend"*.

## What is being integrated

`mediqweb` is a **separate GitHub project**, cloned as a sibling at
`../mediqweb` (branch `development`, `52a82ee`). It holds two folders:

- `mediqweb-fe/` — the staff frontend. Angular 21, standalone components,
  signals, vitest + jsdom, **35 spec files** as cloned.
- `mediqapp-api/` — an empty Hono scaffold: `package.json` and `tsconfig.json`,
  no source at all.

**The backend is this repo's Ktor `:server`, not `mediqapp-api`.** The scaffold
has nothing in it, while this server already models the web's world: `UserRole`
carries all four roles, `users.role` and `users.doctor_id` exist, `signIn` issues
a JWT whose `role` claim is the user's real role, `UserProfileDto` carries `role`
on the wire, and `AppointmentStore` already widens its query for a non-patient
caller. A second API beside this one would be a second implementation of the
double-booking rule, which `AGENTS.md` forbids in as many words.

## What the frontend does today

**23 page components across four areas** — auth 1, admin 8, doctor 8, secretary 6.
Every one renders **in-memory sample data**: three files, `admin.mock-data.ts`,
`doctor.mock-data.ts`, `secretary.mock-data.ts`, plus a `mock-notice` component
whose job is to say the screen is not wired up. There are **zero `HttpClient`
calls** in the app as cloned, and no `environment*.ts` at all — there was no
notion of a server to point at.

That is the whole gap, and it is why the first module is sign-in rather than any
one area: sign-in is the only screen whose mock is *load-bearing* for every other
screen, and it is the cheapest place to prove the seam (HTTP client, session
storage, bearer header, error copy) end to end.

## The modules

| # | Module | Objective | Depends on | Spec |
|---|--------|-----------|------------|------|
| 1 | `web-staff-auth` | Staff sign-in against the real server; session store, bearer interceptor, role guard; the mock gateway and its hardcoded accounts deleted. | — | `SPEC-web-staff-auth.md` — **implemented 2026-10-18** |
| 2 | `web-admin-accounts` | The admin area's directories read and write real rows: patients, doctors, secretaries, specializations. **Staff account creation lives here** — module 1 deliberately left it out. | 1 | not written |
| 3 | `web-doctor-workspace` | The doctor area: appointment queue, patient list, history, and the weekly schedule. The schedule is the first *write* surface (clinic hours, and one-off dated availability). | 1, 2 | not written |
| 4 | `web-secretary-workspace` | The secretary area: appointments, patients, schedules, messages. Mostly reads plus booking on a patient's behalf. | 1, 2 | not written |
| 5 | `web-audit-logs` | The admin audit log, over a real event trail. Needs a server-side table that does not exist yet — the only module whose *server* half is a new aggregate rather than an exposure of one. | 2 | not written |

Ordering rationale: 1 proves the seam; 2 is the smallest area that needs **write**
endpoints, so it is where the "who may call this" question gets answered; 3 and 4
are the two large areas and are independent of each other; 5 is last because it is
the only one that needs new persistence rather than new routes.

## Decisions module 1 fixed for every later module

These were settled once so modules 2–5 inherit them rather than re-deciding:

- **One gateway per area, behind an interface.** `AuthGateway` stays an
  interface; the HTTP implementation is injected through a provider token, so a
  module can be built and tested against a fake without touching the page.
- **One session store.** `StaffSessionStore` (signal + `localStorage` + expiry)
  is the single home of the token. Nothing else stores one, and every request
  carries it through `authInterceptor`, not through a per-call header.
- **Errors are the server's sentences.** A 401/403 surfaces the server's
  `message` verbatim; anything else (dead server, CORS refusal, unreadable body)
  is replaced with one written sentence, because a framework message tells a user
  nothing they can act on.
- **`apiBaseUrl` is `http://localhost:8099` and CORS is an allow-list** of exact
  origins (`MEDIQ_WEB_ORIGINS`). The two are pinned to each other: changing one
  without the other produces a browser error that reads like the server being
  down.
- **The three demo staff accounts come from the server's seed**, not from the
  web's source. The web's `DEMO_STAFF_ACCOUNTS` is a signpost to them.

## What the server does not have yet, per module

Named here so each is a decision rather than a discovery:

- **No admin CRUD routes at all.** `Routes.kt` exposes health, auth, doctors
  (read), appointments (book/cancel/reschedule-request), notifications, and
  profile. There is no route to create a user, list users, or edit a doctor. All
  of module 2's writes, and module 3's schedule writes, need new endpoints.
- **No staff registration.** `POST /auth/register` is the patient path. Staff
  accounts are currently only created by the seed. Module 2.
- **No server-side role gate.** `configureAuth` checks that a token is valid; it
  does not check which role may call the endpoint. Module 1's guard is therefore
  a **UX** gate, not a security boundary: the server would answer a doctor's token
  on an admin route today. Every module that adds a write must add the
  authorization check with it, and `web-admin-accounts` is where the mechanism
  gets decided.
- **No audit or event table.** Module 5.
- **No pagination in the web.** The server has cursors and `MAX_PAGE_SIZE`; the
  web has no notion of a page. Fine at demo volume, wrong at real volume.
- **Token expiry mid-session is unhandled** — a 60-minute TTL and no refresh.
  Already recorded in `AGENTS.md` as a known gap; a browser now makes it visible
  (a tab left open overnight starts 401ing) rather than theoretical.

## Rules that apply to every module

- **No new invented data.** The `*.mock-data.ts` files are deleted area by area,
  and nothing replaces them. A screen that renders a plausible fabricated patient
  is the exact defect `AGENTS.md` forbids for the app, and it is no better here.
- **A mock is not deleted until its area reads the real thing.** The `mock-notice`
  component stays until the last area is wired.
- **Each module proves its seam over HTTP**, not only in a unit test: sign-in was
  proven with `smoke.ps1` checks against a running server. Where a behaviour is
  only reachable through a browser (CORS), the check that proves it lives in
  `smoke.ps1` rather than in a mock.
- **Mutation-check both halves before claiming done.** Both of module 1's
  mutations are recorded in its spec.
- **Server changes go in this repo, web changes go in `../mediqweb`.** The web is
  not vendored in, and no module may reintroduce that question without it being a
  deliberate decision.

## The gate, stated plainly

`CONSTRAINTS.md` covers `:app` and `:server`. The web is a separate repo and
therefore **outside the enforced gate**: neither check script nor either Gradle
test task sees it, and no number in `CONSTRAINTS.md` moves when the web changes.
The web's own gate is `npm test` and `npx ng build`, run by hand, per module.
`CONSTRAINTS.md` records this under "Not enforced, and why" so the gap is not
mistaken for coverage. Widening the gate to include the web is a deliberate
future change, not an oversight.
