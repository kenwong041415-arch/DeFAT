# 03 — Roadmap (phased delivery)

Each phase = Opus plan → Sonnet build → Opus review → owner tries it.
A phase is "done" when the owner has used it on a real device (Samsung).

## Phase 0 — Foundation (this PR)

Blueprint, tech decisions, architecture, formulas, AI pipeline design,
privacy checklist, model-role instructions (CLAUDE.md).

## Phase 1 — Walking skeleton + core math (MVP core)

- Android project scaffold (Kotlin, Compose, Hilt, Room, Firebase).
- Sign-in (Firebase Auth: email + Google).
- Profile setup wizard (age, sex, height, weight, fat %, activity, goal).
- `core-domain`: BMR (Mifflin-St Jeor + Katch-McArdle), TDEE, calorie
  target and macro split calculators — fully unit-tested (`docs/04`).
- Home screen: today's target vs intake ring, remaining kcal + protein.
- Manual meal entry + manual weight entry. Day rollup logic.
- **Exit test**: owner creates own profile, sees a sensible daily target,
  logs a manual meal.

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
