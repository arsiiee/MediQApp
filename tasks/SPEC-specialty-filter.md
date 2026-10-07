# Spec: Doctor List Specialty Filter

> **Approved 2026-10-07, queued behind v3.** Spec only — no implementation yet.
> The plan and task list land when v3 closes. See `tasks/todo.md` "Plan history".

## Objective

`DoctorsScreen.kt:89` hardcodes `Specialty.PEDIATRICS` as the result of tapping a
chip labelled "Filter by specialty". The filter is therefore a lie: the label
offers a choice, the behaviour picks one arbitrary answer, and the chip's selected
state shows a generic filter is on while the list silently shows only
paediatricians. There is no way to pick cardiology, and no way back to "all"
except tapping the same chip.

This replaces the single fake chip with a real specialty picker driven by the same
`DoctorQuery.specialty` the server already filters on (`DoctorStore.kt:47-49`) and
the app already sends (`RetrofitDoctorRepository.kt:34`).

**What it proves when done:** tapping the chip labelled "Dermatology" sends
`specialty=dermatology` and nothing else. **What it does not:** the second chip,
"Filter by clinic location", stays disabled — see Non-goals.

### Acceptance criteria

1. The filter area offers a chip per `Specialty.entries` value, plus an "All" chip
   that clears the filter. No specialty is named in a `ui/` Kotlin literal beyond
   the enum itself.
2. Tapping specialty X sends `DoctorQuery(specialty = X)`. **Not** pediatrics.
   This is the criterion that fails today.
3. Tapping "All" sends `DoctorQuery(specialty = null)` and does not re-send a
   search text that was not typed.
4. Exactly one chip reads as selected, and it is the one the user tapped.
5. Tapping the already-selected specialty clears it (toggle), so there is a route
   back to the unfiltered list without an "All" chip being the only exit.
6. A specialty the server has no doctors for yields the existing empty state, not
   an error.
7. `:app:testDebugUnitTest` green; `:server:test` still green at 50;
   `check-boundaries.ps1` `ui-imports-data` = 0; `check-contrast.ps1` raw-colour
   ratchet stays 4.

## ASSUMPTIONS I'M MAKING — all confirmed by the user 2026-10-07

1. **The picker lists `Specialty.entries` — the enum, not a server-fetched list.**
   The enum is contract, not content: `ReferenceData.kt:29-38` seeds the
   `specialties` table from `Specialty.entries` at every boot, so the app's list
   and the server's table cannot disagree. A `GET /specialties` route would be a
   new endpoint, new DTO, new repository method, and a loading state for a list
   that is already guaranteed to match.
2. **`Specialty` stays the source for the *labels* too** (`displayName`), so a chip
   reads "Dermatology", not `DERMATOLOGY`.
3. **The ViewModel's public API does not change shape.** `onSpecialtySelected(Specialty?)`
   stays; only the chip that calls it changes. `DoctorsViewModelTest` has four
   tests already pinning that method, and they should keep passing untouched.
4. **Toggle-off stays.** Criterion 5 preserves today's behaviour rather than
   replacing it with "All" alone — both, actually.
5. **No new dependency, no new file.** `FlowRow` is already used in
   `SlotPicker.kt` and `DoctorDetailsScreen.kt` with the same
   `@OptIn(ExperimentalLayoutApi::class)`.
6. **"Filter by clinic location" stays disabled and unbuilt.** `DoctorQuery.building`
   and `GET /doctors?building=` both exist and work; this is the same bug class one
   chip over. Called out in Boundaries, not fixed here.

## Tech Stack

Unchanged. Kotlin 2.2, Compose BOM 2026.02, Material 3 `FilterChip` + `FlowRow`.
No new dependency, no new file.

## Commands

```powershell
.\.mdfiles\check-boundaries.ps1                       # ui-imports-data = 0, < 5s
.\.mdfiles\check-contrast.ps1                         # raw-colour ratchet stays 4, < 5s
.\gradlew.bat :app:compileDebugKotlin --console=plain
.\gradlew.bat :app:testDebugUnitTest --rerun-tasks    # measured 174 today
.\gradlew.bat :server:test --console=plain             # 50, unchanged
```

Counting needs `--rerun-tasks`; without it the task is UP-TO-DATE and the previous
XML reads as a skipped suite (`AGENTS.md:27-30`).

## Architecture Decisions

### D1. Chips come from `Specialty.entries`, so no specialty can be a Kotlin literal in `ui/`

```kotlin
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpecialtyFilterRow(
    selected: Specialty?,
    onSelect: (Specialty?) -> Unit,
) {
    val colors = LocalMediQColors.current
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text("All") },
        )
        Specialty.entries.forEach { specialty ->
            FilterChip(
                selected = selected == specialty,
                onClick = { onSelect(if (selected == specialty) null else specialty) },
                label = { Text(specialty.displayName) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = colors.accent.copy(alpha = 0.12f),
                    selectedLabelColor = colors.accent,
                ),
            )
        }
    }
}
```

The toggle expression is the whole behavioural change, and it is what makes
criterion 2 fail against today's code. `Specialty.entries` is the existing pattern
for iterating the enum — `ReferenceData.kt:29` does the same thing server-side, and
the app already knows how to resolve an enum from a wire value, so this adds no
contract risk.

The "All" chip is not decoration: without it the only way to clear is to remember
which chip is lit, and the list is 7 chips wide.

### D2. No server route, no repository change, no ViewModel change

The whole fix is inside `DoctorsScreen`. `RetrofitDoctorRepository.getDoctors`
already maps `query.specialty` to `?specialty=<wireValue>` (`:34`), and
`DoctorStore.kt:47-49` already applies `AND d.specialty_id = ?`. The bug is that
only ever one value was ever sent. That means:

- `ui-imports-data` cannot move — no new import crosses the boundary.
- The four existing `onSpecialtySelected` tests are unchanged and still pass, which
  is the evidence that the ViewModel contract was never the problem.
- There is nothing on the server to change, so `:server:test` stays at 50 and the
  demo seed is untouched.

### D3. The "no hardcoded specialty" guarantee is tested on the ViewModel, not the source

`CONSTRAINTS.md:116` records that no Compose test exists and
`ExampleInstrumentedTest` is still a template, so a test cannot assert on chip
layout. The guarantee is therefore asserted where it can be: the ViewModel receives
exactly the specialty the caller passed and forwards it verbatim.

The composable is left unguarded, and that is stated rather than hidden — the
residual risk is someone reintroducing a literal in `ui/`, which
`check-boundaries.ps1` will *not* catch because it is not an import. See Risks.

## Project Structure

Two files, both edits:

```
app/src/main/java/com/example/mediq/ui/feature/doctors/
  DoctorsScreen.kt      — replace lines 83-100 (the two fake chips) with SpecialtyFilterRow
  DoctorsViewModel.kt   — UNCHANGED

app/src/test/java/com/example/mediq/ui/feature/doctors/
  DoctorsViewModelTest.kt  — +N tests in the "Filters are not debounced" section
```

No new file. If the new tests land, `DoctorsViewModelTest` goes 22 → 22+N and the
`:app` floor moves by N in the same commit.

## Code Style

Existing patterns, nothing new:

- `LoadState<T>` unchanged; the filter reads `state.selectedSpecialty` off the
  same state flow.
- `MaterialTheme.colorScheme.*` and `LocalMediQColors.current.*` only — **zero new
  raw colour literals**, so the ratchet stays 4.
- `FlowRow` + `@OptIn(ExperimentalLayoutApi::class)`, copied from `SlotPicker.kt:52`
  and `:96`.
- KDoc explains *why*, per `AGENTS.md:213` — specifically why the chip list is the
  enum and not a fetched list.

## Testing Strategy

JUnit 4 + the hand-written `FakeDoctorRepository`, no mocking framework. **This
file keeps its own `StandardTestDispatcher`** — it does not use
`MainDispatcherRule`, and that is not changing. The new tests do not assert on
timing, but putting them in this file rather than a new one keeps one owner for the
debounce dispatcher.

| Test | Asserts | Fails without it |
|---|---|---|
| Each specialty reaches the repository verbatim | `onSpecialtySelected` for all six `Specialty.entries` produces six queries whose `specialty` matches, in order | the pediatrics literal, if it were in the ViewModel |
| The chosen specialty reaches the wire field | the request query is `DoctorQuery(specialty = <that one>)` and no other field is set | a query carrying a second filter by accident |
| Clearing sends null, not pediatrics | `onSpecialtySelected(null)` → `specialty == null` | "All" quietly meaning "the default" |
| Toggling off then on re-queries each time | `PEDIATRICS, null, CARDIOLOGY` → three queries, three values | a toggle that only updates the chip row |
| A specialty with no doctors is a `Success`, not an error | fake returns empty for `DERMATOLOGY` → `LoadState.Success(emptyList())` | a 404-shaped empty read becoming an error state |
| A specialty filter and a search compose | search `"Rivera"` + `CARDIOLOGY` → one query with both | the filter clobbering the text |

**Mutation check, required:** change the forwarding line to
`DoctorQuery(specialty = Specialty.PEDIATRICS)` — the exact bug — and confirm at
least four of these fail. A test never seen failing is not evidence;
`CONSTRAINTS.md:95` holds that line for the existing 174.

**Ratchet:** `:app` floor 174 → 174+N. `:server` stays 50. Move the number in
`CONSTRAINTS.md:37` and `:95` in the same commit, per *"Move a number only when the
code moves it."* The user confirmed the floor moves to the measured count, which
requires committing `SeededDataViewModelTest` (v3 Task 4) so a floor of 174 does not
fail CI on a fresh checkout.

## Boundaries

- **Always:** run all five gate commands before declaring done. Update
  `CONSTRAINTS.md`, `README.md`, and `.mdfiles/AGENTS.md` in the same commit.
- **Never:** put a specialty name in a `ui/` string literal — `"Pediatrics"` in a
  label, not `Specialty.PEDIATRICS` in code. The chip list is `Specialty.entries`.
- **Never:** reintroduce a default when `selectedSpecialty` is null. Null means
  *no filter*, which is what "All" sends.
- **Never:** add `TODO(`, `@Suppress`, or a `// remove later` comment. All three
  are at zero and growth is a failure (`CONSTRAINTS.md:17-20`, `:96-97`).
- **Never:** enable `buildConfig`, add a dependency, or touch `DemoData.kt`. None of
  them are needed.
- **Ask first:** adding `GET /specialties` (assumption 1 — the bigger design, not
  this fix). Building the location filter (assumption 6 — same bug class,
  `DoctorQuery.building` already works). Any change to
  `RetrofitDoctorRepository`'s unreachable→empty mapping.

## Success Criteria

1. Tapping "Dermatology" sends `?specialty=dermatology` and shows only
   dermatologists. **Verified on the emulator against a seeded server**, not
   inferred from the code.
2. Tapping "All" returns to the unfiltered list; tapping a lit chip clears it.
3. Only one chip is ever lit.
4. `check-boundaries.ps1` reports `ui-imports-data` = 0; `check-contrast.ps1` green
   with the raw-colour ratchet at exactly 4.
5. `:app:testDebugUnitTest` green, floor moved to the measured count in the same
   commit; `:server:test` green at 50.
6. `README.md` "What works" line 83 gains the filter, and `AGENTS.md` Known gaps
   loses nothing that is still true.

## Risks

| Risk | Impact | Mitigation |
|---|---|---|
| The composable is untested — no Compose test exists | Someone reintroduces a literal filter in `ui/` and the suite stays green | Real. The ViewModel tests pin forwarding, not layout. `check-boundaries.ps1` will not catch it. **The honest fix is a Compose test, which is its own project** (`CONSTRAINTS.md:116`). Recommended: accept this gap for now, record it in `AGENTS.md` Known gaps. |
| A specialty with no doctors shows an empty list | Patient taps "Orthopedics", sees "No doctors yet" | Correct behaviour, and criterion 6 pins it as `Success(emptyList())` rather than an error. The seed has 3 of 6 specialties, so this is reachable in one tap. |
| 7 chips overflow a narrow screen | Ugly on a small device | `FlowRow` wraps rather than clipping, per `SlotPicker.kt:96`. |
| Ratchet collision with the seeded-data work | `CONSTRAINTS.md` floor gets moved twice | Both changes are landing; measure once and set the floor to the real number. `SPEC-seeded-data-screen.md:386-392` flagged this exact collision, and v3 Task 7 moves it to 174 first. |

## Open Questions — all three resolved by the user 2026-10-07

1. **Server-driven specialty list?** **No.** `GET /specialties` would be the
   "right" shape if the clinic ever adds a specialty without shipping an app
   update — worth it later, not for this fix. Enum now; a route only if a
   specialty is added server-side first.
2. **Location filter in the same change?** **No — separate change.** It is the
   identical bug on the adjacent chip and `DoctorQuery.building` already works end
   to end, but two chips in one diff means one reviewer has to hold two filter
   stories, and the location one has no server test behind it either.
3. **Should the filter row live in `core/designsystem`?** **No.** It is doctors-only
   for now. Move it when a second screen filters by specialty, not before.

## Related

- `DoctorsScreen.kt:89` — the line being removed
- `DoctorsViewModel.kt:57-61` — `onSpecialtySelected`, unchanged
- `RetrofitDoctorRepository.kt:34` — already maps the enum to the query param
- `DoctorStore.kt:47-49` — already filters server-side
- `Requests.kt:11-15` — `DoctorQuery`, both filters already modelled
- `Specialty.kt:10-17` — the enum the chips are built from
- `CONSTRAINTS.md:37`, `:95`, `:116` — the ratchets this moves