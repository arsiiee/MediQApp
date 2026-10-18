# MediQ — Task List

## Plan history

| Plan | Status | Scope |
|---|---|---|
| v1 — registration + appointment details | **Complete**, commits `c9df562`..`24d9ef2` | Close the two "Known gaps" in `.mdfiles/AGENTS.md` |
| v2 — token lifecycle | **Complete** 2026-10-18 | 401 handling, session expiry, corrupt-session recovery, SessionSignal, SessionAuthenticator |
| v3 — seeded-data screen | **Complete** 2026-10-07 | One temporary screen that verifies the live backend |
| v4 — doctor specialty filter | **Complete** 2026-10-15 | One chip per `Specialty.entries` plus "All", driven by the real `DoctorQuery.specialty` |

**Active plan:** the six-screen UI redesign, spec at `tasks/SPEC-ui-redesign.md`
with one module spec per step (`tasks/SPEC-ui-*.md`). Modules land one commit
each, both check scripts and the test gate green at every step. `ui-foundation`
(`529e1d6`, `456b1e6`), `ui-auth` (`049b717`), `ui-doctors` (`1ecff3a`), and
`ui-scheduling` (figures 4 and 5, plus the month stepper `8c986b0` had left
unrendered), and `ui-appointment` (figure 6, `f17ffa9`) are done — that was the
last module, so all six screens are restyled. v4 above landed first
because its chips are figure 3's chip row.

**Figure 6 was checked on the device, not asserted** (2026-10-18, `mediq_api36`,
seeded server): the rows, the chip in the app bar, the two actions and the
always-drawn warning card all read correctly off the accessibility tree, and the
drawn colours sample to `MediQGreen`, `MediQError`, and the warning pair. Tapping
cancel re-read the appointment and moved it to `Cancelled` with the actions gone
and the card still there. That closes the last acceptance box the module spec
had open.

**v4 is complete** (2026-10-15). Spec at `tasks/SPEC-specialty-filter.md`. It
touched no file v3 touched, and its floor move was measured rather than
predicted: **189**, from `DoctorsViewModelTest`'s four new tests.

**v2 was archived, not abandoned.** Verified unstarted rather than assumed:
`SessionSignal`, `isAuthFailure`, and `Authenticator` return 0 hits across
`app/src/main`. Its 93 boxes are untouched. Pick it up after v3 — it is the
higher-severity of the two.

---

# v3 — Temporary Seeded-Data Screen

Plan: `tasks/plan.md`. Spec: `tasks/SPEC-seeded-data-screen.md`.

**One question this answers:** is the backend actually working? Today the seed
prints to a console that scrolls away, the Doctors tab shows an empty list whether
the seed ran or the server is down, and "Couldn't reach the clinic" reads the same
for a dropped Wi-Fi and a wrong base URL.

**Verification commands used throughout** (all must stay green):

```powershell
.\gradlew.bat :app:compileDebugKotlin --console=plain
.\gradlew.bat :app:testDebugUnitTest --console=plain     # floor 166, target >= 174
.\gradlew.bat :server:test --console=plain               # floor 50, unchanged
.\.mdfiles\check-boundaries.ps1                          # ui-imports-data = 0
.\.mdfiles\check-contrast.ps1                            # raw-colour ratchet stays 4

# live
$env:MEDIQ_SEED_DEMO="true"
$env:MEDIQ_PORT="8099"
.\gradlew.bat :server:run --console=plain
.\server\scripts\smoke.ps1 -Base http://127.0.0.1:8099
```

Three traps to carry into the tasks:

- **`buildConfig` is off** (`app/build.gradle.kts:37-39`), so `BuildConfig.DEBUG`
  does not exist. Gating this screen behind it costs a build-file change. D3 says
  do not; removal is proven by a command in Task 4 instead.
- **`ui/` may not import `data/`** (`check-boundaries.ps1:108`). The base-URL
  string cannot be read from `RetrofitClient` — it is passed in as a parameter.
- **Unreachable and empty are indistinguishable at this layer.**
  `RetrofitDoctorRepository` maps an unreachable server to an empty `Paged`, and
  that is correct everywhere else. D4 separates the two messages in `ui/` alone;
  do not "fix" it in the repository.

---

## Task 1: `SeededDataViewModel` with 8 tests

**Description:** The whole logic of the screen, written before any composable so
it is testable in isolation. `DoctorsViewModel` with the debounce and the filters
removed: one `read()`, one `LoadState<List<Doctor>>`, the same three-branch catch
(`BackendNotConnectedException` → `Success(emptyList())`, `ApiFailure` →
`Error(e.message)`, `Exception` → a written sentence).

**One test is the point of this task.** "No hardcoded fallback" asserts the state
holds exactly what the repository returned — a fake returning three doctors yields
three rows and never a fourth. A fallback list behind the repository call would
make this screen report "backend working" with the backend dead, which is the one
failure it exists to catch.

**Acceptance criteria:**
- [x] `SeededDataViewModel` exposes `SeededDataUiState(doctors: LoadState<List<Doctor>>)` and `read()`/`refresh()`, with `Factory` reading `AppContainer.doctorRepository` — the `DoctorsViewModel.kt:88-94` pattern
- [x] `DoctorQuery()` with no filters; no hardcoded doctor, fee, licence, hour, or count anywhere in the file
- [x] `BackendNotConnectedException` → `Success(emptyList())`; `ApiFailure` → `Error(e.message)`; other `Exception` → a written sentence, never a framework message
- [x] KDoc states the screen is temporary and names exactly what to delete
- [x] 8 tests: success-with-rows, unfiltered-read, empty, unreachable, 500, `ApiFailure` message mapping, no-hardcoded-fallback, reload-re-queries. (Planned as 7; the unfiltered-read case was added during Task 1 because a query carrying a filter could return fewer rows than the seed created and read as a partial seed.)
- [x] The ViewModel is built with `by lazy` in the test, not assigned in `setUp` (`AGENTS.md` trap §1)

**Verification:**
- [x] `.\gradlew.bat :app:testDebugUnitTest --console=plain` green at 174 (measured from the JUnit XML, 166 baseline + 8)
- [x] `.\.mdfiles\check-boundaries.ps1` green — `ui-imports-data` = 0 (53 files, up from 52)
- [x] **Mutation check** — two run, both caught, both restored:
  - [x] `Error(e.message)` → `Error(e.toString())`: **2 failed** (`a failure message is the server's…`, `a server failure shows the server's own sentence`)
  - [x] `.ifEmpty { listOf(doctor) }` behind the repository call: **2 failed** (`the state holds exactly what the repository returned`, `an empty list is a success, not an error`) — this is the guard the screen exists for, and it fails on the fallback, not on a typo
- [x] Manual check: `Select-String SeededDataViewModel.kt -Pattern 'DEMO-|Clinic 2|Demo Rivera|Sample|Placeholder'` returns nothing

**Dependencies:** None

**Files likely touched:**
- `app/src/main/java/com/example/mediq/ui/feature/debug/seededdata/SeededDataViewModel.kt` (new)
- `app/src/test/java/com/example/mediq/ui/feature/debug/seededdata/SeededDataViewModelTest.kt` (new)

**Estimated scope:** M (2 files)

---

## Task 2: `SeededDataScreen` — three states, base-URL subtitle

**Description:** Renders the state Task 1 produces. Rows, an empty result, and the
server's own error sentence each get their own rendering, because they have
different fixes. The resolved base URL is a subtitle so the host is visible — a
green screen pointed at the wrong machine is the dangerous case, and `10.0.2.2` on a
physical phone is the recorded trap.

**Corrected during Task 2: there are two distinguishable outcomes, not three.**
`RetrofitDoctorRepository.kt:38-40` returns an empty page when the server is
unreachable, which is correct for every other screen. So "server down" and "seed
did not run" arrive here as the *same* `Success(emptyList())` and cannot be told
apart at this layer. Rather than guess, the empty state names both causes and says
why it cannot choose. See the note under Checkpoint B for the probe that would
give a real third state.

**Acceptance criteria:**
- [x] `when` over all three `LoadState` cases, with a visible `Loading` state
- [x] Non-empty: name, specialty, years, fee, location, licence, and the weekday clinic-hours list, so the `doctors` → `clinic_hours` join is visible rather than assumed
- [x] Empty: a message naming `MEDIQ_SEED_DEMO` **and** the unreachable case, stating it cannot separate them. `DEMO-PRC-0001` is shown per row because "is the seed labelled DEMO-*" is a question this screen exists to answer
- [x] Error: `e.message` verbatim, and the "what this does not prove" footnote present on every state
- [x] `baseUrl` is a **parameter**; the file imports nothing from `com.example.mediq.data.`
- [x] Every colour from `MaterialTheme.colorScheme.*` or `LocalMediQColors.current.*` — **zero** raw literals, so the ratchet stays 4
- [x] The demo credential block shows `demo_patient` / `demo12345`, labelled demo-only. No clipboard, no tap-to-fill (spec OQ3)
- [x] Money via `Money.format()`; no formatted string built in the composable
- [x] A **Re-check** button calls `refresh()`. A verification screen that cannot be re-run after a restart is only half a tool

**Verification:**
- [x] `.\gradlew.bat :app:compileDebugKotlin --console=plain` green, no new warnings
- [x] `.\.mdfiles\check-contrast.ps1` green, ratchet still exactly 4
- [x] `.\.mdfiles\check-boundaries.ps1` green — 54 files, up from 53
- [x] `.\gradlew.bat :app:testDebugUnitTest :server:test --console=plain` green at 174 / 50
- [x] Manual check: `Select-String SeededDataScreen.kt -Pattern 'import com\.example\.mediq\.data'` returns nothing
- [x] Manual check: no `Color.White|Black|Gray|…` or `0x[0-9A-F]{6}` in the file — grepped, none
- [x] Manual check: the only literals are `DEMO_USERNAME`, `DEMO_PASSWORD`, the `HH:mm` formatter, and UI copy. No doctor name, fee, licence, or hour

**Dependencies:** Task 1

**Files likely touched:**
- `app/src/main/java/com/example/mediq/ui/feature/debug/seededdata/SeededDataScreen.kt` (new)

**Estimated scope:** S (1 file)

---

## Checkpoint B: the live proof

This is the checkpoint that matters, and it cannot be skipped or deferred to an
emulator walkthrough at the end.

- [x] Server up with `MEDIQ_SEED_DEMO=true`: the screen renders rows, each with `DEMO-PRC-0001..0003` and its clinic hours — **observed 2026-10-18** on `mediq_api36`: `3 doctor(s) read from the server`, `DEMO-PRC-0001` with twelve `clinic_hours` rows (`Mon 09:00–12:00` … `Sat 14:00–17:00`) and `DEMO-PRC-0003`
- [x] **Server stopped, Re-check pressed: the rows are gone and the empty card appears.** This is the line that proves the screen reads the network — a hardcoded screen passes the line above and fails this one, which is the entire reason this screen exists — **observed 2026-10-18**: the server process was killed (nothing left listening on 8099), Re-check was pressed, and the three cards were replaced by `No doctors came back` with the two-cause sentence
- [x] Server up with `MEDIQ_SEED_DEMO` unset: **the same** empty card. Not a third message — see below — **observed 2026-10-18**: the server was restarted with `MEDIQ_SEED_DEMO=false` (`GET /doctors` answered `{"items":[]}`) and Re-check drew the **identical** card, word for word
- [x] `GET /doctors` reachable at the shown base URL, from the device, not from a browser on the host — **observed 2026-10-18**: the screen names `http://10.0.2.2:8099/`, which is routable only inside the emulator's virtual network, and the read it drew came from the app on `mediq_api36`

### Why "server stopped" and "seed unset" cannot read differently here

The plan originally promised three distinct messages. It cannot deliver that, and
the spec's D4 already conceded the limit; this records it so nobody re-litigates it
mid-task.

`RetrofitDoctorRepository.getDoctors` catches `ApiFailure` and returns
`Paged(emptyList())` when `isNetworkFailure` — `RetrofitDoctorRepository.kt:38-40`.
That mapping is **correct** and documented in `AGENTS.md`: an unreachable backend
on a list read is the expected development state, not a failure. By the time
`SeededDataViewModel` sees anything, both cases are `Success(emptyList())`.

Changing the repository to expose it is listed under **Ask first** in the spec's
Boundaries, so it was not done unilaterally.

**The cheap fix, if three states are wanted:** `DoctorRepository.getDoctor` is a
*single-entity* read, so it lets `ApiFailure` propagate (`RetrofitDoctorRepository.kt:42-43`),
and an unknown id returns 404 (`DoctorStore.kt:104`). One extra call with a
non-existent id therefore yields a real reachability signal — `isNetworkFailure`
true means down, a 404 means up — on an existing public route, with no repository
change and no new endpoint. Cost: a second request per read, one more test, and a
deliberate 404 in the log. **Not done: the wording that names both causes is honest,
and this is a temporary screen.** Say the word and it is a 15-minute change.

---

## Task 3: Route, nav host, splash long-press

**Description:** The entry point. A long-press on the "MediQ" wordmark in
`SplashScreen`, a `Screen.SeededData` object, and one `composable` in the nav host.

**`baseUrl` could not come from `RetrofitClient` directly.** `MediQNavHost` is in
`ui/`, and `check-boundaries.ps1:108` bans `com.example.mediq.data.` there, so
importing `RetrofitClient.BASE_URL` would be a violation even though the compiler
accepts it — the exact class of bug `CONSTRAINTS.md` records as having shipped
twice. `AppContainer.baseUrl` is a **getter** over the same constant rather than a
copied value, so `ui/` reaches it through a layer it may already import and the two
cannot drift. That is why this task touches 4 files, not the planned 3.

**The gesture uses the platform long-press timeout, not a 2-second one.**
`detectTapGestures` reads `ViewConfiguration.longPressTimeoutMillis`, so this is
the same ~500 ms as any Android long press. Hand-rolling a 2 s timer would mean
`awaitEachGesture` and a manual clock for no gain. Corrected in the spec and plan;
the gesture is still undiscoverable to a patient.

**Acceptance criteria:**
- [x] `Screen.SeededData : Screen("seeded_data")` in `Routes.kt`
- [x] One `composable(Screen.SeededData.route)` in `MediQNavHost`, passing `AppContainer.baseUrl`
- [x] `AppContainer.baseUrl` is a `val … get() = RetrofitClient.BASE_URL` — single source of truth stays in `data/api/`, `ui/` reads it through `di/`
- [x] `SplashScreen`'s wordmark takes a long press; the two existing buttons and every other literal on that screen are untouched
- [x] A short press on the wordmark navigates nowhere
- [x] No bottom-nav entry: `MediQBottomBar.kt` has 0 references to `SeededData`
- [x] No route reachable from any screen but the splash gesture

**Verification:**
- [x] `.\gradlew.bat :app:compileDebugKotlin --console=plain` green
- [x] `.\.mdfiles\check-contrast.ps1` green — ratchet still exactly 4, `SplashScreen`'s existing 4 unchanged
- [x] `.\.mdfiles\check-boundaries.ps1` green — 54 files, `ui-imports-data` = 0. This is the check that would have caught the direct `RetrofitClient` import
- [x] `.\gradlew.bat :app:testDebugUnitTest :server:test --console=plain` green at 174 / 50
- [x] `git diff SplashScreen.kt` is +20/-1 — the gesture and its imports only, no layout change
- [x] 6 files reference `SeededData`, matching the plan's removal claim

**Dependencies:** Task 2

**Files likely touched:**
- `app/src/main/java/com/example/mediq/ui/navigation/Routes.kt`
- `app/src/main/java/com/example/mediq/ui/navigation/MediQNavHost.kt`
- `app/src/main/java/com/example/mediq/ui/feature/auth/splash/SplashScreen.kt`
- `app/src/main/java/com/example/mediq/di/AppContainer.kt` — **added, not in the original plan**, for the boundary reason above

**Estimated scope:** S (4 files)

---

## Checkpoint C: reachable

> **Closed 2026-10-07 by Task 5.** The static lines were green at Task 3; the
> on-device line had never been run and was the only untested part of the wiring.

- [x] Long-press opens the screen from a cold launch; a short press does not
- [x] `check-boundaries.ps1` reports `ui-imports-data` = 0 — the seam did not buy navigation by breaking the layering
- [x] `check-contrast.ps1` green, ratchet exactly 4
- [x] :app:testDebugUnitTest green at 174
- [x] Review with human before proceeding
- [x] Emulator: launch, long-press the wordmark, screen opens — verified on `emulator-5554`, see Task 5

---

## Task 4: Commit the v3 code

> **Added 2026-10-07.** The original Task 4 bundled the commit with the docs.
> It cannot: `CONSTRAINTS.md` is explicit that a ratchet records what the
> **committed** tree guarantees, and the floor is 166 while the worktree measures
> 174 only because `SeededDataViewModelTest` is untracked. Committing the code
> first is what makes the 174 floor honest. Docs follow in Task 7, after the
> behaviour is observed.
>
> **Done 2026-10-07 as `30d69c7`.** 7 paths, 736 insertions, no `.md`. `HEAD`
> now measures 174 by `git grep -h "@Test" HEAD -- "app/src/test/**"`, which is
> what unblocks Task 7. The `.md` files were already modified from the 2026-10-07
> doc audit and stayed unstaged as intended — they are still uncommitted and are
> Task 7's business. The re-plan itself went in as `46297f1`, separately, because
> planning work is not v3's code.

**Description:** Nothing here changes behaviour. The screen, its ViewModel, its
8 tests, and the 4 wiring files are green and unstaged; this makes them reviewable
as a diff and revertable as a unit.

**Acceptance criteria:**
- [x] One commit containing: `ui/feature/debug/seededdata/` (2 new files), `ui/feature/debug/seededdata/SeededDataViewModelTest.kt` (new), `Routes.kt`, `MediQNavHost.kt`, `SplashScreen.kt`, `AppContainer.kt`
- [x] **No doc file in this commit.** `.mdfiles/` and `README.md` are already modified from the 2026-10-07 doc audit and stayed unstaged — that is a separate change with its own diff
- [x] Commit message states this is the temporary seeded-data verification screen, and that it is deleted on use
- [x] `HEAD` now measures 174 tests — the precondition for Task 7's floor move

**Verification:**
- [x] `.\gradlew.bat :app:compileDebugKotlin --console=plain` green before committing
- [x] `.\gradlew.bat :app:testDebugUnitTest --rerun-tasks` green — **174 app / 50 server**, counted from the JUnit XML, not the task output
- [x] `git show --stat HEAD` lists exactly the 7 code paths and no `.md`
- [x] `git status --short` afterwards shows only the `.md` files
- [x] Secret scan on the staged diff: the only match is `demo12345`, already committed at `HEAD` in `DemoData.kt:132`, `smoke.ps1:13`, and `README.md:93,345`. Not a new secret — a 5th reference to an already-public demo credential

**Dependencies:** Tasks 1, 2, 3

**Files likely touched:**
- 2 new under `app/src/main/java/com/example/mediq/ui/feature/debug/seededdata/`
- 1 new under `app/src/test/java/com/example/mediq/ui/feature/debug/seededdata/`
- `app/src/main/java/com/example/mediq/ui/navigation/Routes.kt`
- `app/src/main/java/com/example/mediq/ui/navigation/MediQNavHost.kt`
- `app/src/main/java/com/example/mediq/ui/feature/auth/splash/SplashScreen.kt`
- `app/src/main/java/com/example/mediq/di/AppContainer.kt`

**Estimated scope:** S (6 paths, no behaviour change)

---

## Task 5: The live proof — Checkpoints B and C

> **Merges the old Checkpoint B and Checkpoint C.** Both needed the emulator and
> neither had it. Checkpoint C's static lines were already green and are checked;
> its on-device line had never been run, which is the only untested part of Task 3.
> Same emulator session, so one boot serves both.

**Description:** Everything so far is a green build and a passing suite. Nothing
has ever run. This is the task that either proves the screen reads the network, or
proves it does not — a hardcoded screen passes "server up" and fails "server
stopped", which is the whole reason this screen exists.

**Done 2026-10-07. Every line observed on `emulator-5554`, not inferred.**

| Check | Result |
|---|---|
| Short press on the wordmark | Stayed on the splash — no navigation |
| Long press (1200ms `input swipe`) | Opened `SeededDataScreen` |
| Subtitle | `Read live from http://192.168.100.14:8099/` |
| Server up, seeded | `3 doctor(s) read from the server`, `DEMO-PRC-0001..0003`, 12 weekday clinic-hours rows, fees `₱700`/`₱800` |
| **Server stopped, Re-check** | **All rows gone, `No doctors came back` card, zero `DEMO-` strings** |
| Server restarted, Re-check | Rows returned — no relaunch needed |

The stopped-server line is the one that matters: it is the behaviour a hardcoded
implementation cannot produce, and it held on the first attempt.

**Acceptance criteria:**
- [x] Cold launch shows the splash; a **short** press on the wordmark navigates nowhere (Checkpoint C)
- [x] A **long** press opens `SeededDataScreen`, subtitle showing the resolved base URL
- [x] Server up, `MEDIQ_SEED_DEMO=true`: rows render, each with `DEMO-PRC-0001..0003` and its clinic hours. **`DEMO-` in the licence is the proof the seed ran** — a real doctor's licence would not carry that prefix
- [x] **Server stopped, Re-check pressed: the rows are gone and the empty card names both causes.** This is the line a hardcoded implementation cannot pass
- [x] Server up with `MEDIQ_SEED_DEMO` unset: the same empty card. Two states, not three — see the note below
- [x] `GET /doctors` reachable at the shown base URL **from the device**, not from a browser on the host

**Verification:**
- [x] `.\gradlew.bat :app:installDebug` (36s) then `adb shell am start -n com.example.mediq/.MainActivity`
- [x] `adb shell uiautomator dump /sdcard/ui.xml` to find the wordmark — read `[352,791][728,940]`, centre **(540, 865)**; Re-check at **(181, 412)**. Guessing tap coordinates from a screenshot is not reliable
- [x] `adb shell input tap` for a short press; `input swipe 540 865 540 865 1200` for a long one — the 1200ms is above `ViewConfiguration.longPressTimeoutMillis`, so the gesture fires
- [x] Server was **stopped and restarted mid-session** rather than assumed down — `Invoke-RestMethod /health` confirmed connection refused before Re-check
- [x] Install 36s. `config.ini` was not touched
- [x] **Boot resumed from snapshot in ~4.3s, not the 86s cold boot.** The emulator log reads `Loading snapshot 'default_boot' ... using 4313 ms`. `AGENTS.md`'s 86s figure is right *for a cold boot* — this run had a snapshot to load. Correcting the note that first credited this run with the cold-boot number

### Two things this run found that no test could

**A second `:server:run` lost the bind race and failed silently in the plan's favour.** A server was already on 8099 (PID 13916, started 8:09 AM, before this session), so the `:server:run` this task started exited 1 with `BindException` while the *pre-existing* server answered every check. Had the run not been read closely, the proof would have been attributed to the wrong process. **Confirmed the PID owns this project's `server/build/classes` classpath before treating a live port as proof.**

**The committed `BASE_URL` is still correct.** `192.168.100.14` is this machine's current Wi-Fi address (`Get-NetIPAddress`), so no constant was edited. Had it drifted, the screen would have shown the empty card while the host answered `127.0.0.1` fine — the `10.0.2.2` trap `AGENTS.md` records, in a new form.

**Dependencies:** Task 4

**Files likely touched:** none — this is an observation, not a change. A failure here is a bug to fix in its own task, not an edit to this one.

**Estimated scope:** S (no files; minutes of wall-clock, most of it waiting on the emulator)

### Why "server stopped" and "seed unset" read the same here

Recorded so nobody re-litigates it mid-task. `RetrofitDoctorRepository.getDoctors`
returns `Paged(emptyList())` when `isNetworkFailure`
(`RetrofitDoctorRepository.kt:38-40`) — **correct**, and documented in `AGENTS.md`
for every other screen. By the time the ViewModel sees anything, both cases are
`Success(emptyList())`. Changing the repository is **Ask first** in the spec's
Boundaries, so the wording names both causes and says it cannot choose.

The cheap fix, if a real third state is wanted: `getDoctor` is a *single-entity*
read, so it lets `ApiFailure` propagate, and an unknown id returns 404. One extra
call with a non-existent id gives a true reachability signal on an existing public
route — no repository change, no new endpoint. **Not done:** this is a temporary
screen and the honest wording already shipped.

---

## Task 6: The removal proof

> **Split out of the old Task 4** so it runs *before* the docs claim it. The
> original plan asserted the screen deletes in one commit; asserting it costs one
> throwaway branch, so it gets proven.

**Description:** The claim "temporary, deletable in one commit" is load-bearing —
it is the reason the screen has no bottom-nav entry, and the reason the removal
mechanism is a KDoc rather than a build flag. A claim that is never tested is how
a debug screen reaches a real patient.

**Done 2026-10-07 on `scratch/removal-proof`, then discarded.** Ran in a
**git worktree** rather than by stashing, because the main tree had four
uncommitted `.md` files and a branch switch would have forced them to travel.

| Step | Result |
|---|---|
| Baseline before deleting anything | **174** app / 50 server, green |
| Deleted the 3 files | `SeededDataScreen.kt`, `SeededDataViewModel.kt`, `SeededDataViewModelTest.kt` |
| Removed the 3 wirings | `Screen.SeededData`, the `composable` block + its import, the gesture + 2 imports |
| `Select-String 'SeededData\|seeded_data'` over `app/src` | **NONE** |
| `:app:compileDebugKotlin :app:testDebugUnitTest :server:test` | **BUILD SUCCESSFUL**, 166 / 50 |
| `check-boundaries.ps1` | clean, 52 files |
| `check-contrast.ps1` | clean, ratchet **4** — unchanged |
| Branch | deleted; main tree still at `8c7d25d` with the screen present |

**The count moved 174 → 166, not up.** `SeededDataViewModelTest` went with the
screen, which is correct: its 8 tests could not survive it. That is also why Task 7's
floor move to 174 must be reverted rather than applied — see below.

**Acceptance criteria:**
- [x] On a scratch branch: delete `SeededDataScreen.kt` and `SeededDataViewModel.kt`, remove `Screen.SeededData` from `Routes.kt`, remove the one `composable(Screen.SeededData.route)` block from `MediQNavHost.kt`, and remove the `combinedClickable` + `onLongPress` from `SplashScreen.kt`
- [x] `.\gradlew.bat :app:compileDebugKotlin` green on that branch
- [x] `:app:testDebugUnitTest` green — `SeededDataViewModelTest` deleted **with** the screen, not left behind failing
- [x] `rg -n "SeededData|seeded_data" app/src` returns nothing on that branch
- [x] **Branch discarded.** The screen stays. This proves deletability, it does not delete

**Verification:**
- [x] `git worktree add -b scratch/removal-proof <tmp>`, make the deletions, compile, then `git worktree remove --force` and `git branch -D scratch/removal-proof`
- [x] `git status --short` afterwards — only the 4 pre-existing `.md` files, no deletion escaped

### The proof found something the KDoc got wrong

**`AppContainer.baseUrl` is now dead code.** It was added in Task 3 solely so the
screen could show its host, and after removal nothing reads it —
`Select-String 'baseUrl' app/src` returns only its own declaration plus
`RetrofitClient`'s `.baseUrl(BASE_URL)` builder call. The removal is therefore
**7 touchpoints, not 6**, and the KDoc's own instruction lists 5.

This is the point of running the proof. The claim as written was wrong in a way
only deletion could reveal, and it is exactly the kind of leftover that turns a
temporary screen into permanent dead code — an unused public API on `AppContainer`
that nothing flags. **Task 7 must add `AppContainer.baseUrl` to the removal list**
so the eventual deletion takes it too.

### One environment note for anyone repeating this

The worktree needed `local.properties` copied in: it is gitignored (`.gitignore:10`)
and machine-specific, so a fresh worktree fails with *"SDK location not found"*.
That is the worktree path, not a repo defect.

**Dependencies:** Task 4

**Files likely touched:** none on the main branch. The deletions live only on the scratch branch.

**Estimated scope:** S (no committed change; the output is the recorded result)

---

## Task 7: Ratchets and docs

> **The remainder of the old Task 4.** Its docs half landed 2026-10-07 as a
> separate correction; what is left is the four claims that depend on Tasks 4-6
> having happened. Last, deliberately — see the note at the head of the old task.

**Description:** `AGENTS.md` states it must be updated in the same change that
alters the repo, and `CONSTRAINTS.md` says a number moves only when the code moves
it. Four claims remain, and three of them are claims about observed behaviour.

**Acceptance criteria:**
- [x] `CONSTRAINTS.md:37` and `:95` raised **166 → 174**, and the `:95` note about `SeededDataViewModelTest` being uncommitted is replaced by a statement that it is committed
- [x] `AGENTS.md` records the entry point, the two-state behaviour, the live proof, and that the screen is temporary — new section "The seeded-data screen (TEMPORARY)"
- [x] `README.md` "What works" gains the entry with **its 7-touchpoint removal command** as a runnable `git rm` plus the four manual edits. The "Not done yet" list stays untouched — this is not a gap being closed
- [x] `CHANGELOG.md` `[Unreleased]` gains an `### Added` entry
- [x] The Task 5 and Task 6 outcomes are written into `tasks/plan.md` and `tasks/todo.md`, including the honest two-states-not-three result
- [x] Task 5's emulator line is recorded as **observed**, not as "should work"

> **The floor was already at 174 when this task started** — the 2026-10-07 doc
> audit moved it once `SeededDataViewModelTest` reached `HEAD` (`30d69c7`), ahead
> of this plan's Task 7. What was actually left here was the three prose claims
> and one stale number: `CONSTRAINTS.md:137` still said ":app now has 166 tests"
> in the "Not enforced" table, contradicting the 174 three lines above it.

**Verification:**
- [x] `.\.mdfiles\check-boundaries.ps1` and `.\.mdfiles\check-contrast.ps1` green — re-run 2026-10-18: boundaries clean over 56 files, contrast clean (31 token pairs, 6 status chips, ratchet 4)
- [x] `.\gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest :server:test --console=plain` green — re-run 2026-10-18 as **213 / 62**, 0 failures, counted from the JUnit XML. The 174 / 50 in this box was the v3-era count; it has since moved with `ui-doctors` (195), `ui-scheduling` (210 / 60) and `ui-appointment` (213 / 62), each recorded in `CONSTRAINTS.md`
- [x] `git diff .mdfiles/ README.md` shows **only** documentation edits — checked against the closing commit `917e0ae`, whose `--stat` lists `.mdfiles/AGENTS.md`, `.mdfiles/CHANGELOG.md`, `.mdfiles/CONSTRAINTS.md`, `README.md`, and `tasks/`, and no code
- [x] The 174 in every doc matches a measured count, not a prediction — every **live** count does: the `CONSTRAINTS.md` rows, the `AGENTS.md` header, and the `README.md` table all read 213 / 62, which is the measured pair. The 166/174/182/195/206 still in the tree are inside dated ratchet notes describing what the number *was*, not claims about what it is

**Dependencies:** Tasks 4, 5, 6

**Files likely touched:**
- `.mdfiles/CONSTRAINTS.md`
- `.mdfiles/AGENTS.md`
- `.mdfiles/CHANGELOG.md`
- `README.md`

**Estimated scope:** S (4 files, docs only)

---

## Superseded: the original Task 4, and why it was split

> Retained for the decision record. All unchecked boxes below were **moved** into
> Tasks 4-7 above, not deleted.

**The docs half is done 2026-10-07, independently of Tasks 2-3.** A doc audit found
> the standing docs had drifted from the tree, so the documentation edits below
> were made in their own commit rather than waiting for the screen. **The
> removal proof is still open** and Task 4 is not complete.
>
> Measured, `--rerun-tasks` (the task otherwise reports UP-TO-DATE and the prior
> run's XML survives, which reads as a stale number):
>
> | Fact | Docs said | Actual |
> |---|---|---|
> | `:app` tests at `HEAD` | 165 | **166** — the floor was one low from the day it was written |
> | `HomeViewModelTest` | 13 | **14** — the six listed classes sum to 166, not 165 |
> | Kotlin files | 97 | **109** |
> | `:app` main LOC | ~6,000 | **6,567** |
> | `:server` LOC | ~3,800 | **3,833** incl. tests (main alone is 2,513) — was right |
> | CI | "**No CI.** Everything here is run by hand." | **CI exists** — `ci.yml`, `android-emulator.yml`, `dependabot.yml`, added in `ef106ef`. The claim asserted the absence of the thing that commit added |
> | Lint | "No lint config" | CI runs `:app:lintDebug` and uploads the report. No detekt/ktlint, but the line was wrong as written |
>
> Two consequences beyond the numbers, now documented in both files:
>
> - ~~**The two check scripts are not in CI.**~~ **Resolved 2026-10-07, outside
>   v3.** The `gates` job in `.github/workflows/ci.yml` now runs both on
>   `ubuntu-latest` with `shell: pwsh`, in parallel with the build jobs, ~10s, no
>   `paths:` filter, no `continue-on-error`. Both were proven to fail on a real
>   violation first. `CONSTRAINTS.md`, `README.md`, and `CHANGELOG.md` updated in
>   the same change. Doing it outside v3 was right: it edits a workflow, which is
>   not this plan's surface, and it was a standalone gap rather than seeded-data
>   work.
> - **The raw-colour ratchet is narrower than it reads.** Found while mutation-
>   checking that CI job: `check-contrast.ps1:177` matches five named `Color.*`
>   constants plus the single hex `0xFFD32F2F`, so `Color(0xFF00FF00)` passes and
>   the ratchet still reads 4. Seven `Color(0x…)` sites are in `ui/` today; six are
>   the status chips (measured separately) and `NotificationsScreen.kt:122`'s
>   `Color(0xFFF5F5F5)` is measured by nothing. Recorded in `CONSTRAINTS.md` under
>   "Not enforced, and why". **Not fixed here** — widening the pattern moves the
>   bar and the floor in the same edit, which `CONSTRAINTS.md` says belongs in its
>   own change. Candidate next task.
> - **`SeededDataViewModelTest` (8) is in the worktree but uncommitted**, so the
>   worktree measures 174 and `HEAD` measures 166. The ratchet floor is set to
>   **166** — what the committed tree actually guarantees — and the original
>   165 -> 174 target moves here when this task's removal proof lands.
>
> Per `CONSTRAINTS.md`, a ratchet records what the code guarantees, so setting it
> to 174 now would have put a floor above the committed count and failed CI on a
> fresh checkout.

**Description:** `AGENTS.md` states it must be updated in the same change that
alters the repo, and `CONSTRAINTS.md` says a number moves only when the code
moves it. Also **prove the screen deletes in one commit** rather than asserting
it — that claim is load-bearing for a temporary screen, and it costs one
throwaway branch.

**Acceptance criteria:**
- [x] `CONSTRAINTS.md:37` and `:95` updated 165 -> 166 in the same commit, and the stale-165 discrepancy (measured 166) corrected rather than carried. Set to 166, not 174 — see the note at the head of this task
- [ ] **→ Task 7.** `CONSTRAINTS.md:37` and `:95` raised 166 -> 174 once Tasks 1-3 are committed, so the ratchet reaches the measured value
- [ ] **→ Task 7.** `README.md` "What works" gains one line naming the screen, its purpose as a backend check, and its removal command. The removal command must include `AppContainer.baseUrl` — Task 6 proved it is dead once the screen goes. The "Not done yet" list is untouched — this is not a gap being closed
- [x] The stale numbers corrected in the same change: `README.md` 165 -> 174 (worktree) with 109 Kotlin files and ~6,600 `:app` main lines; `AGENTS.md` 165 -> 174 and `HomeViewModelTest` 13 -> 14; `CHANGELOG.md` 165 -> 166 and 13 -> 14, since that entry records the committed state
- [x] `README.md` and `CONSTRAINTS.md` stop claiming "No CI" and "no lint". The line about the two check scripts not being in CI was **true when written and is now superseded** — they were wired into `ci.yml`'s `gates` job on 2026-10-07, so both files now say they run in CI
- [ ] **→ Task 7.** `AGENTS.md` records the entry point, the two-state behaviour, and the fact that the screen is temporary. *Note: not three-state — see Task 5.*
- [ ] **→ Task 6, done.** **Removal proven, not asserted:** on a scratch worktree, delete the 3 files and the 3 wirings, then `:app:compileDebugKotlin` green. Branch discarded. **Correction: it is 7 touchpoints, not 6** — `AppContainer.baseUrl` is dead afterwards and must be deleted too
- [x] **→ Task 6, done.** `Select-String 'SeededData|seeded_data'` over `app/src` returns **nothing** after removal, and no file outside `ui/feature/debug/seededdata/` depended on them
- [ ] **→ Task 7.** `CHANGELOG.md` `[Unreleased]` gains the entry

**Verification:**
- [ ] `.\.mdfiles\check-boundaries.ps1` and `.\.mdfiles\check-contrast.ps1` green
- [ ] `.\gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest :server:test --console=plain` green
- [ ] Manual check: `git diff .mdfiles/ README.md` shows only documentation edits

**Dependencies:** Tasks 1, 2, 3 — **superseded by Tasks 4-7 above, which split this in two**

**Files likely touched:**
- `.mdfiles/CONSTRAINTS.md`
- `.mdfiles/AGENTS.md`
- `.mdfiles/CHANGELOG.md`
- `README.md`

**Estimated scope:** S (4 files, docs only)

---

## Checkpoint D: Complete

- [x] Server up: rows render, with `DEMO-PRC-0001..0003` licences. **Server stopped: the rows are gone and the empty card appears** — two states, not three (Task 5)
- [x] Short press navigates nowhere; long press opens the screen (Task 5)
- [x] `:app:testDebugUnitTest` green at 174; `:server:test` green at 50
- [x] `check-boundaries.ps1` = 0 violations; `check-contrast.ps1` green, ratchet 4
- [x] Removal proven on a scratch worktree, not asserted (Task 6) — and it found `AppContainer.baseUrl` dies with the screen, so the removal list is 7 touchpoints not 6
- [x] `CONSTRAINTS.md`, `README.md`, `AGENTS.md`, `CHANGELOG.md` updated in the same commit (Task 7)
- [x] The 174 floor reflects a **committed** count, not a worktree count
- [x] Every new test mutation-checked: break the code, watch it fail, restore — **2 of 8 done** in Task 1 (`e.toString()`, the `.ifEmpty` fallback)
- [x] Ready for review

**v3 is complete.** Every criterion observed rather than inferred: the read path by
mutation, the screen on `emulator-5554` with the server stopped and restarted, the
removal on a scratch branch, and the docs last so no claim preceded its evidence.

**Next after this checkpoint:** the doctor-list specialty filter, planned at
`tasks/SPEC-specialty-filter.md` and held until v3 closes. It is blocked on nothing
technical — no shared files with v3 — only on this plan finishing.

---

# v2 — Token Lifecycle (Expiry, 401 Handling, Refresh)

> **Archived, not started.** 93 boxes below are open and untouched. `SessionSignal`,
> `isAuthFailure`, and `Authenticator` return 0 hits across `app/src/main` —
> verified, not assumed. Pick this up after v3.

Plan: `tasks/plan.md` (archived section). Spec:
`tasks/SPEC-token-lifecycle.md`. **D2 resolved as re-auth** — no
`POST /auth/refresh`, no `refresh_token` column, `:server` floor stays at 50.

---

## v1 — Registration and Appointment Details (complete)

Plan: `tasks/plan.md`. Scope: close the two "Known gaps" in `.mdfiles/AGENTS.md`
(registration wizard not wired to the backend; `AppointmentDetailsScreen` is a
static empty state).

**Verification commands used throughout** (all must stay green):

```powershell
.\gradlew.bat :app:compileDebugKotlin --console=plain   # ~2s warm, ~3min cold
.\gradlew.bat :app:testDebugUnitTest --console=plain    # new in Task 2
.\gradlew.bat :server:test --console=plain              # existing, 17 tests
```

**An emulator is available**: AVD `mediq_api36` (API 36). Boot it headless with
`-no-window -no-audio -no-boot-anim`, expect an ~86s cold boot, then
`:app:installDebug` and `adb shell am start -n com.example.mediq/.MainActivity`.
Sign in with `demo_patient` / `demo12345` while the server runs on port 8099.

Because of that, tasks below may now include an on-device check — but only as a
*manual confirmation of behaviour*, never as the gate. Unit tests remain the
repeatable check; one manual pass cannot catch a regression later.
`adb shell uiautomator dump /sdcard/ui.xml` reads on-screen positions reliably;
guessing tap coordinates from a screenshot is not.

---

## Task 1: Commit the uncommitted working tree as a reviewed baseline

**Description:** The working tree has 24 modified/deleted files and 25 untracked
entries, including the whole `data/` and `di/` layers and every ViewModel — none
of it committed. Committing it first makes Tasks 2-9 reviewable as small diffs
and keeps them revertable. This task changes no behaviour; it only fixes the
state the work starts from.

**Acceptance criteria:**
- [x] `git status` is clean afterwards, with no `.idea/` or `.artifacts/` staged
- [x] `.gitignore` adds `/.idea/` (it currently lists only four specific files under `.idea/`, so the rest still surfaces as untracked) and `.artifacts/`; the `app-tree.txt` snapshot is either ignored or committed deliberately
- [x] The commit message states that this is the networking + server backend work, not new work

**Verification:**
- [x] `git status --short` returns nothing
- [x] `.\gradlew.bat :app:compileDebugKotlin --console=plain` still green (a commit must not change the build)
- [x] Manual check: read `git show --stat` and confirm no build output or IDE config was captured

**Dependencies:** None

**Files likely touched:**
- `.gitignore`
- `app/src/main/java/com/example/mediq/**` (staged only, not edited)
- `server/src/**` (staged only, not edited)

**Estimated scope:** Small (no code edits — staging and one commit)

---

## Task 2: Give `:app` a real unit test suite

**Description:** There is no way to verify any new logic: `:app` depends only on
`testImplementation(libs.junit)`, `ExampleUnitTest` is an Android Studio
template, and there is no emulator. This task adds the minimum harness — a
coroutines test dependency, hand-written fake repositories, and one real test to
prove the harness works. Every later task depends on this.

ViewModels call `viewModelScope`, which needs `Dispatchers.Main`; on the JVM that
throws unless it is swapped, so `kotlinx-coroutines-test` is required rather than
optional.

**Acceptance criteria:**
- [x] `kotlinx-coroutines-test` added to `gradle/libs.versions.toml` using the existing `coroutines` version ref (`1.10.2`), and wired as `testImplementation` in `app/build.gradle.kts`
- [x] `ExampleUnitTest.kt` deleted and replaced with at least one genuine test that asserts a real value
- [x] Hand-written fakes for `AuthRepository` and `AppointmentRepository` exist under `app/src/test/`, implementing the interfaces with settable results and no mocking framework
- [x] `:app:testDebugUnitTest` reports executed tests, not "no tests found"

**Verification:**
- [x] `.\gradlew.bat :app:testDebugUnitTest --console=plain` green with a non-zero test count
- [x] Confirm the Gradle task name via `.\gradlew.bat :app:tasks --all` if `testDebugUnitTest` is not found — AGP 9 may name it differently
- [x] Manual check: deliberately break one assertion, confirm the build fails, then restore it (a harness that cannot fail is not a harness)

**Dependencies:** Task 1

**Files likely touched:**
- `gradle/libs.versions.toml`
- `app/build.gradle.kts`
- `app/src/test/java/com/example/mediq/ExampleUnitTest.kt` (deleted)
- `app/src/test/java/com/example/mediq/fake/FakeAuthRepository.kt`
- `app/src/test/java/com/example/mediq/fake/FakeAppointmentRepository.kt`

**Estimated scope:** Medium (4-5 files)

---

## Checkpoint A: Baseline and harness

- [x] `:app:compileDebugKotlin` green
- [x] `:app:testDebugUnitTest` green and actually running assertions
- [x] `:server:test` still green
- [x] Review with human before proceeding

---

## Task 3: `RegisterViewModel` wizard state machine, with unit tests

**Description:** Creates the single owner of all three registration steps' state
and all three API calls, with no UI wired yet. Written before the screens so the
logic is testable in isolation — the screen work in Tasks 4-6 then only renders
what already exists. Follows the `SignInViewModel` pattern exactly: private
constructor param, `MutableStateFlow` + `StateFlow`, `companion object` with
`viewModelFactory { initializer { RegisterViewModel(AppContainer.authRepository) } }`.

Owns: full name, phone number, date of birth, OTP code, `registrationId`,
per-step loading and error flags, and `devOtpHint`.

**Acceptance criteria:**
- [x] `RegisterViewModel` exposes one `RegisterUiState` covering all three steps, plus `requestOtp()`, `verifyOtp()`, and `register()` functions
- [x] Blank full name, blank phone, or null date of birth sets an error and does **not** call the repository
- [x] `registrationId` is stored by `verifyOtp()` and cleared when `register()` fails, so a retry re-uses it rather than re-verifying
- [x] Unit tests cover: each validation branch, a successful three-step run, a wrong OTP, and a duplicate username

**Verification:**
- [x] `.\gradlew.bat :app:testDebugUnitTest --console=plain` green
- [x] `.\gradlew.bat :app:compileDebugKotlin --console=plain` green
- [x] Manual check: confirm `domain/` gained no Android or Compose imports

**Dependencies:** Task 2

**Files likely touched:**
- `app/src/main/java/com/example/mediq/ui/feature/auth/register/RegisterViewModel.kt`
- `app/src/test/java/com/example/mediq/ui/feature/auth/register/RegisterViewModelTest.kt`

**Estimated scope:** Medium (2 files)

---

## Task 4: Registration nav graph + details screen wired to `requestOtp`

**Description:** Nests the four register destinations in a
`navigation(startDestination = Screen.RegisterDetails)` graph so all four screens
share one `ViewModelStoreOwner`, then wires the details screen to it. The screen
today collects full name and phone only, and navigates on a button press that
discards both — it must gain a **date of birth field**, because the server
rejects registration without one (`AuthServiceTest.kt:221`) and
`RegisterRequest.dateOfBirth` is nullable so the compiler will not catch it.

Also remove the local `remember` state from the screen in favour of the
ViewModel.

**Acceptance criteria:**
- [x] The four register routes live in a nested `navigation()` graph in `MediQNavHost`, and each screen obtains the ViewModel from the graph's `backStackEntry`
- [x] The details screen collects full name, phone number, and date of birth, and calls `requestOtp()` — navigation to the OTP step happens **only** on success
- [x] `Screen.RegisterOTP.route` stays argument-free, and no registration value is passed through a route string

**Verification:**
- [x] `.\gradlew.bat :app:compileDebugKotlin --console=plain` green
- [x] `.\gradlew.bat :app:testDebugUnitTest --console=plain` green
- [x] Manual check: grep the register package for `mutableStateOf` — no local form state should remain on these screens

**Dependencies:** Task 3

**Files likely touched:**
- `app/src/main/java/com/example/mediq/ui/navigation/MediQNavHost.kt`
- `app/src/main/java/com/example/mediq/ui/navigation/Routes.kt`
- `app/src/main/java/com/example/mediq/ui/feature/auth/register/RegisterDetailsScreen.kt`

**Estimated scope:** Medium (3 files)

---

## Task 5: OTP screen wired to `verifyOtp`

**Description:** Replaces the OTP screen's local `var otp` and unconditional
navigation. Verify now calls `verifyOtp()` and stores the `registrationId` in the
ViewModel; navigation to the credentials step happens only on success.

`RetrofitAuthRepository.requestOtp` returns the OTP string because the server has
`returnCodeToCaller = true`. Show it as a clearly-labelled dev-only hint, and
never require it — the screen must work when the server returns no code.

**Acceptance criteria:**
- [x] Verify calls `verifyOtp()`, and a wrong code shows the server's message without clearing the typed code or advancing
- [x] The lockout case (five wrong attempts, per `AuthServiceTest`) shows a distinct message rather than the generic failure
- [x] The dev OTP is displayed only when non-empty, labelled as dev-only
- [x] Tests cover the success and wrong-code paths against `FakeAuthRepository`

**Verification:**
- [x] `.\gradlew.bat :app:testDebugUnitTest --console=plain` green
- [x] `.\gradlew.bat :app:compileDebugKotlin --console=plain` green
- [x] `.\gradlew.bat :server:test --console=plain` green — the lockout threshold must match `AuthServiceTest`

**Dependencies:** Task 4

**Files likely touched:**
- `app/src/main/java/com/example/mediq/ui/feature/auth/register/RegisterOTPScreen.kt`
- `app/src/test/java/com/example/mediq/ui/feature/auth/register/RegisterViewModelTest.kt`

**Estimated scope:** Small (2 files)

---

## Task 6: Credentials and success screens wired to `register`

**Description:** The last step. `register()` returns an `AuthSession` and
`RetrofitAuthRepository.register` persists it via `TokenStore`, so the user is
**already signed in** when the success screen renders. The success screen also
hardcodes "Welcome to MediQ, Jesse.", which must come from the session instead.

Client-side checks (password >= 8, passwords match) mirror the server so the user
gets an immediate answer; the server stays the authority.

See Open Question 1 in the plan — recommended default is to navigate to
`Screen.Home` and pop the auth stack rather than sending a signed-in user to the
sign-in form.

**Acceptance criteria:**
- [x] Mismatched or under-8-character passwords set an error and never call `register`; a duplicate username shows the server's message
- [x] On success the screen shows the real name from the returned `AuthSession`, and the hardcoded "Jesse" string is gone
- [x] If `registrationId` is null on this step (e.g. the process was killed after OTP verification), navigate back to the details step instead of calling `register`
- [x] Tests cover: successful registration, password mismatch, duplicate username, and the missing-`registrationId` guard

**Verification:**
- [x] `.\gradlew.bat :app:testDebugUnitTest --console=plain` green
- [x] `.\gradlew.bat :app:compileDebugKotlin --console=plain` green
- [x] `.\gradlew.bat :server:test --console=plain` green
- [x] Optional sub-item: `server/scripts/smoke.ps1` has no OTP or register coverage — add checks for `POST /auth/otp/request`, `/verify`, `/register` if HTTP-level proof is wanted
- [x] Manual check: read `Logout`/`SignIn` navigation in `MediQNavHost` and confirm the back stack does not return a signed-in user to the wizard
- [x] Emulator walkthrough: register a new account end-to-end. `AuthService.returnCodeToCaller` is `true`, so the OTP comes back in the HTTP response — read it from `adb logcat | Select-String okhttp` rather than guessing, then confirm the new account can sign in

**Dependencies:** Task 5

**Files likely touched:**
- `app/src/main/java/com/example/mediq/ui/feature/auth/register/RegisterCredentialsScreen.kt`
- `app/src/main/java/com/example/mediq/ui/feature/auth/register/RegisterSuccessScreen.kt`
- `app/src/test/java/com/example/mediq/ui/feature/auth/register/RegisterViewModelTest.kt`

**Estimated scope:** Medium (3 files)

---

## Checkpoint B: Registration

- [x] All three wizard steps covered by unit tests against a fake repository
- [x] Every registration failure mode in `AuthServiceTest` has a UI path
- [x] `:app:compileDebugKotlin` and `:app:testDebugUnitTest` green
- [x] Both "not connected to the backend" bullets in `.mdfiles/AGENTS.md` now describable as done

---

## Task 7: `AppointmentDetailsViewModel` and a real details screen

**Description:** The screen already receives `appointmentId` from its route and
ignores it, always rendering `EmptyState`. Add a ViewModel that loads the
appointment and render it.

Must follow the `AGENTS.md` rule strictly: expose `LoadState<Appointment>` and
`when` over all three cases in the UI.

**Trap to avoid:** `AGENTS.md` says `BackendNotConnectedException` maps to
`LoadState.Success(emptyList())` on **list** reads only. This is a single-entity
read, so it maps to `LoadState.Error` — copying `AppointmentsViewModel`'s catch
block here would be wrong.

**Acceptance criteria:**
- [x] `AppointmentDetailsViewModel` loads by the route's `appointmentId` and exposes `LoadState<Appointment>`; the screen `when`s over Loading, Success, and Error
- [x] A null `appointmentId` renders a clear empty state and makes no network call
- [x] The screen displays doctor, date, time, location, fee via `Money.format()`, status via `displayName`, and reason for visit
- [x] Dates and times render through `toClinicDate()` / `toClinicTime()`, never device-local zone

**Verification:**
- [x] `.\gradlew.bat :app:testDebugUnitTest --console=plain` green — tests cover success, error, and the null-id case
- [x] `.\gradlew.bat :app:compileDebugKotlin --console=plain` green
- [x] Manual check: confirm no `EmptyState("No appointment selected")` string remains as the main content
- [x] Emulator walkthrough: open an appointment from the Appointments tab and confirm the details render with real server data

**Dependencies:** Task 2 (fakes) — independent of Tasks 3-6, so it can run in parallel

**Files likely touched:**
- `app/src/main/java/com/example/mediq/ui/feature/appointments/AppointmentDetailsViewModel.kt`
- `app/src/main/java/com/example/mediq/ui/feature/appointments/AppointmentDetailsScreen.kt`
- `app/src/test/java/com/example/mediq/ui/feature/appointments/AppointmentDetailsViewModelTest.kt`

**Estimated scope:** Medium (3 files)

---

## Task 8: Cancel and reschedule actions gated on `AppointmentStatus.isActionable`

**Description:** `AppointmentRepository` already exposes `cancel` and
`requestReschedule`, and `AppointmentStatus.isActionable` already encodes which
statuses permit a change. Wire both actions, and refresh the appointment after a
successful mutation so the status badge updates.

Note the server-side rule from `AGENTS.md`: cancelling deletes the `slot_claims`
row to free the slot, so this cannot be done by updating status alone. Do not
modify `schema.sql` or the booking transaction.

**Acceptance criteria:**
- [x] Cancel and Reschedule render **only** when `status.isActionable`, and are absent for `COMPLETED`, `CANCELLED`, and `DECLINED`
- [x] A successful cancel or reschedule request refreshes the displayed appointment; a failure shows the server message and leaves the appointment unchanged
- [x] Reschedule requires a chosen slot and submits a `RescheduleRequest` with the real `appointmentId`
- [x] Tests cover: cancel success, cancel failure, reschedule success, and the `isActionable` gate

**Verification:**
- [x] `.\gradlew.bat :app:testDebugUnitTest --console=plain` green
- [x] `.\gradlew.bat :app:compileDebugKotlin --console=plain` green
- [x] `.\gradlew.bat :server:test --console=plain` green — `AppointmentLifecycleTest` guards the cancel path; it must not be weakened
- [x] Manual check: `git diff --stat server/` is empty — this task must not touch the server
- [x] Emulator walkthrough: cancel an appointment, confirm the status badge updates and the slot reappears as bookable in `server/scripts/smoke.ps1`'s rebooking check

**Dependencies:** Task 7

**Files likely touched:**
- `app/src/main/java/com/example/mediq/ui/feature/appointments/AppointmentDetailsViewModel.kt`
- `app/src/main/java/com/example/mediq/ui/feature/appointments/AppointmentDetailsScreen.kt`
- `app/src/test/java/com/example/mediq/ui/feature/appointments/AppointmentDetailsViewModelTest.kt`

**Estimated scope:** Medium (3 files)

---

## Checkpoint C: Appointment details

- [x] Details screen renders a real appointment in all three `LoadState` cases
- [x] Cancel and reschedule absent for completed, cancelled, and declined appointments
- [x] `:server:test` still green
- [x] Review with human before proceeding

---

## Task 9: Update `.mdfiles/AGENTS.md` and `.mdfiles/CHANGELOG.md`

**Description:** `.mdfiles/AGENTS.md` states it "must be updated in the same
change that alters the repo" and is "the only orientation a new session gets".
Both gaps it lists are now closed, and the two new facts a session would
otherwise waste a diagnostic pass rediscovering are the test commands and the
registration nav graph.

**Acceptance criteria:**
- [x] Both "Known gaps" bullets about the registration flow and `AppointmentDetailsScreen` are **removed**, not reworded — per that file's own rule, an item leaves only once it is implemented
- [x] The "Build environment" section documents `:app:testDebugUnitTest` alongside `:app:compileDebugKotlin`
- [x] The "Architecture" section records that the four register screens share one graph-scoped `RegisterViewModel`
- [x] The "Docs location" section names `tasks/` so it no longer contradicts the plan files
- [x] `.mdfiles/CHANGELOG.md` gains entries under `[Unreleased]` for the wizard and the details screen

**Verification:**
- [x] Manual check: re-read `AGENTS.md` top to bottom and confirm every claim is still true of the tree
- [x] Manual check: confirm no gap bullet claims work that is not implemented
- [x] `git diff .mdfiles/` shows only documentation edits

**Dependencies:** Tasks 6 and 8

**Files likely touched:**
- `.mdfiles/AGENTS.md`
- `.mdfiles/CHANGELOG.md`

**Estimated scope:** Small (2 files)

---

## Checkpoint D: Complete

- [x] Both "Known gaps" bullets removed from `AGENTS.md`
- [x] All acceptance criteria met across Tasks 1-9
- [x] `:app:compileDebugKotlin`, `:app:testDebugUnitTest`, `:server:test` all green
- [x] Ready for review

---

# v2 — Token Lifecycle (Expiry, 401 Handling, Refresh)

Plan: `tasks/plan.md` → Token Lifecycle. Spec:
`tasks/SPEC-token-lifecycle.md`. **D2 resolved as re-auth** — no
`POST /auth/refresh`, no `refresh_token` column, `:server` floor stays at 50.

**Verification commands used throughout** (all must stay green):

```powershell
.\gradlew.bat :app:compileDebugKotlin --console=plain
.\gradlew.bat :app:testDebugUnitTest --console=plain     # floor 165, target >= 172
.\gradlew.bat :server:test --console=plain               # floor 50, unchanged
.\.mdfiles\check-boundaries.ps1                          # ui-imports-data = 0
.\.mdfiles\check-contrast.ps1
.\server\scripts\smoke.ps1 -Base http://127.0.0.1:8099   # needs :server:run up
```

Two traps to carry into the tasks:

- **`TokenStore` takes a `android.content.Context` and there is no Robolectric**,
  so a `TokenStoreTest` cannot construct one. Validation has to be a pure
  function over bytes.
- **OkHttp's `Authenticator` re-sends whatever it is handed**, and
  `MediQApiService` has five non-idempotent routes. Without an explicit method
  check a 401 on a booking re-issues the `POST`.

---

## Task 1: `ApiFailure.isAuthFailure`

**Description:** One flag beside the existing `isNetworkFailure`, keyed on
`status == 401`. This is what lets the prompt and the tests talk about "the
token was rejected" without pattern-matching on a message string. Pure
additive change to a file that is already compiled into `:server` via
`sharedDomain`, so it must not import `android` or `androidx.compose`.

**Acceptance criteria:**
- [x] `isAuthFailure` is a `val` property on `ApiFailure`, `get() = status == 401`, declared immediately after `isNetworkFailure` (`ApiFailure.kt:38`)
- [x] KDoc states the contrast with `isNetworkFailure`: one means "never reached the server", the other "reached it and was told who you are is no longer valid"
- [x] `ApiErrorsTest` gains cases for a 401, a 403, and a network failure, asserting `isAuthFailure` and `isNetworkFailure` are independent

**Verification:**
- [x] `.\gradlew.bat :app:testDebugUnitTest --console=plain` green
- [x] `.\gradlew.bat :server:test --console=plain` green — `ApiFailure` is in `sharedDomain`, so a bad import breaks the server build
- [x] `.\.mdfiles\check-boundaries.ps1` green

**Dependencies:** None

**Files likely touched:**
- `app/src/main/java/com/example/mediq/domain/model/ApiFailure.kt`
- `app/src/test/java/com/example/mediq/data/api/ApiErrorsTest.kt`

**Estimated scope:** XS (2 files)

---

## Task 2: `SessionSignal` in `domain/`

**Description:** The `ui/`↔`data/` seam. A `MutableStateFlow<Boolean>` owned by
`AppContainer`, created before the Retrofit client and handed to both the OkHttp
stack and the nav host. It lives in `domain/` so `ui/` can read it without
importing `data/`, and it must not import `android` or Compose.

**Acceptance criteria:**
- [x] `domain/model/SessionSignal.kt` owns a `MutableStateFlow<Boolean>` exposed as `StateFlow<Boolean>`, plus `fun markExpired()` and `fun clear()`
- [x] It starts `false`; a fresh install and a signed-out patient are indistinguishable to the nav host
- [x] KDoc names the bug in the past tense — five screens each rendering an unactionable "Please sign in again."
- [x] `MediQNavHost` importing `AppContainer.sessionSignal` is not a boundary violation; `check-boundaries.ps1` still reports `ui-imports-data` = 0

**Verification:**
- [x] `.\gradlew.bat :app:testDebugUnitTest --console=plain` green
- [x] Tests cover: starts false, `markExpired()` flips it, `clear()` flips it back
- [x] `.\.mdfiles\check-boundaries.ps1` green — `domain-imports-android` = 0
- [x] Manual check: `Select-String -Path domain/model/SessionSignal.kt -Pattern 'import (android|androidx)'` returns nothing

**Dependencies:** Task 1

**Files likely touched:**
- `app/src/main/java/com/example/mediq/domain/model/SessionSignal.kt` (new)
- `app/src/test/java/com/example/mediq/domain/model/SessionSignalTest.kt` (new)

**Estimated scope:** S (2 files)

---

## Task 3: Pure session validation + `TokenStore` validate-on-read

**Description:** Kills Bug 2 at the boundary. Today `getSession()` wraps only
the Gson parse (`TokenStore.kt:29`), so a blob whose `expiresAt` will not parse
survives the read and throws later at `ApiMappers.kt:52`, outside every
`runCatching` — leaving the Profile screen wedged in `LoadState.Error` with the
token still stored.

**The constraint:** `TokenStore`'s constructor takes a `Context` and there is no
Robolectric, so validation must be a pure function over bytes that tests can
call directly. `TokenStore` keeps only the `SharedPreferences` call.

**Acceptance criteria:**
- [x] A pure function maps a session blob to a domain session or null, and calls `toDomain()` **inside** its own guard so a malformed `expiresAt` yields null rather than throwing
- [x] `TokenStore.getSession()` uses that function; when a stored blob exists but will not map, the key is removed and null is returned
- [x] `currentSession()` returns null for a corrupt blob without throwing, and the store is empty afterwards
- [x] The null-`getSession()` case (no key at all) does not call `clearSession()`
- [x] KDoc records why the parse was moved inside the guard, not that it "throws"

**Verification:**
- [x] `.\gradlew.bat :app:testDebugUnitTest --console=plain` green
- [x] Tests cover: valid blob, malformed `expiresAt`, malformed `dateOfBirth`, empty string, absent key
- [x] Tests assert no exception escapes — `@Test(expected = ...)` is the wrong tool here, assert the returned null
- [x] Manual check: `.\gradlew.bat :app:compileDebugKotlin --console=plain` green

**Dependencies:** Task 1

**Files likely touched:**
- `app/src/main/java/com/example/mediq/data/api/TokenStore.kt`
- `app/src/main/java/com/example/mediq/data/api/SessionMapper.kt` (new — the pure function)
- `app/src/test/java/com/example/mediq/data/api/SessionMapperTest.kt` (new)
- `app/src/test/java/com/example/mediq/data/repository/RetrofitAuthRepositoryTest.kt` (new — first test in `data/repository/`)

**Estimated scope:** M (4 files)

---

## Checkpoint A: Signal and validation

- [x] `:app:compileDebugKotlin` green
- [x] `:app:testDebugUnitTest` green, above the 165 floor
- [x] `:server:test` green at 50
- [x] `check-boundaries.ps1` reports `domain-imports-android` = 0
- [x] Review with human before proceeding

---

## Task 4: `SessionAuthenticator` — clear, signal once, retry reads only

**Description:** The core of D1. OkHttp's `Authenticator` is what re-issues a
request; `AuthInterceptor` cannot retry because an interceptor only sees the
response. Three responsibilities, in order: refuse to re-issue a write, clear
the dead token, raise the signal.

**The write guard is the load-bearing part.** `Authenticator` re-sends whatever
request it was handed, and `MediQApiService` declares five non-idempotent
routes: `POST /appointments`, `DELETE /appointments/{id}`, `PATCH`,
`PUT /…`, `POST /…/reschedule-request`. `POST /appointments` has no idempotency
key, so a retried write is how a patient gets double-booked — the first attempt
may have succeeded server-side and lost its response.

**Acceptance criteria:**
- [x] Returns null for any method outside the read allow-list, so no non-idempotent request is ever re-issued
- [x] Clears `TokenStore` on every 401 it handles, so a dead token is not re-sent on the next request
- [x] Calls `sessionSignal.markExpired()` — and the signal is **idempotent**, so five concurrent 401s produce one state change, not five
- [x] Caps retries per request: a response carrying more than one prior 401 attempt returns null rather than looping
- [x] KDoc names the double-booking hazard in the past tense

**Verification:**
- [x] `.\gradlew.bat :app:testDebugUnitTest --console=plain` green
- [x] Tests cover: 401 on a `GET` re-issues exactly once; 401 on `POST` / `DELETE` / `PATCH` / `PUT` re-issues **zero** times; five concurrent 401s → one signal transition; the store is empty afterwards
- [x] **Mutation check:** temporarily allow `POST` in the allow-list, confirm the write test fails, restore. A guard nobody has broken is not known to work.
- [x] Manual check: `.\gradlew.bat :app:compileDebugKotlin --console=plain` green

**Dependencies:** Tasks 2, 3

**Files likely touched:**
- `app/src/main/java/com/example/mediq/data/api/RetrofitClient.kt` — `SessionAuthenticator` beside `AuthInterceptor`
- `app/src/test/java/com/example/mediq/data/api/SessionAuthenticatorTest.kt` (new)

**Estimated scope:** M (2 files)

---

## Task 5: `AppContainer` owns the signal; hand it to the OkHttp stack

**Description:** Wiring only. Ordering is the constraint:
`AppContainer.kt:41` builds the Retrofit client before any repository, so the
signal has to be constructed above line 41 and threaded through
`RetrofitClient.create(tokenStore, sessionSignal)`.

**Acceptance criteria:**
- [x] `AppContainer` creates the `SessionSignal` first and exposes it as a `lateinit` alongside the existing ones
- [x] `RetrofitClient.create` takes the signal and registers the `Authenticator` on the `OkHttpClient.Builder` alongside the two interceptors
- [x] Every existing repository is still constructed from the same `api` instance — no second Retrofit client
- [x] The `debug`/`release` `buildLoggingInterceptor` pair is untouched

**Verification:**
- [x] `.\gradlew.bat :app:compileDebugKotlin --console=plain` green
- [x] Manual check: `git diff app/src/main/java/com/example/mediq/di/AppContainer.kt` is wiring only, no logic
- [x] Emulator: sign in, browse Doctors (public route, no token), confirm no crash and no spurious prompt

**Dependencies:** Task 4

**Files likely touched:**
- `app/src/main/java/com/example/mediq/di/AppContainer.kt`
- `app/src/main/java/com/example/mediq/data/api/RetrofitClient.kt`

**Estimated scope:** S (2 files)

---

## Task 6: `currentSession()` contract simplification

**Description:** Once `getSession()` validates on read, `currentSession()`'s
own expiry branch is redundant: null already means "no usable session". Leaving
both means two places decide what a dead session is, and they will disagree.

**Acceptance criteria:**
- [x] `currentSession()` returns `tokenStore.getSession()`'s domain result directly, with the `isExpired` branch removed
- [x] An expired session still returns null and leaves the store empty — the expiry test moves into the store, it does not disappear
- [x] `ProfileViewModel.kt:58`'s generic `catch (e: Exception)` path is no longer reachable from a corrupt blob; if a guard is still wanted there, it is documented as defence-in-depth
- [x] Tests cover: expired → null + store cleared; corrupt → null + store cleared + no throw; valid → the session

**Verification:**
- [x] `.\gradlew.bat :app:testDebugUnitTest --console=plain` green
- [x] `.\gradlew.bat :server:test --console=plain` green
- [x] Manual check: `rg isExpired app/src/main` — every remaining call site is one that can actually see an unexpired-but-stored blob

**Dependencies:** Task 3

**Files likely touched:**
- `app/src/main/java/com/example/mediq/data/repository/RetrofitAuthRepository.kt`
- `app/src/test/java/com/example/mediq/data/repository/RetrofitAuthRepositoryTest.kt`

**Estimated scope:** S (2 files)

---

## Checkpoint B: Request path

- [x] Five concurrent 401s produce exactly one signal transition — proven by test, not by inspection
- [x] A 401 on `POST /appointments` issues zero additional requests
- [x] A corrupt `expiresAt` yields null and an empty store, with no throw
- [x] `:app:testDebugUnitTest` green
- [x] Review with human before proceeding

---

## Task 7: `MediQNavHost` observes the signal, one-tap sign-in

**Description:** Closes acceptance criteria 1-3. Today the signal has nowhere to
land; the nav host is the only place that can redirect from any screen without
adding a seventh sign-in entry point.

**The prompt is a modal, not a screen replacement** (Open Question 1 in the
spec). A modal satisfies "one tap, from that screen" and stops the patient
tapping cancel/reschedule against a token the server has already rejected.

**`LoadState.Error` is not an option here.** It carries a `String` and nothing
else (`LoadState.kt:18`), so threading a 401 through it would change 10
ViewModels and their tests. The signal already carries the fact.

**Acceptance criteria:**
- [x] A single `LaunchedEffect` at the nav host collects the signal; no screen-level 401 handling is added
- [x] On `true`, one modal with a sign-in action appears, dismissable only by signing in
- [x] The sign-in action navigates to `Screen.SignIn` with `popUpTo` that **keeps** the booking back stack entry rather than clearing the whole stack
- [x] After a successful sign-in the patient lands on Home and the prompt is gone — Open Question 2, decided
- [x] `SignInScreen`'s existing success navigation is reused, not duplicated
- [x] The signal is cleared when the modal resolves, so a rotate does not re-prompt

**Verification:**
- [x] `.\gradlew.bat :app:testDebugUnitTest --console=plain` green
- [x] `.\gradlew.bat :app:compileDebugKotlin --console=plain` green
- [x] `.\.mdfiles\check-boundaries.ps1` green — `ui-imports-data` = 0
- [x] Manual check: `rg "status ==" app/src/main/java/com/example/mediq/ui` returns nothing; no screen branches on 401 itself
- [x] Emulator: `$env:MEDIQ_TOKEN_TTL_MINUTES="2"`, sign in, wait, confirm **one** prompt per screen on Home, Doctors, Appointments and Profile

**Dependencies:** Task 5

**Files likely touched:**
- `app/src/main/java/com/example/mediq/ui/navigation/MediQNavHost.kt`
- `app/src/main/java/com/example/mediq/ui/navigation/Routes.kt`

**Estimated scope:** M (2 files)

---

## Task 8: Clear the signal on sign-out

**Description:** The one High-severity risk in the spec. `signOut()` clears the
token but nothing else, so the signal would keep its last value and the next
patient to sign in on that device could inherit a stale prompt. Small task,
carried on its own so it is not forgotten inside Task 5's wiring.

**Acceptance criteria:**
- [x] `signOut()` clears the signal alongside `TokenStore.clearSession()`
- [x] A sign-out followed by a sign-in shows no prompt
- [x] Tests cover the ordering — signal cleared even when the server call fails (`ProfileViewModel.kt:44` already `runCatching`s it)

**Verification:**
- [x] `.\gradlew.bat :app:testDebugUnitTest --console=plain` green
- [x] Manual check: `ProfileViewModelTest` has a sign-out-then-sign-in case
- [x] Emulator: sign out, sign back in, confirm no prompt appears

**Dependencies:** Tasks 5, 6

**Files likely touched:**
- `app/src/main/java/com/example/mediq/data/repository/RetrofitAuthRepository.kt`
- `app/src/test/java/com/example/mediq/data/repository/RetrofitAuthRepositoryTest.kt`

**Estimated scope:** XS (2 files)

---

## Task 9: `smoke.ps1` — a booking across an expiry produces exactly one `POST`

**Description:** Success criterion 4 is an HTTP-level claim, and a unit test
cannot make it. The script currently exercises `DELETE /appointments/{id}` and
no auth-expiry path, so the guarantee that matters most — no double-booking —
has no repeatable check above the emulator.

**Acceptance criteria:**
- [x] `smoke.ps1` signs in, waits past the TTL, then submits a booking and asserts exactly **one** `POST /appointments` reached the server
- [x] The assertion fails loudly on two POSTs; it does not merely warn
- [x] Existing `smoke.ps1` checks are unchanged and still pass

**Verification:**
- [x] `.\server\scripts\smoke.ps1 -Base http://127.0.0.1:8099` green against `:server:run` with `MEDIQ_TOKEN_TTL_MINUTES=2`
- [x] `.\gradlew.bat :server:test --console=plain` green at 50
- [x] Manual check: `git diff server/` shows only `scripts/smoke.ps1` — no route, no schema change (D2 = re-auth means none is needed)

**Dependencies:** Tasks 4, 7

**Files likely touched:**
- `server/scripts/smoke.ps1`

**Estimated scope:** S (1 file)

---

## Checkpoint C: Prompt

- [x] `MEDIQ_TOKEN_TTL_MINUTES=2` verified on the emulator — one prompt per screen, never three
- [x] A booking submitted across an expiry produces exactly one `POST`, checked in `smoke.ps1`
- [x] A corrupt `expiresAt` recovers on its own, with no app-data clear
- [x] `:server:test` green at 50
- [x] Review with human before proceeding

---

## Task 10: `AGENTS.md`, `CHANGELOG.md`, `README.md`, `CONSTRAINTS.md`

**Description:** `AGENTS.md` states it must be updated in the same change that
alters the repo, and is the only orientation a new session gets. The gap
bullet this closes is named there, and success criterion 5 says the fix does not
count as done without it.

**Acceptance criteria:**
- [x] The token-expiry gap bullet is **removed**, not reworded — per that file's own rule an item leaves only once it is implemented
- [x] Auth section records the `SessionSignal` seam, so the next session does not re-derive it
- [x] Auth section notes that a refresh endpoint can be added later without changing the seam, and that D2 chose re-auth
- [x] `CONSTRAINTS.md` ratchets updated **in the same commit**: `:app` floor 165 → the new count, `:server` floor stays 50 (D2 added no route, so there is nothing to move)
- [x] `README.md:146-147` and the `CHANGELOG.md` `[Unreleased]` section both updated

**Verification:**
- [x] Manual check: re-read `AGENTS.md` top to bottom and confirm every claim is true of the tree
- [x] Manual check: no gap bullet claims work that is not implemented
- [x] `git diff .mdfiles/` shows only documentation edits
- [x] `.\.mdfiles\check-boundaries.ps1` and `.\.mdfiles\check-contrast.ps1` green

**Dependencies:** Tasks 7, 8, 9

**Files likely touched:**
- `.mdfiles/AGENTS.md`
- `.mdfiles/CHANGELOG.md`
- `.mdfiles/CONSTRAINTS.md`
- `README.md`

**Estimated scope:** Small (4 files, docs only)

---

## Checkpoint D: Complete

- [x] `:app:testDebugUnitTest` green at ≥172; `:server:test` green at 50
- [x] `check-boundaries.ps1` reports `ui-imports-data` = 0 and `domain-imports-android` = 0
- [x] `README.md:146-147` and the `AGENTS.md` known-gap bullet updated in the same commit
- [x] Every new test mutation-checked: break the code, watch it fail, restore
- [x] Ready for review

---

# v4 — `mediq-web` integration, module 1: `web-staff-auth`

**Spec:** `tasks/SPEC-web-staff-auth.md`. Capability map for the other four
modules and the backend decision: `tasks/SPEC-web.md`.

**Shape of this version:** the staff frontend (`mediqweb`, a sibling clone at
`../mediqweb`) signs in against this repo's Ktor `:server` instead of three
hardcoded accounts. It is the first of five modules; the other four are scoped
but unbuilt. **The web repo is outside `CONSTRAINTS.md`'s gate** — its own
`npm test` and `ng build` are its gate, and the gap is recorded in
`CONSTRAINTS.md` under "Not enforced, and why".

## Task 1: CORS, so a browser may call the server at all

**Description:** Every route already worked over HTTP; none of it worked from a
browser. A preflight is an unauthenticated `OPTIONS` request, so the plugin has to
be installed before `Authentication`, and the origin must be matched as the whole
`scheme://host:port` — Ktor 3.1.3's `allowHost(host, schemes, subDomains)` ignores
the port entirely, so it would allow every port on the machine.

**Acceptance criteria:**
- [x] `ktor-server-cors` added to the version catalog and `server/build.gradle.kts` (**disclosed deviation** — the spec said "ask first" for a new runtime dependency)
- [x] `install(CORS)` before `ContentNegotiation`, with `allowOrigins { it == origin }` per configured origin
- [x] `MEDIQ_WEB_ORIGINS` (comma-separated) with a default of `http://localhost:4200,http://127.0.0.1:4200`
- [x] `Authorization` and `Content-Type` allowed as request headers

**Verification:**
- [x] Preflight from `http://localhost:4200` → 200 with the origin echoed back and `Allow-Headers: Authorization, Content-Type`
- [x] `http://localhost:4201` and `https://evil.example` → 403 with no `Allow-Origin`
- [x] Mutation: making the install conditional fails the preflight check and nothing else (29 passed / 1 failed, 405, no `Allow-Origin`); restored

**Dependencies:** none

**Files likely touched:** `gradle/libs.versions.toml`, `server/build.gradle.kts`, `server/src/main/kotlin/com/example/mediq/server/Main.kt`, `server/src/main/kotlin/com/example/mediq/server/Db.kt`

**Estimated scope:** Small (4 files)

---

## Task 2: A staff seed — one account per role

**Description:** Each web area needs an account that can reach it. `seed()` used
to early-return when the demo flag was set, so adding staff meant restructuring it
into `seedDoctors`, `seedPatient`, and `seedStaff` rather than appending rows.

**Acceptance criteria:**
- [x] `demo_admin`, `demo_doctor`, `demo_secretary`, all with password `demo12345`
- [x] The doctor and secretary carry `users.doctor_id` for the seeded `Demo Rivera`; the admin carries none, deliberately
- [x] Idempotent per username, so booting twice does not duplicate rows
- [x] The seeded password is the demo password — demo mode only, and the web's login page now says so in the UI

**Verification:**
- [x] `StaffSignInTest` (6 tests) green; `:server:test` 62 → **68**
- [x] `smoke.ps1` signs in as all three and reads each `profile.role` back over HTTP

**Dependencies:** Task 1 (nothing to seed *for* otherwise)

**Files likely touched:** `server/src/main/kotlin/com/example/mediq/server/DemoData.kt`, `server/src/test/kotlin/com/example/mediq/server/StaffSignInTest.kt`

**Estimated scope:** Small

---

## Task 3: Staff sign-in proven over HTTP

**Description:** A store-level test cannot see a JWT claim as the route layer
reads it, and the server has no HTTP test harness. `smoke.ps1` is where an
end-to-end claim gets checked, so the staff checks went there.

**Acceptance criteria:**
- [x] Three checks — one per role — asserting the returned `profile.role` is its own
- [x] One check that a doctor's token can read the appointment list (the widening module 3 and 4 depend on)
- [x] Both CORS checks, positive and negative
- [x] Wrong password and unknown username stay indistinguishable (unchanged)

**Verification:**
- [x] `smoke.ps1` **30 passed / 0 failed** (was 24), against a seeded server on `:8099`

**Dependencies:** Tasks 1, 2

**Files likely touched:** `server/scripts/smoke.ps1`

**Estimated scope:** Small

---

## Task 4: The web's HTTP layer, session store, and bearer interceptor

**Description:** As cloned, the app had no `HttpClient` at all. This task adds the
whole request path once so no later module re-decides it: one `apiBaseUrl`, one
session store, one interceptor, one gateway per area behind an interface.

**Acceptance criteria:**
- [x] `environments/environment.ts` with `apiBaseUrl: 'http://localhost:8099'`
- [x] `StaffSessionStore` — signal + `localStorage` + expiry, tested for surviving a reload
- [x] `authInterceptor` injects `Authorization: Bearer` from the store and leaves anonymous requests alone
- [x] `HttpAuthGateway`: the server's sentence on 401/403, one written sentence for everything else (dead server, CORS, unreadable body), and **no session stored** for a `patient` role
- [x] `staff-login` resolves `AUTH_GATEWAY` to the HTTP implementation; `provideHttpClient(withInterceptors([authInterceptor]))` in `app.config.ts`

**Verification:**
- [x] 20 new web tests across `http-auth.gateway.spec.ts` (10), `staff-session.spec.ts` (8), `auth.interceptor.spec.ts` (2)
- [x] Mutation: dropping `isStaffRole` from `toSignInResult` fails exactly `HttpAuthGateway > refuses a patient`; restored byte-identically

**Dependencies:** Tasks 1, 2

**Files likely touched:** `src/environments/`, `src/app/core/services/staff-session.ts`, `src/app/core/interceptors/`, `src/app/auth/pages/staff-login/http-auth.gateway.ts`, `src/app/app.config.ts`

**Estimated scope:** Medium

---

## Task 5: Role guard and the three area trees

**Description:** A role-gated area has to be refused by the router, not by the
page it lands on — a page that decides for itself leaves the data fetch reachable
by deep link. **This is a UX gate, not a security boundary**: the server does not
yet check which role may call an endpoint, which is why module 2 must decide the
authorization mechanism.

**Acceptance criteria:**
- [x] `staffRoleGuard` per role on the admin, doctor, and secretary trees
- [x] No session, a mismatched role, and a non-staff role all return a `UrlTree` to `/login`
- [x] `StaffRole` union + `isStaffRole` in `shared/domain/`, re-exported by `auth.gateway.ts` so the domain type has one home

**Verification:**
- [x] 6 guard tests covering the matching role, both mismatches, no session, and a non-staff role

**Dependencies:** Task 4

**Files likely touched:** `src/app/core/guards/staff-role.guard.ts`, `src/app/shared/domain/staff-role.ts`, `src/app/app.routes.ts`

**Estimated scope:** Small

---

## Task 6: Delete the mock gateway, make the sample accounts honest

**Description:** `MockAuthGateway` and its three hardcoded accounts (`123123`) were
the thing being replaced, and their own comments said to delete them when the API
landed. The login page's "sample accounts" copy also claimed there was no API
connected, which stops being true here.

**Acceptance criteria:**
- [x] `mock-auth.gateway.ts` and `mock-auth.gateway.spec.ts` deleted (**disclosed deviation** — the spec said no existing spec would be deleted; 8 tests went with it)
- [x] `DEMO_STAFF_ACCOUNTS` replaces the hardcoded list and points at the server's seed
- [x] Sample-account copy rewritten: they come from the server's demo seed and exist only in demo mode
- [x] The identifier field reads "Username", because the server looks accounts up by username only (`users.email` has no sign-in lookup)

**Verification:**
- [x] `components.spec.ts` and `staff-login.spec.ts` updated, asserting against `DEMO_STAFF_ACCOUNTS` and asserting `123123` is *not* present
- [x] Web suite: **+26 added, −8 deleted = +18**; 35 → 38 spec files, 782 → 800 tests

**Dependencies:** Tasks 4, 5

**Files likely touched:** `src/app/auth/pages/staff-login/`, `src/app/shared/components/sample-accounts/`

**Estimated scope:** Small

---

## Task 7: The web's own gate, run for the first time

**Description:** The clone had never had its tests run. Its suite is the only gate
it has, since `CONSTRAINTS.md` does not cover it.

**Acceptance criteria:**
- [x] `npm test` run and its numbers recorded rather than assumed
- [x] `npx ng build` clean, with the staff-login page as its own lazy chunk
- [x] The one failure investigated rather than reported as flake

**Verification:**
- [x] **800 tests, 799 passed.** The single failure is `doctor-schedule.spec.ts > one-off dates > marks the dates that already carry an exception`
- [x] Proven **pre-existing and date-rot**, not caused by this work: `MOCK_DATED_AVAILABILITY` spaces its three exceptions `+9`, `+16`, `+23` days from today, and on 2026-10-18 `+23` is 2026-11-10 — outside the picker's ~6-week grid. Reproduced on the **stashed pristine clone**: 35 files / 782 tests / the same one failure
- [x] `ng build` clean, initial 289.77 kB, `staff-login` chunk 12.30 kB
- [x] Not fixed here: it is the doctor area's fixture, a different module's file

**Dependencies:** Tasks 4, 5, 6

**Files likely touched:** none — verification only

**Estimated scope:** Small

---

## Task 8: Docs — the floor, and the web-outside-the-gate record

**Description:** `AGENTS.md` must be updated in the same change that alters the
repo, and the honest record here includes a *new* kind of gap: a whole frontend
that the gate does not see.

**Acceptance criteria:**
- [x] `CONSTRAINTS.md` server floor 62 → **68**, with the reason and the mutation evidence recorded
- [x] `CONSTRAINTS.md` gains a row under "Not enforced, and why" naming the web as outside the gate, with its own gate's numbers
- [x] `AGENTS.md`: the `:server:test` count, the `smoke.ps1` count (24 → 30), the staff seed accounts, and a CORS / `MEDIQ_WEB_ORIGINS` section
- [x] `tasks/SPEC-web.md` written (the capability map) and `SPEC-web-staff-auth.md` closed with per-criterion verdicts and its three deviations

**Verification:**
- [x] Both check scripts green; `:server:test` 68; `:app:testDebugUnitTest` 213
- [x] The `:app` floor did not move — this module added no app test

**Dependencies:** Tasks 1–7

**Files likely touched:** `.mdfiles/CONSTRAINTS.md`, `.mdfiles/AGENTS.md`, `tasks/`

**Estimated scope:** Small (docs only)

---

## Checkpoint A: Complete

- [x] `:server:test` green at **68** (was 62); `:app:testDebugUnitTest` green at **213** (unchanged)
- [x] `smoke.ps1` green at **30 passed / 0 failed** against a seeded server, including the CORS preflight both ways
- [x] Web gate run: `npm test` 799/800 with the one failure proven pre-existing, `ng build` clean
- [x] Both halves mutation-checked, both restorations verified byte-identical
- [x] Every deviation disclosed in the spec: the `ktor-server-cors` dependency, the deleted mock spec, the username-only identifier
- [x] Ready for review

---

# v4 — `mediq-web` integration, module 2: `web-admin-accounts`

**Spec:** `tasks/SPEC-web-admin-accounts.md`. It carries the recon evidence — five
fields the admin screens display that the schema cannot produce — and the four
decisions the rest of the module waits on.

**Shape of this version:** the admin area's directories get real server reads, and
the server gets its **first role check**. Nothing here is a write. Each write needs
either a `schema.sql` column (account status) or a UI decision (specialization
create/edit, mobile number and date of birth on the create-staff form), and
building one before that decision would put a screen that looks real next to a
button that quietly does nothing. **The web half is not wired yet**, for the same
reason: the status badge and the Enable/Disable buttons sit on the same rows.

## Task 1: `listByRole`, and the two columns nobody selected

**Description:** The admin directories could not be listed at all — `UserStore` had
create, findByUsername, findById, updateProfile, and the sessions, and no way to
read a set of accounts. And two columns the screens already displayed were in the
table but in no `SELECT`: `users.created_at` ("joined on") and `users.doctor_id`
("which doctor this staff account sits behind"). `DoctorStore` could read one
doctor or many and had no counts.

**Acceptance criteria:**
- [x] `UserRow.doctorId` and `UserRow.createdAt`, in `SELECT_COLUMNS` and mapped in `readUser` (timestamps normalised at the boundary like every other store)
- [x] `UserStore.listByRole(role)`, ordered by `full_name` so two screens cannot disagree about the order of one list
- [x] `DoctorStore.specialtyCounts()` — one `GROUP BY`, **omitting** specialties with no doctors so the route fills the zero from the `Specialty` enum
- [x] No `schema.sql` change: every column used already existed

**Verification:**
- [x] `AdminAccountsTest`: `listByRole` filters and orders, and a role nobody holds comes back **empty** rather than as the whole table (what a dropped `WHERE` breaks while every other assertion still passes)
- [x] `created_at` round-trips and is not the epoch; `doctor_id` reads back after a raw `UPDATE`, and null for unlinked staff
- [x] `specialtyCounts` counts per specialty and returns **absent**, not `0`, for one nobody practises

**Dependencies:** none

**Files likely touched:** `server/src/main/kotlin/com/example/mediq/server/db/UserStore.kt`, `db/DoctorStore.kt`

**Estimated scope:** Small

---

## Task 2: The authorization mechanism, and the first gated routes

**Description:** `authenticate(AUTH_JWT)` proves a token is valid and its session
live, and says nothing about who may call the endpoint — so a doctor's token was
accepted on every authenticated route the server had. Module 2 owns this decision
because it is the first module to add a route. The mechanism is
`Caller.requireRole(vararg allowed, message)`: 403 rather than 404, the message has
no default so every route names the sentence its caller will read, and a role the
server does not recognise is refused rather than reported as a bad request.

**Acceptance criteria:**
- [x] `requireRole` in `Routes.kt`, beside `requirePrincipal`, with the reason a route added without it is readable by every role
- [x] `GET /admin/accounts` — the three directories in one response (`patients`, `doctors`, `secretaries`), admin only
- [x] `GET /admin/specializations` — all six `Specialty` entries with live counts, a missing count filled with `0`
- [x] `AdminAccountDto` / `AdminAccountsDto` / `AdminSpecializationDto` + `UserRow.toAdminDto()`, with a comment on what is deliberately absent and why
- [x] No existing DTO changed (additive only)

**Verification:**
- [x] `smoke.ps1`: 401 with no token, 200 for an admin, **403 for a doctor's token**, on both routes
- [x] Content asserted, not only status: the directory carries the seeded staff account with its `doctorId` and `createdAt`, patients and staff are in their own lists, and the six specialties include both a non-zero and a zero count
- [x] Mutation: the membership test replaced by `allowed.isEmpty()` fails **exactly** the admin 403 check (37 passed / 1 failed) and nothing else; restored
- [x] An earlier mutation attempt (`mine == null && mine !in allowed`) did not compile — the `&&` kills the smart cast — so it proved nothing and is recorded rather than passed off as evidence

**Dependencies:** Task 1

**Files likely touched:** `server/src/main/kotlin/com/example/mediq/server/http/Routes.kt`, `http/Dtos.kt`, `server/scripts/smoke.ps1`

**Estimated scope:** Medium

---

## Task 3: `AdminAccountsTest`, and the DTO asserted directly

**Description:** Server tests are store level and there is no HTTP harness, so the
split module 1 established holds: the store answers what the data *is*, `smoke.ps1`
answers what the *route* does. One test asserts on the DTO rather than the row,
because `explicitNulls = false` means a null or mismapped field leaves the response
silently instead of failing anything.

**Acceptance criteria:**
- [x] Five tests, each with **its own database** — they create accounts of overlapping roles and assert on whole lists, so a shared one would make every assertion depend on run order
- [x] The DTO test pins every field the directories read, including that `dateOfBirth` and `createdAt` reach it as strings
- [x] No existing test deleted or weakened

**Verification:**
- [x] `:server:test` **73 / 0 failures** (was 68), counted from the test-results XML rather than from the diff

**Dependencies:** Tasks 1, 2

**Files likely touched:** `server/src/test/kotlin/com/example/mediq/server/AdminAccountsTest.kt`

**Estimated scope:** Small

---

## Task 4: Docs — the floor, and the four decisions

**Description:** The floor ratchets with the code, and this module's honest record
is mostly about what it did **not** build.

**Acceptance criteria:**
- [x] `CONSTRAINTS.md` server floor 68 → **73**, with the mutation evidence and the reason for each new test
- [x] `CONSTRAINTS.md`'s `:server:test` row also corrected: its "Today" cell still read **60** while the enforced floor was already 68 — module 1's raise moved the rule and left the column behind
- [x] `AGENTS.md`: `:server:test` 68 → 73, `smoke.ps1` 30 → 38, and a paragraph on the per-route authorization rule
- [x] `README.md`: the dashboard row, the staff-web feature row, the pre-commit floor, the fixed role-gate gap, a new gap for the missing account status, and the sibling-clone note
- [x] `tasks/SPEC-web.md`: module 2's map row points at its spec, and the "no role gate" gap now names what is gated and what is not
- [x] `tasks/SPEC-web-admin-accounts.md` — the recon table, the mechanism, the acceptance verdicts, three deviations, and the four open decisions

**Verification:**
- [x] Both check scripts green; `:app:testDebugUnitTest` **213** (unchanged — this module added no app or web test); `:server:test` 73; `smoke.ps1` 38/0
- [x] No `schema.sql` change, no new dependency, no test weakened, `CONSTRAINTS.md` not weakened to pass

**Dependencies:** Tasks 1–3

**Files likely touched:** `.mdfiles/CONSTRAINTS.md`, `.mdfiles/AGENTS.md`, `.mdfiles/CHANGELOG.md`, `README.md`, `tasks/`

**Estimated scope:** Small (docs only)

---

## Task 5: Wire the admin reads and the create form (NOT STARTED — nothing gates it now)

**Description:** The endpoints exist and are proved over HTTP; no page consumes
them. Every open decision was answered on 2026-10-18 and both server halves are
built: `users.status` is real, disabling is enforced at sign-in and on every
authenticated request, `PATCH /admin/accounts/{id}/status` is behind Enable/Disable,
and `POST /admin/accounts` creates a staff account and, for a doctor, its practice
record. None of that is claimed as a web feature — this task is the web wiring
itself, which is a change of its own and cannot land in the same commit as the
server because only the server is inside this repo's gate.

**Acceptance criteria:** *(all unchecked, deliberately)*
- [ ] `AdminAccountsGateway` + `HttpAdminAccountsGateway`, following `http-auth.gateway.ts`
- [ ] `AdminSession` reads from the gateway instead of `MOCK_PATIENTS` / `MOCK_DOCTORS` / `MOCK_SECRETARIES` / `MOCK_SPECIALIZATIONS`
- [ ] The status badge and Enable/Disable wired to `PATCH /admin/accounts/{id}/status`, and the badge drawn from the server's `status` rather than a default
- [ ] The create form collects a mobile number and a date of birth (both `NOT NULL`) and, for `role = doctor`, a practice-details section (specialization, licence, years, fee, bio, building, floor, room, languages) as `POST /admin/accounts` requires
- [ ] The create button calls `POST /admin/accounts` and inserts the returned account into the list rather than re-reading it
- [ ] `specializationId` and `licenseNumber` joined from `GET /doctors` via `doctorId` rather than duplicated onto the account
- [ ] The four `*.mock-data.ts` exports for the directories deleted, and their specs moved onto a fake gateway
- [ ] `mock-notice` stays until the last admin area is wired
- [ ] A doctor created here is **not** bookable until someone gives them hours, and the screen says so rather than implying the profile is complete

**Dependencies:** Tasks 8, 9, 10 — all done, so only the work gates this

**Files likely touched:** `mediqweb-fe/src/app/admin/` (sibling clone, not this repo)

**Estimated scope:** Medium

---

## Checkpoint A: Complete for the server read half

- [x] `:server:test` green at **73** (was 68); `:app:testDebugUnitTest` green at **213** (unchanged)
- [x] `smoke.ps1` green at **38 passed / 0 failed** against a seeded server on `:8099`, including the admin 403
- [x] The role gate mutation-checked: one failure, and it was the 403 check; restored byte-identically (verified by `git diff`, which shows only the intended files)
- [x] No new dependency, no test deleted or weakened, no gate edited
- [x] The web half is **not** claimed as done — it is Task 5

---

## Task 6: The schema decisions, built — `users.status` and `specialties.description`

**Description:** All four answers landed on 2026-10-18. The status column was the
one that mattered, because it was the only decision gating a screen that already
exists — and a status nothing checks is worse than no status, so the column, the
sign-in refusal, and the per-request token refusal are one change.

**Acceptance criteria:**
- [x] `users.status VARCHAR(16) NOT NULL DEFAULT 'active'`, wire values `active` / `inactive` to match the web's `AccountStatus`
- [x] `UserStatus` in `server/.../db/` rather than `domain/model/`, because that package is compiled into the app as `sharedDomain`, has no notion of a disabled account, and the server parses what it shares from untrusted input
- [x] `AuthService.signIn` refuses a disabled account **after** the password check — checking first would tell anyone who typed a username that the account exists, which is what the shared message exists to hide
- [x] `activeSession` → `liveSession`, joining the account's status into the query that already runs on every authenticated request, so a disable bites on the next call instead of at token expiry
- [x] A status this build cannot read reads as **inactive** on both readers, so a hand-edited row can never hand out a working account
- [x] `PATCH /admin/accounts/{id}/status`, admin-only via `requireRole`, validating its input and refusing a self-disable (the direction that strands the only area that could re-enable it)
- [x] `specialties.description` seeded from `ReferenceData` and read back per specialty; the list still comes from the `Specialty` enum, so the six cannot be widened past what the patient app offers
- [x] Idempotent `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` beside each changed table, because `CREATE TABLE IF NOT EXISTS` leaves an existing database without the column while every SELECT names it

**Verification:**
- [x] `:server:test` **82 / 0** (was 73), counted from the test-results XML after `--rerun-tasks`
- [x] `smoke.ps1` **50 / 0** (was 38) against a seeded server on `:8099`
- [x] Mutation, unit: either `?: UserStatus.INACTIVE` fallback changed to `ACTIVE` fails exactly `a status this build cannot read reads as disabled` (82 tests, 1 failed)
- [x] Mutation, route: `configureAuth`'s status test replaced by a bare `session != null` fails exactly the "existing token stops working" check (49 passed / 1 failed)
- [x] Both mutations restored and verified against `git diff` and by non-ASCII character count, which is how a PowerShell round-trip would have shown damage

**Dependencies:** Task 4; the four decisions

**Files likely touched:** `server/src/main/resources/schema.sql`, `db/UserStatus.kt`, `db/UserStore.kt`, `db/AuthService.kt`, `db/DoctorStore.kt`, `ReferenceData.kt`, `http/Dtos.kt`, `http/Routes.kt`

**Estimated scope:** Medium

---

## Task 7: The status tests, and the smoke checks that make it a boundary

**Description:** The store tests answer what the data is; only `smoke.ps1` can
answer whether a *route* refuses a disabled account, because the status and the
role are read by the route layer.

**Acceptance criteria:**
- [x] `AdminAccountsTest` 5 → 11: a new account is active and setStatus flips both ways; an unreadable status reads as disabled on both readers; `liveSession` carries the status and still returns null for a revoked session; the DTO carries the status; every `Specialty` entry has a description; a null description is absent rather than blank
- [x] `StaffSignInTest` 6 → 9: a disabled account is refused **and** a wrong password still gets the shared message; an already-issued token stops working; re-enabling lets the account back in
- [x] `smoke.ps1`: 401/403/400/404 on the status route, self-disable refused, disable → sign-in refused → live token refused → re-enable → sign-in works, and a description on all six specialties
- [x] No existing test deleted or weakened; `git diff` shows the new lines only

**Verification:**
- [x] `:server:test` **82 / 0**; `smoke.ps1` **50 / 0**, and the earlier 50/0 run was on the same restored tree the mutation runs were undone to

**Dependencies:** Task 6

**Files likely touched:** `server/src/test/kotlin/com/example/mediq/server/AdminAccountsTest.kt`, `StaffSignInTest.kt`, `server/scripts/smoke.ps1`

**Estimated scope:** Small

---

## Task 8: Docs — the floor, the decisions recorded, and the gap that shrank

**Description:** The four decisions stop being questions, the floor moves with the
code, and the README gap about "no account status" becomes the two gaps that are
actually left.

**Acceptance criteria:**
- [x] `CONSTRAINTS.md` server floor 73 → **82**, with the per-test reason and both mutations recorded
- [x] `AGENTS.md`: `:server:test` 73 → 82, `smoke.ps1` 38 → 50, and the smoke line names the disable boundary
- [x] `README.md`: the dashboard row, the staff-web feature row, the pre-commit floor, the role-gate gap naming the status route, and the account-status gap replaced by the two that remain (no create route; `lastActiveOn` still absent)
- [x] `CHANGELOG.md`: the write half, including that the web is still not wired and there is no create route
- [x] `tasks/SPEC-web-admin-accounts.md`: "Open questions" → "Decisions", the part-2 section, criteria 11-18, and the schema boundary corrected
- [x] `tasks/SPEC-web.md`: module 2's row

**Verification:**
- [x] Both check scripts green; `:app:testDebugUnitTest` **213** (unchanged — no app test was added); `:server:test` 82; `smoke.ps1` 50/0
- [x] No gate script edited, no test weakened, `CONSTRAINTS.md` not weakened to pass

**Dependencies:** Tasks 6, 7

**Files likely touched:** `.mdfiles/CONSTRAINTS.md`, `.mdfiles/AGENTS.md`, `.mdfiles/CHANGELOG.md`, `README.md`, `tasks/`

**Estimated scope:** Small (docs only)

---

## Task 9: `POST /admin/accounts` — decided and built

**Description:** The answer to decision 4 was "add mobile number and date of birth
to the form and a real `POST /admin/accounts`", and the question it opened — where
a `role = doctor` account's `doctors` row comes from — was answered by putting a
**practice-details section on the same form** and writing both rows in one
transaction. The alternative ("create doctor-less staff only") was rejected: a
directory whose "Add doctor" button produces an account the doctor area cannot use
is worse than no button. Nothing is invented; every `NOT NULL` column of the
`doctors` table is required by the route and refused by name when missing.

**Acceptance criteria:**
- [x] The form grows a practice-details section, so the role selector keeps `doctor` and the route writes a real `doctors` row
- [x] `UserStore.createIn` and `DoctorStore.insert` take the caller's `Connection`, and `StaffAccountService.create` opens the single `db.tx { }` that writes both — `Database.tx` takes a fresh connection per call, so two store calls would be two transactions that can half-apply
- [x] `doctors.existsIn` refuses a secretary assigned to a doctor that is not there — `users.doctor_id` has no foreign key, so nothing else would
- [x] Mobile number and date of birth are required by the route (both `NOT NULL` today); the web form that collects them is Task 5
- [x] Admin-only via `requireRole`, with the collisions the register route already handles (username, email, mobile) answered as the same 400s
- [x] A created account is active by default, can be created `inactive`, and can sign in with the password the administrator set
- [x] `MIN_PASSWORD_LENGTH`, `normalizeUsername` and `normalizeMobile` hoisted into shared `internal` declarations, so the two write paths cannot enforce different password floors or store different forms of one username
- [x] `db/StaffAccountService.kt` owns every refusal as a sentence naming the field, including the two ranges the `doctors` table carries as `CHECK`s — a negative fee would otherwise answer 500
- [x] A `patient` role is refused, with the reason recorded

**Verification:**
- [x] `StaffAccountCreateTest` (19): the two-row write, the link, every refusal, that a refusal writes **nothing** (asserted on row counts, not only messages), the stored normal form, the no-clinic-hours consequence, and a direct `db.tx` rollback
- [x] `:server:test` **101 / 0** (was 82), counted from the test-results XML after `--rerun-tasks`
- [x] `smoke.ps1` **61 / 0** (was 50), run twice against one server to prove the section's timestamped usernames do not collide with themselves
- [x] No existing test deleted or weakened; no gate script edited

**Dependencies:** Task 8; the doctor-row decision

**Files likely touched:** `server/.../http/Routes.kt`, `db/StaffAccountService.kt` (new), `db/UserStore.kt`, `db/DoctorStore.kt`, `db/AuthService.kt`, `http/Dtos.kt`, `Main.kt`, `StaffAccountCreateTest.kt` (new), `smoke.ps1`

**Estimated scope:** Medium

---

## Task 10: Docs — the floor, the create route, and the gap that closed

**Description:** The floor moves with the code again, and the README's "the admin
directories cannot be added to yet" gap becomes the gaps that are actually left.
The web's create form is the one that matters: it still sends neither a mobile
number, a date of birth, nor any practice detail, all of which the route now
requires.

**Acceptance criteria:**
- [x] `CONSTRAINTS.md` server floor 82 → **101**, with the per-test reason, the smoke count 50 → 61, and the stale value cell (it read 73 while the floor said 82) corrected and recorded as the same drift the previous paragraph notes
- [x] `AGENTS.md`: `:server:test` 82 → 101, `smoke.ps1` 50 → 61, the per-route authorization section names the create route, and a new paragraph records the two-rows-in-one-transaction rule and the no-fields-via-a-store-call reason
- [x] `AGENTS.md` architecture list gains `db/StaffAccountService.kt`, and the username-normalisation convention now names three writers and the shared `internal` declarations
- [x] `README.md`: the dashboard row, the staff-web feature row, the pre-commit floor, the "cannot be added to yet" gap replaced (a new doctor has no clinic hours; the web form is unwired; the password is handed over out of band), and the module-status paragraph
- [x] `CHANGELOG.md`: the create route, the one-transaction write, the shared constants, and the two counts; plus the earlier read-half entry corrected where it said no create route exists
- [x] `tasks/SPEC-web-admin-accounts.md`: the status header, the fifth decision and its practice-details answer, the create-route section, the code-lives table, the boundaries, criteria 19-27, and the two deviations that moved
- [x] `tasks/SPEC-web.md`: module 2's row
- [x] `tasks/todo.md`: Task 9 closed with its verdicts, this task added, and Task 5's "still gated on Task 9" corrected

**Verification:**
- [x] Both check scripts green; `:app:testDebugUnitTest` **213** (unchanged — no app test was added); `:server:test` 101; `smoke.ps1` 61/0
- [x] No gate script edited, no test weakened, `CONSTRAINTS.md` not weakened to pass — the floor moved up, and the one edit to a value that was already wrong moved it to the enforced number

**Dependencies:** Task 9

**Files likely touched:** `.mdfiles/CONSTRAINTS.md`, `.mdfiles/AGENTS.md`, `.mdfiles/CHANGELOG.md`, `README.md`, `tasks/`

**Estimated scope:** Small (docs only)

---

## Checkpoint B: Complete for the schema and status half

- [x] `:server:test` green at **82** (was 73); `:app:testDebugUnitTest` green at **213** (unchanged)
- [x] `smoke.ps1` green at **50 passed / 0 failed** against a seeded server on `:8099`
- [x] Disabling mutation-checked at both layers: one unit test fails for the fail-closed fallback, one smoke check fails for `configureAuth`; both restored and verified
- [x] The schema changed **with a decision recorded for each column**, and with the additive `ALTER` that keeps an existing database working
- [x] The web half is **not** claimed as done — it is Task 5

---

## Checkpoint B2: Complete for the create half

- [x] `:server:test` green at **101** (was 82); `:app:testDebugUnitTest` green at **213** (unchanged)
- [x] `smoke.ps1` green at **61 passed / 0 failed** against a seeded server on `:8099`, run twice against the same server
- [x] `POST /admin/accounts` proved over HTTP as an administrator, refused over HTTP with 401 and 403, and every 400 exercised
- [x] The created doctor's practice record read back through the public `GET /doctors/{id}`, the created account signed in with the password the administrator chose, and both new rows required to appear in the directory
- [x] A refusal proved to write **nothing** — asserted on row counts, because a message-only assertion passes on a route that half-writes and then throws
- [x] The one-transaction claim proved directly: a throw after the `doctors` insert removes that row, which no public call path can reach because every refusal happens before the insert
- [x] No schema change was needed — every column the route writes already existed
- [x] The web half is **not** claimed as done — the create form still lacks the mobile number, date of birth, and practice fields; that is Task 5
