# 03 — Roadmap (phased delivery)

Each phase = Opus plan → Sonnet build → Opus review → owner tries it.
A phase is "done" when the owner has used it on a real device (Samsung).

## Phase 0 — Foundation (this PR)

Blueprint, tech decisions, architecture, formulas, AI pipeline design,
privacy checklist, model-role instructions (CLAUDE.md).

## Phase 1 — Walking skeleton + core math (MVP core)

Implemented per [`docs/plans/phase1-implementation-plan.md`](plans/phase1-implementation-plan.md)
(Opus plan, Sonnet build). Two deviations from the summary below, made
explicit in that plan's §2 and carried forward here:

- **D1 — no Firebase Auth / Firestore in Phase 1.** The app is local-first:
  profile in DataStore, meals/weights in Room. No Firebase project exists
  yet, and the dev container cannot reach Google's Maven repo, so adding
  the Firebase plugin without `google-services.json` would break every
  build. Sign-in + Firestore sync move to **Phase 1.5**, once the owner has
  created the Firebase project (see that plan's §13 and §14).
- **D3 — Firestore security-rules tests move to Phase 1.5 with it** (there
  are no rules and no project to test against yet).

What actually shipped:

- Android project scaffold (Kotlin, Compose, Hilt, Room) — no Firebase yet.
- Profile setup wizard (sex, birth date, height, weight, fat %, activity,
  goal, medical disclaimer).
- `core-domain`: BMR (Mifflin-St Jeor + Katch-McArdle), TDEE, calorie
  target, macro split, day rollup, safe-rate check, MET table — fully
  unit-tested (`docs/04`).
- Home screen: today's target vs intake ring, remaining kcal + protein.
- Manual meal entry + manual weight entry. Day rollup logic.
- English + Traditional Chinese (HK) strings.
- **Exit test**: owner creates own profile, sees a sensible daily target,
  logs a manual meal.

## Phase 1.6 — UI revision + meal types

Implemented per [`docs/plans/phase1.6-ui-revision-plan.md`](plans/phase1.6-ui-revision-plan.md)
(Opus plan, Sonnet build). Unplanned in the original roadmap: it exists
because the owner installed the Phase 1 build, confirmed the metabolism math,
and reported that the home screen was cluttered and that meals needed to be
split by meal type.

- Home rebuilt to the owner-approved "coach" layout: ring, three compact
  macro tiles, meals grouped by meal type with per-type subtotals. The
  full-width macro bars are gone — at zero they read as full, which is what
  the owner found confusing.
- Five meal types (早餐 / 午餐 / 下午茶 / 晚餐 / 小食), inferred from the clock
  when a meal is created and back-filled onto existing rows by a Room v1 → v2
  migration.
- Navigation drawer; weight logging moves off Home into it. A new History
  screen shows any past day. 進度圖表 appears disabled — it is Phase 4.
- Meal editor: meal-type chips, working date/time pickers, a frequent-food
  quick-add list, and a block on saving a 0-kcal meal.
- Fixed pine/jade brand theme; Material You dynamic colour removed.
- A stable debug keystore is committed so test builds install over each other
  instead of forcing an uninstall that wipes the tester's data (`docs/06`).
- **Exit test**: owner reads their macro standing at a glance, logs a meal at
  the right meal type in under 20 seconds, and finds yesterday in 歷史記錄.

## Phase 2 — Wearables via Health Connect

- Health Connect permissions flow + per-brand onboarding guides
  (Samsung Health / Xiaomi Mi Fitness / Fitbit sync setup).
- Pull steps, active energy, exercise sessions, sleep, weight (smart
  scale); WorkManager background sync; double-counting guard (`docs/04`).
- Manual exercise entry fallback (MET-based).
- **Exit test**: steps + workout from the owner's watch appear in the app
  and adjust the daily balance.

## Phase 3 — AI meal estimation

- Cloud Functions proxy with auth + per-user daily quota; Claude key in
  Secret Manager.
- `/estimateMeal`: food photo → items + portions + macros (editable card).
- Text/voice description estimation; nutrition-label photo (serving math).
- Confidence display + easy correction UX (corrections stored for later
  personalization).
- **Exit test**: photo of a real cha chaan teng meal returns a usable
  estimate in < 10 s; owner judges accuracy acceptable.

## Phase 4 — Restaurant menu helper + progress engine

- `/analyzeMenu`: menu photo + remaining budget + goals → ranked picks
  with reasoning ("揀呢個：蒸魚飯，走汁").
- Trend engine: 7-day moving averages, weekly rate-of-loss vs plan,
  plateau detection, adaptive target suggestions (trainer-gated).
- Progress comments + smart reminders (FCM + local notifications),
  weekly check-in flow (photos, measurements).
- **Exit test**: two weeks of real data produces a correct weekly report
  and at least one useful automatic comment.

## Phase 5 — Trainer dashboard + release

- Web dashboard (Firebase Hosting): student list, red flags, notes,
  message push.
- Streaks, water tracking, diet-break suggestions.
- Play Store closed testing: health declaration, privacy policy page,
  data-safety form (`docs/06`), store listing (EN + 繁中).
- **Exit test**: 3–5 real students onboarded in closed testing.

## Later / parking lot

- iOS app (SwiftUI + HealthKit for Apple Health), sharing `core-domain`
  via Kotlin Multiplatform.
- Barcode scanning against a licensed food database.
- Payments/subscription if the owner wants to commercialize.
- Voice-first logging, meal planning (pre-planned menus), recipe library.
