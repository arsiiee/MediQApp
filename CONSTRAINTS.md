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
| Architecture | `ui-imports-data` = 0 | `.\check-boundaries.ps1` | every edit, < 5s |
| Architecture | `domain-imports-android` = 0 | `.\check-boundaries.ps1` | every edit, < 5s |
| Architecture | `api-call-not-wrapped` = 0 | `.\check-boundaries.ps1` | every edit, < 5s |
| Tests | `:server:test` green, 36 tests minimum | `.\gradlew.bat :server:test` | task end |
| Tests | `:app:testDebugUnitTest` green | `.\gradlew.bat :app:testDebugUnitTest` | task end |

Combined task-end gate, ~90s budget:

```powershell
.\check-boundaries.ps1
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

Both are the same failure: a rule stated in prose, never checked. That is what
this file and `check-boundaries.ps1` exist to stop recurring.

## Ratchets (measured 2026-10-06, must not worsen)

Set at today's value rather than at an aspiration, so the gate is green on day
one. Move a number only when the code moves it.

| Metric | Today | Direction | Note |
|--------|-------|-----------|------|
| `ui-imports-data` | 0 | must not grow | Was **7** until 2026-10-06 — every one `BackendNotConnectedException`, caught by ViewModels from `data/repository/`. Moved to `domain/model/`, where it belongs, and the ratchet dropped from 7 to 0. |
| `domain-imports-android` | 0 | must not grow | Would break `:server`, which compiles `domain/` as `sharedDomain`. |
| `api-call-not-wrapped` | 0 | must not grow | An unwrapped call is how the `HTTP 401` bug returns. |
| `:server:test` count | 36 | must not fall | `BookingConcurrencyTest` is 4 of those and cannot be checked by hand. |
| Suppressions | 0 | must not grow | |
| Stubs (`TODO(`) | 0 | must not grow | |

## Known debt

None outstanding. The one item this file was created with — seven
`ui-imports-data` violations — was fixed the same day by moving
`BackendNotConnectedException` to `domain/model/`, and the ratchet went from 7
to 0 rather than being left at a number that permitted the bug.

## Exceptions

None.

## Not enforced, and why

| Dimension | Why not |
|-----------|---------|
| Test coverage | Needs JaCoCo or Kover in both modules. Worth adding — `:app` has 12 tests across ~54 source files. |
| Security scanning | Semgrep/osv-scanner have nowhere to run without CI. Genuinely relevant: the repo has a dev JWT secret in source, guarded only by `MEDIQ_ENV=production`. |
| Lint | Zero lint config today (no detekt, ktlint, or `.editorconfig`). Adopting one means writing a config and absorbing findings across the existing tree. |
| Secrets scanning | Would flag the intentional dev JWT secret in `Db.kt` on every run until it is allowlisted. |
