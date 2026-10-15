# Spec: UI Redesign to the Six Design Figures

> **Proposed 2026-10-15. Awaiting approval. No implementation yet.**
> This file is the capability map for the initiative. Per-module specs land one
> at a time as `tasks/SPEC-ui-<module-id>.md`, after the map is approved.
> See `tasks/todo.md` for where this slots against the two already-queued specs.

## Objective

The repo carries six screen designs as SVG at `docs/final-project/assets/`:
`ui-01-signin`, `ui-02-register-otp`, `ui-03-doctors`, `ui-04-doctor-slots`,
`ui-05-booking`, `ui-06-appointment`. Each is a 340x726 phone mockup with exact
positions, sizes, fill colours, copy, and a caption stating the behaviour it
depends on. They are the graded deliverable's figures.

**The app's screens do not match them.** `DoctorsScreen.kt:74` renders a plain
`OutlinedTextField` placeholder reading `"Search..."` while Fig. 3 specifies a
green app bar reading `Doctors` with a `12 results` count, a pill search field
with a clear button, and a horizontal chip row. There is no shared green app
bar, no chip row, no avatar circle, no info card, and no status chip anywhere
in `ui/`.

**This is a restyle, not new screens.** All six figures map onto screens that
already exist, are already wired to real data, and are already covered by 182
unit tests and all ten ViewModels:

| Figure | Screen | Existing file | Exist |
|---|---|---|---|
| 1 Sign in | Sign in | `ui/feature/auth/signin/SignInScreen.kt` | yes |
| 2 Register OTP | Registration step 2 | `ui/feature/auth/register/RegisterOTPScreen.kt` | yes |
| 3 Doctors | Doctors list | `ui/feature/doctors/DoctorsScreen.kt` | yes |
| 4 Doctor + slots | Doctor details + picker | `ui/feature/doctors/DoctorDetailsScreen.kt`, `ui/feature/booking/SlotPicker.kt` | yes |
| 5 Booking | Confirm booking | `ui/feature/booking/BookingFlowScreen.kt` | yes |
| 6 Appointment | Appointment detail | `ui/feature/appointments/AppointmentDetailsScreen.kt` | yes |

### The evidence that these figures are this app's design

Not assumed — three independent facts:

1. **The figures use this repo's *corrected* token values.** Fig. 6's status
   green is `#2E7D32`, which `Color.kt:79` records as `MediQSuccess`, *"Was
   #388E3C, which measured 4.12:1 and failed AA."* Fig. 6's warning text is
   `#B25E00`, which `Color.kt:82` records as `MediQWarning`, *"Was #FBC02D,
   which measured 1.66:1 and was unreadable."* Both figures use the **fixed**
   values, not the originals. A design drawn separately would not know which
   greens this repo had already rejected.
2. **The figures' content is the demo seed.** They show three doctors, `Dr. Ana
   Reyes` / Dermatology / `DEMO-PRC-0001`, which is exactly
   `MEDIQ_SEED_DEMO=true` (`AGENTS.md`: three invented doctors, `DEMO-*`
   licence numbers). The figures show sample rows for the only rows this repo
   can produce.
3. **`Doctor.kt` already holds every field the figures display** —
   `licenseNumber` for the PRC badge, `location.floor`/`room` for
   `Main Bldg · 3F · Rm 312`, `consultationFee` for `₱850.00`,
   `clinicHours` for the day chips. Its KDoc says the screen joins these:
   *"the screen decides how to join these into 'Main Building — 2F — Clinic
   204'"*. The model was built for these figures; the screens were never
   brought up to them.

### Acceptance criteria (patient-visible)

1. Each of the six screens, rendered on the emulator against a seeded server,
   is recognisably the figure: same app bar, same content order, same states,
   same copy.
2. A screen's meaning does not change. Every screen still renders all three
   `LoadState` cases, and no figure's static sample text reaches the app as a
   literal.
3. No screen is reachable only through a redesigned screen. A patient can open
   the app and complete sign-in → doctors → doctor details → book → appointment
   without touching a screen this initiative has not shipped.
4. `check-contrast.ps1` is green with the raw-colour ratchet at 4 and every new
   token pair measured, not eyeballed.
5. No `:app` or `:server` test count falls; every new test is mutation-checked.

## Capability Map

Five modules. Every one is a vertical slice that can be accepted or rejected
alone, and every one ends with the full gate green.

| Module id | Responsibility | Figures | Depends on |
|---|---|---|---|
| `ui-foundation` | New tokens the figures need + the shared components they reuse: app bar, chip row, doctor card, avatar, primary/outline buttons, info card, status chip, slot states | 1-6 (all) | — |
| `ui-auth` | Sign-in and the registration OTP step | 1, 2 | `ui-foundation` |
| `ui-doctors` | Doctors list: app bar count, search field, chips, result cards | 3 | `ui-foundation` |
| `ui-scheduling` | Doctor details, slot picker, confirm booking | 4, 5 | `ui-foundation` |
| `ui-appointment` | Appointment detail: status chip, detail grid, actions, warning card | 6 | `ui-foundation` |

**Build order: `ui-foundation` → `ui-auth` → `ui-doctors` → `ui-scheduling` →
`ui-appointment`.**

`ui-auth` is second rather than last despite being figure 1, because sign-in is
the only door into the other four screens. A regression there blocks on-device
verification of everything downstream, so it should be the first thing made
solid, not the last. The remaining three follow the patient's order through
the journey, which is the order their verification needs.

The dependency graph is flat: no module needs a sibling. The journey chains
(sign in to reach doctors), the code does not.

### Why `ui-foundation` is its own module and not a task inside `ui-doctors`

The shared components are needed by four modules, and the token work is a
prerequisite that the contrast gate must pass **before** any screen uses the
new colours. Folding it into `ui-doctors` would put a `Color.kt` change and a
`check-contrast.ps1` change inside a doctors-list diff, where a reviewer
judging the list would be judging the palette at the same time.

## The Palette: What This Actually Costs

The figures' palette is **almost entirely this repo's existing tokens**. No
re-theming is needed.

| Figure value | Used for | Maps to | Verdict |
|---|---|---|---|
| `#006B3C` | every app bar, primary button, selected day/slot | `MediQGreen` | exact |
| `#E8F5E9` | avatar circle, fee card, info card, step 1-2 badges | `MediQLightGreen` | exact |
| `#212121` | names, headings, values | `MediQTextPrimary` | exact |
| `#5F5F5F` | specialty, captions, labels | `MediQTextSecondary` | exact |
| `#8A8A8A` | input borders, chevrons, unselected chip borders | `MediQOutline` | exact |
| `#F5F5F5` | raised surface, day chips, OTP boxes | `MediQSurface` | exact |
| `#B00020` | error text and border | `MediQError` | exact |
| `#2E7D32` | `Confirmed` status | `MediQSuccess` | exact |
| `#B25E00` | build note / action-rule headings | `MediQWarning` | exact |
| `#0F5132` | initials and text on light green | no exact match; `MediQGreen` serves the role | decide in `ui-foundation` — `MediQGreen` on `MediQLightGreen` is already documented at 5.89:1, so this may need no new token at all |
| `#D7EFE0` | subtitle and small labels on brand green | **none** | new token |
| `#E5E7EB` | card border | **none** | new token |
| `#E0E0E0` | claimed-slot fill | **none** | new token |
| `#9CA3AF` | field placeholder text | **none** | new token |
| `#94A3B8` | inline hint text | **none** | new token |
| `#FFF8E6` / `#7A4A00` | warning container and its body | **none** | new tokens |
| `#FDECEF` | error banner container | **none** | new token |
| `#CBD5E1`, `#475569`, `#E5E7EB` (as figure chrome) | figure caption bar, slide separator | — | **not app UI** |

The last row matters: the caption bar and the light separator at the bottom of
each file are the *document's* furniture, not the screen. Nothing under
`docs/final-project` should be implemented.

### Three of the new greys fail WCAG AA as drawn

Measured with the WCAG 2.1 relative-luminance formula. **These are estimates to
be re-measured by `check-contrast.ps1`, not trusted:**

| As drawn | Where | Ratio | Problem |
|---|---|---|---|
| `#8A8A8A` on `#E0E0E0` | Fig. 4, claimed slot `11:30` | ~2.6:1 | fails 4.5:1 text, and fails the 3:1 non-text floor |
| `#9CA3AF` on `#FFFFFF` | Figs. 1, 5, placeholder text | ~2.8:1 | fails 4.5:1 |
| `#94A3B8` on `#FFFFFF` | Figs. 4, 5, 6, inline hint text | ~3.0:1 | fails 4.5:1 |

`MediQTextSecondary` (`#5F5F5F`) on `#E0E0E0` measures ~4.8:1 and passes. So
each of the three has a one-token fix, and it is a **visible deviation from the
figure** — the placeholder reads darker than drawn. This is the fork recorded
as Open Question 1: match the figure pixel-for-pixel, or keep the bar this repo
already enforces. Recommendation: keep the bar, and record the deviation.

Note the figures are otherwise *consistent* with the bar, which is why there
are only three. That is the same reason fact 1 above is strong evidence: whoever
drew these measured the greens.

### Consequence for `check-contrast.ps1`

`CONSTRAINTS.md:140` records that the raw-colour ratchet only matches
`Color.Gray|LightGray|DarkGray|White|Black` plus one hex, so *"an arbitrary
literal like `Color(0xFF00FF00)` passes it — proved by mutation"*. Every new
colour above is an arbitrary literal.

So the new tokens **must** be added to `Color.kt` with measured ratio comments
**and** added to what the script measures. Implementing the figures while
leaving the new colours unmeasured would take the known hole in the gate and
drive eleven tokens through it. `CONSTRAINTS.md` says widening the pattern "is
its own change, because it moves the bar and the floor at the same time" — this
**is** that change, and it belongs in `ui-foundation`.

What the script does **not** flag, read at `check-contrast.ps1:177-199`: the
second rule matches `.background(Color.White)` and `topAppBarColors(…,
containerColor = Color.White)` only, so a white *filled control* is deliberately
allowed. The figures' white pills — Fig. 6's `Confirmed` chip, the white
`CANCEL` / `RESCHEDULE` buttons — fall in that allowance and need no token. The
figures' white *cards* do not: they sit white on a white page separated by
nothing but a `#E5E7EB` hairline, so a card must be a bordered surface token,
not a `Color.White` background.

## What the Figures Require That the App Does Not Have

Surfaced here rather than discovered mid-build. Each needs a decision; none is
a restyle.

| # | Figure | Requirement | Today | Decision needed |
|---|---|---|---|---|
| 1 | 1 | `Forgot password?` link | no route, no endpoint, no screen | out of scope, or a new module |
| 2 | 2 | `Resend code in 0:42` countdown, `expires in 4:32` | `RegisterViewModel` has OTP but no countdown state | new UI state |
| 3 | 2 | 4-step stepper `Details / Verify / Login / Done` | four screens exist, no stepper | new shared component |
| 4 | 3 | `12 results` count | `Paged` items; pagination unused | `items.size`, or the server's total |
| 5 | 3 | `Next open: Tue 09:00 · 4 slots` **per row** | one doctors read, no availability per row | client-side from `clinicHours`, or N+1 reads |
| 6 | 4 | month stepper `‹ November 2026 ›` | documented gap: *"Reschedule searches the current month only … a month stepper is one change in two ViewModels"* | in scope, closes a known gap |
| 7 | 4 | two slots drawn green at once | `SlotPicker` selects one | figure is likely wrong; confirm |
| 8 | 5 | `Amount paid at the clinic ₱0.00` | no such field | static copy |
| 9 | 6 | `BOOKED ON 07 Oct 2026` | need to confirm `createdAt` exists on `Appointment` | DTO/server change if not |
| 10 | 3 | bottom bar reads `Visits` / `Alerts` | bar reads `Bookings` / `Messages` | IA rename, see Open Question 2 |
| 11 | 3, 4, 5, 6 | `₱850` in the list, `₱850.00` in details | `Money.format()` is whole pesos only | second formatter, or one format |

Item 5 is the one with an architectural cost. A per-row availability read is N+1
requests for a list; `Doctor.clinicHours` can already answer *`Next open: Tue
09:00`* with no request at all, but *`4 slots`* is a count of bookable slots for
that date, which only `getAvailability` knows. Recommendation: render the day
and time from `clinicHours` and **omit the count**, unless the availability read
is already in flight. Recorded as Open Question 3.

## Non-goals

- **Not** `enableEdgeToEdge()`. The mockups draw a white status bar with dark
  icons and start the green bar *below* it (`y=34` after a 30px status strip),
  which is what `Scaffold`'s insets already produce. `CONSTRAINTS.md:123`
  already scopes insets as its own task; the figures do not need it.
- **Not** a dark-mode palette. `CONSTRAINTS.md:124` records the pastel status
  containers as known debt, and this initiative **adds** two containers
  (`#FFF8E6`, `#FDECEF`). That enlarges the debt; it does not settle it. Stated
  so it is not discovered as a surprise.
- **Not** new endpoints, DTOs, or repository methods, except where item 9 forces
  one.
- **Not** `@Preview` functions. The app has **zero** today, and any preview of a
  doctor card would need an invented doctor — forbidden by *"No fake/invented
  data"* (`AGENTS.md`). Figures are verified on the emulator against the seeded
  server, which is this repo's existing pattern (*"Verified on the emulator
  against a seeded server, not inferred from the code"*).
- **Not** the seeded-data screen, the Home screen, the appointments list,
  Profile, Notifications, or the register steps 1/3/4 — **no figure covers
  them**. See Open Question 4.

## Commands

```powershell
# Gate, from the repo root, per CONSTRAINTS.md "combined task-end gate"
.\.mdfiles\check-boundaries.ps1
.\.mdfiles\check-contrast.ps1
.\gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest :server:test --console=plain

# Counts need --rerun-tasks, or the task is UP-TO-DATE and the previous XML reads
# as a skipped suite (AGENTS.md:32-34)
.\gradlew.bat :app:testDebugUnitTest --rerun-tasks

# On-device verification, which is how every figure is proved
$env:MEDIQ_SEED_DEMO="true"; $env:MEDIQ_PORT="8099"
.\gradlew.bat :server:run --console=plain        # second terminal
.\gradlew.bat :app:installDebug --console=plain  # AVD mediq_api36 attached
```

## Testing Strategy

JUnit 4 + hand-written fakes. No mocking framework, no Compose UI test beyond
the one that exists (`MediQBottomBarSemanticsTest`), because
`CONSTRAINTS.md:137` records that no Compose test harness exists and building
one is its own project.

That leaves a real gap this spec states rather than hides: **a restyle is
mostly composables, and composables are untestable here.** So each module's
proof is split, and both halves are required:

| Half | What it proves | How |
|---|---|---|
| Unit | The *meaning* did not change — every screen still maps `Loading` / `Success` / `Error`, and no new literal enters a state | ViewModel tests, the existing 182 as a floor |
| Gate | The *palette* is legal — every new token measured, ratchet still 4, no boundary crossed | `check-contrast.ps1`, `check-boundaries.ps1` |
| Device | The *screen* matches the figure | emulator + seeded server + the figure side by side |

The device half is the only thing that can catch a wrong spacing, and it is
manual. Every module's success criteria therefore names what to look at on
screen, not just a command.

**Mutation checks, required per module.** `CONSTRAINTS.md:95` holds this line
for the existing tests. Removing the accent from a state, or rendering a
`LoadState.Error` as empty, must fail the suite.

**Ratchets.** `:app` moves up by the tests each module adds, never down.
`check-contrast.ps1`'s 4 does not move. `CONSTRAINTS.md:37` and `:95` are
updated in the same commit, per *"Move a number only when the code moves it."*

## Boundaries

- **Always:** run both check scripts and the test gate before declaring a module
  done. Update `CONSTRAINTS.md` ratchets, `.mdfiles/AGENTS.md`, and `README.md`
  in the same commit — `AGENTS.md` has a standing rule that the orientation file
  changes with the repo.
- **Always:** add a new colour as a measured token in `Color.kt` **and** to what
  `check-contrast.ps1` measures. A token the script cannot see is not a token.
- **Never:** hardcode a colour in `ui/`. The ratchet only counts five named
  constants, so an unmeasured literal passes the gate — that is a hole to close,
  not a licence.
- **Never:** put a figure's sample content in the app. No `Dr. Ana Reyes`, no
  `DEMO-PRC-0001`, no `₱850`, no `12 results` as a literal. Every one of those is
  a row the server returns or it is a lie (`AGENTS.md`, *"No fake/invented
  data"*).
- **Never:** add `@Preview`, `@Suppress`, `TODO(`, or a skipped test. All are at
  zero and growth is a failure (`CONSTRAINTS.md:16-18`).
- **Never:** change a screen's behaviour to match a figure. A figure shows what a
  state looks like, not what the screen should do. Where the two disagree, the
  behaviour and its test win and the figure is wrong — item 7 is a live example.
- **Ask first:** adding a route or endpoint (item 1, `Forgot password?`); touching
  `schema.sql` or a DTO (item 9); renaming a bottom-nav label (item 10, breaks
  `MediQBottomBarLabelsTest`); widening the raw-colour pattern in
  `check-contrast.ps1` beyond the new tokens.

## Success Criteria

1. Each of the six screens is recognisably its figure on the emulator, verified
   against a seeded server, with the figure open beside it.
2. All five modules merged, each having ended with both check scripts green and
   the test gate green.
3. `:app:testDebugUnitTest` green at its new floor; `:server:test` green at 59;
   every new test mutation-checked.
4. `check-contrast.ps1` green, ratchet exactly 4, and every colour in the
   palette table above measured by the script rather than by eye.
5. `check-boundaries.ps1` reports `ui-imports-data` = 0 and
   `domain-imports-android` = 0 — the restyle bought no shortcut across a layer.
6. `README.md` and `.mdfiles/AGENTS.md` describe the new shared components and
   the closed month-stepper gap, in the same commit as the code.

## Risks

| Risk | Impact | Mitigation |
|---|---|---|
| The three failing greys are "fixed" by copying the figure | The app ships text below AA, in the exact place the gate exists to catch | Decision in Open Question 1; each is one token swap, measured, and the deviation is recorded rather than silent |
| `ui-foundation` grows into a design system rewrite | A prerequisite swallows the initiative | It adds only the eleven colours the figures use and only the components two or more figures share. Anything used once stays in its screen |
| A figure is wrong (item 7, two green slots) and the code follows it | A behaviour change smuggled in as a restyle | Boundaries: the figure loses. Open Question 5 |
| 182 existing tests pin current layouts indirectly | A restyle fails tests that were right | If a fix fails many tests by one, `AGENTS.md`'s rule applies: read it as tests pinned to an implementation detail, and do not loosen them |
| Manual device verification is skippable under time pressure | "Green gate" mistaken for "matches the figure" | Each module's success criteria names what to look at, so the manual half is a named deliverable |

## Open Questions

1. **Match the figures' three sub-AA greys, or keep the bar?**
   Recommendation: keep the bar. Use `MediQTextSecondary` for placeholders and
   hint text, and for the claimed-slot label, and record each deviation in
   `AGENTS.md`. The alternative — lightening to match — is the exact failure
   `check-contrast.ps1` was written for (`CONSTRAINTS.md:70-92`).
2. **Rename the bottom bar to the figure's wording?** Fig. 3's footer reads
   `Home / Doctors / Visits / Alerts / Profile`; the app reads `Home / Doctors /
   Bookings / Messages / Profile`. `Bookings` and its accessible name
   (*"Bookings, appointments"*) are deliberate and pinned by
   `MediQBottomBarLabelsTest` (6 cases). Recommendation: keep `Bookings` and
   `Messages`. Swapping a working tab for an `Alerts` tab the figure's footer
   implies is a navigation change, not a restyle.
3. **`Next open: … · 4 slots` per row (item 5)?** Recommendation: render the day
   and time from `Doctor.clinicHours` — no request — and drop the count, unless
   availability is already being read. A per-row availability call is N+1 reads
   on a list, for a number the list does not need.
4. **Do the six undesigned screens get the new shared components?** Home,
   the appointments list, Profile, Notifications, and register steps 1/3/4 have
   no figure. Recommendation: **yes for the app bar and buttons only**, in a
   final module `ui-consistency`, so the app does not read as two apps. But that
   is scope this spec does not include until it is asked for, and it would need
   its own acceptance criteria.
5. **Is the two-green-slot figure a mistake (item 7)?** Recommendation: treat as
   a mistake. One slot selected at a time, per `SlotPicker` and its tests.
6. **`₱850` in the list and `₱850.00` in details (item 11)?** Recommendation: two
   formatters on `Money` — `format()` unchanged for lists, `formatWithCentavos()`
   for the fee card and the detail grid. Money stays minor units
   (`AGENTS.md` Conventions).
7. **Does this land before or after `SPEC-specialty-filter.md`?** That spec is
   approved and queued, and it builds the chip row **inside `DoctorsScreen`**,
   which is the same chips Fig. 3 specifies. Recommendation: land the filter
   first — it is smaller, already approved, and its chips are the figure's
   chips. `ui-doctors` then restyles around a row that already works, instead of
   building a row and immediately rebuilding it.

## Related

- `docs/final-project/assets/ui-0{1..6}*.svg` — the six figures, **untracked in
  git today**. They are the design source for every module; until they are
  committed, a fresh checkout cannot read the spec's own references. Asked as a
  boundary, `Ask first`, but it should probably just be done.
- `app/src/main/java/com/example/mediq/core/designsystem/theme/Color.kt` — the
  tokens the palette maps onto
- `app/src/main/java/com/example/mediq/core/designsystem/theme/Type.kt` — the
  type scale the figures' font sizes map onto
- `.mdfiles/check-contrast.ps1:177` — the raw-colour pattern this initiative
  must widen
- `.mdfiles/CONSTRAINTS.md:116`, `:123`, `:124`, `:140` — the ratchets, the two
  known debts this touches, and the hole it must not drive tokens through
- `tasks/SPEC-specialty-filter.md` — overlapping work in `DoctorsScreen`, and
  the ordering fork in Open Question 7
- `tasks/SPEC-token-lifecycle.md` — unrelated despite the name; that token is an
  auth token. Both move the same `:app` floor, so they cannot land in parallel
