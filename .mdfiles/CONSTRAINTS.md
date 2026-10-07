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
| Architecture | `ui-imports-data` = 0 | `.\.mdfiles\check-boundaries.ps1` | every edit, < 5s; **and every CI run** |
| Architecture | `domain-imports-android` = 0 | `.\.mdfiles\check-boundaries.ps1` | every edit, < 5s; **and every CI run** |
| Architecture | `api-call-not-wrapped` = 0 | `.\.mdfiles\check-boundaries.ps1` | every edit, < 5s; **and every CI run** |
| Architecture | `secret-field-no-keyboard-options` = 0 | `.\.mdfiles\check-boundaries.ps1` | every edit, < 5s; **and every CI run** |
| Architecture | `secret-field-not-password-keytype` = 0 | `.\.mdfiles\check-boundaries.ps1` | every edit, < 5s; **and every CI run** |
| Tests | `:server:test` green, 50 tests minimum | `.\gradlew.bat :server:test` | task end |
| Tests | `:app:testDebugUnitTest` green, 174 tests minimum | `.\gradlew.bat :app:testDebugUnitTest --rerun-tasks` | task end |
| Contrast | Every token pair clears WCAG AA; 0 hardcoded page backgrounds; raw-colour ratchet = 4 | `.\.mdfiles\check-contrast.ps1` | every edit, < 5s; **and every CI run** |

Both check scripts resolve paths relative to the working directory, so they must
be run from the repo root even though they live in `.mdfiles/`.

Combined task-end gate, ~90s budget:

```powershell
.\.mdfiles\check-boundaries.ps1
.\.mdfiles\check-contrast.ps1
.\gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest :server:test --console=plain
```

**Both check scripts also run in CI**, in a `gates` job in
`.github/workflows/ci.yml`, so the rows above are no longer manual. Wired
2026-10-07; before that they were described here as enforced but only ever ran
by hand, and a violation of either merged green. The job needs no JVM or Android
SDK (`pwsh` is preinstalled on `ubuntu-latest`), takes ~10s, and runs in parallel
with the build jobs. It is **not** a step inside the `android` job — parking a
10-second check behind a 3-minute SDK download is the cost that teaches people to
ignore a red pipeline — and it carries **no `paths:` filter**, because these
scripts check the current state of the tree rather than a diff.

## Why architecture boundaries are the enforced dimension

Not coverage, not security scanning — those still need tooling this repo does not
have. Architecture boundaries won because they needed **no new dependency**: two
PowerShell scripts over the source tree, which is why they were enforceable
immediately even while nothing ran automatically here. The rules already existed
in `AGENTS.md` under a heading reading *"Rules that are easy to break"*, and
**nothing checked any of them**. Three independent failures followed from that,
all with fully green builds:

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

The fourth was the scripts themselves. They were described here as enforced
while running only when a human remembered to type them, so a boundary or
contrast violation merged green. Both now run in CI (see above) — which is the
same lesson one level up: a rule with no command producing its verdict is a rule
nobody is checking.

## Ratchets (measured 2026-10-07, must not worsen)

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
| `:app:testDebugUnitTest` count | 174 | must not fall | Added 2026-10-06 at the current value, when ViewModels first became testable: 23 in `data/api/`, plus `RegisterViewModelTest` (22) and `AppointmentDetailsViewModelTest` (27). The 23 were the whole suite until then — every ViewModel was untested because the harness did not exist. Raised from 51 when the appointment cancel/reschedule actions were wired: 10 more cover the `isActionable` gate, a cancel that must reload, a refused cancel, and a reschedule that must carry the real id and a chosen slot. Raised again from 61 when the reschedule slot picker was built: 10 more cover the availability reads, the bookable-only filter, discarding a slot when the date changes, and that a picker cannot be opened on an appointment that cannot be changed. Raised from 71 to **166** when the six remaining untested ViewModels were covered: `DoctorsViewModel` (22), `BookingViewModel` (19), `SignInViewModel` (16), `ProfileViewModel` (14), `HomeViewModel` (14), `NotificationsViewModel` (10). Every one of those 95 was mutation-checked — the ViewModel was deliberately broken and the suite had to fail — so the count is 166 tests that can fail, not 166 that pass. **Corrected 2026-10-07 from 165:** the six listed classes sum to 166, and `HomeViewModelTest` holds 14, not the 13 recorded here — the floor was one low from the day it was written. **Then 166 → 174 the same day**, once `SeededDataViewModelTest` (8) reached `HEAD`. The floor tracks the committed tree, and the screen that test covers is a temporary backend-verification tool whose removal is already proven on a scratch branch (`tasks/todo.md`, v3 Task 4). So this number is expected to come **down** to 166 when that screen is finally deleted — that is the ratchet working, not a regression, and it must be lowered in the same commit as the deletion. |
| Suppressions | 0 | must not grow | |
| Stubs (`TODO(`) | 0 | must not grow | |
| Raw colour literals in `ui/` | 4 | must not grow | The 4 are `SplashScreen`'s white-on-brand-green, which measures 6.63:1. Was **25** on 2026-10-06 — 22 `Color.Gray` at 3.95:1 and friends, all invisible to a green build because nothing measured them. **The ratchet counts five named constants plus one hex, not raw colour in general** — see "Not enforced, and why". |
| Hardcoded page backgrounds | 0 | must not grow | Was **10**. Caused the dark scheme's white-on-white 1.00:1. |

## Known debt

| Item | Why it is still open |
|------|---------------------|
| `window.statusBarColor` is deprecated | Pre-existing warning in `Theme.kt`. Replacing it means `enableEdgeToEdge()`, which changes how insets are handled app-wide — its own task, not a contrast fix. |
| Dark mode is contrast-correct but not tonally tuned | Status chips keep fixed light pastel containers, so they stay correct in both modes but read as bright blocks on a dark surface. Needs a dark status palette, which is a design decision. |
| **Six compiler warnings, not one** | Added 2026-10-07. `README.md` claimed `statusBarColor` was "the one remaining compile warning". `.\gradlew.bat --warning-mode all :app:compileDebugKotlin --rerun-tasks` reports **6**: that one, plus 5 uses of the deprecated `KeyboardOptions(capitalization, autoCorrect, …)` constructor at `RegisterCredentialsScreen.kt:91,115,143` and `SignInScreen.kt:107,134`. All pre-existing and committed, none introduced by the doc work. The constructor wants the new `autoCorrectEnabled` parameter. Checked with `--warning-mode all`, because the default mode collapses all six into "Deprecated Gradle features were used" with no detail. |
| **Gradle 10 will not build this** | `.\gradlew.bat --warning-mode all :server:compileKotlin` reports `Configuration.setVisible(boolean) has been deprecated. This is scheduled to be removed in Gradle 11`. Not ours — grepping all four build scripts for `visible` returns 0 hits, so it comes from a plugin (AGP 9.3.3 or KGP 2.2.10). Nothing in this repo can fix it short of a plugin bump, so it is recorded rather than enforced. Worth re-checking on the next AGP upgrade. |
| **Three JVM versions in one build** | The Gradle daemon runs JBR 21, `server/build.gradle.kts` pins `JavaLanguageVersion.of(17)`, and `gradle-daemon-jvm.properties` requests `toolchainVersion=25`. So `:server:run` launches on Adoptium **17** while `:app` compiles on 21 — visible in a `:server:run` failure, which names the toolchain JVM rather than the daemon's. Not a bug today (17 satisfies the `JavaVersion.VERSION_11` target in `app/build.gradle.kts:34-35`), but the docs describing a single "JDK 21+" are describing the daemon only, and a `--warning-mode all` build or a JVM-sensitive server bug would be diagnosed against the wrong number. |

## Exceptions

None.

## Not enforced, and why

| Dimension | Why not |
|-----------|---------|
| Test coverage | Needs JaCoCo or Kover in both modules. Worth adding — `:app` now has 166 tests and **every one of the ten ViewModels is covered**, but no instrumentation or Compose test exists, so the `ui/` composables themselves are still unverified by anything that runs without a device. `ExampleInstrumentedTest` remains an untouched Android Studio template. |
| Security scanning | Semgrep/osv-scanner have no workflow. CI exists (`.github/workflows/ci.yml`, added in `ef106ef`) so there is somewhere to add them, but neither has been wired. Genuinely relevant: the repo has a dev JWT secret in source, guarded only by `MEDIQ_ENV=production`. |
| Lint | `:app:lintDebug` runs in CI on the stock Android rules, so lint is a gate — but the repo carries no lint **config** (no detekt, ktlint, or `.editorconfig`), so those stock defaults are the whole bar. |
| The raw-colour ratchet is narrower than it reads | `check-contrast.ps1:177` matches `Color.Gray\|LightGray\|DarkGray\|White\|Black` plus the single hex `0xFFD32F2F`. An arbitrary literal like `Color(0xFF00FF00)` passes it — proved by mutation on 2026-10-07, not inferred. So "raw-colour ratchet = 4" means *those five constants*, not raw colour in general. There are 7 `Color(0x…)` sites in `ui/` today: 6 are the status chips, which a separate rule measures and reports, and **`NotificationsScreen.kt:122`'s `Color(0xFFF5F5F5)` is measured by nothing.** Widening the pattern is its own change, because it moves the bar and the floor at the same time. |
| Secrets scanning | Would flag the intentional dev JWT secret in `Db.kt` on every run until it is allowlisted. |
