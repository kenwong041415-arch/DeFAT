# 02 — Architecture

## System overview

```
┌─────────────── Android app (Kotlin + Compose) ───────────────┐
│  UI (Compose screens)                                        │
│  ViewModels                                                  │
│  Domain layer  ← pure Kotlin: all nutrition/metabolism math  │
│  Data layer                                                  │
│   ├─ Room (local cache, offline-first logs)                  │
│   ├─ Firestore sync (profile, logs, targets, feedback)       │
│   ├─ Health Connect client (steps, calories, sleep, weight)  │
│   └─ CameraX (food / label / menu photos)                    │
└──────────────────────────────────────────────────────────────┘
                │ Firebase Auth (sign-in)
                ▼
┌──────────────────── Firebase (asia-east2) ───────────────────┐
│  Firestore  — user data (rules: user reads own docs;         │
│               trainer role reads all students)               │
│  Storage    — meal/check-in photos (private per user)        │
│  Cloud Functions:                                            │
│   ├─ /estimateMeal   (photo|text|label → macros JSON)        │
│   ├─ /analyzeMenu    (menu photo + remaining budget → picks) │
│   ├─ /dailyDigest    (scheduled: progress comments, alerts)  │
│   └─ quota + auth middleware                                 │
│  FCM        — reminders & trainer messages                   │
└──────────────────────────────────────────────────────────────┘
                │ (server-side only, key in Secret Manager)
                ▼
                      Claude API (vision + text)
```

## App module structure

```
app/            Compose UI, navigation, DI wiring (Hilt)
core-domain/    pure Kotlin: entities + calculators (no Android deps)
core-data/      Room, Firestore, Health Connect, repositories
core-ai/        client for our Cloud Functions endpoints
```

`core-domain` is the future Kotlin Multiplatform seed — keep it free of
Android imports.

## Data model (Firestore, first cut)

- `users/{uid}` — profile: name, sex, birthDate, heightCm, activityLevel,
  goal {targetWeightKg, targetFatPct, targetDate}, dietary flags, locale,
  role (`student` | `trainer`), trainerId.
- `users/{uid}/weights/{date}` — weightKg, fatPct, loggedAt, source
  (`manual` | `healthconnect`). *(Phase 1.5 plan §4 named this `weights`;
  the first cut here said `measurements`. `waistCm` and other tape
  measurements get their own collection when they exist.)*
- `users/{uid}/days/{date}` — cached daily rollup: bmr, tdee, activeKcal,
  steps, intakeKcal, protein/carbs/fatG, balance, status comment.
- `users/{uid}/meals/{mealId}` — timestamp, mealType (breakfast|lunch|
  afternoonTea|dinner|snack), type (photo|text|label|manual), photoRef,
  aiEstimate {kcal, proteinG, carbsG, fatG, confidence, items[]},
  userOverride {…}, finalValues {…}.
  `mealType` was added in Phase 1.6 and already exists on-device (Room v2);
  Phase 1.5 sync must carry it.
- `users/{uid}/feedback/{id}` — generated comments, trainer notes.
- `users/{uid}/exercise/{id}`, `users/{uid}/water/{epochDay}`,
  `users/{uid}/foodOverrides/{foodId}` — added in Phase 1.5. `foodOverrides`
  holds only the rows the owner edited in the 食物庫; the seeded library is
  identical on every install and never syncs.

**The list of subcollections lives in one place** —
`core-domain/.../sync/SyncCollections.kt` (`SYNCED_SUBCOLLECTIONS`). The sync
engine, the delete-account routine and the rules tests all read it.

**Sync model (Phase 1.5).** Room is the single write path and the UI reads
only Room, so a sync failure can never block logging. Every synced row carries
`updatedAt`, `syncedAt` (null or older than `updatedAt` = pending) and, for
deletable rows, a `deletedAt` tombstone. Conflict is last-write-wins per
document on the client `updatedAt`; equal timestamps keep local. Pulling is
driven by a separate server-assigned `serverUpdatedAt`, because a phone's own
clock cannot be trusted to order another phone's commits. Pulling only ever
upserts; it never deletes a local row because a cloud document is missing.

Rule of thumb: the app always trusts `finalValues` (user-confirmed), never
the raw AI estimate.

## Key flows

1. **Meal log**: snap photo → upload to Storage → call `/estimateMeal` →
   show editable result card (items, portions, macros) → user confirms →
   save `finalValues` → day rollup updates.
2. **Daily sync**: on app open + WorkManager periodic job → read Health
   Connect (steps, active kcal, exercise sessions, sleep, weight) → update
   `days/{date}` → recompute balance → refresh home screen ring.
3. **Evening digest** (scheduled function): compute day result → write a
   progress comment → FCM push ("今日做得好！蛋白質達標 ✅").

## Non-functional requirements

- Offline: logging must work with no network (Room queue → sync later).
  AI estimation requires network; fall back to manual entry offline.
- All health data reads happen on-device; only derived numbers the user
  logs are uploaded (see `docs/06` for what never leaves the phone).
- Every calculator function in `core-domain` has unit tests with known
  reference values before it is used in UI.
