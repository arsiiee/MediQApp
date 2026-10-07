# Constraints

Last reviewed: 2026-10-06

The quality bar for this repo, as numbers rather than prose. `.mdfiles/AGENTS.md`
states the same architectural rules in words; this file states them with a
command that produces a verdict, so a violation is caught by running something
rather than by someone remembering.

Read this before writing code. **Do not weaken it to make a change pass.** If a
rule is wrong, change it here explicitly, in its own change, where the diff
shows the bar moving.

## Floor (always enforced, no setup required)

- No new suppression comments: `@Suppress`, `@SuppressLint`, `noinspection`, `// nosemgrep`
- No unimplemented stubs: `TODO(`, `throw NotImplementedError`, `NotImplementedException`
- No skipped or deleted tests without a reason in the commit message
- No secrets in source. The dev JWT secret in `Db.kt` is deliberate and guarded
  by `ServerConfig.validate()`; adding a *second* one is not
- This file does not get weakened to make a change pass

## Enforced with numbers

Every row names the command that produces the verdict. A rule with a number and
no command is an aspiration, not a constraint.

| Dimension | Rule | Checked by | Runs at |
|-----------|------|-----------|---------|
| Types | Zero compile errors in `:app` and `:server` | `.\gradlew.bat :app:compileDebugKotlin :server:compileKotlin` | every edit |
| Architecture | `ui-imports-data` = 0 | `.\.mdfiles\check-boundaries.ps1` | every edit, < 5s |
| Architecture | `domain-imports-android` = 0 | `.\.mdfiles\check-boundaries.ps1` | every edit, < 5s |
| Architecture | `api-call-not-wrapped` = 0 | `.\.mdfiles\check-boundaries.ps1` | every edit, < 5s |
| Architecture | `secret-field-no-keyboard-options` = 0 | `.\.mdfiles\check-boundaries.ps1` | every edit, < 5s |
| Architecture | `secret-field-not-password-keytype` = 0 | `.\.mdfiles\check-boundaries.ps1` | every edit, < 5s |
| Tests | `:server:test` green, 50 tests minimum | `.\gradlew.bat :server:test` | task end |
| Tests | `:app:testDebugUnitTest` green, 165 tests minimum | `.\gradlew.bat :app:testDebugUnitTest` | task end |
| Contrast | Every token pair clears WCAG AA; 0 hardcoded page backgrounds; raw-colour ratchet = 4 | `.\.mdfiles\check-contrast.ps1` | every edit, < 5s |

Both check scripts resolve paths relative to the working directory, so they must
be run from the repo root even though they live in `.mdfiles/`.

Combined task-end gate, ~90s budget:

```powershell
.\.mdfiles\check-boundaries.ps1
.\.mdfiles\check-contrast.ps1
.\gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest :server:test --console=plain
```

## Why architecture boundaries are the enforced dimension

Not coverage, not security scanning — those need tooling this repo does not
have, and nothing runs automatically here because there is no CI. The rules
already existed in `AGENTS.md` under a heading reading *"Rules that are easy to
break"*, and **nothing checked any of them**. Two independent failures followed
from that, both with fully green builds:

1. The server published an error contract (`ErrorDto`) that the client never
   read. Patients saw `"HTTP 401"` instead of the message written for them. The
   whole test suite passed throughout.
2. Seven ViewModels imported `BackendNotConnectedException` from
   `data/repository/`, directly violating *"Screens and ViewModels must not
   reference `data/` directly"*. Found by the first run of `check-boundaries.ps1`
   and fixed the same day — the exception moved to `domain/model/`, where it
   belongs, and `ui-imports-data` went from 7 to 0.
3. Not one of the ten `OutlinedTextField`s in the app set `keyboardOptions`.
   Compose therefore used `KeyboardOptions.Default` — `autoCorrect = true`, no
   `KeyboardType` — so Android kept autocorrect and suggestions live on the
   masked fields and rewrote the characters as they were typed. A correct
   password reached the server as a different string and returned the same
   `"Those details don't match an account."` as a wrong one, so nothing on
   screen distinguished a typo from a bad password. Five masked fields were
   affected, sign-in included, and the build was green throughout.

All three are the same failure: a rule stated in prose, never checked. That is
what this file and `check-boundaries.ps1` exist to stop recurring. The third is
the one to read twice — it is invisible in review, invisible to the compiler,
and invisible to the entire test suite, and it shipped in the most safety-
critical place in the app.

## Ratchets (measured 2026-10-06, must not worsen)

Set at today's value rather than at an aspiration, so the gate is green on day
one. Move a number only when the code moves it.

| Metric | Today | Direction | Note |
|--------|-------|-----------|------|
| `ui-imports-data` | 0 | must not grow | Was **7** until 2026-10-06 — every one `BackendNotConnectedException`, caught by ViewModels from `data/repository/`. Moved to `domain/model/`, where it belongs, and the ratchet dropped from 7 to 0. |
| `domain-imports-android` | 0 | must not grow | Would break `:server`, which compiles `domain/` as `sharedDomain`. |
| `api-call-not-wrapped` | 0 | must not grow | An unwrapped call is how the `HTTP 401` bug returns. |
| `secret-field-no-keyboard-options` | 0 | must not grow | Was **5** on 2026-10-06 — every masked field in the app, sign-in included. See failure 3 above. |
| `secret-field-not-password-keytype` | 0 | must not grow | Was **0**, and stays 0. Added at the same time as the rule above; nothing set the wrong key type because nothing set one at all. |
| `:server:test` count | 50 | must not fall | `BookingConcurrencyTest` is 4 of those and cannot be checked by hand. Raised from 36 on 2026-10-06 — `AppointmentLifecycleTest` (13), `AvailableDatesTest` (4), `OtpLockoutTest` (3) and the 7 username-normalisation cases in `AuthServiceTest` were added, so the floor moves with the code. |
| `:app:testDebugUnitTest` count | 165 | must not fall | Added 2026-10-06 at the current value, when ViewModels first became testable: 23 in `data/api/`, plus `RegisterViewModelTest` (22) and `AppointmentDetailsViewModelTest` (27). The 23 were the whole suite until then — every ViewModel was untested because the harness did not exist. Raised from 51 when the appointment cancel/reschedule actions were wired: 10 more cover the `isActionable` gate, a cancel that must reload, a refused cancel, and a reschedule that must carry the real id and a chosen slot. Raised again from 61 when the reschedule slot picker was built: 10 more cover the availability reads, the bookable-only filter, discarding a slot when the date changes, and that a picker cannot be opened on an appointment that cannot be changed. Raised from 71 to 165 when the six remaining untested ViewModels were covered: `DoctorsViewModel` (22), `BookingViewModel` (19), `SignInViewModel` (16), `ProfileViewModel` (14), `HomeViewModel` (13), `NotificationsViewModel` (10). Every one of those 94 was mutation-checked — the ViewModel was deliberately broken and the suite had to fail — so the count is 165 tests that can fail, not 165 that pass. |
| Suppressions | 0 | must not grow | |
| Stubs (`TODO(`) | 0 | must not grow | |
| Raw colour literals in `ui/` | 4 | must not grow | The 4 are `SplashScreen`'s white-on-brand-green, which measures 6.63:1. Was **25** on 2026-10-06 — 22 `Color.Gray` at 3.95:1 and friends, all invisible to a green build because nothing measured them. |
| Hardcoded page backgrounds | 0 | must not grow | Was **10**. Caused the dark scheme's white-on-white 1.00:1. |

## Known debt

| Item | Why it is still open |
|------|---------------------|
| `window.statusBarColor` is deprecated | Pre-existing warning in `Theme.kt`. Replacing it means `enableEdgeToEdge()`, which changes how insets are handled app-wide — its own task, not a contrast fix. |
| Dark mode is contrast-correct but not tonally tuned | Status chips keep fixed light pastel containers, so they stay correct in both modes but read as bright blocks on a dark surface. Needs a dark status palette, which is a design decision. |

## Exceptions

None.

## Not enforced, and why

| Dimension | Why not |
|-----------|---------|
| Test coverage | Needs JaCoCo or Kover in both modules. Worth adding — `:app` now has 165 tests and **every one of the ten ViewModels is covered**, but no instrumentation or Compose test exists, so the `ui/` composables themselves are still unverified by anything that runs without a device. `ExampleInstrumentedTest` remains an untouched Android Studio template. |
| Security scanning | Semgrep/osv-scanner have nowhere to run without CI. Genuinely relevant: the repo has a dev JWT secret in source, guarded only by `MEDIQ_ENV=production`. |
| Lint | Zero lint config today (no detekt, ktlint, or `.editorconfig`). Adopting one means writing a config and absorbing findings across the existing tree. |
| Secrets scanning | Would flag the intentional dev JWT secret in `Db.kt` on every run until it is allowlisted. |
