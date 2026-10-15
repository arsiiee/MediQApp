# Spec: `ui-doctors` — the doctors list, figure 3

> Module 3 of `tasks/SPEC-ui-redesign.md`. Parent spec approved; this module is
> the doctors list restyle, and it restyles **around** the specialty filter that
> `SPEC-specialty-filter.md` already landed (parent spec Open Question 7).

## Objective

Make `DoctorsScreen` recognisably figure 3 (`docs/final-project/assets/ui-03-doctors.svg`)
on the emulator, with the screen still wired to `DoctorsViewModel` and the real
`DoctorRepository`. A restyle: no screen gains or loses a behaviour, and every
value shown is real data.

## Scope

In:

1. **Green app bar** — `MediQBrandAppBar` titled `Doctors`, with the result count
   in its `actions` slot. The count is `items.size` from the current
   `LoadState.Success` (parent spec item 4 — no server total while pagination is
   unused), and it is absent while loading, erroring, or empty.
2. **Pill search box** — `#F5F5F5` fill (`surface`), `#8A8A8A` border (`outline`),
   `rx=21`, leading magnifier, and a clear button that appears only when the field
   is non-empty. The clear button writes `""` through the existing
   `onSearchTextChange`, so the 300 ms debounce and the request behaviour are
   untouched.
3. **Doctor card** — avatar, name, `Specialty · N yrs`, the `PRC · <licence>`
   pill, fee and `consultation`, a hairline, the clinic location, and
   `Next open: <Day> <HH:mm>` when the doctor has clinic hours.
4. **The "Next open" line** — derived client-side from `Doctor.clinicHours`
   (parent spec item 5 and Open Question 3). The `· 4 slots` the figure also
   draws is **omitted**: that is a count of bookable slots for one date and only
   `getAvailability` knows it, so per-row it would be N+1 requests for a number
   the list does not need.

Out (deliberate):

- **The filter chip row is not touched here.** It landed in the specialty-filter
  change; this module only draws cards below it.
- **The bottom bar keeps `Bookings` / `Messages`**, not the figure's
  `Visits` / `Alerts` (parent spec Open Question 2 — renaming is a navigation
  change, and `MediQBottomBarLabelsTest` pins the current labels).
- **`Money.format()` stays whole pesos** (`₱850`, the figure's list form). The
  `₱850.00` form is `ui-scheduling` / `ui-appointment`'s
  `formatWithCentavos()` (parent spec Open Question 6).
- **No new endpoint, DTO, or repository method.**

## Decisions

### 1. `Next open` is domain logic, not a composable

`nextClinicSession(hours, now)` lives in `domain/model/ClinicTime.kt` beside the
zone helpers, not in the screen, so it can be tested without a device:

- measured from the **clinic zone**, not the phone's, so a phone set elsewhere
  does not pick a different day;
- a session that has **already opened today is next week's**, so a doctor
  mid-clinic is not offered as "next open" at a time already past;
- a session opening **exactly now** counts as today's;
- `null` when the doctor has no hours, which is why figure 3's third card has no
  `Next open` line at all.

`NextClinicSessionTest` (6 tests) pins each branch, including the
clinic-zone case (`2026-10-15T17:00Z` is Friday at the clinic but Thursday in
UTC).

### 2. The card is `background`, the pill is `surface`, and the fee is `accent`

Three token choices worth stating, because each is a trap:

- **Card fill is `MaterialTheme.colorScheme.background`** (`#FFFFFF` in light),
  not `surface`. In light mode `surface` is `#F5F5F5`, which is exactly the fill
  the `PRC` pill uses — a `surface` card would swallow its own badge. The figure
  draws a white card on a white page and separates them with the border.
- **The card border is `outlineVariant`** (`#D9D9D9`), the figure's `#E5E7EB`
  hairline role, not the `#8A8A8A` `outline` a *control* needs.
- **The fee and the spinner use `LocalMediQColors.current.accent`**, not
  `MediQGreen`. The brand green on `#121212` is ~1.9:1, so a `MediQGreen` fee is
  unreadable in dark mode — which is the whole reason `accent` is a separate role
  from `primary`.

### 3. The search box is its own composable, on purpose

`MediQBrandTextField` draws a label *above* its box because it is a form field.
The figure's search box has no label and is a pill, and its clear button is only
wanted when there is something to clear. Bending the shared field into a search
mode would give every form field a pill/clear switch it does not want, so the
search box is a small private composable instead. The chip row and the cards do
adopt the shared components (`MediQBrandAppBar`, `MediQAvatar`, `MediQDivider`).

### 4. `Next open` is read once per composition

`Instant.now()` is captured once per composition and truncated to the minute, so
rows cannot disagree about "now" by milliseconds. It is not a `StateFlow`: the
line is not live-updating, which the figure does not ask for and a list does not
need.

## Acceptance criteria

1. On `mediq_api36` against the seeded server, the doctors list draws the green
   app bar with a real count, the pill search box, the chip row, and the cards,
   with figure 3 open beside it.
2. The count equals the number of cards drawn, and disappears while the list is
   loading or empty.
3. A doctor with hours shows `Next open: <Day> <HH:mm>`; a doctor without hours
   shows no such line (figure 3's third card).
4. Typing debounces and searches as before — the clear button is the only new
   control and it only writes `""`.
5. `check-boundaries.ps1` clean; `check-contrast.ps1` clean at 31 pairs, 6 chips,
   ratchet 4.
6. `:app:testDebugUnitTest` green at **195** (189 + 6); `:server:test` green at 59.
   `NextClinicSessionTest` is mutation-checked.
7. `CHANGELOG.md`, `AGENTS.md`, `CONSTRAINTS.md`, `README.md` describe the change
   in the same commit.

## Risks

| Risk | Impact | Mitigation |
|---|---|---|
| `Next open` disagrees with the server's idea of the next slot | A row promises a day/time the picker disagrees with | The line says only *next open*, a property of `clinicHours`, which is what the server seeds slots from. It does not claim a bookable slot. |
| The count goes stale after a filter change | A wrong number beside the list | It is derived from the same `state.doctors` the list renders, so it cannot drift from what is drawn. |
| Card and page are both white in light mode | Cards read as one blob | `outlineVariant` border, the figure's own separation. |

## Notes

`:app` main grows by the screen rewrite and the domain helper; the floor moves to
195 tests and is recorded in `CONSTRAINTS.md` in this same change.
