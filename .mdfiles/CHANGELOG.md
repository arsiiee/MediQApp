# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Added
- **A temporary seeded-data screen that verifies the backend from the device.**
  Long-press the "MediQ" wordmark on the splash screen to open it. It reads
  `GET /doctors` live through `DoctorRepository` and renders exactly what came
  back — doctors, `DEMO-*` licence numbers, clinic hours — or the reason there are
  none. It exists because the seed prints to a console `:server:run` scrolls away,
  the Doctors tab renders an empty list whether the seed ran or the server is
  down, and "Couldn't reach the clinic" reads the same for a dropped Wi-Fi and a
  wrong base URL.

  **No value on it is hardcoded**, and that is the design rather than a detail: a
  hardcoded list renders identically whether the backend is alive or dead, so it
  would report a healthy clinic while proving nothing. Proven on `emulator-5554`
  on 2026-10-07 — server up renders three doctors; server stopped and Re-check
  pressed empties the screen entirely; server restarted and Re-check brings them
  back. `SeededDataViewModelTest` (8) pins the state to the repository's answer
  verbatim, and both mutations (`e.toString()`, a fallback list behind the read)
  were confirmed to fail it.

  **Not a feature, and has no bottom-nav entry**, so nothing about it is visible
  to a patient. `AppContainer.baseUrl` was added so `ui/` could show the host it is
  calling without importing `data/`. Removal is **7 touchpoints, not 6** — that
  getter is dead once the screen is gone — and the deletion was proven on a
  scratch branch rather than asserted. See `AGENTS.md` "The seeded-data screen".

### Fixed
- **`GET /doctors?search=` ignored the specialty it advertises.** The Doctors
  screen is labelled *"Search doctor name or specialty"*, and the query matched
  `doctors.full_name` and nothing else. A patient who typed "dermatology" got
  zero results and no explanation, which reads as "this clinic has no
  dermatologists" rather than "the search ignored half of what I typed".
  Measured against a running server before the change: `?search=Rivera` returned
  1, `?search=dermatology` returned 0, `?search=pediatrics` returned 0.
  - `search` is now a case-insensitive substring match on the doctor's name, the
    specialty's **display name** (`Internal Medicine`, `Ob-Gynecology`), and its
    **wire value** (`internal_medicine`, `ob_gynecology`). Both specialty
    spellings are searched because neither derives from the other — a
    display-name-only match fails on the wire form, and an id-only match fails on
    anything with a space or hyphen in it.
  - **Wider result set.** Existing name matches still match, but specialty terms
    may add doctors to a page. No endpoint, DTO or schema change; pagination
    consumers should not assume searches return the previous count.
  - `building` is deliberately still *not* searched. It has its own query
    parameter and its own filter chip, so overlapping them would leave the patient
    unable to tell which one they were using. Pinned by a test.
  - Uses an `EXISTS` subquery because specialty is only a search predicate; the
    doctor row projection remains unchanged. A join with explicit doctor columns
    would also work, but the subquery keeps the filter local to `search`.
  - **A first attempt used `REPLACE(s.id, '_', ' ')` and was rejected by its own
    mutation check.** That clause was unreachable by any test — display name
    already covers every specialty where the two forms agree, and the id covers
    the rest — so no query could fail *only* because it was removed. A clause
    nothing can distinguish is dead weight; the two real columns are each covered
    by a named test. Removing the `display_name` clause now fails exactly 2 of 9.
  - New `DoctorSearchTest` (9). `:server:test` goes 50 → **59**, so the
    `CONSTRAINTS.md` floor moved with the code in the same change.
  - **Still not fixed:** an empty search result and an unreachable server both
    reach the screen as an empty list. `RetrofitDoctorRepository` maps an
    unreachable server to an empty `Paged`, which is correct everywhere else, so
    the screen cannot tell "matched nothing" from "could not reach the clinic".
- **The bottom navigation's "Appointments" label wrapped onto two lines**,
  rendering as "Appointment" / "s" and making the bar taller than its four
  siblings. Fixed in `MediQBottomBar`.
  - **The label is now "Bookings", not smaller.** The reason is arithmetic, and
    Material3's own decompiled AAR (1.4.0, BOM `2026.02.01`) supplies the terms:
    `NavigationBar` lays its items out with `Arrangement.spacedBy(8.dp)`, so each
    of five labels gets `(screenWidth - 32.dp) / 5` — **75.8 dp** at 411 dp,
    **65.6 dp** at 360 dp, **57.6 dp** at 320 dp. "Appointments" at the repo's
    `labelMedium` (12 sp, 0.5 sp) is about **78.5 dp**, which exceeds the 411 dp
    budget — hence the original wrap. "Bookings" is about **52 dp** and fits at
    every width at the default font scale.
  - **Two intermediate fixes were tried and rejected on measurement.** Dropping
    to `labelSmall` with `letterSpacing = 0.sp` fixed 411 dp but still truncated
    at 360 dp ("Appointe…") and 320 dp ("Appointm…"). `labelSmall` is also *not* a
    token in `Type.kt` — seven are defined and it is not one of them, so it falls
    through to the Material3 baseline, meaning the change traded the repo's own
    12 sp `labelMedium` for an undefined 11 sp fallback plus a hand-patched
    tracking value at the call site. Reverted to `labelMedium` untouched.
  - **An earlier diagnosis in this change's own comment was wrong.** It attributed
    the shortfall to Material3's internal label padding. There is none: the 8 dp
    is inter-item spacing in `NavigationBar`'s `Row`, and `NavigationBar` exposes
    no `arrangement` parameter, so the 32 dp is unreachable through any public API
    or `Modifier`. A custom item layout would recover the gaps and still fail at
    320 dp. The comment now states the verified mechanism.
  - **The clickable tab's accessible name starts with its visible label.** The
    bookings entry uses "Bookings, appointments" for voice control (WCAG 2.5.3).
    A JVM test caught that "Appointments" alone did not contain "Bookings", but
    an on-device Compose test caught a second bug: Material3 swallowed the icon's
    `contentDescription` in the merged clickable node. The name now belongs to
    `NavigationBarItem`'s semantics, and the icon is decorative. The screen's
    own header still reads "My appointments".
  - `maxLines = 1` with an explicit `TextOverflow.Ellipsis` is kept as a guard
    rail for any future label that is too long. Both other failure modes were
    observed on a device first: wrapping to "Appointment" / "s", and a mid-word
    clip to "Appointmen'" when `softWrap = false` arrived without an overflow.
  - **A wrong claim was caught before shipping.** This was first reported as "a
    product decision, not a defect, so I would leave it". Measuring disproved
    that: 360 dp is the most common Android width, so a truncated label there is
    a real defect. An adversarial review then found two further errors in the
    first fix — the wrong padding mechanism, and `labelSmall` not being a token —
    both corrected above.
  - Related, and *not* a bug: at 360 × 640 dp the Login button measured 6 px tall.
    That was a short-screen squeeze rather than a wrapping label, recorded so it
    is not mistaken for this defect, which it resembles.
  - **Still open — WCAG 1.4.4 Resize Text.** At `fontScale` 1.3 on a 320 dp
    screen, "Doctors", "Bookings" and "Messages" all truncate ("Docto…",
    "Booki…", "Mess…"); "Home" and "Profile" still fit. Headroom at the default
    scale is thin — "Messages" measures 57.5 dp against a 57.6 dp budget — so
    truncation begins at roughly `fontScale` 1.18 on 320 dp and 1.24 on 360 dp.
    Not fixed: 200% text in a five-item bar needs a different layout (icon-only
    at large scales), not a shorter word. `check-contrast.ps1` cannot see this —
    it measures colour pairs only, with no notion of width or truncation.
  - **Coverage boundary:** six JVM tests pin navigation label data and one
    on-device Compose test verifies the rendered accessible name; neither proves
    text is untruncated at 320 dp or with large font scaling. That still requires
    a layout test or visual check. `ExampleInstrumentedTest` remains a template.
- **A short screen can hide the sign-in button entirely.** At 320 × 568 dp the
  form is cut off after "Forgot Password?" with no scroll, and no `Login` button
  appears in the accessibility tree at all — a patient on such a device cannot
  sign in. Confirmed by `uiautomator` dump rather than by eye: the string "Login"
  is absent from the dump. It reproduces on **height**, not width alone — at
  360 × 800 dp the same screen is fine. **Not fixed**; it is a different defect
  from the nav label and was found while measuring that one.
  - Related, and *not* a bug: at 360 × 640 dp the Login button measured 6 px tall.
    That was a short-screen squeeze rather than a wrapping label, recorded here so
    it is not mistaken for the label defect it resembles.
- **The doctor list never loaded when a patient opened the screen.**
  `DoctorsViewModel` had no `init` block. `DoctorsUiState.doctors` starts as
  `LoadState.Loading` (`DoctorsViewModel.kt:26`) and only `onSearchTextChange`
  moved it, so the Doctors tab showed a spinner indefinitely unless the patient
  typed in the search box. Fixed with `init { refresh() }` — the same
  `viewModelScope.launch { loadDoctors() }` the Re-check affordance uses, so there
  is one way to start a read rather than two.
  - **The suite was green because a test pinned it.** `DoctorsViewModelTest`
    carried a test asserting the repository was *never* queried, with the message
    "`DoctorsViewModel` has no `init` load, so an unqueried list is expected". It
    described the implementation rather than the requirement, so it could never
    go red when the bug was fixed. The other 21 tests could not catch it either —
    each called `onSearchTextChange` or `onSpecialtySelected` explicitly and so
    created the state it needed.
  - Proven on the emulator, not inferred: before the fix, four taps over ninety
    seconds produced **zero** `GET /doctors` requests in logcat and one keystroke
    produced the request immediately; after it, tapping Doctors fires the request
    with no interaction. Screenshots `.artifacts/04-doctors.png` (spinner),
    `08-doctors-after-typing.png` (loads only after typing), and
    `10-doctors-fixed.png` (loads on open, all three seeded doctors).
  - Adding the initial load made **ten** existing tests fail on the off-by-one
    initial query. They were not loosened to `>= 1`; a `loadedViewModel()` helper
    lets the opening load land and clears the fake's query log, so every debounce
    and filter assertion keeps its original meaning.
  - Review exposed a second bug from that initial read: its job was not owned by
    the search or filter job, so a slow opening response could replace newer
    filtered results. A request-version guard now invalidates older reads as soon
    as text or specialty changes, including during the 300 ms debounce. Two
    deterministic delayed-response tests failed before the guard and pass after;
    the app suite rises from 180 to 182.
  - The trap is recorded in `AGENTS.md` under "A test that pins a bug is worse
    than no test", alongside the reasoning for why the counts were not relaxed.
  - Found by driving the emulator to take screenshots, which is the argument for
    having a device in the loop: no unit test, no boundary script, and no CI gate
    could have surfaced this, and `smoke.ps1` does not assert that a screen loads
    on open.
- **`README.md` claimed a shipped feature was broken.** Its status banner said "a
  newly registered account cannot cancel or reschedule its own appointments."
  That was written at `3a112fb` and fixed at `21977ad`/`24d9ef2`, but the line was
  never revisited. `AppointmentDetailsViewModel.cancel()`/`requestReschedule()`
  exist, are gated on `AppointmentStatus.isActionable`, and are covered by
  `AppointmentDetailsViewModelTest`. The banner now names three real blockers
  (OTP returned in the response body, plaintext token storage, no idempotency key
  on booking), and **What works** gained the cancel/reschedule bullet it had been
  missing — that omission is why nothing contradicted the stale banner.
- **The raw-colour ratchet is narrower than its name, and the mutation proved
  it.** `check-contrast.ps1:177` matches `Color.Gray`, `LightGray`, `DarkGray`,
  `White`, `Black`, plus the single hex `0xFFD32F2F`. Adding
  `Color(0xFF00FF00)` to a `ui/` file left the ratchet at 4 and exited 0. So
  "raw-colour ratchet = 4" means *those five constants*, not raw colour in
  general, and `CONSTRAINTS.md` now says so. Seven `Color(0x…)` sites exist in
  `ui/` today: six are the status chips, measured by a separate rule, and
  `NotificationsScreen.kt:122`'s `Color(0xFFF5F5F5)` is measured by nothing.
  Widening the pattern is a separate change — it moves the bar and the floor at
  once — and is recorded rather than done here.
- **Documentation drift, measured rather than eyeballed.** The standing docs had
  fallen behind the tree in ways that asserted the absence of things that exist.
  Three of the corrections overturned a claim rather than refreshing a number:
  - `README.md` said "**No CI.** Everything here is run by hand." CI exists —
    `.github/workflows/ci.yml`, `android-emulator.yml`, `dependabot.yml`, added
    in `ef106ef`. It runs `:server:test`, `:app:lintDebug`,
    `:app:testDebugUnitTest`, `:app:assembleDebug` on every push and PR.
  - `README.md` said "No lint config". `:app:lintDebug` is a CI gate on the stock
    Android rules. No detekt/ktlint is still true, but the line was wrong as
    written.
  - `README.md` claimed `statusBarColor` was "the one remaining compile warning".
    `--warning-mode all` reports **6** — that one plus 5 uses of the deprecated
    `KeyboardOptions` constructor at `RegisterCredentialsScreen.kt:91,115,143` and
    `SignInScreen.kt:107,134`. The default warning mode hides this behind one
    line about deprecated Gradle features.
  - `CONSTRAINTS.md` and `README.md` both said the build needs "JDK 21+". True of
    the daemon only: `:server` pins `JavaLanguageVersion.of(17)` and
    `gradle-daemon-jvm.properties` requests 25, so `:server:run` executes on
    Adoptium 17. Harmless today, but it is why a `:server:run` failure names a
    JVM the build docs never mention.
  - Numbers: `:app` tests 165 → **174**; `HomeViewModelTest` 13 → **14** (the six
    classes `CONSTRAINTS.md` lists sum to 166, not 165 — the floor was one low
    from the day it was written); Kotlin files 97 → **110**; `:app` main ~6,000 →
    **~6,900** lines; boundary-check files 52 → **54**.
  - **Gradle 10 will not build this repo.** `--warning-mode all` reports
    `Configuration.setVisible(boolean) … removed in Gradle 11`. `visible` appears
    0 times in all four build scripts, so it comes from AGP 9.3.3 or KGP 2.2.10.
    Recorded as known debt; only a plugin bump clears it.
  - The `:app` ratchet floor moves 165 → **174** in two steps, both recorded in
    `CONSTRAINTS.md`: 165 → 166 when the stale floor was corrected, then
    166 → 174 once `SeededDataViewModelTest` (8) reached `HEAD`. It is expected to
    come back **down** to 166 when the temporary seeded-data screen is deleted,
    which the v3 removal proof has already shown builds green — a lower number
    with a stated reason is the ratchet working, so that drop must be made in the
    same commit as the deletion.

### Added
- **The two `.mdfiles` check scripts are CI gates.** `check-boundaries.ps1` and
  `check-contrast.ps1` now run on every push and PR in a new `gates` job in
  `.github/workflows/ci.yml`. Until this, `CONSTRAINTS.md` described nine rows as
  "enforced with numbers" while six of them named a command that ran only when a
  human remembered — a boundary or contrast violation merged green. The recorded
  reason this repo keeps hitting is *a rule stated in prose and never checked*;
  this removes the hand-run step from that failure mode.
  - A separate job, not steps inside `android`. The scripts read source files and
    need no JVM, no Android SDK, and no Gradle, so they finish in ~10s. Hanging
    them off the Android job would park a 10-second check behind a 3-minute SDK
    download, which is the cost that teaches people to ignore a red pipeline. They
    run in parallel with both build jobs.
  - `shell: pwsh` on `ubuntu-latest`, where PowerShell is preinstalled — the job
    needs no setup step.
  - **No `paths:` filter, deliberately.** These scripts check the current state of
    the tree, not a diff; `check-boundaries.ps1`'s own header argues that a
    diff-scoped check only catches a violation on the change that introduced it and
    never on the file it was added to afterwards. Filtering by path would
    reintroduce that hole one layer up, where nothing would catch it.
  - Neither step sets `continue-on-error`. The scripts use the floor-guard exit
    contract — 0 clean, 1 violation, 2 could not run — and all three fail the step,
    because **a 2 must never read as a 0.**
  - Both were proven to fail on a real violation before being wired, rather than
    assumed: adding an `import com.example.mediq.data.api.RetrofitClient` to a
    `ui/` file made `check-boundaries.ps1` exit 1 with `ui-imports-data`, and a
    fifth `Color.Gray` made `check-contrast.ps1` exit 1 against the ratchet of 4.
    Both were restored and re-verified clean.
- **CI.** `.github/workflows/ci.yml` (server build + tests, Android lint, unit
  tests, `assembleDebug`, dependency graph) and `android-emulator.yml`
  (instrumented tests on `main` pushes, off the PR critical path because a booted
  AVD costs ~10 minutes), plus `.github/dependabot.yml` for Gradle and
  GitHub Actions. Both gate on every push and PR to `main`.
- **Unit tests for the six ViewModels that had none.** `:app:testDebugUnitTest`
  went from 71 to **166**, and all ten ViewModels are now covered. Every one of
  the 95 new tests was mutation-checked — the ViewModel was deliberately broken
  and the suite had to fail — because a test that has never failed is not
  evidence. `DoctorsViewModelTest` (22), `BookingViewModelTest` (19),
  `SignInViewModelTest` (16), `ProfileViewModelTest` (14),
  `HomeViewModelTest` (14), `NotificationsViewModelTest` (10).
  - The mutations that caught something real: swapping `SignInViewModel`'s
    `ApiFailure?.message` back to `e.message` (the `HTTP 401 ` regression this
    app already shipped once), removing its `isLoading` guard, turning
    `HomeViewModel`'s `minByOrNull` into `maxByOrNull` so the home screen would
    offer the *latest* appointment as "next", removing `ProfileViewModel`'s
    `runCatching` around sign-out, dropping `BookingSelection.clear()` on
    success, removing `DoctorsViewModel`'s debounce `delay`, and turning
    `NotificationsViewModel`'s unconnected-backend branch into an error.
  - Two harness traps are documented in `AGENTS.md` under "Two traps in the
    ViewModel tests" because each yields a **green** suite that asserts nothing:
    an eager `init` load settling before the fixture is set (build ViewModels
    with `by lazy`), and a non-suspending fake making `LoadState.Loading`
    unobservable (the `signInGate` / `getDoctorsGate` / `bookGate`
    `CompletableDeferred`s).
  - `DoctorsViewModelTest` deliberately does **not** use `MainDispatcherRule`: a
    debounce cannot be tested against an eager dispatcher, so it drives its own
    `StandardTestDispatcher` on a `TestCoroutineScheduler`.
  - The fakes grew what these tests needed and previously hardcoded: settable
    results and errors for every call, plus recording of queries, filters, and
    login attempts. Two fakes that had no class at all —
    `FakeNotificationRepository`, `FakeProfileRepository` — now exist.
- **New known gaps recorded, all found while writing the tests above.**
  - The profile **cannot be edited from the app**, and `ProfileRepository` is a
    dead constructor parameter on `ProfileViewModel` — nothing in `ui/` ever calls
    `updateProfile`. `README.md` had listed "Profile read and update" under
    **What works**; that claim is now corrected to read-only.
  - `BookingUiState.canSubmit` reads the `BookingSelection` global, so it is not
    derivable from the `StateFlow` a composable collects and will not trigger
    recomposition when the selection clears.
  - `HomeViewModel`'s two concurrent loads read-modify-write `_uiState.value`
    without serialisation, so one can clobber the other on a real dispatcher. The
    unit tests cannot see it — the test dispatcher happens to serialise them.
  - Which layer owns the unreachable-server rule is now pinned by a test rather
    than only by prose: it is the repository's job, so at the ViewModel layer an
    `ApiFailure` is honestly an error. `HomeViewModel` is the documented
    exception, mapping it to `Success(null)`.

### Fixed
- **Registration could not create an account from the app.** All four
  `ui/feature/auth/register/` screens held their values in local
  `remember { mutableStateOf(...) }` and navigated between each other on a button
  press. Nothing called `AuthRepository`, so the full name and phone number were
  discarded on Continue and **no account could be created through the UI at all**,
  while `:app:compileDebugKotlin` stayed green — not calling the API is not a
  compile error, and no test covered it. The server-side OTP and registration
  flow was already complete and tested.
  - `RegisterViewModel` now owns all three steps and all three calls
    (`requestOtp`, `verifyOtp`, `register`) in one `RegisterUiState`, and the
    four routes are nested in a `navigation()` graph so they share it. Step 3
    cannot succeed without step 2, so a per-screen ViewModel would have had to
    pass values forward by hand.
  - Each screen now advances **only on server success**, not on a tap. A wrong
    OTP keeps the typed code and shows the server's sentence, including the
    lockout's own message rather than a generic failure.
  - **The details step gained a date-of-birth field**, which is not optional:
    `AuthService` answers a null one with 400 "A date of birth is required."
    *after* its registration-id, username, email, and mobile checks, and
    `RegisterRequest.dateOfBirth` is nullable, so the compiler could not catch the
    omission. Confirmed against a running server. Collected with a date picker
    rather than a text field, because the server only accepts `YYYY-MM-DD`.
  - `registrationId` lives in the ViewModel and nowhere else. `otpVerified` is the
    UI's view of it, which doubles as the guard for the credentials step: process
    death between steps 2 and 3 loses the id, and the user is sent back rather
    than submitting a request the server can only refuse.
  - **The success screen greeted patients as "Jesse"** — a hardcoded personal name
    that no real patient shares. It now comes from the returned `AuthSession`.
  - That screen also navigated to `Screen.SignIn` after registering.
    `RetrofitAuthRepository.register` had already stored the session via
    `TokenStore`, so the user was signed in and was being asked to authenticate
    again. It now goes straight to Home and pops the auth stack.
  - `RegisterDetailsScreen`'s phone field gained `KeyboardType.Phone` and a
    `+63917…` placeholder; the old `+63 917 555 0142` placeholder taught a spaced
    format `AuthService.normalizeMobile` rejects. `RegisterViewModel` also strips
    separators before sending, mirroring the server's `^\+?[0-9]{10,15}$`.
  - Guard: `RegisterViewModelTest`, 22 tests, all against a fake
    `AuthRepository`. Each asserts **whether the repository was reached**, not just
    the resulting state — a state-only assertion would still pass against a
    ViewModel that ignored its inputs.
- **`AppointmentDetailsScreen` ignored the `appointmentId` from its route.** The
  composable took the id and rendered a fixed "No appointment selected"
  `EmptyState` for every appointment, so tapping a real appointment led to a
  screen claiming none existed. The signature asked for the value and the body
  discarded it, which neither the compiler nor any existing test could see.
  `AppointmentRepository.getAppointment` and the Retrofit call already existed and
  were unwired from any UI.
  - `AppointmentDetailsViewModel` loads by that id and exposes
    `LoadState<Appointment>`; the screen `when`s over all three cases and renders
    doctor, clinic-zone date and time, location, fee, status, and reason for
    visit.
  - A null or blank id renders a clear empty state and **makes no network call** —
    the assertion that distinguishes "loaded the appointment the user tapped" from
    "loaded something, somehow".
  - `BackendNotConnectedException` maps to `LoadState.Error` here, **not** to
    `Success(emptyList())` as on the list screens. That mapping is for reads where
    "no results" and "no backend" look the same; a single-entity success carrying
    nothing would tell a patient with a real appointment that it does not exist.
  - Guard: `AppointmentDetailsViewModelTest`, 7 tests against a fake
    `AppointmentRepository` that records the ids it was asked for.
- **Sign-in rejected correct credentials.** `MessagesScreen` aside, the cause was
  that no `OutlinedTextField` in the app set `keyboardOptions`, so Compose used
  `KeyboardOptions.Default` — `autoCorrect = true`, no `KeyboardType`. Android
  kept autocorrect and suggestions live on the masked fields and rewrote the
  characters as they were typed, so `demo12345` reached the server as a different
  string. The server answered with `"Those details don't match an account."` —
  the same message as a genuinely wrong password, so nothing on screen
  distinguished a keyboard typo from a bad credential. The build was green and
  the server was healthy the whole time; the credentials really were correct.
  `PasswordVisualTransformation` only masks what is *displayed* and says nothing
  about what the keyboard may do to the value.
  - Client: `keyboardOptions` with `KeyboardType.Password` (password, confirm
    password) and `KeyboardType.NumberPassword` (OTP), plus `autoCorrect = false`
    and `KeyboardCapitalization.None` on every identifier field.
  - Server: `AuthService.normalizeUsername` trims and lowercases at `register`
    *and* `signIn`, so the two cannot disagree. Also closes the case where
    `Case` and `case` registered as two accounts indistinguishable at sign-in.
  - Guard: `check-boundaries.ps1` Rule 4, keyed on `PasswordVisualTransformation`
    so a new secret input is covered without editing the script. Both rules were
    verified to fail on a deliberately reverted field before being accepted.
- **The sign-in message could not distinguish a typo from a wrong password.**
  Server-side normalization above means a stray space no longer produces a dead
  end, but the two cases remain deliberately indistinguishable to a caller for
  anti-enumeration reasons.

- **A patient could not cancel an appointment from the app.**
  `AppointmentRepository.cancel` existed, `DELETE /appointments/{id}` existed, and
  `AppointmentStatus.isActionable` already encoded which statuses permit a change —
  but no screen called any of them, so the actions were unreachable and
  `isActionable` gated nothing. The repository method being present is not the
  same as a patient having a button.
  - Cancel and Reschedule render only when
    `LoadState.Success.data.status.isActionable`, so they are absent for
    `COMPLETED`, `CANCELLED`, `DECLINED`, and `UNKNOWN`.
  - A successful cancel **reloads** the appointment. Cancelling deletes the
    `slot_claims` row to free the slot and only the server can change the status;
    without the reload the badge still read "Confirmed" and the screen kept
    offering a second cancel the server would refuse.
  - A refused or undeliverable change shows the server's sentence and leaves the
    displayed appointment untouched — no refresh on failure, because the server
    refused and what is on screen is still the truth. An unreachable backend says
    explicitly that the change **was not made**, since a generic "couldn't reach"
    leaves a patient unsure whether they still hold the booking.
  - Guard: `AppointmentDetailsViewModelTest`, 10 new tests. The fake repository
    records the cancelled ids and the `RescheduleRequest`s it was handed, so a
    cancel that reported success without reaching the repository cannot pass.

### Known gaps
- None outstanding from the registration work. `RegisterDetailsScreen`'s phone
  field gap listed here is fixed above, along with the unwired wizard.
- **Reschedule searches the current month only.** The picker opens on
  `YearMonth.now()` with no month stepper. `DoctorDetailsScreen` has the same
  limit, so a month stepper is one change in two ViewModels rather than two
  independent bugs.

- **Reschedule was wired but unusable.** The endpoint, the repository call, and
  `RescheduleRequest.requestedSlotId` were all correct, and the screen collected
  that id from an `OutlinedTextField` — but a slot id is a
  `UUID.nameUUIDFromBytes("$doctorId|$startsAt")`. No patient can type one, so the
  feature was reachable and impossible. Same class of bug as the unwired wizard:
  the call site existed, the thing the user has to do did not.
  - `SlotPicker` — a stateless date strip and slot grid in
    `ui/feature/booking/`, **extracted from `DoctorDetailsScreen`**, which had the
    same markup inline. Two hand-maintained copies of that chip styling is how they
    drift; the alternative was a second copy, not a shared component.
  - `AppointmentDetailsViewModel` now takes `DoctorRepository` as well:
    `/doctors/{id}/availability` and `/doctors/{id}/slots` belong to the doctor
    aggregate, and the domain split keeps them there.
  - Availability is fetched **only when the picker is opened.** Two requests on
    every visit to the screen would be spent on a picker most visits never open.
  - **Only bookable slots are offered**, filtered in the ViewModel rather than the
    composable so a test pins it. `RESERVED`, `BLOCKED`, and `UNKNOWN` are all
    excluded — and `UNKNOWN` especially, since offering a slot because its status
    might be fine is the exact guess the enum's `UNKNOWN` member exists to prevent.
  - Changing the date **discards the slot chosen on the previous one**, and closing
    the picker forgets it. Otherwise a patient picks 9:00 on Tuesday, taps Wednesday
    by mistake, and submits a Tuesday slot while reading Wednesday's list.
  - A previous slots request is cancelled when another date is tapped, for the same
    reason `DoctorDetailsViewModel` does it: a slow Tuesday response would otherwise
    land after Wednesday was chosen and replace its list with Tuesday's.
  - An unconnected backend leaves the picker **empty rather than failed** — it is a
    list read, and an error there would put a red sentence above an appointment the
    patient can still cancel.
  - Guard: `AppointmentDetailsViewModelTest`, 10 new tests. Both the bookable
    filter and the discard-on-date-change were verified by mutation — breaking
    each fails exactly its own test and nothing else.

### Changed
- **Step completion is now a consumable event, not a sticky flag.** The wizard's
  screens `LaunchedEffect` on the flag saying their step succeeded
  (`otpRequested`, `otpVerified`, `registeredName`). A flag stays true for the
  life of the ViewModel, so the effect fired again every time the user navigated
  *back* to that step — system back from the OTP screen would bounce them
  straight forward to it, trapping them in the wizard with no way out. Replaced
  with `pendingStep: RegisterStep?`, which the screen clears via `onStepHandled()`
  once it has navigated. `otpVerified` stays as real state, because the
  credentials step reads it as a guard.
- **`NavBackStackEntry.parent` is not public in navigation 2.10.1.** It is the
  obvious way to get a nested graph's back stack entry — which is how a
  graph-scoped ViewModel is shared between its destinations — and it fails to
  compile. The register graph therefore carries an explicit route and its screens
  use `navController.getBackStackEntry(Screen.RegisterFlow.route)`. Worth knowing
  before anyone copies that snippet from a tutorial written against navigation
  2.7.

### Added
- **A ViewModel test harness for `:app`.** ViewModel logic could not be unit
  tested at all before this: every ViewModel calls `viewModelScope`, which posts
  to `Dispatchers.Main`, and that dispatcher throws on the JVM unless
  `kotlinx-coroutines-test` swaps it. Added that dependency plus hand-written
  fakes (`FakeAuthRepository`, `FakeAppointmentRepository`, `TestFixtures`) and a
  `MainDispatcherRule` — no mocking framework, matching the hand-built-fake
  convention the `data/api` tests already set. A fake that records what it was
  asked for says more about a state machine than a verify-count does.
  The rule installs an `UnconfinedTestDispatcher` so a launch runs to completion
  eagerly; without it a test that never awaits its coroutine passes while proving
  nothing.
  - `ExampleUnitTest` was an untouched Android Studio template asserting
    `2 + 2 == 4` and was deleted in favour of real tests. `:app` went from 23
    tests, all in `data/api/`, to 51.
  - The harness was verified by deliberately reverting the phone sanitisation and
    confirming the test failed, then restoring it — a harness that cannot fail is
    not a harness.
- **`check-boundaries.ps1` Rule 4** — `secret-field-no-keyboard-options` and
  `secret-field-not-password-keytype`. Both ratcheted at 0; the first was **5**
  before the fix above. `CONSTRAINTS.md` records this as the third instance of
  the repo's recurring failure mode: a rule stated in prose, never checked, and
  invisible to the compiler and the full test suite.

- **README at the repo root**: A user-facing description of what MediQ is, what
  works today, and an explicit list of what does not — including that
  registration cannot create an account from the app, that `AppointmentDetails`
  and Messages are stubs, and the three security items that must close before
  any real patient. It leads with the fact that this is unfinished, because the
  app has 15 screens and a working backend and the gap between those two numbers
  is the first thing a new reader needs to know. `.mdfiles/README.md` is now an
  index of that folder rather than a second copy of the README.
- **Contrast enforcement**: `check-contrast.ps1`, which parses the real tokens out
  of `Color.kt` and the real status chip pairs out of `AppointmentsScreen.kt`,
  measures them with the WCAG 2.1 relative-luminance formula, and fails on any
  pair below its threshold or any hardcoded page background. It keeps no copy of
  the hex values, because a checker holding a stale list passes clean while the
  token is reverted to a failing one — which is exactly what happened to
  `MediQTextSecondary` before this existed.
- **Mode-aware colour roles**: `MediQExtendedColors` (accent, secondary text,
  outline) provided through `LocalMediQColors`. Material's `ColorScheme` has no
  slot for an accent drawn on the themed background, and `primary` must stay the
  dark brand green in both schemes — filled buttons take their label from
  `onPrimary`, so a light `primary` in dark mode puts white-on-pale-green labels
  on every button at 1.79:1.
- **Quality bar**: Added `CONSTRAINTS.md` and `check-boundaries.ps1`, which mechanically enforce the architecture rules `AGENTS.md` previously stated in prose only. The rules are: `domain/` imports no Android or Compose, `ui/` never imports `data/`, and every `api.` call sits inside `call { }`. All three had already been violated or ignored with fully green builds.
- **API test suite**: `ApiErrorsTest` (11 tests) covering error-body parsing, malformed bodies, and network failures. `:app` previously had no runnable unit tests.
- **Network Layer**: Added Retrofit, OkHttp, and Gson dependencies to the Android app to enable HTTP communication with the Ktor server.
- **API Models**: Created data transfer objects (DTOs) and mappers (`ApiDtos.kt`, `ApiMappers.kt`) to translate JSON data from the server into domain models safely.
- **Token Storage**: Implemented `TokenStore.kt` using `SharedPreferences` to persist user authentication sessions across app restarts.
- **Application Class**: Added `MediQApp.kt` to initialize the app's dependency graph (`AppContainer`) at startup.
- **Live Repositories**: Created real server-backed repositories (`RetrofitAuthRepository`, `RetrofitDoctorRepository`, `RetrofitAppointmentRepository`, `RetrofitNotificationRepository`, `RetrofitProfileRepository`) to replace the static `EmptyRepositories`.

### Changed
- **Every screen now follows the theme, so dark mode is legible**: `AppointmentsScreen`,
  `HomeScreen`, `DoctorsScreen`, `NotificationsScreen`, `DoctorDetailsScreen`,
  `BookingFlowScreen`, `AppointmentDetailsScreen`, the four registration screens,
  `SignInScreen`, `EmptyState` and `MediQBottomBar` all hardcoded
  `Color.White` backgrounds, `Color.Black`/`Color.Gray` text, or raw `Color(0x…)`
  status chips while `Theme.kt` follows `isSystemInDarkTheme()`. In dark mode that
  text rendered light-on-light and was invisible. All of them now read
  `MaterialTheme.colorScheme.*` or `LocalMediQColors.current.*`. Three measured
  fixes came out of it: `MediQTextSecondary` `#757575` → `#5F5F5F` (3.95:1 →
  6.39:1 on white), the pending-appointment chip label `#F9A825` → `#8F5000`
  (1.85:1 → 6.28:1 on its `#FFF8E1` container), and the notification timestamp
  off `Color.LightGray`, which was 1.50:1 and effectively invisible at 12sp.
- **Profile Screen follows the Material theme**: `ProfileScreen` hardcoded `Color.White` backgrounds, `Color.Black` text, and raw `Color(0x…)` values, while `Theme.kt` follows `isSystemInDarkTheme()`. In dark mode its text rendered light-on-light and was effectively invisible. Surfaces, labels, the role badge, the avatar, and the sign-out button now use `colorScheme.background` / `surfaceVariant` / `primaryContainer` / `error` and their `on*` pairs. This was the first of the four screens converted; the rest are in the entry above.
- **Dev Base URL**: `RetrofitClient.BASE_URL` switched from `http://10.0.2.2:8099/` to this PC's LAN address, `http://192.168.100.14:8099/`. `10.0.2.2` is an alias that exists only inside the emulator's virtual network, so on a physical phone it is unroutable, every call times out, and the app reports "Couldn't reach the clinic" while the server is answering `127.0.0.1` perfectly well — which is what made sign-in fail on 2026-10-06 with the server up and healthy. The LAN address is the one value that works on both targets. `network_security_config.xml` was updated in step to permit cleartext to it, and both must change together when the DHCP address moves.
- **Sign-In Flow**: Wired `SignInScreen` to `SignInViewModel`. The app now securely verifies credentials against the server instead of navigating blindly to the home screen.
- **Doctor Details**: Wired `DoctorDetailsScreen` to `DoctorDetailsViewModel` to fetch and display live doctor profiles, clinic hours, and available booking time slots.
- **Appointments & Notifications**: Wired `AppointmentsScreen` and `NotificationsScreen` to their respective ViewModels to pull real user data from the API instead of showing static empty states.
- **Profile Screen**: Wired `ProfileScreen` to display live user details and integrated a fully working "Sign Out" flow that clears the session.
- **App Configuration**: Updated `AndroidManifest.xml` to include the `INTERNET` permission and allow local cleartext traffic (`10.0.2.2`, `127.0.0.1`, and the LAN host) for development testing.
- **Architecture boundaries enforced**: Added `CONSTRAINTS.md` and `check-boundaries.ps1`. The rules `AGENTS.md` stated in prose — `domain/` imports no Android, `ui/` never imports `data/`, every `api.` call sits inside `call { }` — are now checked by a command rather than remembered. The first run found 7 violations of the second rule, which were fixed rather than ratcheted in.
- **`BackendNotConnectedException` moved to `domain/model/`**: Seven ViewModels imported it from `data/repository/`, violating "Screens and ViewModels must not reference `data/` directly". The compiler cannot catch this — the import compiles — so the type now lives with the other things ViewModels catch.
- **Project Rules**: Updated `AGENTS.md` to reflect the new networking architecture, note the token storage decision, and clear out resolved "Known gaps".

### Fixed
- **Correct credentials could be rejected as a wrong password**: usernames were
  stored and looked up raw, so a phone keyboard that autocorrected or
  autocompleted — `Demo_patient ` instead of `demo_patient` — produced the same
  opaque "Those details don't match an account." as a genuinely wrong password,
  leaving a patient with no way to tell a typo from a bad password. `register`
  now stores a trimmed, lowercased username and `signIn` looks up the same form,
  so the two can never disagree. The uniqueness check uses the same normalized
  value, so `Case_Collide` and `case_collide` can no longer become two accounts
  that are indistinguishable at sign-in. An empty username is rejected outright
  rather than appearing to be tried. Seven tests in `AuthServiceTest` pin this,
  including one that normalization must not weaken the password check.
- **Dark-mode text was invisible on four screens**: `AppointmentsScreen`,
  `HomeScreen`, `DoctorsScreen` and `NotificationsScreen` set `Color.White`
  page backgrounds while `Theme.kt` follows `isSystemInDarkTheme()`, so in dark
  mode theme-coloured text landed on a hardcoded white surface at 1.00:1. The
  conversion to `colorScheme` and `LocalMediQColors` is in "Changed" above; what
  is fixed here is that the defect can no longer come back silently, because
  `check-contrast.ps1` now fails on any hardcoded page background.
- **Four colour tokens failed WCAG AA and three measured fixes followed**:
  `MediQSuccess` `#388E3C` → `#2E7D32` (4.12:1 → 5.13:1 on white),
  `MediQWarning` `#FBC02D` → `#B25E00` (1.66:1 → 4.67:1), and the dark-scheme
  `error` red, which measured 1.96:1 on the dark raised surface and now lightens
  to `#FF6B6B` at 5.17:1. Each token now carries its measured ratio in a comment,
  and `check-contrast.ps1` re-measures rather than comparing against a stored
  list.
- **Status bar icons were dark on a dark bar in dark mode**:
  `isAppearanceLightStatusBars` was keyed off `darkTheme`, but `statusBarColor` is
  `colorScheme.primary`, which stays the dark brand green in both schemes. Dark
  mode therefore drew dark icons on a dark green bar at 3.17:1. The flag is now
  unconditionally `false`, which matches the bar that is actually drawn.
- **A wrong claim in `AGENTS.md`, corrected with a test**: the "Known gaps" list asserted that the server's `ignoreUnknownKeys = false` makes every *added* response field a breaking change for installed builds. It does not — that setting governs how the **server** parses inbound request bodies, while the client parses responses with Gson, whose default ignores unknown fields. `GsonLeniencyTest` (3 tests) pins the actual behaviour. The real risk is the mirror one and is now stated instead: Gson cannot tell "the server never sent this" from "the field was renamed", so a rename arrives as `null` with no error and a non-null Kotlin DTO field reads as null at runtime. Renames need a contract test.
- **Unrecognised wire values no longer resolve to a confident wrong answer**: `ApiMappers` fell back to the first enum entry, so an appointment status this build did not recognise became `PENDING_CONFIRMATION` — a cancelled appointment shown to a patient as awaiting confirmation — and a slot status fell back to `BLOCKED`, having previously been `AVAILABLE`, i.e. bookable on a guess. Nothing was logged, so no trail pointed at the rename that caused it. `AppointmentStatus` and `SlotStatus` now carry an explicit `UNKNOWN` member that is neither actionable nor upcoming; the status chip renders grey rather than borrow a status colour. `UserRole` and `Specialty` deliberately keep positional fallbacks, because `:server` compiles `domain/model/` and parses both from untrusted input — `UnknownWireValueTest` (8 tests) pins the distinction.
- **Patient-facing error messages**: The client never read the server's error contract. Every non-2xx response carries `ErrorDto(error, message)` with copy written for a patient, but nothing consumed it — ViewModels surfaced Retrofit's raw `HttpException.message` instead, so a wrong password displayed `"HTTP 401"` rather than "Those details don't match an account." `BookingViewModel` was worse: it discarded the 409 `slot_taken` message and told the patient to try again on a booking that could never succeed. Added `ApiFailure` (`domain/model/ApiFailure.kt`) and `data/api/ApiErrors.kt`, whose `call { }` wrapper reads the error body; all 5 Retrofit repositories and 8 ViewModels now route through it.
- **HTTP failures disguised as empty states**: The `ui-imports-data` review turned up a related defect — list reads caught `IOException` (and generic `Exception` in ViewModels), which cannot distinguish an unreachable server from an HTTP failure. A 500 or 409 on a list read rendered as "you have no appointments". Repositories now branch on `ApiFailure.isNetworkFailure`, so only a genuinely unreachable server becomes an empty state.
- **Booking State Bug**: Fixed an issue in `BookingViewModel` where the selected slot (`BookingSelection`) was not being cleared after a successful booking. This prevents users from accidentally re-submitting a stale booking.
