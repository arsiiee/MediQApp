# Spec: `ui-appointment` — appointment detail, figure 6

> Module 5 of `tasks/SPEC-ui-redesign.md`. Parent spec approved; the first four
> modules are merged. This is the last one the capability map names.

## Objective

Make `AppointmentDetailsScreen` recognisably figure 6
(`docs/final-project/assets/ui-06-appointment.svg`) on the emulator against the
seeded server, still wired to `AppointmentDetailsViewModel`, with every value on
screen real data — and add the one field the figure needs that the contract does
not carry yet.

## Scope

In:

1. **Green `MediQBrandAppBar` titled `Appointment`**, with the status chip in its
   `actions` slot (figure draws the pill top-right of the green bar).
2. **Doctor card**: `MediQAvatar`, `displayName`, and
   `"<Specialty> · PRC <licence>"`, in a bordered card.
3. **The fact grid** through `MediQKeyValueGrid`: `DATE`, `TIME`, `LOCATION`,
   `FEE`, `STATUS`, `BOOKED ON`.
4. **Reason-for-visit card** on `surface`, with its label, the body in default
   content colour, and the figure's inline hint.
5. **Two stacked full-width actions**: `Request a reschedule` (green outline) and
   `Cancel this appointment` (red outline), both inside the existing
   `canCancelOrReschedule` gate.
6. **The amber warning card** (`MediQInfoCard(tint = Warning)`) and the
   cancelling-frees-the-slot hint beneath it.
7. **`createdAt` end to end** — parent spec item 9, the `BOOKED ON` row.

Out (deliberate):

- **No behaviour change.** The three `LoadState` cases, the reschedule picker and
  its slot rules, the `isActionable` gate, and every written failure sentence are
  untouched. Parent spec's "the figure loses" line applies.
- **No schema change.** `appointments.created_at` already exists
  (`schema.sql:85`, `NOT NULL`); only the contract gains the field.
- **No new token, no new literal, no gate edit.** This module adds no colour the
  palette table does not already map.

## Decisions

### D1 — `BOOKED ON` comes from `createdAt`, nullable all the way down

Parent spec item 9 asked whether `Appointment` carries a booking timestamp. It
did not; the column did. The path is `appointments.created_at` →
`AppointmentRow.createdAt` → domain `Appointment.createdAt: Instant? = null` →
`AppointmentDto.createdAt: String? = null` → the grid row.

Nullable on the client, for two reasons that are one reason: the field is new, so
a build talking to an older server sees an absent key, and Gson hands back `null`
for "absent" and "renamed" identically (`AGENTS.md`, the error-contract section).
Parsing is guarded, so a malformed value yields `null` rather than an exception
that blanks the screen. **The row is hidden when `createdAt` is null** — a `BOOKED
ON` of nothing is worse than no row.

The field defaults to `null`, so every existing fixture, fake, and server
constructor still compiles without edits.

### D2 — The status chip moves into the app bar, and stays the shared component

Figure 6 draws a **white** pill with `#2E7D32` text on the green bar.
`MediQStatusChip` draws a light container instead (`MediQLightGreen` for
`CONFIRMED`), and it is used as-is for two reasons:

- It is the single place the six status pairs are mapped, and
  `check-contrast.ps1:170` parses exactly six `X -> a to b` pairs out of that
  file. A second mapping for "on the brand bar" would take the count to seven and
  fail the gate, or fork the mapping into a screen where nothing measures it.
- Every container it can draw is light, so each pill carries its own measured
  pair onto the green bar with no new pair to measure.

**Recorded deviation:** the pill background reads `#E8F5E9` rather than
`#FFFFFF` in light mode. The body keeps the status as plain text in the `STATUS`
row — the figure's green text is `#2E7D32`, which is measured on the light
background only, so drawing it in dark mode would put an unmeasured pair on
screen.

### D3 — The figure's two columns collapse to one grid, as figure 5's did

Figure 6 lays `DATE / TIME / LOCATION` beside `FEE / STATUS / BOOKED ON`.
`ui-scheduling` already resolved the same geometry in figure 5 to one
`MediQKeyValueGrid` of label/value rows, because that component's contract is
"one row per pair, a screen adds a row by adding a pair". Two screens drawing the
same facts differently is worse than either one matching its mockup's column
count. The row order follows the figure: left column first, then the right.

### D4 — The warning card is drawn always

Figure 6 draws the buttons *and* the note that explains their absence. The note
is most useful exactly when the buttons are gone (cancelled, completed, declined
— "shows no action at all"), so it sits outside the `canCancelOrReschedule`
branch rather than inside it, and its copy explains the rule instead of pointing
at buttons that are not there.

### D5 — The two actions take two colours, both already measured

`MediQButton(Outline)` draws label and border in the mode-dependent accent
(measured light and dark, both modes). The cancel button is the same shape in
`MaterialTheme.colorScheme.error`, which the gate already measures as
`light error on raised` and `dark error on background` — so the red outline adds
no row and no literal. Order follows the figure: reschedule above cancel.

### D6 — The figure's `#94A3B8` hints render as `secondaryText`

The inline hint under the reason card and the cancelling-frees-the-slot line are
`#94A3B8` in the figure, which is one of the three sub-AA greys the parent spec
recorded as failing 4.5:1 and which no token holds. Figure 5 shipped its identical
hint as `LocalMediQColors.current.secondaryText`; this one does the same, and the
deviation stays recorded rather than silently darkening.

### D7 — Location prints as `building · floor · room`, verbatim

The screen currently renders `"${building} · Floor ${floor} · ${room}"`, which
with the seed's `floor = "2F"` reads **"Main Building · Floor 2F · Clinic 204"**.
The figure writes `Main Bldg · 3F · Rm 312` — the three fields joined, no words
inserted. The redundant prefix goes; the data does not change.

## Acceptance criteria

1. On `mediq_api36` against the seeded server, the detail screen draws figure 6
   with the figure open beside it.
2. All three `LoadState` cases still render, and no figure sample
   (`Dr. Ana Reyes`, `DEMO-PRC-0001`, `Itchy rash…`, `07 Oct 2026`) appears as a
   literal — every row is read off the appointment.
3. `BOOKED ON` shows the appointment's own `created_at`; with the field absent or
   malformed on the wire the row is hidden and the screen still renders (three
   client tests, through Gson rather than by constructing the DTO).
4. The two actions are stacked in the figure's order, appear only when
   `canCancelOrReschedule`, and the reschedule picker's behaviour is unchanged.
5. `check-boundaries.ps1` clean; `check-contrast.ps1` clean — **31 token pairs,
   6 status chips, ratchet 4**, unchanged: no token, no literal, no script edit.
6. `:app:testDebugUnitTest` green at **213** (210 + 3), `:server:test` green at
   **62** (60 + 2 — one per SELECT, since the list and the single read are
   separate queries and fixing only one is how this field ships half-wired),
   counted from the XML with `--rerun-tasks`.
7. **Both halves mutation-checked**: dropping the `createdAt` mapping in
   `ApiMappers.kt` fails the client suite; dropping `createdAt = row.createdAt`
   in `AppointmentStore.toAppointment` fails the server suite. Each restored and
   re-run green.
8. `.mdfiles/AGENTS.md`, `.mdfiles/CONSTRAINTS.md`, `.mdfiles/CHANGELOG.md`,
   `README.md`, and `tasks/todo.md` updated in the same commit, with the two
   ratchet floors moved to the measured counts.

## Risks

| Risk | Mitigation |
|---|---|
| The status chip in the app bar reads as a colour fork away from the figure | D2 states the reason in the spec and the deviation goes in `AGENTS.md`; the six-pair mapping is untouched, which is what the gate asserts |
| `createdAt` on a list response is a field no list screen reads | It is one column the query already has; adding a second query or a second DTO for one row is more contract than the row is worth |
| The always-visible warning card points at buttons that are gone | D4 — its copy describes the rule, not the buttons, and the figure draws it unconditionally |
| Nullable `createdAt` silently hides the row on a real booking | The client test covers absent *and* malformed; the server test covers the row reaching the DTO, so hiding only happens when the wire really lacks it |

## Commands

```powershell
.\.mdfiles\check-boundaries.ps1
.\.mdfiles\check-contrast.ps1
.\gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest :server:test --console=plain
.\gradlew.bat :app:testDebugUnitTest :server:test --rerun-tasks   # count the floors from the XML

$env:MEDIQ_SEED_DEMO="true"; $env:MEDIQ_PORT="8099"
.\gradlew.bat :server:run --console=plain             # second terminal
.\gradlew.bat :app:installDebug --console=plain       # AVD mediq_api36 attached
```

## Related

- `tasks/SPEC-ui-redesign.md` — the capability map, item 9 (`BOOKED ON`), and
  the Boundaries line that made this a decision to raise rather than a field to
  add quietly
- `tasks/SPEC-ui-scheduling.md` — the `MediQKeyValueGrid` precedent for D3, and
  `SlotPicker`, which the reschedule branch still draws through
- `docs/final-project/assets/ui-06-appointment.svg` — the figure
- `server/src/main/resources/schema.sql:85` — the column this reads
- `app/src/main/java/com/example/mediq/ui/feature/appointments/AppointmentDetailsViewModel.kt` —
  unchanged by this spec
