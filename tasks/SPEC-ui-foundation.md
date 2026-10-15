# Spec: ui-foundation — Design Tokens and the Shared Component Set

> **Proposed 2026-10-15. Awaiting approval. No implementation yet.**
> Module `ui-foundation` of `tasks/SPEC-ui-redesign.md`. The other four modules
> build on this one and cannot start until it is green.

## Objective

Deliver the colour tokens and shared components that Figures 1-6 are drawn
against, so four screen modules can apply one vocabulary instead of five.

Two things make this its own module rather than a task inside a screen:

1. **It is the gate's problem, not a screen's.** `check-contrast.ps1` measures
   the palette, and the palette must clear it *before* any screen uses the new
   colours. A `Color.kt` change and a `check-contrast.ps1` change belong in a
   diff where moving the bar is the subject, not buried in a doctors-list diff
   where a reviewer judging the list is judging the palette at the same time.
2. **The figures need nine colours the app does not have, and the app has
   colours the figures do not know about.** Reconciling them is design work.

### What the figures actually cost

Measured with the WCAG 2.1 relative-luminance formula. The method reproduces
this repo's own documented numbers exactly — `MediQTextSecondary` on white
`6.39`, `MediQOnBrand` on `MediQGreen` `6.63`, `MediQGreen` on `MediQLightGreen`
`5.89` — so the new numbers can be trusted to the same standard.

**Nine values the figures use already exist as tokens.** `#006B3C`,
`#E8F5E9`, `#212121`, `#5F5F5F`, `#8A8A8A`, `#F5F5F5`, `#B00020`, `#2E7D32`,
`#B25E00`. No re-theme. What remains splits three ways: three figure greys that
**fail AA and must not be adopted**, one figure pair that is a **duplicate of a
pair the app already has**, and nine values that are genuinely new.

### Acceptance criteria

1. Every colour in the tables below is a named token in
   `core/designsystem/theme/Color.kt`, and every pair is a row in
   `check-contrast.ps1` that passes at its stated minimum.
2. `ui/` contains **zero** `Color(0x…)` literals and no more than 4 raw colour
   literals — the same 4 `SplashScreen` has today.
3. The shared components exist, compile, and are used by at least one figure's
   screen only after this module lands. No screen renders differently because of
   this module: it is a pure refactor plus additions.
4. `check-contrast.ps1` prints a success line whose counts reflect the new rows,
   and exits 0.
5. `:app:testDebugUnitTest` stays at or above 182; `:server:test` stays at 59.

## Decisions

**D1 — Reuse before adding.** A figure value that equals an existing token is
not a new token. Nine are reuses; the status chips add four more (below).

**D2 — The three sub-AA figure greys are not adopted.** The figures draw
placeholder, hint, and claimed-slot text in greys that fail AA as drawn:

| As drawn | Where | Ratio | AA needs | Adopted instead | Ratio |
|---|---|---|---|---|---|
| `#8A8A8A` on `#E0E0E0` | Fig. 4, claimed slot `11:30` | 2.62:1 | 4.5 | `MediQTextSecondary` on `MediQClaimedSurface` | **4.84:1** |
| `#9CA3AF` on `#FFFFFF` | Figs. 1, 5, placeholders | 2.54:1 | 4.5 | `MediQTextSecondary` | **6.39:1** |
| `#94A3B8` on `#FFFFFF` | Figs. 4, 5, 6, hint lines | 2.56:1 | 4.5 | `MediQTextSecondary` | **6.39:1** |

The cost is real and stated: placeholders and hints render darker than the
figure draws them. Recorded in `AGENTS.md` rather than left for someone to
"fix" back.

One nuance worth keeping: the claimed slot's `#E0E0E0` fill against white is
only **1.32:1**, below the 3:1 non-text floor. WCAG 1.4.11 exempts *inactive*
components, and a claimed slot cannot be selected, so the fill is legal as
drawn. The label inside it is text and is not exempt — which is the 2.62:1
above. Fix the text; leave the fill, as the figure has it.

**D3 — The figures' warning pair duplicates one the app already has.** Fig. 2's
build note and Fig. 6's action-rule card use `#FFF8E6` with `#7A4A00` body and
`#B25E00` heading. `AppointmentsScreen.kt:219` already carries
`#FFF8E1` / `#8F5000` for the pending chip. The two are interchangeable:

- `#7A4A00` on `#FFF8E1` = **7.04:1**
- `#8F5000` on `#FFF8E6` = **5.98:1**

So there is one warning pair, not two. Adopt the existing one
(`MediQWarningContainer` = `#FFF8E1`, `MediQWarningOnContainer` = `#8F5000`,
**5.96:1**) and use it for the chip and the cards both.

**The figure's heading colour is the trap here.** `#B25E00` is `MediQWarning`,
and on the warning container it measures **4.41:1 — it fails.** `MediQWarning`
stays what it is: warning text on the *page* background (4.67:1 on white, an
existing row). It must never be the text *on* a warning container. The
container's text role is `MediQWarningOnContainer`.

**D4 — One error pair, not two.** Fig. 1's banner is `#B00020` on `#FDECEF`
(**6.43:1**); the cancelled chip is `#C62828` on `#FFEBEE` (**4.92:1**). Unify
on the figure's, because it reuses the existing `MediQError` token, scores
higher, and makes the cancelled chip agree with the error banner. The cancelled
chip's appearance shifts slightly; that is the visible cost.

**D5 — Fixed pastels stay fixed.** The status containers do not change with the
mode today, and `CONSTRAINTS.md:124` records that as known debt. Promoting them
to tokens **preserves** that behaviour. This module does not fix it and does not
claim to: a dark status palette is a design decision, explicitly out of scope in
`SPEC-ui-redesign.md`. It stays a "Known debt" row.

**D6 — Where each role lives.** Mode-varying roles go through the theme
(`MediQExtendedColors`, or the `DarkColorScheme`/`LightColorScheme` blocks), as
everything else does. Mode-fixed brand and status colours are top-level `val`s
in `Color.kt`, which is how `MediQGreen` and `MediQOnBrand` already work.
`MediQExtendedColors` gains exactly one field: `claimedSurface`.

Two Material slots are currently **unset** and therefore silently fall back to
the Material baseline — `Theme.kt`'s own comment notes that anything left unset
"arrives as baseline *purple*". `errorContainer` / `onErrorContainer` are among
them. They get set here to `MediQErrorContainer` / `MediQError`, so a Material
component that reads them is brand-correct rather than baseline-red.

## New Tokens

| Token | Value | Pair measured | Ratio | Min |
|---|---|---|---|---|
| `MediQOnBrandMuted` | `#D7EFE0` | on `MediQGreen` | **5.46:1** | 4.5 |
| `MediQOnBrandMuted` | `#D7EFE0` | on `MediQDarkGreen` / `MediQContainerDark` | **8.25:1** | 4.5 |
| `MediQClaimedSurface` (light) | `#E0E0E0` | `MediQTextSecondary` on it | **4.84:1** | 4.5 |
| `MediQClaimedSurface` (dark) | reuses `MediQSurfaceVariantDark` `#2A2A2A` | `MediQTextSecondaryDark` on it | **5.36:1** | 4.5 |
| `MediQWarningContainer` | `#FFF8E1` | — container | — | — |
| `MediQWarningOnContainer` | `#8F5000` | on `MediQWarningContainer` | **5.96:1** | 4.5 |
| `MediQErrorContainer` | `#FDECEF` | — container; also Material `errorContainer` | — | — |
| `MediQInfoContainer` | `#E3F2FD` | — container | — | — |
| `MediQInfoContent` | `#1565C0` | on `MediQInfoContainer` | **5.03:1** | 4.5 |
| `MediQNeutralContainer` | `#EEEEEE` | — container | — | — |
| `MediQNeutralContent` | `#616161` | on `MediQNeutralContainer` | **5.34:1** | 4.5 |

`MediQOnBrandMuted` is one value for both brand surfaces, so it needs no dark
sibling — 5.46:1 on `MediQGreen` and 8.25:1 on `MediQDarkGreen`.

### The status chips: 12 literals become 6 token pairs

`AppointmentsScreen.kt:218-225` holds twelve raw `Color(0x…)` literals. They are
the only reason `ui/` has any hex literals at all, and
`CONSTRAINTS.md:140` records that the ratchet does not see them because it
matches five named constants plus one hardcoded hex.

| Status | Today (literal) | Ratio | Becomes |
|---|---|---|---|
| `CONFIRMED` | `#E8F5E9` / `#2E7D32` | 4.56:1 | `MediQLightGreen` / `MediQSuccess` — **already tokens, no new ones** |
| `PENDING_CONFIRMATION` | `#FFF8E1` / `#8F5000` | 5.96:1 | `MediQWarningContainer` / `MediQWarningOnContainer` |
| `COMPLETED` | `#E3F2FD` / `#1565C0` | 5.03:1 | `MediQInfoContainer` / `MediQInfoContent` |
| `CANCELLED`, `DECLINED` | `#FFEBEE` / `#C62828` | 4.92:1 | `MediQErrorContainer` / `MediQError` — **improves to 6.43:1** |
| `UNKNOWN` | `#EEEEEE` / `#616161` | 5.34:1 | `MediQNeutralContainer` / `MediQNeutralContent` |

`CONFIRMED` is the tightest chip at 4.56:1 and it is already two tokens, so the
mapping is a pure rename there. The fourth new container is shared with the
error banner (D4), which is why cancellation gets four containers' worth of
value out of three new tokens' cost.

`NotificationsScreen.kt:122`'s `Color(0xFFF5F5F5)` is **exactly**
`MediQSurface` (`Color.kt:51`), so it converts with no visual change, and it is
the last hex literal outside `AppointmentsScreen.kt`.

## The Gate Change

`check-contrast.ps1` has to move with the code, in this same change, and the
move is a **widening** — the floor the ratchet states does not move.

| | Before | After |
|---|---|---|
| Ratchet pattern (`:177`) | `Color.(Gray\|LightGray\|DarkGray\|White\|Black)` + `Color(0xFFD32F2F)` | the same five names **+ every** `Color(0x[0-9A-Fa-f]{8})` |
| Ratchet number (`:175`) | 4 | **4** — `SplashScreen`'s `Color.White` sites, unchanged |
| Hex literals in `ui/` | 13, invisible to the ratchet | **0** |
| Status chips (`:152-153`) | 12 literals read from `AppointmentsScreen.kt` by regex | 6 token pairs read from `MediQStatusChip.kt`, resolved with the existing `Get-Token` |
| Token pair rows (`:113`) | 24 | 24 + 7 new rows = 31 |
| Success line (`:207`) | `check-contrast: clean (24 token pairs, 6 status chips, raw-colour ratchet 4)` — the measured baseline, 2026-10-15 | `clean (31 token pairs, 6 status chips, raw-colour ratchet 4)` |

The 7 new rows are the pairs in the token tables above that nothing measures
today: `MediQOnBrandMuted` on each brand surface, the claimed label, and the
warning, error, info, and neutral content-on-container pairs. The claimed
label's *dark* pairing needs no new row — `dark secondary on raised`
(`:124`) is already `MediQTextSecondaryDark` on `MediQSurfaceVariantDark`, which
is the same pair at 5.36:1.

Widening the pattern **before** converting the literals would fail the gate at
17. Converting them **before** widening leaves the hole. They land together, and
the diff shows both halves.

**Section 2 fails loudly rather than silently.** If the chip pairs move out of
`AppointmentsScreen.kt` without the regex being repointed, the script adds
`chips      no status chip pairs found in AppointmentsScreen.kt` and exits 1.
The coupling is annoying and it is also the reason this cannot go wrong quietly
— better than a rule nobody checks, which is the whole premise of
`CONSTRAINTS.md`.

### Documentation this module must update, in the same commit

- `CONSTRAINTS.md:140` — the "ratchet is narrower than it reads" row. It is
  **less** narrow afterwards, and the honest replacement claim is narrower in
  three different ways: the pattern covers named constants and `Color(0x…)` but
  **not** `Color(red =, green =, blue =)`, not indirect construction, and not
  `.copy(alpha = …)` — which is how `SplashScreen.kt:71` attenuates a token. It
  also still misses a literal on a line that is *entirely* a comment.
- `CONSTRAINTS.md:116` — the ratchet row's note, to record that the pattern
  widened while the number stayed 4, and why.
- `CONSTRAINTS.md:124` / `:125` — the pastel debt row stays open (D5), and the
  six-warning row changes if the shared text field absorbs the 5 deprecated
  `KeyboardOptions` call sites.
- `.mdfiles/AGENTS.md` — the "Colour literals are still spread across `ui/`"
  bullet under Known gaps, and the Conventions bullet for masked fields.
- `README.md` — only if it makes a claim this touches.

## Shared Components

`core/designsystem/component/`, one file per component, mirroring
`MediQBottomBar.kt` and `EmptyState.kt`. Only components **two or more figures**
need live here; anything a single figure uses stays in that figure's module.

| Component | Figures | Notes |
|---|---|---|
| `MediQBrandAppBar` | 1, 2, 3, 4, 5, 6 | The green bar. Title, optional count/subtitle, optional back (use `Icons.AutoMirrored.Filled.ArrowBack` — the non-mirrored one is deprecated and warns), optional trailing content for Fig. 6's status chip. |
| `MediQBrandTextField` | 1, 2, 3, 5 | Label above, placeholder role, leading/trailing icon (Fig. 3's clear button). **This is where `keyboardOptions` centralises.** Five deprecated call sites (`RegisterCredentialsScreen.kt:91,115,143`, `SignInScreen.kt:107,134`) collapse into one place, which is how the recorded 5 of the 6 warnings get fixed once rather than five times. Boundary rule 4 still applies to whatever it renders. |
| `MediQButton` | 1, 2, 4, 5 (primary), 6 (outline) | One component, `variant = Primary \| Outline`. Splitting a button pair across two modules is worse than including both. |
| `MediQAvatar` | 3, 4 | Circle, initials, tinted container with the on-tint content role. |
| `MediQInfoCard` | 2, 5, 6 | Bordered surface; optional tint variant for the warning/error/info containers. **A card is a bordered surface, never `Color.White`** — the figures put white cards on a white page separated only by a hairline (1.24:1, decorative, no floor). The border role is the existing `outlineVariant` (`#D9D9D9` light / `#49454F` dark), not a new token. |
| `MediQStatusChip` | 3, 4, 5, 6 | Consumes the chip tokens above and becomes the single source of the status mapping — the file `check-contrast.ps1` section 2 reads. |
| `MediQSlotChip` | 4, 6 (via `SlotPicker`) | Available / selected / claimed. Claimed's label is `MediQTextSecondary` per D2. Shared because `SlotPicker` is already shared between doctor details and the reschedule picker. |
| `MediQKeyValueGrid` | 4, 5, 6 | Label/value rows with `outlineVariant` hairlines. |
| `MediQDivider` | 3, 4, 5, 6 | One `outlineVariant` hairline. |

Deliberately **not** here: Fig. 3's filter chip row (one figure — and
`SPEC-specialty-filter.md` already builds it, so it belongs to `ui-doctors`) and
Fig. 2's 4-step indicator (one figure, four screens — belongs to `ui-auth`).

## Staging

Two commits, each ending with the full gate green:

1. **Tokens + gate.** `Color.kt`, `Theme.kt`, `check-contrast.ps1`, the chip
   conversion in `AppointmentsScreen.kt` and `NotificationsScreen.kt`, and the
   four doc updates. **One deliberate visual change:** the CANCELLED and DECLINED
   chips move from `#C62828` on `#FFEBEE` (4.92:1) to `MediQErrorContainer`/
   `MediQError`, which measures 6.43:1 (decision D4). Every other chip is
   pixel-identical — the literals became tokens carrying the same values — and
   `NotificationsScreen.kt`'s `Color(0xFFF5F5F5)` is exactly `MediQSurface`, so
   the only row that should look different is a cancelled or declined one.
2. **Components.** The nine component files. Still zero visual change until a
   screen adopts them, which is the next module.

## Commands

```powershell
# Gate, from the repo root
.\.mdfiles\check-boundaries.ps1
.\.mdfiles\check-contrast.ps1
.\gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest :server:test --console=plain

# The counts the gate prints, so they can be compared to the tables above
.\gradlew.bat :app:testDebugUnitTest --rerun-tasks
```

## Testing Strategy

**What is testable here, and what is not.** Tokens are constants and components
are composables; `CONSTRAINTS.md:137` records that no Compose test harness
exists beyond one bottom-bar test, and building one is its own project. So the
proofs split, and both halves are required:

| Claim | Proof |
|---|---|
| Every new pair clears AA | `check-contrast.ps1` — the rows in the tables above, as the script's own numbers |
| No hex literal survives in `ui/` | the widened ratchet at 4, with `ui/`'s hex count at 0 |
| The chip mapping is unchanged | the converted chips' ratios are the "Today" column above; a change in a ratio is a change in the mapping |
| Nothing renders differently | the appointments list and notifications screen on the emulator, against the seeded server, before and after |
| No boundary crossed | `check-boundaries.ps1`, all five counters at 0 |
| The test floors hold | 182 and 59 |

**No new unit tests are expected in commit 1**, and that is a claim the spec
makes rather than a gap it hides: the change is a rename of literals to
constants with identical values, and the only machine-checkable property is
contrast, which the script owns. If a test *is* added it must be mutation-checked
like every other one.

**Mutation checks required for commit 2**, since components are where a
regression could hide: a component that renders its container as `Color.White`
or hardcodes a status colour must fail the gate. Verified by doing it.

**Ratchet:** `:app` stays at or above 182. The contrast ratchet stays at 4 while
the pattern that produces it widens — this is the one change in the module where
a number *does not* move and the bar does.

## Boundaries

- **Always:** add a colour as a token *and* as a measured row. A token the script
  cannot see is not a token — that is the whole reason this module exists.
- **Always:** do the doc updates in the same commit. `AGENTS.md` has a standing
  rule that the orientation file changes with the repo.
- **Never:** adopt a figure's grey that fails AA (D2). The figures are the
  design; they are not the accessibility bar.
- **Never:** use `MediQWarning` as text on a warning container (4.41:1), or put
  a fixed pastel container behind text without a measured row.
- **Never:** put a card's container as `Color.White`. Use the surface role and
  the `outlineVariant` hairline.
- **Never:** add a shared component only one figure uses. That is how a design
  system becomes a dumping ground, and it is why the chip row and the step
  indicator are not listed above.
- **Ask first:** changing a value in `CONSTRAINTS.md`'s ratchet table other than
  the two notes listed; removing the `color` parameter from any existing public
  composable another module already calls.

## Success Criteria

1. `check-contrast.ps1` exits 0 and prints the new counts; every pair in the
   tables above appears as a row in it.
2. `ui/` has 0 hex literals and exactly 4 raw-colour literals (`SplashScreen`).
3. All nine components compile and are reachable from
   `core/designsystem/component/`.
4. `:app:testDebugUnitTest` ≥ 182, `:server:test` = 59, `check-boundaries.ps1`
   all-zero.
5. The appointments list and the notifications screen are pixel-identical to
   before, confirmed on the emulator against a seeded server.
6. The four doc updates are in the same commit as the code.

## Risks

| Risk | Impact | Mitigation |
|---|---|---|
| A literal is hidden by commenting it out | The widened pattern reads stronger than it is | `check-contrast.ps1:178` filters out any line whose whole line starts with `^\s*//`, so `// Color(0xFF00FF00)` is invisible to it. That hole predates this module, is not closed by it, and a narrowing claim must not imply otherwise — `CONSTRAINTS.md:140`'s replacement wording has to say so |
| The chip regex is repointed and then quietly stops matching | Six chip pairs go unmeasured while the script prints clean | The `count -eq 0` guard makes "found none" a violation, so it fails loudly; test it by moving the file |
| `MediQClaimedSurface` is added but a screen reaches for the figure's `#E0E0E0` directly | The literal re-enters `ui/` | The widened ratchet catches it — the second reason to widen in the same change |
| Nine components is more than the four screen modules need | Unused code ships, and `:app` grows with no test movement | Each component names the figures that need it in the table above; a component with no adopter by the end of `ui-appointment` is deleted, not kept "just in case" |
| The five deprecated `KeyboardOptions` sites are *not* fixed by centralising | The 6-warning debt stays and the claim "fixed once" is false | Either the shared field absorbs them in commit 2 and the row is updated, or the row is left alone and this spec does not claim it. Not both, and not silently |

## Open Questions

1. **Unify the cancelled chip onto the figure's red (D4)?** It raises the chip
   from 4.92:1 to 6.43:1 and deletes a literal red, at the cost of a small
   visible change to a chip that is already shipped and already correct.
   Recommendation: yes — one red, matching the error banner the figures show.
2. **Adopt the figure's `#7A4A00` or the app's `#8F5000` for warning text (D3)?**
   Both pass on either container (7.04 and 5.98). Recommendation: `#8F5000`, the
   existing value — one fewer change, and its ratio against the container the
   chip already uses is the one the gate already reports.
3. **Does the shared text field take the deprecated-constructor fix?** Includes
   it and closes 5 of the 6 recorded warnings, but adds a second concern to a
   restyle commit. Recommendation: yes, because centralising is the only reason
   the fix is cheap, and doing it later means touching five screens again.
4. **`MediQInfoContainer` / `MediQNeutralContainer` have no figure.** They exist
   because the chips need them to stop being literals. Confirm that promoting a
   colour no figure specifies is wanted, as opposed to leaving those two chips
   as documented literals. Recommendation: promote — a colour nothing measures
   is the failure this repo is organised against.

## Related

- `tasks/SPEC-ui-redesign.md` — the capability map this module belongs to
- `app/src/main/java/com/example/mediq/core/designsystem/theme/Color.kt` — 82
  lines, every value with a measured comment; this module is the only one that
  should edit it
- `app/src/main/java/com/example/mediq/core/designsystem/theme/Theme.kt:31-52,
  78-103` — `MediQExtendedColors` and the two schemes; `:78` and `:103` are the
  unset `errorContainer`/`onErrorContainer` slots, and `:78` is also where
  `outlineVariant` is defined
- `.mdfiles/check-contrast.ps1:113-146` (token rows), `:152-165` (status chips,
  the section this module repoints), `:175-186` (the ratchet this module widens)
- `.mdfiles/CONSTRAINTS.md:116`, `:124`, `:140` — the ratchet row, the pastel
  debt this module preserves, and the narrowness row it narrows differently
- `app/src/main/java/com/example/mediq/ui/feature/appointments/AppointmentsScreen.kt:218-225`
  — the twelve literals
- `app/src/main/java/com/example/mediq/ui/feature/notifications/NotificationsScreen.kt:122`
  — the thirteenth, which is `MediQSurface` spelled out
