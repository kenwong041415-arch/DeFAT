# Phase 1.5 — Implementation Plan (sign-in, cloud sync, security rules)

Status: approved plan, ready for implementation.
Author: Opus planning pass (per `CLAUDE.md` model roles).
Implementer: Sonnet. Follow this document literally. If something here
contradicts `docs/`, stop and flag it rather than silently choosing.

Predecessors: Phase 1 (`phase1-implementation-plan.md`), Phase 1.6, Phase 1.7.
Phase 1's **D1** deferred Firebase to this phase because no project existed;
its **D3** deferred the Firestore security-rules tests with it. Both land here.

The Firebase project now exists. Confirmed against the owner's
`google-services.json` and their answer:

| | |
|---|---|
| Package name | `com.defat.app` — matches `applicationId` exactly |
| Firestore location | `asia-east2` (Hong Kong) — matches `docs/02` and `docs/06` |
| Analytics | not enabled |
| OAuth client | none (no SHA-1 registered — fine, see §3 D2) |
| Project id | `project-57faddb5-33ae-46ba-bfa` (auto-generated, permanent, cosmetic) |

---

## 1. Scope

1. **Firebase Auth** — email + password sign-up and sign-in, password reset.
2. **Firestore sync** — profile, meals, weights, exercise, water, day rollups.
   Offline-first: Room stays the write path, Firestore mirrors it.
3. **First-sign-in adoption of existing local data** — the owner already has
   real logged meals on their phone. They must survive and must not duplicate.
4. **Security rules + emulator tests** — Phase 1 D3, finally.
5. **Export my data / delete my account** — `docs/06` principle 3.
6. **CI** — build with `google-services.json` from a GitHub Actions secret.

### Out of scope

Health Connect (Phase 2), AI and Cloud Functions (Phase 3), the trainer
dashboard and the trainer↔student link (Phase 5), Google Sign-In (§3 D2),
App Check (§3 D5), FCM push.

### 1.1 Exit test

> The owner signs up on their phone, sees every meal and weight they had
> already logged still there, signs in on a second device and sees the same
> data, logs a meal with the network off and watches it appear in Firestore
> once the network returns, and can export their data and delete their
> account from inside the app.

---

## 2. The security position — read before writing any rule

Two facts about this project, established while wiring up the config:

- **The GitHub repository is public.**
- **The APK is published as a login-free release asset**, deliberately, so
  the owner can install test builds from their phone.

`google-services.json` is `.gitignore`d (`docs/06`) and must stay so, but the
API key inside it ships in every APK, and that APK is downloadable by anyone.
Google documents this key as not secret, and that is correct — but it means:

> **Firestore security rules are the only thing protecting the owner's and
> their students' health data. There is no second line of defence in this
> phase.**

Consequences, all binding:

1. Rules are **deny-by-default**. Every collection is denied unless a rule
   explicitly allows it.
2. Every rule gets an **emulator test, including a negative test** — another
   signed-in user must be proven unable to read the document.
3. No rule may ever grant access on a client-supplied field. Ownership is
   `request.auth.uid` matched against the document path, never a `userId`
   field in the payload, which a caller controls.
4. **App Check** is the missing second line and is deliberately deferred
   (§3 D5). Record it in `docs/06` as an accepted gap with its closing
   condition: before any student other than the owner uses the app.

---

## 3. Decisions

| # | Decision | Reason |
|---|---|---|
| D1 | **Email + password auth**, not anonymous | A trainer must be able to identify a student. Anonymous-then-upgrade adds an account-linking path and a failure mode (losing data on a failed upgrade) for no benefit here. Password reset via Firebase's own email. |
| D2 | **No Google Sign-In in this phase** | It needs a SHA-1 fingerprint registered per signing key, including the debug key, and the `oauth_client` array in the current config is empty. Email/password works today with no extra console setup. Adding Google Sign-In later is additive. |
| D3 | **Room stays the single write path; Firestore mirrors it** | Logging must work offline (`docs/02` non-functional requirements). The UI reads Room only, so a sync failure can never block or slow logging, and no screen needs a loading state it did not already have. |
| D4 | **Last-write-wins per document, on a server timestamp** | One user with one or two devices editing their own log. Proper merge semantics would cost a vector clock and buy nothing measurable. Stated as a simplification in `docs/02`, revisited only if the trainer ever edits a student's rows (Phase 5). |
| D5 | **App Check deferred** | Its Play Integrity provider wants Play Store distribution, and the debug provider needs a token registered per install — friction that buys little while the owner is the only user. Closing condition in §2.4. |
| D6 | **Sync is WorkManager + an on-open pass**, not a live Firestore listener | Listeners hold a socket and cost battery and quota for data that changes a few times a day. A one-shot pull on open and a periodic push covers it. |
| D7 | **The local database is not wiped on sign-out** | Signing out must not destroy a student's logged meals. Sign-out clears the session, not the data; the next sign-in with a *different* uid is the only thing that clears local rows, and it warns first (§6.3). |

---

## 4. Data model

Follows `docs/02` exactly. Every path is under `users/{uid}`, so ownership is
structural and a rule is one line.

```
users/{uid}                     profile: sex, birthDate, heightCm, activityLevel,
                                goal{...}, dayStartMinutes, locale, role, updatedAt
users/{uid}/meals/{mealId}      the Room row, plus baseFoodId, modifierIds, updatedAt
users/{uid}/weights/{entryId}   weightKg, fatPct, source, loggedAt, updatedAt
users/{uid}/exercise/{entryId}  activityKey, met, minutes, weightKgAtTime, kcal, updatedAt
users/{uid}/water/{epochDay}    ml, updatedAt
users/{uid}/days/{date}         the cached rollup (bmr, tdee, intake, balance), updatedAt
```

Not synced, by `docs/06` principle 2: the seeded `food_items` and
`food_modifiers` tables. They are identical on every install and shipped with
the app. **Owner-edited food rows are the exception** and do sync, under
`users/{uid}/foodOverrides/{foodId}` — they are the owner's professional
judgement and must survive a reinstall.

`role` is `student` by default. Nothing in this phase reads it; it exists so
Phase 5 does not need a migration.

---

## 5. Security rules

`firestore.rules` at the repo root, deployed from CI is **out of scope** —
the owner deploys from the console or the CLI, and §10 says how.

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    function isOwner(uid) {
      return request.auth != null && request.auth.uid == uid;
    }

    match /users/{uid} {
      allow read, write: if isOwner(uid);

      match /{sub=**} {
        allow read, write: if isOwner(uid);
      }
    }
  }
}
```

That is the whole rule set for this phase, and its brevity is the point: all
user data lives under a path keyed by the uid, so there is nothing else to
allow. **No trainer rule yet** — Phase 5 adds it, with its own tests, when
there is a trainer↔student link to check against.

### 5.1 Rules tests (Phase 1 D3)

`firestore-tests/` — Node, `@firebase/rules-unit-testing`, run against the
emulator. Not wired into the Android CI job (it needs the Firebase emulator
and Node); it gets its own workflow job that runs on changes to
`firestore.rules` or `firestore-tests/`.

Required cases, each named for what it proves:

1. A signed-in user reads and writes their own `users/{uid}` — allowed.
2. **A different signed-in user reads `users/{uid}` — denied.**
3. **A different signed-in user writes `users/{uid}/meals/{id}` — denied.**
4. **An unauthenticated client reads `users/{uid}` — denied.**
5. A signed-in user reads and writes every subcollection of their own tree.
6. **A write to a collection outside `users/` — denied** (deny-by-default).
7. A user cannot create a document at `users/{otherUid}` by putting their own
   uid in the payload — the path is what matters, not the body.

Cases 2, 3, 4, 6 and 7 are the ones that matter. A rules test suite without
negative cases proves nothing.

---

## 6. Sync

### 6.1 Shape

`core-data/sync/` — a `SyncEngine` with three operations:

- `pushPending()` — every local row whose `syncedAt` is null or older than
  `updatedAt` is written to Firestore, in batches of 400 (the 500-write limit
  with headroom), then stamped `syncedAt`.
- `pullSince(lastPullAt)` — documents with `updatedAt >` the last pull are
  fetched and upserted into Room.
- `fullPull()` — used once, on first sign-in on a device (§6.3).

Triggers (D6): on app open, on sign-in, and a WorkManager periodic job every
6 hours with `NetworkType.CONNECTED`. No listener.

Every Room entity gains `updatedAt: Long` and `syncedAt: Long?`. **That is a
Room v3 → v4 migration**, additive, same discipline as Phase 1.7's: exposed as
`MIGRATION_3_4_STATEMENTS`, tested with sqlite-jdbc, no `DEFAULT` unless the
entity declares the identical one, and `fallbackToDestructiveMigration` stays
forbidden. Back-fill `updatedAt` to the row's existing timestamp and
`syncedAt` to null, so every pre-existing row is treated as pending — which
is exactly what §6.3 needs.

### 6.2 Conflict (D4)

On pull, a document whose `updatedAt` is newer than the local row's wins and
overwrites it. Equal timestamps keep local. Deletions are tombstones
(`deletedAt`), never hard deletes, so a delete on one device propagates
instead of being resurrected by the other device's push.

### 6.3 First sign-in — the part that can lose the owner's data

The owner has real logged meals on their phone from before this phase. Three
cases, and the implementation must distinguish them:

| Case | What it means | What happens |
|---|---|---|
| **A** | Local rows exist, no account has ever signed in on this device | **Adopt**: stamp every local row `syncedAt = null`, then `pushPending()`. Nothing is downloaded first. The local data becomes this account's cloud data. |
| **B** | Signing in as the **same** uid as last time | Normal `pullSince()` then `pushPending()`. |
| **C** | Signing in as a **different** uid | The local rows belong to someone else. **Warn, and require an explicit choice** before anything is deleted (§6.4). |

Case A is the owner's actual path and must be tested with a populated
database. Getting it wrong deletes real logged meals, which is the single
worst outcome available in this phase.

Store the last signed-in uid in DataStore to tell B from C.

### 6.4 Case C dialog — exact text

| Key | EN | zh-rHK |
|---|---|---|
| `signin_other_account_title` | `This phone has records from another account` | `呢部電話有另一個帳戶嘅記錄` |
| `signin_other_account_body` | `Signing in as %1$s will replace the records already on this phone. They have not been uploaded to any account.` | `用 %1$s 登入會取代電話上面現有嘅記錄。嗰啲記錄未上載到任何帳戶。` |
| `signin_other_account_replace` | `Replace` | `取代` |
| `signin_other_account_cancel` | `Cancel` | `取消` |

`Replace` is destructive, so it is the **non-default** action and does not get
the accent colour. Cancel returns to the sign-in screen with nothing changed.

---

## 7. App layer

- **New `auth` route group**: sign-up, sign-in, forgot-password. Shown ahead
  of onboarding when no user is signed in.
- **Onboarding runs after sign-in**, not before, so the profile it writes has
  a uid to belong to. An existing local profile (Case A) skips onboarding.
- **我 tab** gains: the signed-in email, 登出, 匯出我嘅資料, 刪除帳戶.
- **A sync status line** on the 我 tab: `上次同步：14:32` or `未同步`, plus
  一陣再試 on failure. No spinner on Home — D3 means Home never waits on sync.
- Sign-out confirms, and its dialog says plainly that records stay on the
  phone (D7).

### 7.1 Export and delete (`docs/06` principle 3)

- **Export**: writes a single JSON file of every local row through the system
  share sheet. Local-only, so it works offline and needs no Cloud Function.
- **Delete account**: deletes the Firestore subtree, then the Auth user, then
  wipes Room and DataStore. Requires re-entering the password (Firebase
  requires recent sign-in for account deletion, and the prompt is honest about
  why). The confirm dialog names what is deleted and says it cannot be undone.
- Deleting the Firestore subtree from the client needs a recursive delete of
  each subcollection; there is no client-side recursive delete. List and
  batch-delete each known subcollection explicitly, and **test that a meal
  really is gone afterwards**, since a missed subcollection leaves health data
  in the cloud after the user asked for it to be destroyed.

---

## 8. Build and CI

- `app/build.gradle.kts`: add `com.google.gms.google-services`,
  `firebase-bom`, `firebase-auth-ktx`, `firebase-firestore-ktx`.
- `google-services.json` stays `.gitignore`d. Add
  `app/google-services.example.json` with the same shape and placeholder
  values, per `docs/06`, so a fresh clone fails with a clear message.
- **CI**: a step before the Gradle steps writes the secret to the file.

```yaml
      - name: Write google-services.json
        env:
          GOOGLE_SERVICES_JSON: ${{ secrets.GOOGLE_SERVICES_JSON }}
        run: |
          set -euo pipefail
          if [ -z "${GOOGLE_SERVICES_JSON}" ]; then
            echo "GOOGLE_SERVICES_JSON secret is not set" >&2
            exit 1
          fi
          printf '%s' "${GOOGLE_SERVICES_JSON}" > app/google-services.json
```

The explicit emptiness check matters: without it a missing secret writes an
empty file and the build fails later with an unrelated-looking Gradle error.

- The secret is **not** available to pull requests from forks. Only the owner
  pushes here, so this is fine; note it in the workflow so a future
  contributor is not confused.

---

## 9. Tests

**Domain** (`-c settings-domain.gradle.kts`, baseline 196):
- `SyncDecisionTest` — the A/B/C table in §6.3, as a pure function over
  (hasLocalRows, lastUid, newUid).
- `ConflictResolutionTest` — newer wins, equal keeps local, tombstone beats
  an older write.

**core-data**:
- `MIGRATION_3_4` sqlite-jdbc test: columns added, existing meals and weights
  intact, `updatedAt` back-filled from the row's own timestamp, `syncedAt`
  null for every pre-existing row.

**Rules**: §5.1, seven cases, five of them negative.

**App**: `AuthViewModelTest`, and a `SyncEngine` test against a fake
Firestore covering push batching at the 400 boundary.

---

## 10. Owner actions

1. **Add the GitHub Actions secret** — Settings → Secrets and variables →
   Actions → New repository secret, named `GOOGLE_SERVICES_JSON`, with the
   whole file as its value. CI cannot build without it.
2. **Enable Email/Password sign-in** — Firebase console → Authentication →
   Sign-in method → Email/Password → Enable.
3. **Publish the rules** once this PR merges — console → Firestore → Rules,
   paste `firestore.rules`, Publish. (Or `firebase deploy --only firestore:rules`
   if the CLI is installed.) **Until this is done the database is still in
   whatever mode the console left it in**, which for Production mode means
   everything is denied and sync will fail loudly — the safe direction.
4. Nothing about billing. This phase runs entirely inside the free Spark
   plan; Blaze is only needed for Cloud Functions in Phase 3.

---

## 11. Risks

### R1 (CRITICAL) — first sign-in destroying the owner's existing records

§6.3 Case A. The owner has real logged meals that have never been uploaded
anywhere. An implementation that pulls before it pushes, or that treats an
empty cloud as the truth, erases them. Mitigation: the A/B/C decision is a
pure, unit-tested function; Case A never downloads first; and the acceptance
script in §1.1 is run on a database that already has rows.

### R2 (HIGH) — rules are the only protection

§2. Public repo, login-free APK, API key in the APK by design. Mitigation:
deny-by-default, five negative tests, no rule keyed on a client-supplied
field, and App Check recorded in `docs/06` as the gap to close before any
student other than the owner uses the app.

### R3 (MEDIUM) — account deletion leaving data behind

§7.1. There is no client-side recursive delete, so a subcollection added
later will be silently missed by a hand-written delete. Mitigation: the
delete routine iterates a single `SYNCED_SUBCOLLECTIONS` list that the sync
engine also uses, so adding a collection to one adds it to the other; and a
test asserts the tree is empty afterwards.

### R4 (MEDIUM) — Room v3 → v4 while v3 has only just shipped

Phase 1.7 PR B's v3 may not even be installed on the owner's phone before
v4 arrives. Both migrations must therefore be correct in sequence, 2→3→4, and
the sqlite-jdbc test runs the chain, not just the last step.

### R5 (LOW) — the auto-generated project id

`project-57faddb5-33ae-46ba-bfa` is permanent and appears in Firestore URLs
and future Cloud Function endpoints. Cosmetic only. Not worth recreating the
project and re-doing the console setup.
