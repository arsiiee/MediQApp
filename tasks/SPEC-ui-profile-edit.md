# Spec: `ui-profile-edit` — Edit-profile screen (Android)

> Standalone module. Wires the existing-but-never-called
> `ProfileRepository.updateProfile` path into the UI.
> Follows the conventions of `tasks/SPEC-ui-*.md`.
>
> **Status: approved and implemented 2026-10-19.** Both open questions were
> answered by the user before any code: **a separate screen**, and **date of birth
> as a plain text field**. See "Decisions" at the bottom, which also corrects one
> wrong assumption this spec opened with.

## Objective

Let a signed-in user edit **their own** profile from the Android app.

Everything below the ViewModel is already live and tested — this module adds
only UI, ViewModel, and a session-cache write:

| Layer | State today |
|---|---|
| Server `PUT /profile` | Live, role-free (any signed-in account), edits only `principal.userId`'s row — covered by `ProfileUpdateTest` |
| `RetrofitProfileRepository.updateProfile` | Live, maps `UserProfile` → `UpdateProfileDto` |
| `MediQApiService.updateProfile` (`@PUT("profile")`) | Live |
| `ProfileViewModel` | **Dead constructor parameter** — takes `ProfileRepository`, never reads it (`ProfileViewModelTest.kt:40-44`) |
| `ProfileScreen` | Display-only; `refresh()` and `signOut()` are its only actions |

## Assumptions (correct now or during review)

1. **Separate `EditProfileScreen` route**, not inline editing — matches the
   app's navigation-per-screen pattern (booking, notifications, doctor detail).
2. **Editable:** `fullName`, `email`, `mobileNumber`, `dateOfBirth`, `sex`,
   `address` — exactly the six nullable fields the server accepts.
   **Not editable:** username, role, password — absent from the server's
   `UpdateProfileDto`, so sending them is impossible by construction.
3. **All four roles** (patient/doctor/secretary/admin) may edit their own
   profile: `PUT /profile` sits in the authenticated group, not a role-gated
   one.
4. **The session cache must be rewritten after a successful save.**
   `AuthRepository.currentSession()` reads `TokenStore` (SharedPreferences),
   not the server — without a cache write the profile screen would keep
   showing the old name/email after saving.
5. `sex` gets a "Not specified" option (server accepts `null`; the field is
   already optional in the UI at `ProfileScreen.kt:114`).

## UI

- **Entry:** an `Edit profile` row (`ProfileOption`, pencil icon) in the
  signed-in branch of `ProfileScreen`, above Notifications. Hidden when not
  signed in.
- **`EditProfileScreen`:** `MediQBrandAppBar` with back arrow, then six fields
  prefilled from the session, a server-error banner (`MediQInfoCard` error
  variant) on save failure, and a pill `Save` button (`MediQButton`, loading
  state while saving). On success: navigate back with the updated profile
  reflected on the profile screen.
- **Colours:** existing tokens only. If a new pair appears, it must pass
  `check-contrast.ps1` (WCAG AA, ratchet 4).

## ViewModel (TDD slice)

`ProfileUiState` gains an edit slice: field values, per-field validation
errors, `isSaving`, `saveError`. New behaviour:

- `updateProfile()` → validate → `profileRepository.updateProfile(merged)` →
  write the **server-returned** profile into the session cache → update UI
  state. The server response is the source of truth (it normalises).
- Validation (client-side, mirrors server rules; no network call when invalid):
  - `fullName` — non-blank
  - `email` — blank **or** valid address (server treats blank as null)
  - `mobileNumber` — `^\+?[0-9]{7,15}$`
  - `dateOfBirth` — valid ISO date, in the past
  - `sex`, `address` — optional, no constraints

## Session cache (one domain interface addition)

- `AuthRepository` (domain — **pure Kotlin, no Android imports**; the server
  compiles this directory) gains:
  `suspend fun updateCachedProfile(profile: UserProfile)`
- `RetrofitAuthRepository`: rewrite `TokenStore` with the current token/expiry
  + new profile. `FakeAuthRepository`: in-memory. `EmptyRepositories` updated.

## Commands

```powershell
.\gradlew.bat :app:compileDebugKotlin        # build
.\gradlew.bat :app:testDebugUnitTest         # tests (213 today)
.\.mdfiles\check-boundaries.ps1              # architecture (56 files)
.\.mdfiles\check-contrast.ps1                # colour contrast (only if new pairs)
```

## Testing Strategy

ViewModel unit tests only, per repo convention (all 10 ViewModels covered, no
new Compose UI tests). TDD: RED → GREEN per slice. The
`FakeProfileRepository` already tracks `updateRequests`, `updateResult`, and
`updateProfileError` — it was built for this. `FakeAuthRepository` gains the
new method.

Cases: save success calls repo once with edited fields merged over the current
profile and updates the session; server failure surfaces the message and keeps
the form; each validation rule blocks the save with no repo call; signed-out
save is impossible (no entry point).

## Boundaries

- **Always:** domain stays pure Kotlin; use existing tokens and components;
  run all four gates before done; one increment = one commit.
- **Ask first:** new dependencies (none expected), any server contract change,
  new colour tokens.
- **Never:** send fields the server's DTO rejects; skip a gate; persist the
  session anywhere but `TokenStore`.

## Success Criteria

1. `Edit profile` row opens a prefilled form for any signed-in role.
2. Saving updates the server **and** the profile screen's display without
   re-signing in (session cache rewrite proven by test).
3. Server error messages render in the banner; the form keeps its values.
4. Invalid input never reaches the network.
5. All four gates pass: compile, tests (255 in `:app` after this change),
   boundaries, contrast.

## Decisions (approved 2026-10-19)

1. **Separate screen, not inline editing.** `EditProfileScreen` is its own route,
   reached from an `Edit profile` row on the signed-in profile. It takes the
   **same** `ProfileViewModel` as `ProfileScreen` — `navController.getBackStackEntry(Screen.Profile.route)`,
   the pattern the register wizard already uses — so a save updates the state the
   profile screen renders through and popping back shows the new values with no
   refresh call. **This corrects the alternative the open question implied:** the
   cost of a separate route was assumed to be a second ViewModel to keep in step,
   and sharing the entry removes it.
2. **Date of birth is a plain text field** in ISO `yyyy-MM-dd`, with the format as
   supporting text until validation replaces it. **This spec's assumption #2 said
   the registration flow collects no date of birth — it does, with a Material 3
   picker** (`RegisterDetailsScreen`). So there *was* an existing pattern to match,
   and the user chose against it deliberately. The registration picker's format is
   display-only (`MMM d, yyyy`); the edit field is the ISO form the server stores
   and `TokenStore` caches, which is the one thing about this choice a later edit
   should not "fix" to match registration.

