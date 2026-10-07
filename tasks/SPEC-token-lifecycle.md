# Spec: Token Lifecycle (Expiry, 401 Handling, Refresh)

## Objective

**A patient whose token expires mid-session is told once, clearly, and can get
back to sign-in from wherever they are.** Today five screens render the sentence
"Please sign in again." with no way to act on it, and the only sign-in affordance
in the app sits on a sixth screen (Profile) that the patient has to find first.

Two defects, one initiative. They share a cause — expiry is checked in exactly
one place, and that place is not on the request path — and they are fixed by the
same seam.

### Acceptance criteria (patient-visible)

1. Any screen that receives a 401 offers a sign-in action, within one tap, from
   that screen. No screen requires the patient to navigate to Profile first.
2. The message appears once per expiry, not once per in-flight request. A screen
   with three concurrent reads shows one prompt, not three.
3. After signing back in, the patient lands somewhere useful — the screen they
   were on, or Home — and the prompt is gone.
4. A stored session with a corrupt or unparseable `expiresAt` clears itself and
   offers sign-in. It never wedges the Profile screen in a permanent error.

### Non-goals

- **Not** moving the token to `EncryptedSharedPreferences` / Keystore. Separate
  change, already tracked at `AGENTS.md` Auth §3. Fixing expiry first is correct:
  encryption does not help a token that is already dead.
- **Not** lengthening the 60-minute TTL. That hides the bug.
- **Not** changing the OTP `returnCodeToCaller` security gap.
- **Not** adding CI. There is none, and adding it is its own project.

## Tech Stack

Unchanged. Kotlin 2.2, Retrofit 2.11.0 + OkHttp 4.12.0 + Gson 2.11.0,
Ktor server. No new dependency is required — OkHttp's `Authenticator` interface
is already on the classpath and is what does this job.

## Current State (verified against the filesystem, not assumed)

| Fact | Evidence |
|---|---|
| Expiry is checked in exactly one place | `RetrofitAuthRepository.currentSession()` at `RetrofitAuthRepository.kt:53-62` — the only `isExpired` call in the app |
| …and its only caller is one ViewModel | `ProfileViewModel.kt:53` is the sole caller of `currentSession()` (`rg` over `app/src/main`) |
| The interceptor never checks expiry | `AuthInterceptor` at `RetrofitClient.kt:66-79` calls `tokenStore.getAccessToken()`, which is `TokenStore.kt:32` — a bare field read with no expiry test |
| No screen branches on a 401 | No `status ==` comparison exists anywhere in `ui/`. Every 401 becomes `LoadState.Error(e.message)` |
| The only sign-in affordance | `ProfileScreen.kt:85-91` — renders "Sign in to view your details" when `currentSession()` returns null |
| A corrupt session blob wedges the screen | `ApiMappers.kt:52` calls `Instant.parse(expiresAt)` unguarded; `RetrofitAuthRepository.kt:55` calls `toDomain()` outside the `runCatching` that guards `TokenStore.kt:29`. `ProfileViewModel.kt:58` catches it as generic `Exception` → `LoadState.Error`, token never cleared |
| There is no refresh endpoint | `Routes.kt:71-129` — `/auth` has `sign-in`, `sign-out`, `otp/request`, `otp/verify`, `register`. Nothing else |
| Sessions expire server-side too | `UserStore.activeSession` (`UserStore.kt:174-185`) filters `expires_at > ?`, so the row dies with the token. There is no sliding window |
| `TokenStore`, `AuthInterceptor`, `RetrofitAuthRepository` have **zero** tests | No match under `app/src/test` for any of the three. `ApiErrorsTest` covers message mapping only, never an expiry or a mid-session 401 |
| `check-boundaries.ps1` does **not** scan `data/api/` | `check-boundaries.ps1:54` scans `$root/domain`, `$root/ui`, `$root/data/repository` only. The new `Authenticator` lands in `data/api/` and is therefore **unenforced** by any rule — its correctness rests entirely on tests |

### Baseline counts, measured 2026-10-07

`@Test` occurrences, counted from source rather than read off the docs:

| Suite | Count | Ratchet in `CONSTRAINTS.md` | Agrees? |
|---|---|---|---|
| `:app:testDebugUnitTest` | **166** | 165 | **No — off by one** |
| `:server:test` | **50** | 50 | Yes |

The discrepancy is `HomeViewModelTest` — 14 `@Test` in the file, documented as
13 at `CONSTRAINTS.md:95` and `AGENTS.md`. The docs are stale by one, not the
tests. **This change fixes that line as a side effect**, because the ratchet has
to be touched anyway and lowering it to match a stale number would be exactly the
kind of quiet bar-moving `CONSTRAINTS.md` forbids.

### The two bugs, traced

**Bug 1 — no central 401 handling.** After the TTL, every protected call 401s.
`ApiErrors.kt:78-81` maps 401 → `"Please sign in again."`, and each ViewModel
renders it inline as `LoadState.Error`. The patient is told to sign in again by
five screens and given the means to do it by none of them.

**Bug 2 — permanent wedge on a corrupt session.** This is the Gson-silent-null
hazard from `README.md:154-161` landing on the one code path that has a recovery
path to break. A session blob with a malformed timestamp throws
`DateTimeParseException` *before* the expiry check runs, so the token is never
cleared and `ProfileScreen` sits in `LoadState.Error` indefinitely. Permanent for
that install; the only exit is clearing app data.

## Architecture Decisions

These are the decisions the spec exists to force. Each is a real fork.

### D1. Where the 401 handler lives

**Decision: OkHttp `Authenticator` + a domain-owned session signal.**

`AuthInterceptor` cannot fix this alone — an interceptor sees the response but
retrying is `Authenticator`'s job. The seam has to respect two existing rules:
`ui/` must not import `data/`, and `domain/` must not import Compose or Android.

Proposed shape:

- `ApiFailure` gains `isAuthFailure` alongside the existing `isNetworkFailure`
  (`domain/model/ApiFailure.kt:38`), keyed on `status == 401`. Same pattern, same
  file, no new concept.
- A `SessionSignal` in `domain/` — a `MutableStateFlow<Boolean>` or a
  `SharedFlow<Unit>` — holds "the session is dead". `AppContainer` owns the
  instance and hands it to both the OkHttp stack and the nav host.
- `AuthInterceptor` (or a sibling `SessionAuthenticator`) clears `TokenStore` and
  signals on a 401.
- `MediQNavHost` collects the signal and navigates to `Screen.SignIn`, clearing
  the back stack.

Rejected alternative: handle 401 per-ViewModel. Six places to get it wrong, and
the sixth will be written by someone who has not read this file.

### D2. Refresh endpoint or re-auth

**Open question — see below.** The two shapes:

| | Refresh endpoint | Silent re-auth |
|---|---|---|
| Server work | New route, new `sessions` handling, sliding expiry | None |
| Client storage | Access **and** refresh token | **Password**, or nothing at all |
| Patient experience | Invisible | Re-enters a password |
| New risk | Refresh token is a second long-lived credential in plaintext `SharedPreferences` | Storing a password is worse; without one, re-auth is a dead end |

The current session model is deliberately server-side (`Routes.kt:304-332`): the
token carries a `sid` and every authenticated request re-checks the row, so
sign-out is immediate. A refresh endpoint has to preserve that property — a
refresh that issues a new `sid` must revoke or extend the old one, or sign-out
stops being immediate.

### D3. What happens to a booking in flight when the token dies

**Decision: never auto-retry a write. Abort, say so, keep the patient's input.**

`POST /appointments` has no idempotency key (`README.md:134`, `AGENTS.md` Known
gaps). Auto-retrying a non-idempotent write after a 401 is how a patient gets
double-booked: the first attempt may well have succeeded server-side and lost its
response.

So the retry path covers **reads only**. A write that 401s surfaces the message
and stops. The booking form keeps the patient's reason and slot selection so
re-submitting is one tap, not retyping.

### D4. `Instant.parse` guard

**Decision: guard at the mapper, and make the failure explicit.**

`AuthSessionDto.toDomain()` is the only unguarded `Instant.parse` on a field
that comes from persisted bytes. Options: return null (changes the mapper's
signature and every caller), or throw a typed failure the session reader already
knows how to handle.

Chosen: `TokenStore.getSession()` validates the blob is mappable — not merely
that Gson parsed it — and clears + returns null when it is not. `currentSession()`
then has one contract: null means "no usable session", full stop. That kills Bug 2
at the boundary rather than at each read site.

## Commands

```powershell
# Compile gate
.\gradlew.bat :app:compileDebugKotlin :server:compileKotlin --console=plain

# Tests (the actual gate; counts below must not fall)
.\gradlew.bat :app:testDebugUnitTest --console=plain
.\gradlew.bat :server:test --console=plain

# Architecture + contrast, < 5s each, from repo root
.\.mdfiles\check-boundaries.ps1
.\.mdfiles\check-contrast.ps1

# HTTP verification, needs :server:run up
.\server\scripts\smoke.ps1 -Base http://127.0.0.1:8099

# Manual expiry check (see Success Criteria 5)
$env:MEDIQ_TOKEN_TTL_MINUTES="2"
.\gradlew.bat :server:run --console=plain
```

`MEDIQ_TOKEN_TTL_MINUTES` already exists (`ServerConfig`, default 60) and is how
this gets verified without waiting an hour.

## Project Structure

No new packages. Changes land in files that already exist, plus tests beside
their subjects:

```
app/src/main/java/com/example/mediq/
  domain/model/ApiFailure.kt          + isAuthFailure
  domain/model/SessionSignal.kt       NEW — the ui<->data seam
  data/api/RetrofitClient.kt          Authenticator added
  data/api/TokenStore.kt              validate-on-read, not parse-on-read
  data/repository/RetrofitAuthRepository.kt   currentSession() contract simplified
  di/AppContainer.kt                  owns the SessionSignal instance
  ui/navigation/MediQNavHost.kt       observes the signal

app/src/test/java/com/example/mediq/
  data/api/TokenStoreTest.kt          NEW
  data/api/SessionSignalTest.kt       NEW
  data/repository/RetrofitAuthRepositoryTest.kt  NEW
```

## Code Style

Existing patterns, nothing new invented. A failure type carries a flag rather
than a subclass, following `isNetworkFailure`:

```kotlin
// domain/model/ApiFailure.kt — matches the existing idiom at line 38
/**
 * True when the server rejected the token.
 *
 * Distinct from [isNetworkFailure]: that one means "never reached the server",
 * this one means "reached it and was told who you are is no longer valid".
 * The two demand opposite UI — a retry button and an empty list respectively.
 */
val isAuthFailure: Boolean get() = status == 401
```

KDoc carries the *why*, per `AGENTS.md`. Every new file states the bug it exists
to prevent, in the past tense, so the next reader knows it is load-bearing.

## Testing Strategy

Framework: JUnit 4 + `kotlinx-coroutines-test`, hand-written fakes. **No mocking
framework** — this repo's stated position, and `CONSTRAINTS.md` Exceptions.

Mandatory cases:

| Test | Asserts | Fails without the fix |
|---|---|---|
| Expired stored session | `currentSession()` clears the token and returns null | token kept, session treated as live |
| Corrupt `expiresAt` | `currentSession()` clears and returns null; **no** throw | `DateTimeParseException`, screen wedged |
| One 401, one signal | five concurrent reads → signal fires once | five prompts |
| 401 clears the store | `TokenStore` is empty after the interceptor handles a 401 | stale token re-sent forever |
| Read auto-retried after refresh | a `GET` is re-issued once and succeeds | no retry |
| **Write never auto-retried** | a `POST /appointments` 401 issues **zero** additional requests | double-booking |
| Sign-in returns to Home | after the prompt resolves, the nav destination is Home | patient stranded |

The write case is the one that matters most and is mutation-checked: deliberately
enable write-retry and the suite must fail.

New tests **raise** the `CONSTRAINTS.md` ratchet. `:app` goes 166 → ~173.
`:server` stays 50 if D2 resolves to re-auth (no server change), or 50 → ~52 if it
resolves to a refresh route. The floor moves in the same commit, per
`CONSTRAINTS.md` "Move a number only when the code moves it."

## Boundaries

- **Always:** run `check-boundaries.ps1` and `check-contrast.ps1` before
  declaring done. Update `CONSTRAINTS.md` ratchets in the same commit.
  Update `.mdfiles/AGENTS.md` in the same commit — it has a standing rule for
  this.
- **Consider adding to `check-boundaries.ps1`:** the `Authenticator` is the one
  piece of this work that lands outside every scanned directory
  (`check-boundaries.ps1:54`). The script's own header argues that whole-tree
  checks exist precisely because "the violation being guarded against is *added*
  a plain api.foo() call", which "a diff-scoped check only catches on the diff
  that introduced it and never on the file it was added to afterwards". The same
  argument applies to a retry that fires when it should not — and the guard
  script cannot see it today. Extending the scanned-path list to include
  `data/api` is a small change that makes the rule real. **Not doing it leaves
  the most safety-critical new code in the app with no mechanical check.**
- **Ask first:** adding `POST /auth/refresh` or a `refresh_token` column (D2 is
  an API and schema change); changing the TTL default; touching `schema.sql`.
- **Never:** store the password to enable silent re-auth. Auto-retry any
  non-idempotent write. Weaken the `ui-imports-data` or
  `domain-imports-android` ratchets to fit the seam.

## Success Criteria

1. `MEDIQ_TOKEN_TTL_MINUTES=2`, signed in, wait: every screen shows one prompt
   with a working sign-in action. Verified on the emulator, not inferred.
2. `:app:testDebugUnitTest` green at ≥173 tests (from a 166 baseline);
   `:server:test` green at its new floor. `CONSTRAINTS.md:37` and `:95` both
   updated, and the stale `HomeViewModelTest` 13 → 14 corrected in the same edit.
3. `check-boundaries.ps1` reports `ui-imports-data` = 0 and
   `domain-imports-android` = 0. The seam did not buy navigation by breaking the
   layering.
4. A booking submitted across an expiry produces **exactly one** `POST` — checked
   in `smoke.ps1`, not just in a unit test.
5. `README.md:146-147` and the `AGENTS.md` Known-gaps bullet are updated in the
   same commit, or the fix does not count as done.
6. Each new test mutation-checked: break the code, watch it fail, restore.

## Risks

| Risk | Impact | Mitigation |
|---|---|---|
| `Authenticator` retry loops if refresh itself 401s | Medium | Cap retries per request; a retried request that 401s again propagates. Test it. |
| SessionSignal leaks the last signed-in user's state across sign-out | High | Clear the signal in `signOut()` alongside `TokenStore.clearSession()`. One test. |
| Navigating to sign-in loses the patient's in-progress booking form | Medium | D3 keeps the form state; navigation preserves the back stack entry, or the patient returns and re-taps. Decide at task time. |
| A refresh token in plaintext prefs enlarges the existing plaintext-token gap | Medium | Known and stated (`AGENTS.md` Auth §3 already covers it). Refresh is worse than access, so this raises the priority of the encryption work, not lowers it. |

## Open Questions

1. **Refresh endpoint, or accept re-auth on expiry?** *(D2 — the fork that
   decides the size of this work.)* Re-auth needs no server change and no stored
   password if the patient simply signs in again; that is a legitimate, smaller,
   honest fix. Refresh is invisible but adds a route, a schema column, and a
   second long-lived credential. **Recommendation: re-auth for now.** It closes
   every acceptance criterion above, and refresh can be added later without
   touching the client seam — D1 is needed either way.
2. **Where does sign-in land afterwards — Home, or the screen they were on?**
   Recommendation: Home. Returning to a detail screen re-issues a read
   immediately, which is the request path we are trying to make predictable.
3. **Does the expired-session prompt replace the screen's content or sit above
   it?** Recommendation: a modal, so the patient cannot keep tapping
   cancel/reschedule against a dead token.

## Related

- `README.md:146-147` — the gap bullet this closes
- `AGENTS.md` Known gaps → API contract review, the same bullet
- `tasks/plan.md` — the registration/appointment-details design record, kept
  separate from this