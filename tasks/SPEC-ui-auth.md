# Spec: `ui-auth` — Sign-in and the registration OTP step

> **Module 2 of 5** in `tasks/SPEC-ui-redesign.md`. Figures 1 and 2.
> Depends on `ui-foundation` (merged: `529e1d6` tokens, `456b1e6` components).

## Objective

Bring `SignInScreen.kt` (Fig. 1) and `RegisterOTPScreen.kt` (Fig. 2) up to the
figures: the same chrome, the same content order, the same copy, the same
states — without touching what either screen does.

## Decision carried in (user, 2026-10-15)

**Restyle only.** Figure 2 draws three things the app has no state for — the
`Resend code in 0:42` countdown, the `It expires in 4:32` timer, and the 4-step
stepper. All three are deferred; they are new behaviour, not a restyle, and the
parent spec's boundary is *"Never: change a screen's behaviour to match a
figure."* They stay recorded as items 2 and 3 in the parent spec.

## What each figure requires

### Fig. 1 — Sign in

Full-bleed green header (`MediQGreen`) carrying the wordmark and the tagline,
then a labelled username field, a labelled password field with a visibility
toggle, a server-written error banner, a pill primary button, a centred
`Forgot password?` link, a hairline, and `New to MediQ?` / `Create an account`.

### Fig. 2 — Registration, step 2

A green app bar with a back arrow, the number the code went to, a six-box code
display, a warning card for the development hint, and a pill primary button.

## No new tokens

Every colour in both figures is already a token, and several are *exactly* the
value the token holds — which is the parent spec's evidence that these figures
are this repo's design:

| Figure value | Token | Measured |
|---|---|---|
| `#006B3C` header, button, focus | `MediQGreen` | `MediQOnBrand` on it 6.63:1 |
| `#D7EFE0` tagline | `MediQOnBrandMuted` | 5.46:1 on `MediQGreen` |
| `#5F5F5F` labels, `New to MediQ?` | `MediQTextSecondary` | 6.39:1 on white |
| `#8A8A8A` field / box border | `MediQOutline` | 3.45:1 on white |
| `#F5F5F5` code box | `MediQSurface` | `MediQTextSecondary` on it 5.86:1 |
| `#E8F5E9` focused box | `MediQLightGreen` | `MediQGreen` on it 5.89:1 |
| `#FDECEF` / `#B00020` error banner | `MediQErrorContainer` / `MediQError` | 6.43:1 |
| `#FFF8E6` / `#B25E00` build note | **not adopted** | see below |

So `Color.kt` is unchanged, the token-pair count stays 31, and the ratchet
stays 4.

**The one value not adopted is Fig. 2's build-note card.** `#B25E00` on
`#FFF8E6` measures **4.24:1** and fails AA for normal text — the same defect
class the foundation already corrected for the status chips. The card uses the
foundation's measured warning pair instead (`MediQWarningOnContainer` on
`MediQWarningContainer`, 5.96:1).

## One existing element is remapped, not restyled

Fig. 2's build note is developer prose (`AuthService.returnCodeToCaller = true`).
Putting it on screen verbatim would show a patient an implementation detail, and
the parent spec forbids a figure's sample content becoming a literal. The card
therefore renders the screen's **existing** `devOtpHint` — the app's own copy,
real state, no new string.

## Component changes (three, each to avoid a regression)

The foundation components were written with no consumer. Adopting them as-is
would have **lost** behaviour, so each gains one parameter or role:

| Component | Change | Why — without it |
|---|---|---|
| `MediQBrandTextField` | `keyboardActions` parameter | the password field's `onDone = { signIn() }` would be dropped |
| `MediQButton` | `loading` parameter | `isLoading` would stop rendering — a `LoadState` lost |
| `MediQInfoCard` | tinted border and a tint-matched content colour | the error banner's `#B00020` border, and the measured pairs (`MediQError` on `MediQErrorContainer` etc.) would go unused while text fell back to `onSurface` |

## Copy

Follows the figure where the figure has copy: the button reads `Sign in`, the
link `Forgot password?`, and the register action `Create an account` under
`New to MediQ?`. No figure sample content is added — the username placeholder
stays the app's own `e.g. maria.santos`, and the error sentence still comes from
the server.

Fig. 2's `Five wrong attempts locks this number` is kept as a pre-warning
because the rule is real (`AuthService`, `MAX_OTP_ATTEMPTS`, five guesses) —
worded `Five wrong attempts locks this code.`, because the counter is on the
code row, not the number.

**Named deviations from Fig. 2, recorded rather than silently dropped:** the
countdown, the expiry timer, and the stepper are absent (deferred, above).

## Acceptance criteria

1. `SignInScreen` renders the green header, both labelled fields, the error
   banner when the server refuses, the pill button and its spinner, the
   `Forgot password?` link, and `New to MediQ?` / `Create an account`.
2. `RegisterOTPScreen` renders the app bar with a back arrow, the number, six
   boxes, the development hint as a warning card, and the pill button.
3. **No behaviour changes.** Sign-in still navigates to Home on success and
   clears the auth stack; the password field still fires `signIn()` on `Done`;
   the OTP screen still advances only on `RegisterStep.CREDENTIALS`; every
   screen still renders all three `LoadState` cases; `uiState.isLoading` still
   renders a spinner on both buttons.
4. Both check scripts green, ratchet still 4, token pairs still 31.
5. `:app` test count does not fall (185 floor).

## Verification, and what it cannot reach

- Gate + unit: the full task-end gate, counts with `--rerun-tasks`.
- **Device is not reached in this session.** The emulator and seeded server are
  the parent spec's required proof for "matches the figure", and the
  `Resend`/timer/stepper are absent anyway. The one construct that genuinely
  needs the device is the six-box OTP display: a transparent `BasicTextField`
  layered over six bordered boxes. Digit rendering and tap-to-focus are the risk.
  It must be looked at on `mediq_api36` before this module is called accepted.
