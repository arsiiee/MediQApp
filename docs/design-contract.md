# MediQ design contract

Scope: `app/src/main/java/com/example/mediq/ui` — Jetpack Compose, Material 3, min SDK 24.

This contract is the bar every UI change on `feature/frontend` clears. It was
derived from the code's own visual language, not invented on top of it.

## 1. Evidence

- **Design system in code:** `ui/theme/Color.kt`, `Type.kt`, `Spacing.kt`, `Shape.kt`.
- **Reference screens:** `screen.png`, `screen2.png`, `screen3.png` at the repository
  root. These could not be read by the tooling used for this work, so every decision
  below is derived from the implemented screens instead. **Assumption to verify:
  if those screenshots show a different visual direction, they win, and this contract
  is revised before further screen work lands.**
- **Usage census:** 128 raw `Color(0x…)` literals across ~40 distinct values, all of
  which resolve to a handful of intent clusters (border grey, scaffold background,
  error, warning, success, avatar tile). The token file consolidates those clusters.
- **Best existing file:** `ui/components/MediQBottomBar.kt` — token-driven, no literals,
  single-owner nav logic. It is the reference pattern for the rest.

## 2. Design tokens

| Layer | File | Rule |
| --- | --- | --- |
| Colour | `ui/theme/Color.kt` | Semantic tokens only. No `Color(0x…)` outside `ui/theme`. |
| Spacing | `ui/theme/Spacing.kt` | 4dp scale. Screen gutter is always `MediQSpacing.gutter`. |
| Radius | `ui/theme/Spacing.kt` | `sm` chips/inputs, `md` buttons, `lg` cards, `pill` badges. |
| Elevation | `ui/theme/Spacing.kt` | Cards are 2dp + a 1dp `MediQBorder`. Never more. |
| Type | `ui/theme/Type.kt` | `MaterialTheme.typography.*`. No inline `fontSize`. |
| Shape | `ui/theme/Shape.kt` | Wired into `MaterialTheme.shapes`. |

## 3. Screen jobs and states

| Screen | Job | Primary action | States required |
| --- | --- | --- | --- |
| SignIn | Get an existing patient in | Sign in | idle, invalid credentials, loading |
| Register (4 steps) | Create an account | Continue | idle, invalid field, OTP sent/expired, loading |
| Home | Orient: next appointment, then book | Book a consultation | loading, has-next-appointment, none, error |
| Doctors | Find a doctor | Open doctor details | loading, results, empty search, error |
| DoctorDetails | Decide on one doctor | Pick a slot | loading, available, no slots |
| BookingFlow | Confirm a slot | Confirm booking | idle, submitting, slot taken, error |
| BookingSuccess | Confirm it happened | View appointment | success, conflict |
| Appointments | Track bookings | Open appointment | loading, upcoming, past, empty, error |
| AppointmentDetails | Manage one booking | Reschedule / cancel | loading, confirmed, cancel-confirm, cancelled |
| Messages | Reach the secretary | Open conversation | loading, conversations, empty, error |
| SecretaryChat | Resolve a question | Send | loading, messages, sending, failed-send |
| Profile | Manage account and health data | Edit profile | loading, filled, error |
| EditProfile | Change account details | Save | idle, dirty, invalid, saving, saved |
| Notifications | Read updates | Open the related screen | loading, unread/read, empty |

Screens marked *loading / error / empty* are currently unimplemented. Adding them is
in scope; changing data sources to supply them is not.

## 4. Accessibility (WCAG 2.1 AA)

1. Every interactive element is at least `MediQSize.minTouchTarget` (48dp) on its
   touch target, even when the drawn control is 40dp.
2. Icon-only controls carry a `contentDescription` describing the action, not the icon
   ("Notifications", not "bell").
3. Status is never conveyed by colour alone — the status chip carries its text label,
   the notification indicator carries a description.
4. Body text contrast ≥ 4.5:1, large text ≥ 3:1 against its own background.
5. Text scales with the system font size; no fixed-height containers that clip at
   200% font scale.

## 5. Patterns to reject

- Two brand greens in one screen (`0xFF2D8650` next to `MediQGreen`). One primary token;
  the lighter value becomes `MediQHeaderControl`, used only for controls on the header.
- Shadow-heavy cards (the current 5dp). 2dp plus a border.
- Seven near-identical border greys. Two: `MediQBorder`, `MediQDivider`.
- Spinners for content loading. Skeletons that match the final layout.
- Purple/gradient "AI aesthetic", oversized uniform padding, stock card grids.
- Placeholder copy such as "Lorem ipsum" or bare "TBD".

## 6. Increment order

Each step ends with a green `:app:assembleDebug`, is revertable on its own, and does not
touch screens outside its slice.

1. **Foundation** — contract + token files + `MaterialTheme.shapes`. No call-site changes.
2. **HomeScreen** — reference implementation of the token system.
3. **MediQBottomBar, MainActivity** — verify touch targets and Scaffold insets.
4. **DoctorsScreen, DoctorDetailsScreen** — list + detail, search empty state.
5. **BookingScreens, AppointmentDetailsScreen, AppointmentsScreen** — state-heavy flow.
6. **AuthScreens** — split the 990-line file, form validation states.
7. **Placeholders (Messages, chat)**, **ProfileScreen**, **NotificationsScreen**.
8. **Sweep** — `grep` for remaining `Color(0x` and inline `fontSize`; must return zero
   outside `ui/theme`.
