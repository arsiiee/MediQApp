# Spec: `ui-scheduling` — doctor details and slot picker, figures 4 and 5

> Module 4 of `tasks/SPEC-ui-redesign.md`. Parent spec approved. Its ViewModel
> half landed first, in `8c986b0` (the month stepper in `DoctorDetailsViewModel`
> and the `ui/format/MoneyFormat.kt` extraction). This spec covers what is left:
> the two screens, the shared picker they draw through, and the one display
> decision the figure forces.

## Objective

Make `DoctorDetailsScreen` recognisably figure 4
(`docs/final-project/assets/ui-04-doctor-slots.svg`) and `BookingFlowScreen`
recognisably figure 5 (`ui-05-booking.svg`) on the emulator against the seeded
server, with both still wired to their ViewModels and every value on screen
real data.

## Scope

In:

1. **Figure 4 — `DoctorDetailsScreen`.**
   - Green `MediQBrandAppBar` titled `Doctor details`.
   - Header: `MediQAvatar`, the name, `Specialty · N years experience`, and
     `PRC licence <number>` joined with the doctor's real `languages`.
   - Light-green fee card: `Consultation fee`, `₱850.00`
     (`Money.formatWithCentavos()`, parent spec item 11), and the clinic
     location on the right.
   - `Clinic hours` rendered as weekday chips.
   - `Choose a date` with the **month stepper** — this is the screen half of
     `8c986b0`, which put the state in the ViewModel and left it unrendered.
   - The date strip as day/number cells, with `openSlotCount` as the selected
     cell's `N open` caption (parent spec item 5, resolved below).
   - The slot grid in the three states `MediQSlotChip` already draws, plus the
     `Grey = already claimed…` legend.
   - The bottom action reading `Continue to booking`.
2. **Figure 5 — `BookingFlowScreen`.**
   - Green `MediQBrandAppBar` titled `Confirm booking`.
   - Header: avatar, name, specialty.
   - `DATE` / `TIME` / `LOCATION` rows through `MediQKeyValueGrid`, then a
     hairline and the `Consultation fee` / `Amount paid at the clinic` pair.
   - `Reason for visit (optional)` with its privacy hint, the short confirm
     tick, the error line, `Book appointment`, and the protection note.
   - The screen scrolls; today it does not, and the figure's content is taller
     than a phone.
3. **`SlotPicker` (shared with the reschedule picker)** takes the figure-4 date
   cell, the month stepper as one optional parameter, and
   `MediQSlotChip` for the grid.
4. **`BookingSelection.Selection` gains `endsAt`, `doctorInitials`, and the
   floor** — figure 5 draws a time *range*, an avatar, and a floor, and the
   selection currently carries none of the three.

Out (deliberate):

- **Figure 6 / `ui-appointment`.** `AppointmentDetailsViewModel` still filters
  to `isBookable`, so the claimed state reaches only this screen for now.
  `ui-appointment` decides whether to pass claimed slots through, and then owns
  the one test that pins the current filter.
- **The bottom bar keeps `Bookings` / `Messages`** (parent spec Open Question 2).
- **No new endpoint, DTO, or repository method.** Every value drawn below
  already exists on `Doctor`, `AvailableDate`, `TimeSlot`, or the selection.
- **`BookingSuccessScreen` has no figure** and is untouched.

## Decisions

### D1 — A claimed slot is drawn grey; a blocked one is not drawn at all

Figure 4 draws `11:30` and `01:30` greyed with the legend *"Grey = already
claimed by another patient"*. Today `DoctorDetailsScreen` filters the list to
`isBookable`, so nothing but bookable slots can reach the grid.

This is the parent spec's *"never change a screen's behaviour to match a figure"*
line, and it does **not** apply here, for a reason that is already in the merged
tree: `ui-foundation` shipped `MediQSlotChip` with an explicit
`MediQSlotState.Claimed`, and shipped `MediQClaimedSurface` with the contrast row
for its label, **for figures 4 and 6**. A picker that hides claimed slots makes
both of them dead code in this module.

What the *booking* can do does not change: a claimed chip is not clickable
(`MediQSlotChip` passes `onClick` only for `Available`), and `canContinue` still
requires a selected slot.

The split is on **truthfulness of the legend**, not on bookability:

| `SlotStatus` | Drawn? | As |
|---|---|---|
| `AVAILABLE` | yes | bookable, or selected |
| `RESERVED` | yes | `Claimed` — the legend's sentence is true |
| `BLOCKED` | **no** | the clinic blocked it; calling it "claimed by another patient" would be a lie |
| `UNKNOWN` | **no** | the app cannot read it (the enum's own KDoc: the safe answer is no) |

The filter moves into `DoctorDetailsUiState.visibleSlots`, so a test pins it —
the same reason `AppointmentDetailsViewModel` filters inside its state rather
than in the composable. The call-site filter in `DoctorDetailsScreen` goes.

### D2 — The month stepper belongs to the shared picker, as one optional parameter

Figure 4 draws `‹ November 2026 ›` on the `Choose a date` line. `SlotPicker` is
shared with figure 6's reschedule picker, which has no figure and no stepper
today, so the stepper is a nullable `MonthStepper(label, canGoBack, onPrevious,
onNext)` rather than four loose arguments. `null` renders no chevrons at all —
the reschedule picker's rendering does not move.

### D3 — Clinic hours are weekday chips plus the times that actually apply

Figure 4 draws seven weekday chips and one `09:00–12:00`. Real `clinicHours` is
one row per session, and the seed gives every open day *two* (`09:00–12:00` and
`14:00–17:00`), so a single range cannot be assumed:

- **Open days** render as chips with primary text, **closed days** as chips with
  secondary text — that is the figure's own dimming, and it is truthful: a day
  absent from `clinicHours` is a day the doctor does not see patients.
- **Times are drawn only when they are true for every open day.** If all open
  days share the same ranges (the seeded case), they are printed once, right-
  aligned under the chips, exactly as the figure draws them. If they differ, the
  screen falls back to one line per day, which is the only rendering that cannot
  claim a time belongs to a day it does not.

### D4 — The bio stays, though the figure does not draw it

`Doctor.bio` is readable nowhere else in the app. Dropping the `About` block to
match a mockup that omitted it would leave a real field unreadable. It sits
between the fee card and the clinic hours; everything the figure *does* draw
still appears in the figure's order.

### D5 — The selection carries the three display facts figure 5 needs

`BookingFlowScreen` never loads the doctor; `BookingSelection` is its whole
source. So:

- `endsAt` — figure 5's `TIME 09:00 – 09:30` is a range, and `startsAt` alone
  can only say `09:00`.
- `doctorInitials` — the avatar, taken from `doctor.initials` at the only place
  that has the `Doctor`. Parsing `"Dr. Ana Reyes"` back into initials on the
  booking screen would be re-deriving what the model already answered.
- `locationDisplay` gains the floor: it is built as `building · room` today,
  so figure 5's `Main Building · 3rd Floor · Room 312` is not renderable.

The one test fixture that builds a `Selection` gains the three fields with it;
no assertion changes.

## Acceptance criteria

1. On `mediq_api36` against the seeded server, doctor details draws figure 4 and
   confirm booking draws figure 5, each with the figure open beside it.
2. Every screen still maps all three `LoadState` cases, and no figure sample
   (`Dr. Ana Reyes`, `DEMO-PRC-0001`, `₱850`, `4 open`) appears as a literal.
3. The stepper is reachable: `‹` is disabled on the current month, `›` re-reads
   availability for the month now shown, and stepping clears the chosen date and
   slot (`8c986b0`'s tests, now exercised by something on screen).
4. A reserved slot renders as `Claimed` and is not selectable; a blocked or
   unknown-status slot does not render.
5. `check-boundaries.ps1` clean; `check-contrast.ps1` clean — **31 token pairs,
   6 status chips, ratchet 4**, unchanged: this module adds no token and no
   literal.
6. `:app:testDebugUnitTest` green at its new floor; `:server:test` green at 60.
   The new `visibleSlots` tests are mutation-checked.
7. `CHANGELOG.md`, `.mdfiles/AGENTS.md`, `.mdfiles/CONSTRAINTS.md`, `README.md`
   describe the change in the same commit, and `CONSTRAINTS.md`'s `:app` floor
   is **corrected from the stale 195 to the measured count** — the tree has held
   206 since `8c986b0` and the ratchet never moved with it.

## Risks

| Risk | Impact | Mitigation |
|---|---|---|
| The figure's clinic-hours block assumes one range per day | A wrong time printed beside a day | D3's uniform fast path, with the per-day fallback as the only non-uniform rendering |
| Showing claimed slots reads as a behaviour change smuggled into a restyle | A reviewer has to reconstruct why the picker shows more | D1 states the reason in the spec *and* in the ViewModel KDoc, and `visibleSlots` is test-pinned |
| The figure's copy is adopted verbatim and an error path loses its wording | A patient sees less than before | Only labels change; `e.message` and every written failure sentence are untouched |
| `Selection` grows three fields | The "one place to edit when the flow changes" accretes | They are display facts the confirmation screen cannot derive; the alternative is parsing a formatted name back into initials |

## Commands

```powershell
.\.mdfiles\check-boundaries.ps1
.\.mdfiles\check-contrast.ps1
.\gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest :server:test --console=plain
.\gradlew.bat :app:testDebugUnitTest --rerun-tasks     # count the floor from the XML

$env:MEDIQ_SEED_DEMO="true"; $env:MEDIQ_PORT="8099"
.\gradlew.bat :server:run --console=plain             # second terminal
.\gradlew.bat :app:installDebug --console=plain       # AVD mediq_api36 attached
```

## Related

- `tasks/SPEC-ui-redesign.md` — the capability map, and item 6 (month stepper),
  item 8 (`Amount paid at the clinic`), item 11 (`₱850.00`) which this module
  closes
- `tasks/SPEC-ui-foundation.md` — `MediQSlotChip`'s `Claimed` state and D2's
  sub-AA greys, which D1 and the legend above depend on
- `docs/final-project/assets/ui-04-doctor-slots.svg`, `ui-05-booking.svg`
- `app/src/main/java/com/example/mediq/ui/feature/booking/SlotPicker.kt` — the
  shared picker both this module and figure 6 draw through
