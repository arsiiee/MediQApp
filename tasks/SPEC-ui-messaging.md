# Spec: `ui-messaging` — Patient↔clinic messaging

> First feature in this repo that adds **tables**, **endpoints**, and a **screen
> set** at once. Follows the conventions of `tasks/SPEC-ui-*.md`.
>
> **Status: awaiting approval.** Written 2026-10-19. Nothing is implemented and
> no plan or task list exists yet — both wait on your sign-off of this file.
>
> The four open questions were answered by the user before this was written; see
> "Decisions" for the answers and for the one thing this spec had to resolve
> *between* two of them.

## Objective

Let a signed-in **patient** read and send messages, in two kinds of thread:

- **Clinic thread** — one per patient, answered by any staff member.
- **Doctor thread** — one per (patient, doctor) pair, answered by that doctor.

Staff reply through the API; they get no UI in this module (their screens are a
separate capability with a separate consumer, and would need their own spec).

Today the Messages tab is a 20-line stub (`ui/feature/messages/MessagesScreen.kt`,
a `Box` with `Text("Messages")`), there is no `messages` table, and no
`/messages` route. The README, `CHANGELOG.md`, and the previous spec all describe
Messages as an admitted stub — this module is what removes that.

| Layer | State today |
|---|---|
| Tab, route, `MainActivity` bottom-bar entry | **Already wired.** `Screen.Messages` is in `bottomNavItems` and in `showBottomBar` |
| `MessagesScreen` | Stub, 20 lines, no ViewModel |
| Server `/messages` | **Does not exist** |
| `schema.sql` | No `message_threads`, no `messages` |
| `AppContainer` | No `messageRepository` |
| `NotificationStore`, `AppointmentStore` | The patterns to copy — nothing to reuse |

## Assumptions (correct now or during review)

1. **My invention, not your instruction** — messages are plain text. No
   attachments, no images, no read receipts shown to the other party, no typing
   indicators, no editing or deleting a sent message.
2. **My invention** — a message body is trimmed, non-blank, and at most **2000
   characters**, enforced on both sides. The client blocks; the server refuses
   with a sentence written for a patient.
3. **My invention** — one unread concept: `messages.read_at`, set when the
   *counterpart* opens the thread. The patient's unread count is therefore the
   staff-authored messages they have not read.
4. **From the repo** — no push exists (no FCM), so a thread is fresh when it is
   opened and when it is pulled. Nothing polls.
5. **From the repo** — `domain/` is compiled into `:server` as `sharedDomain`, so
   the two new models import no Android types.
6. **From the repo** — new enum values carry explicit `wireValue`s
   (`MessageScope`, `MessageAuthorRole`), and `UNKNOWN` is *not* added to either:
   `firstOrNull { … } ?: UNKNOWN` is for values the server sends that this build
   may not know, but here the server is the only writer and both values are
   server-owned. A read that cannot parse one is a bug, so it fails loud.

## Data model

Two tables, appended to `schema.sql` after `notifications`, with the table-level
rule comment the file's style asks for.

```sql
CREATE TABLE IF NOT EXISTS message_threads (
    id                    VARCHAR(36) PRIMARY KEY,
    user_id               VARCHAR(36) NOT NULL,
    scope                 VARCHAR(16) NOT NULL,
    doctor_id             VARCHAR(36),
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    last_message_at       TIMESTAMP WITH TIME ZONE,
    last_message_preview  VARCHAR(140),
    CONSTRAINT message_threads_user_fk   FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT message_threads_doctor_fk FOREIGN KEY (doctor_id) REFERENCES doctors (id),
    CONSTRAINT message_threads_scope_chk CHECK (
        (scope = 'clinic' AND doctor_id IS NULL) OR
        (scope = 'doctor' AND doctor_id IS NOT NULL)
    )
);

CREATE INDEX IF NOT EXISTS idx_message_threads_user
    ON message_threads (user_id, last_message_at DESC);

CREATE TABLE IF NOT EXISTS messages (
    id                      VARCHAR(36) PRIMARY KEY,
    thread_id               VARCHAR(36) NOT NULL,
    author_user_id          VARCHAR(36) NOT NULL,
    author_role             VARCHAR(16) NOT NULL,
    body                    VARCHAR(2000) NOT NULL,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL,
    read_at                 TIMESTAMP WITH TIME ZONE,
    related_appointment_id  VARCHAR(36),
    CONSTRAINT messages_thread_fk FOREIGN KEY (thread_id) REFERENCES message_threads (id),
    CONSTRAINT messages_author_fk FOREIGN KEY (author_user_id) REFERENCES users (id),
    CONSTRAINT messages_author_role_chk CHECK (author_role IN ('patient', 'staff')),
    CONSTRAINT messages_related_appointment_fk
        FOREIGN KEY (related_appointment_id) REFERENCES appointments (id)
);

CREATE INDEX IF NOT EXISTS idx_messages_thread ON messages (thread_id, created_at);
```

Four decisions inside that SQL, because each is one a later edit could undo:

1. **A thread id is derived, not random.** `id = UUID.nameUUIDFromBytes(threadKey)`
   where `threadKey` is `"$userId|clinic|"` or `"$userId|doctor|$doctorId"`. This
   is the `slot_claims` mechanism — a natural key that makes create idempotent
   under concurrent requests. A random UUID would let two taps produce two clinic
   threads, and a `SELECT`-then-`INSERT` check has a window between the two
   statements. The `CHECK` still guards the pair (`scope` and `doctor_id` must
   agree), because a derived key makes the pair *load-bearing*.
   Consequently **there is no separate unique index and no `thread_key` column** —
   the primary key is the guarantee, the same way it is for a slot claim.
2. **`last_message_at` and `last_message_preview` are denormalised.** A thread
   list must not run a per-thread subquery for its ordering key and its preview
   line. Both are written in the *same transaction* as the message insert, and a
   test pins that they cannot drift from the messages table. This is the one
   place in this schema where a value is stored twice on purpose.
3. **`related_appointment_id` is on the message, not the thread.** A thread is
   patient-scoped (`clinic`) or doctor-scoped (`doctor`); it is never
   appointment-scoped, or a patient would collect one thread per booking. The
   appointment a message is *about* is a property of that message — exactly what
   `notifications.related_appointment_id` already does.
4. **`author_role` is `patient` or `staff`, not the four `UserRole` values.** A
   patient must never see which staff member's role replied, and the wire value
   would leak the internal role names. The author's *name* is resolved at read
   time by joining `users`.

## API contract

All under `authenticate(AUTH_JWT)`, in a new `route("/messages")` block in
`Routes.kt`. Pagination uses the existing `call.pageSize()` / `call.pageOffset()`
/ `call.nextCursor()` helpers — no new pagination mechanism.

| Method | Path | Body | Returns |
|---|---|---|---|
| `GET` | `/messages/threads` | — | `PagedDto<MessageThreadDto>`, most recent activity first |
| `POST` | `/messages/threads` | `{ "doctorId": String? }` | `MessageThreadDto` — find-or-create |
| `GET` | `/messages/threads/{threadId}` | — | `MessageThreadDto` |
| `GET` | `/messages/threads/{threadId}/messages` | — | `PagedDto<MessageDto>` — newest first |
| `POST` | `/messages/threads/{threadId}/messages` | `{ "body": String, "relatedAppointmentId": String? }` | `MessageDto`, 201 |
| `PATCH` | `/messages/threads/{threadId}/read` | — | `{ "ok": true }` |

`MessageThreadDto`: `id`, `scope`, `doctorId?`, `doctorName?`, `lastMessageAt?`,
`lastMessagePreview?`, `unreadCount`, `createdAt`.

`MessageDto`: `id`, `threadId`, `body`, `authorRole`, `authorName`, `createdAt`,
`readAt?`, `relatedAppointmentId?`, `mine`.

Three contract decisions:

- **`POST /messages/threads` answers 200, never 201.** It is idempotent: the
  second call creates nothing and the client cannot tell which call it made, so
  a 201 would be a claim the server cannot honour. (`POST /admin/accounts` is
  201 because it always creates.) Interpreting the derived-key collision as a
  re-read is what makes this true under a race, not just in the happy path.
- **`mine` is computed server-side** from the caller's user id, and
  `authorUserId` is deliberately **not** sent. The patient UI needs "is this
  mine" and a display name; shipping other users' ids to a client adds surface
  for nothing.
- **Unread is a count, not a flag.** The thread list computes it in the same
  query with `SUM(CASE WHEN m.author_role = 'staff' AND m.read_at IS NULL THEN 1
  ELSE 0 END)`, grouped by thread — portable to H2 and Postgres without a
  `FILTER` clause or a second round trip.

### Authorization

| Caller | May read / write |
|---|---|
| Patient | Only threads where `message_threads.user_id` = their own id |
| Doctor | `clinic` threads; `doctor` threads where `threads.doctor_id` = their `users.doctor_id` |
| Secretary | `clinic` threads |
| Admin | `clinic` threads (see Open Question 1) |
| A doctor with no `users.doctor_id` link | `clinic` threads only |

**A thread you may not read answers 404, not 403.** `AppointmentStore.get`
already does this — a patient who guesses another patient's id learns nothing
from the response, whereas a 403 confirms the row exists. Writes to a thread you
cannot read are 404 by the same rule, checked *before* validation, so a
validation error cannot be used to probe for ids.

## UI

Three surfaces, all patient-facing.

**1. `MessagesScreen` (rewrite of the stub).**

- App bar via `MediQBrandAppBar` with a `New message` action.
- Thread rows: doctor or clinic name, preview line, relative-or-short date, and
  an unread count chip when non-zero (reuses the unread-row treatment from
  `NotificationsScreen`: `MediQLightGreen` fill, accent dot).
- `LoadState.Loading` → the accent spinner; `Error` → `EmptyState` with the
  server's sentence; `Success(empty)` → `EmptyState` naming both ways to start a
  thread, in the style of the seeded-data screen's two-causes copy.
- **`New message` opens a chooser with two entries:** "The clinic", then the
  doctor list. This is the one place the two entry points from the decisions meet
  — the clinic thread has to be startable without first booking, and picking a
  doctor has to be possible without attending an appointment.
- **Pull-to-refresh** via Material 3 `PullToRefreshBox`, plus a refresh on open.
  This is the first gesture refresh in the app (nothing under `ui/` matches
  `PullToRefresh|SwipeRefresh|refreshable` today), and it is the one new
  `@OptIn(ExperimentalMaterial3Api::class)`; `NotificationsScreen` already
  carries that opt-in, so it is not a new kind of annotation. There is no
  non-experimental equivalent for the gesture, which is what makes this
  different from the `ExposedDropdownMenuBox` the edit-profile spec rejected.
  **Verify the import exists in this BOM as Slice E's first step** — the
  alternative if it does not is an explicit refresh action in the app bar, not a
  silent drop of the requested behaviour.

**2. `MessageThreadScreen` (new route `message_thread/{threadId}`).**

- Messages oldest-at-the-bottom, drawn from the newest-first pages reversed once
  for display.
- `Load earlier messages` at the top when `nextCursor != null`. A chat that
  silently hides older messages is the appointments pagination gap
  (`AGENTS.md`: a patient with 25 appointments sees 20 with no indication)
  repeated in a place where it matters more.
- Composer: multiline text field, send button disabled while blank or sending.
- **A draft survives a failed send.** The text is cleared only on success and is
  restored on failure, mirroring `BookingViewModel` keeping its selection when a
  booking is refused.
- Opening the thread marks it read; the unread count on the list is gone on
  return with no refresh call.

**3. `AppointmentDetailsScreen` gains a `Message clinic` row.**

- It is a **navigation** action, not a mutation, so it sits **outside**
  `canCancelOrReschedule` — a cancelled appointment can still be messaged about.
  Gating it on `isActionable` would be the plausible mistake.
- It opens the clinic thread with the composer **prefilled**:
  `About my appointment on Tue 20 Oct at 10:00 with Dr. Demo Rivera — ` and
  carries `relatedAppointmentId` on the first message.

## ViewModel slices (TDD, each slice one commit)

| Slice | Content | Tests |
|---|---|---|
| A | `MessageThread`, `Message`, `MessageScope`, `MessageAuthorRole` in `domain/model/`; `MessageRepository` interface; DTOs, mappers, `MediQApiService` methods, `RetrofitMessageRepository`, `EmptyRepositories`, `AppContainer` | mapper + repository |
| B | `schema.sql`, `MessageStore`, `/messages` routes, server DTOs, seed, `messages` in `Main.kt` wiring | `MessageStoreTest`, `MessagesApiTest` |
| C | `MessagesViewModel` — list load, refresh, empty, error, unread counts, open-clinic-thread, new-doctor-thread | ~12 |
| D | `MessageThreadViewModel` — load thread + first page, load earlier, send, mark read, validation | ~12 |
| E | `MessagesScreen`, `MessageThreadScreen`, `Routes.kt`, `MediQNavHost.kt`, `AppointmentDetailsScreen` row | none (repo convention) |
| F | `CONSTRAINTS.md` floors, `AGENTS.md` inventory + gap closure, `CHANGELOG.md`, root `README.md` stub list, screenshots | — |

Slice order matters: B depends on nothing in A, but C depends on A, and E on
C+D. A and B are independent and could be built in either order; B cannot be
tested on device without A.

## Commands

```powershell
.\gradlew.bat :app:compileDebugKotlin :server:compileKotlin        # types
.\gradlew.bat :app:testDebugUnitTest --rerun-tasks                 # app tests (255 today)
.\gradlew.bat :server:test --rerun-tasks                           # server tests (106 today)
.\.mdfiles\check-boundaries.ps1                                    # architecture (58 files)
.\.mdfiles\check-contrast.ps1                                      # contrast (31 pairs, ratchet 4)
.\server\scripts\smoke.ps1 -Base http://127.0.0.1:8099             # over HTTP, needs the server up
.\gradlew.bat :app:installDebug                                    # on mediq_api36
```

## Testing Strategy

Per repo convention: **ViewModel unit tests, mapper and repository tests, server
tests, and smoke checks. No new Compose UI tests** — the bottom bar and the
edit-profile form set no precedent for testing composables, and
`MediQBottomBarSemanticsTest` is the only Compose test in the tree.

- `:app` — new `FakeMessageRepository` (recording lists + a `sendGate`
  `CompletableDeferred` so a double-send is observable rather than vacuous, and
  `TestFixtures.message(...)`/`thread(...)` with every field defaulted).
  `MessagesViewModel` and `MessageThreadViewModel` are both `by lazy` in tests
  because both load in `init` (the eager-dispatcher trap in `AGENTS.md`).
- `:server` — `MessageStoreTest` for the derived id, the find-or-create under a
  second call, the denormalised columns staying in step, the unread computation,
  and the cross-patient 404. `MessagesApiTest` for validation, the blank body
  refusal, and the role matrix.
- `smoke.ps1` — a Messages section: the thread list is 200, `POST
  /messages/threads` twice returns the *same* thread id, a blank body is 400, a
  second patient's token gets 404 on the first patient's thread, and a doctor's
  token can read a clinic thread.
- **Mutation check, as the last edit did twice** — break the find-or-create
  (make the id random) and confirm the double-create test fails; break the
  unread computation and confirm the list test fails. Restore both.

Floors move and are recorded, never silently: `:app` 255 → **255 + n**,
`:server` 106 → **106 + n**, measured with `--rerun-tasks` and written into
`CONSTRAINTS.md` with the date and reason, in the same commit as the tests.

## Boundaries

- **Always:** run all four gates before calling a slice done; `domain/` stays
  pure Kotlin; every `api.` call inside `call { }`; enum values explicit on the
  wire; list reads absorb an unreachable server into an empty page while writes
  propagate; dates through `toClinicDate()` and money through `Money.format()`;
  one slice = one commit.
- **Ask first:** the `CONSTRAINTS.md` floors; adding a `NotificationType` value
  (it is shared-domain and a wire contract — see Open Question 3); touching
  `docs/final-project/` (Open Question 4); any new dependency (none expected —
  `PullToRefreshBox` is in the current BOM).
- **Never:** store a random thread id; fabricate message content that reads as a
  real clinical conversation (the seed is `MEDIQ_SEED_DEMO`-only and
  `DEMO`-labelled, per the no-fake-data rule); let a patient read or write
  another patient's thread; weaken a check to land a slice; surface a framework
  exception's `message`.

## Success Criteria

1. A patient sees the clinic thread and any doctor threads, most recent first,
   each with a preview and an unread count.
2. `New message` creates a doctor thread, or opens it if it already exists — the
   second tap cannot produce a second thread (proven by test and by smoke).
3. `Message clinic` from an appointment opens the prefilled clinic thread, and
   the sent message carries the appointment id.
4. Sending appends the message and clears the composer; a failed send restores
   the draft and shows the server's sentence.
5. A blank or over-2000-character body never reaches the network, and is refused
   with 400 if it does.
6. Opening a thread marks staff messages read, and the count is still zero after
   a cold start (server-persisted, not in-memory only).
7. Cross-tenant access is 404 for both reads and writes, for patient→patient and
   doctor→other-doctor.
8. All four gates pass, with the new floors recorded in `CONSTRAINTS.md`.

## Open Questions

1. **Should an admin read `clinic` threads?** Recommendation: yes — an admin is
   clinic staff and already holds the widest read on accounts. Say so if you want
   them excluded, and the matrix narrows to doctor + secretary.
2. **Is `Message clinic` prefilling the composer the right reconciliation of
   "from an appointment" with "one clinic thread per patient"?** Those two
   answers cannot both be literal: an appointment-scoped thread means a new
   thread per booking. This spec keeps threads patient-scoped and puts the
   appointment on the first *message*. The alternative is a third scope,
   `appointment`, which contradicts the clinic-thread answer. **Recommendation:
   as specified.** This is the one place the design had to choose between your
   answers.
3. **Should a staff reply raise a notification?** Without one, a patient learns
   about a reply only by opening the tab — and the app has no push. Adding it
   means a new `NotificationType` wire value (`message_reply`) and a row written
   in the same transaction as the reply, which is how booking already notifies.
   **Recommendation: yes, patient-only** (a staff-side row would be written for a
   screen that does not exist). It is a wire-contract change, so it is asked
   rather than assumed.
4. **Should `docs/final-project/assets/erd.svg` and the PDF gain the two new
   tables?** They are your document and are untracked in git; the ERD is now
   stale by two tables. **Recommendation: update them in a separate change** —
   not silently as part of a code slice.
5. **Retention and limits.** Nothing here deletes a message, and no rate limit
   stops a patient sending 10,000. Both are acceptable for a demo and
   unacceptable for real patients. **Recommendation: record as known gaps rather
   than build now.**

## Decisions (answered by the user, 2026-10-19)

1. **Participants: a clinic thread *and* per-doctor threads.** Both scope values
   exist; the `CHECK` keeps them consistent with `doctor_id`.
2. **Patient UI only.** Staff reply through the API and through `smoke.ps1`.
   There is no doctor or admin screen anywhere in the app today, so a staff inbox
   is a second capability with a second consumer and would need its own spec.
3. **Both entry points.** `New message` on the tab, and `Message clinic` on an
   appointment — plus a clinic entry inside the chooser so the clinic thread is
   startable without a booking.
4. **Refresh on open plus pull-to-refresh.** No polling, no push: honest for a
   clinic where a reply takes minutes to hours, and it costs no battery.
