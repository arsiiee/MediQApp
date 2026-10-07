# Spec: Temporary Seeded-Data Screen

## Objective

**One screen that answers "is the backend actually working?" — from the device,
against the live server, with nothing invented.**

Right now that question has no single answer. The seed prints to a console
window that `:server:run` scrolls away, the Doctors tab renders an empty list
whether the seed ran or the server is down, and "Couldn't reach the clinic"
reads the same whether the Wi-Fi dropped or the port is wrong. The LAN address
trap in `AGENTS.md` (Networking) has already cost a diagnostic pass for exactly
that reason.

This adds one temporary screen that calls the live backend through the normal
repository and reports what came back: the rows that exist, or the specific
reason there are none. Every value on screen is read over HTTP at the moment the
screen opens. **Nothing is hardcoded — not a doctor, not a count, not a
placeholder row.**

### What a green screen proves, and what it does not

Stated up front, because a screen that says "backend OK" without this is worse
than no screen.

| This screen proves | This screen does **not** prove |
|---|---|
| The server process is up and accepting connections | That the base URL points at the *right* host — a wrong LAN IP answers from a different machine |
| The database is reachable and the `doctors` join to `clinic_hours` returned rows | Anything about auth: it is reached pre-sign-in, so no bearer token is sent |
| `MEDIQ_SEED_DEMO=true` actually inserted rows | That any **write** works — booking, cancel, profile update are untested here |
| The full serialisation round-trip works: HTTP → Gson → DTO → domain → Compose | That the wire contract matches for *added/renamed* fields (`README.md:154-161`) |

The three seeded doctors are the evidence, but they are **read over the network,
not held in the app.** A hardcoded list would render identically with the server
dead, which defeats the entire purpose — it would report "backend working" while
proving nothing. That is why D1 is the load-bearing decision in this spec.

### Acceptance criteria

1. Long-pressing the "MediQ" wordmark on `SplashScreen` for 2 seconds opens a
   `SeededDataScreen`.
2. **Every value rendered on the screen arrives over HTTP at the moment the
   screen opens.** No doctor, fee, licence, hour, or count is a Kotlin literal.
3. The screen lists every doctor the server returned, with name, specialty,
   years, fee, location, licence number, and weekday clinic hours — so a
   successful `doctors` → `clinic_hours` join is visible, not assumed.
4. **The three failure modes read differently**, because they have different
   fixes and currently all look like "empty list":
   - server unreachable → "Couldn't reach the server" (check the base URL / port)
   - server reachable, zero doctors → "`MEDIQ_SEED_DEMO` is not set" (check the env var)
   - server returned an error → the server's own sentence, verbatim
5. The screen shows the demo sign-in credentials (`demo_patient` / `demo12345`),
   clearly labelled as demo-only. These are the one exception to criterion 2 and
   the reason is in Boundaries.
6. Verified live on the emulator: with the server up the list renders; with the
   server stopped the unreachable message renders and does not hang or crash.

### Non-goals

- **Not** a general debug menu. One screen, one job.
- **Not** write, delete, or reset anything. Read-only, deliberately — a screen
  that can wipe rows is not a screen you forget to delete.
- **Not** a Compose UI test. See Open Question 1.
- **Not** a change to `DemoData.kt`, `schema.sql`, or any server route. The seed
  already works; this only shows it.

## ASSUMPTIONS I'M MAKING

1. **"Temporary" means deletable in one commit**, with no leftover markers and no
   disabled code. Not "ship it and delete it later when someone remembers."
2. **Long-press on the splash wordmark** is the entry point. Chosen because it
   leaves zero permanent UI, so removal is a one-line deletion rather than a
   button someone has to hunt down. See D2 for the alternatives.
3. **The screen's job is verifying the backend, not browsing the seed.** So it
   shows only doctors plus the demo patient — the one public, no-auth route that
   returns seeded rows. Appointments and notifications are excluded: this screen
   is reached pre-sign-in, so they would 401 and the screen would report a
   backend failure that is really an auth one.
4. **The demo password is hardcoded in the app** (`demo_patient` / `demo12345`).
   Everything else is read over HTTP. It already lives in `README.md:92-93` and
   `tasks/todo.md:33`, and `DemoData.kt:19` prints it to stdout too. Stated in
   Boundaries because it is still a credential in an APK — the one place this
   screen holds a literal.
5. **`BuildConfig.DEBUG` is not available to gate this.** Verified: `app/build.gradle.kts:37-39`
   declares only `compose = true`, and `rg BuildConfig` over the tree returns
   nothing. Gating would mean enabling `buildConfig = true`, which is a
   build-file change — larger than the screen. See D3.

→ **Correct me now, or I proceed on these.**

## Tech Stack

Unchanged. Kotlin 2.2, Compose BOM 2026.02, Material 3, navigation-compose
2.10.1, manual DI via `AppContainer`. **No new dependency.**

## Commands

```powershell
# Gate, in order, from repo root
.\.mdfiles\check-boundaries.ps1        # ui-imports-data = 0, < 5s
.\.mdfiles\check-contrast.ps1          # raw-colour ratchet stays 4, < 5s
.\gradlew.bat :app:compileDebugKotlin --console=plain
.\gradlew.bat :app:testDebugUnitTest --console=plain
.\gradlew.bat :server:test --console=plain

# See the seed
$env:MEDIQ_SEED_DEMO="true"
$env:MEDIQ_PORT="8099"
.\gradlew.bat :server:run --console=plain

# On device
.\gradlew.bat :app:installDebug
adb shell am start -n com.example.mediq/.MainActivity
```

## Architecture Decisions

### D1. The screen reads through `DoctorRepository` — it hardcodes no doctors

**Decision: the screen is an ordinary consumer of `DoctorRepository.getDoctors()`,
exactly like `DoctorsViewModel`. Nothing is cached in the app.**

The tempting shortcut is to build the demo list in Kotlin so the screen renders
with no server. That is precisely the failure this screen exists to catch: **a
hardcoded list renders identically whether the backend is alive or dead**, so it
would report "backend working" while proving nothing — a green light wired to
nothing. It would also break `AGENTS.md`'s *"No fake/invented data... Do not
reintroduce placeholder doctors"*, which is the rule `DemoData.kt:19` was written
to respect from the server side.

Consequence: **the screen is empty when the server is down.** That is the honest
answer, and criterion 4 accepts it.

The one thing that *is* a Kotlin literal is the demo password. See Boundaries —
it is a credential in the APK, it is deliberate, and it is named there rather
than buried.

### D2. Entry point: long-press the splash wordmark

**Decision: 2-second long-press on the "MediQ" text in `SplashScreen`.**

| Option | Permanent surface | Cost to delete later |
|---|---|---|
| **Splash long-press** | none — no new visible control | delete 4 lines |
| Button on `SignInScreen` | a visible button patients can see | delete the button + its nav line |
| Sixth bottom-nav tab | changes `MediQBottomBar`, a shared component | delete a tab, restore the list |

`SplashScreen` is the one screen whose whole content is already exempt from the
colour ratchet (`CONSTRAINTS.md:98` — its 4 literals measure 6.63:1), so putting
the gesture there adds zero new colour literals and cannot fail `check-contrast.ps1`.

Rejected: the bottom-nav tab, because it edits a shared component and a stray
"Seed" tab is exactly what ends up in a release build.

### D3. No `BuildConfig.DEBUG` gate — removal is manual, and that is the point

**Decision: no build-type gate. The screen ships in debug builds until it is
deleted.**

A `BuildConfig` gate needs `buildFeatures { buildConfig = true }` in
`app/build.gradle.kts` — a change to a shared build file, for a screen whose
entire purpose is to exist briefly. Worse, a gated screen is a screen nobody
tests on release and nobody remembers to remove: the gate makes it look
maintained.

Instead the spec states the removal as a **testable acceptance criterion**
(criterion 7 below): `rg -n "SeededData" app/src` returns nothing after the
deletion commit. The removal is verified by a command, not by memory.

### D4. The three failure modes get three different messages

**Decision: distinguish "unreachable" from "reachable but empty" from "the server
answered badly" — on this screen only.**

Today all three render as an empty list. They have three different fixes, and
`README.md:367-378` is a standing record of how long that ambiguity cost:
`10.0.2.2` on a physical phone reports "couldn't reach the clinic" while the
server answers `127.0.0.1` perfectly well.

There is a documented tension here and it is real. `AGENTS.md` Conventions says
`BackendNotConnectedException` maps to `Success(emptyList())` on list reads, and
`RetrofitDoctorRepository` implements exactly that — a list read catches an
unreachable-server `ApiFailure` and returns an empty `Paged`. So by the time the
ViewModel sees anything, **unreachable and empty are genuinely indistinguishable.**

Resolution, and it is narrow on purpose: the screen uses a doctor **count** as a
second signal, and the two messages are told apart by what the count can be.

| Repository returned | Screen says | What to fix |
|---|---|---|
| non-empty `items` | the list | nothing |
| empty `items`, no error | "Reachable, but `MEDIQ_SEED_DEMO` is not set" | the env var |
| `LoadState.Error` | the server's own sentence, verbatim | whatever the server said |

No new exception type, no change to `RetrofitDoctorRepository`, and
`AGENTS.md`'s unreachable-rule is untouched — it still holds everywhere else. The
distinction here is *cosmetic and diagnostic*, and it is honest about its limits:
an empty list also results from a reachable server whose database was wiped, and
the message points at the most likely cause rather than claiming certainty.

## Project Structure

```
app/src/main/java/com/example/mediq/
  ui/feature/debug/seededdata/
    SeededDataScreen.kt        NEW — list + credential block + BASE_URL subtitle
    SeededDataViewModel.kt     NEW — LoadState<List<Doctor>>, mirrors DoctorsViewModel
  ui/feature/auth/splash/SplashScreen.kt    + combinedClickable on the wordmark
  ui/navigation/Routes.kt                   + object SeededData : Screen("seeded_data")
  ui/navigation/MediQNavHost.kt             + one composable(Screen.SeededData.route)

app/src/test/java/com/example/mediq/
  ui/feature/debug/seededdata/SeededDataViewModelTest.kt   NEW — 8 tests
```

Four production files touched, all additive except two one-line insertions.
No `domain/`, no `data/`, no `di/`, no server.

The screen shows `RetrofitClient.BASE_URL` as a subtitle. **`ui/` may not import
`data/`** (`check-boundaries.ps1` Rule 2, `ui-imports-data` ratchet = 0), so the
host string cannot be read from the constant directly — the value is passed in as
a parameter from `MediQNavHost`, which is also where the wiring already lives.
That keeps the ratchet at 0 and means the screen shows what it was *given*; the
test does not assert the literal address, because `README.md:363-367` documents
that it changes per network.

## Code Style

Existing patterns, nothing new invented. The ViewModel is `DoctorsViewModel` with
the debounce removed:

```kotlin
package com.example.mediq.ui.feature.debug.seededdata

/**
 * Lists what `MEDIQ_SEED_DEMO=true` actually inserted.
 *
 * TEMPORARY. Delete this file, `SeededDataScreen.kt`, `Screen.SeededData`, the
 * NavHost line, and the long-press on the splash wordmark in one commit. Nothing
 * else depends on it, and nothing may start depending on it.
 *
 * It reads through [DoctorRepository] rather than hardcoding the three demo
 * doctors, because `AGENTS.md` forbids invented doctors in `app/` and
 * `DemoData.kt` keeps them labelled on the server side for exactly that reason.
 */
class SeededDataViewModel(
    private val doctorRepository: DoctorRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SeededDataUiState())
    val uiState: StateFlow<SeededDataUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(doctors = LoadState.Loading)
            _uiState.value = _uiState.value.copy(
                doctors = try {
                    LoadState.Success(doctorRepository.getDoctors(DoctorQuery()).items)
                } catch (e: BackendNotConnectedException) {
                    LoadState.Success(emptyList())
                } catch (e: ApiFailure) {
                    LoadState.Error(e.message)
                } catch (e: Exception) {
                    LoadState.Error("Couldn't read the seeded data. Try again in a moment.")
                }
            )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { SeededDataViewModel(AppContainer.doctorRepository) }
        }
    }
}
```

Screen conventions, all existing:

- `LoadState<T>` for state, `when` over all three cases.
- Money via `Money.format()`, never a formatted string built in the screen.
- `MaterialTheme.colorScheme.*` and `LocalMediQColors.current.*` for every
  colour. **Zero raw colour literals** — the ratchet is 4 and all 4 belong to
  `SplashScreen`.
- No masked text field on this screen, so `check-boundaries.ps1` Rule 4 does not
  apply.

## Testing Strategy

JUnit 4 + `kotlinx-coroutines-test`, hand-written fakes, no mocking framework.
The new ViewModel follows the two traps in `AGENTS.md` "Two traps in the
ViewModel tests": build with `by lazy` because it loads in `init`, and use
`FakeDoctorRepository.getDoctorsGate` if a test needs an in-flight state.

| Test | Asserts | Fails without it |
|---|---|---|
| Success with rows | `LoadState.Success` holds every doctor the fake returned | — |
| Empty list | `Success(emptyList())`, **not** `Error` | seed-off shows a scary error |
| Unreachable server | `Success(emptyList())` via `BackendNotConnectedException` | crash or false error |
| Server 500 | `LoadState.Error` carrying the **server's** sentence | `HttpException.message` leaks `"HTTP 500 "` |
| `ApiFailure` mapping | message is `e.message`, never a framework string | the `README.md:154-161` bug returns |
| **No hardcoded fallback** | the rendered row set equals the repository's, verbatim — a fake returning 3 doctors yields exactly 3 rows and never a 4th | a hardcoded demo list leaking in |
| Reload | a second `load()` re-queries the repository | stale list after a reseed |

The **"no hardcoded fallback"** case is the one this screen exists for. It is
written as a rendering test over the ViewModel state rather than a source
inspection, so it fails if anyone ever adds a fallback list behind the repository
call — which is the exact way this screen would start lying.

**Mutation check, required:** break the `ApiFailure` branch to surface
`e.message`'s superclass text and confirm the suite fails. A test never seen
failing is not evidence — `CONSTRAINTS.md:95` holds that line for the existing 166.

Ratchet: `:app:testDebugUnitTest` 166 -> **174**. Update `CONSTRAINTS.md:37` and
`:95` in the same commit, per *"Move a number only when the code moves it."*
See Open Question 2 — this collides with v2.

`:server:test` stays at 50. No server change.

## Boundaries

- **Always:** run all five gate commands before declaring done. Update
  `CONSTRAINTS.md`, `README.md`, and `.mdfiles/AGENTS.md` in the same commit.
- **Never:** put a doctor name, licence number, fee, hour, or count into a Kotlin
  file, and never render a fallback list when the read fails. **Every value on
  this screen comes from the server.** This is the one rule that makes the screen
  a verification tool rather than a decoration — a hardcoded row would render
  identically with the backend dead, which is the exact failure it exists to
  catch. Guarded by the "no hardcoded fallback" test above.
- **Never:** show a green/"OK" indicator that is not derived from a live response.
  A static "Backend: OK" label would be a lie with a green light on it.
- **Never:** add `TODO(`, `@Suppress`, or a `// remove before release` comment.
  `CONSTRAINTS.md:17-20` puts all three at zero and treats growth as a failure.
  The KDoc says what to delete and where; that is the whole removal mechanism.
- **Never:** let this screen become a general debug menu, or let a second
  screen depend on `SeededDataViewModel`. One consumer, so deletion stays one
  commit.
- **Ask first:** enabling `buildConfig = true` (D3 says don't; that is the
  default). Any change to `DemoData.kt` — the seed is already correct and this
  screen does not fix it. Anything that changes
  `RetrofitDoctorRepository`'s unreachable→empty-list mapping to give this
  screen a richer signal; D4 achieves the same result in `ui/` instead.

## Success Criteria

1. `MEDIQ_SEED_DEMO=true`, server up, app installed: long-press "MediQ" for 2s →
   doctors listed with real hours and `DEMO-PRC-0001..0003` licences.
   **Verified on the emulator, not inferred.**
2. **Server stopped, screen reopened:** the unreachable message appears and names
   the thing to check. No crash, no hang, no leftover rows from a previous load.
   This is the criterion that proves the screen is reading the network rather
   than a constant — a hardcoded version passes criterion 1 and fails this one.
3. **Server up with `MEDIQ_SEED_DEMO` unset:** a *different* message from
   criterion 2, pointing at the env var. Three states, three messages.
4. The credential block shows `demo_patient` / `demo12345`, and typing that pair
   on the sign-in screen succeeds against a seeded server. (No clipboard and no
   tap-to-fill — see Open Question 3.)
5. `check-boundaries.ps1` reports `ui-imports-data` = 0.
6. `check-contrast.ps1` green, raw-colour ratchet still exactly 4.
7. `:app:testDebugUnitTest` green at 174, `:server:test` green at 50, with
   `CONSTRAINTS.md` ratchets moved in the same commit.
8. **Removal is one command:** `rg -n "SeededData|seeded_data" app/src` lists
   exactly 6 files and no test depends on it, so a later `git rm` of those files
   plus two deletions compiles clean. Proven by doing it on a scratch branch
   before this lands — not asserted on faith.
9. `README.md` "What works" gains one line naming the screen, its purpose as a
   backend check, and its removal command. The "Not done yet" list is untouched —
   this is not a gap being closed, it is a temporary tool.

## Risks

| Risk | Impact | Mitigation |
|---|---|---|
| Nobody deletes it | A demo screen reaches a real patient, showing invented doctors as though they were real | The exact hazard `AGENTS.md` names. Mitigated by criteria 8-9, and by it having no entry in the bottom nav so nobody treats it as a feature. |
| The screen reads as proof the whole backend is healthy | Someone trusts a green screen that only exercised `GET /doctors` | The "what this does not prove" table at the top of this spec is copied into the screen's KDoc and shown as a footnote on screen. |
| Wrong LAN IP answers from another machine | Green screen, wrong server | The screen shows the resolved `RetrofitClient.BASE_URL` as a subtitle, so the host is visible rather than assumed. (`10.0.2.2` on a physical phone is the trap — `AGENTS.md` Networking.) |
| Demo password in an APK | `demo12345` is in the APK strings | Already true of `README.md` and stdout. Weak password on a demo-only account, never on a real database. Stated in Boundaries, not hidden. |
| Long-press on splash collides with the existing buttons | Nothing — different targets, and it is a 2-second hold | — |

## Open Questions

1. **Skip the composable test?** `CONSTRAINTS.md:116` notes no Compose test
   exists and `ExampleInstrumentedTest` is still a template. **Recommendation:
   skip it, and do not start that project here.** The ViewModel carries all the
   logic; the composable renders three states. Six ViewModel tests beat one
   brittle instrumentation test on a screen scheduled for deletion.
2. **The ratchet collides with v2.** `SPEC-token-lifecycle.md:61` measured 166
   against a 165 floor and found the docs stale by one; `tasks/todo.md:393`
   targets ≥172 for v2's Task 10. Both this spec and v2 now claim the same
   territory. **Recommendation: land this first, re-measure, and set the floor to
   the real count. Whichever lands second moves it again** — the point of the
   ratchet is that it is measured, not predicted.
3. **Does it need a "copy credentials" clipboard write?** **Recommendation: no.**
   `LocalClipboardManager` is deprecated in this Compose BOM and the system
   clipboard is readable by other apps. Tap-to-fill on the sign-in screen is
   better — but that is a different screen and is out of scope here. For now the
   credentials are readable on screen and typed by hand.

## Related

- `DemoData.kt` — the seed this displays; unchanged by this spec
- `README.md:92-93`, `:331-347` — how the seed is started today
- `CONSTRAINTS.md:37`, `:95`, `:98`, `:116` — the ratchets this moves
- `tasks/SPEC-token-lifecycle.md` — the active v2 plan; shares the test-count ratchet
