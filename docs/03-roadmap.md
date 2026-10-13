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

## Phase 1.5 — Sign-in, cloud sync, security rules

Planned in [`docs/plans/phase1.5-implementation-plan.md`](plans/phase1.5-implementation-plan.md).
Deferred here from Phase 1 (its D1 and D3) because no Firebase project
existed. The project now does: `asia-east2` (Hong Kong), package
`com.defat.app`, Analytics off.

- Email + password auth; Google Sign-In deliberately deferred (no SHA-1
  registered, and it adds console setup for no gain yet).
- Offline-first sync: Room stays the write path, Firestore mirrors it, so a
  sync failure can never block logging.
- **Security rules are the only protection in this phase** — the repository
  is public and the test APK is published login-free, so the Firebase API key
  is public by design. Rules are deny-by-default and five of the seven
  required rules tests are negative. App Check is recorded as an accepted gap
  to close before any student other than the owner uses the app.
- Export my data and delete my account (`docs/06` principle 3).
- **Exit test**: the owner signs up, finds every meal they had already logged
  still there, sees the same data on a second device, logs offline and
  watches it sync, and can export and delete their account from the app.

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

## Phase 1.7 — CaLoMei-aligned structure (two PRs)

Planned in [`docs/plans/phase1.7-implementation-plan.md`](plans/phase1.7-implementation-plan.md)
(Opus plan, Sonnet build). Unplanned in the original roadmap: the owner found
a competitor app (CaLoMei), asked for the same structure, and said
explicitly **語氣唔跟** — copy the structure, not the snarky tone.

Ships as two PRs because the combined scope is too large for one review.

**PR A — structure**

- **BMR formula change**: Henry (2005) "Oxford" equations replace
  Mifflin-St Jeor on the no-body-fat path, by the owner's decision as the
  nutrition professional. Katch-McArdle is unchanged on the body-fat path —
  the competitor's own displayed figure turned out to be Katch-McArdle too,
  so following it meant changing nothing where it matters most (`docs/04`).
- **Bottom navigation** (今日 / 歷史 / + / 進度 / 我). This **reverses the
  P1.6 drawer decision** on the owner's instruction; the drawer is deleted.
  P1.6's plan is left as written so the history stays legible.
- Week date strip on Home; Home renders any of the last 7 days.
- **Day-start time** setting (default 04:00) so a 2 a.m. snack counts to the
  previous day. Invasive — it changes day bucketing everywhere.
- Dark-first theme, keeping DeFAT's pine/jade; no amber.
- Profile wizard now requires age 18+.

**PR B — logging features**

- **食物庫** — a seeded, searchable, source-tagged, owner-editable food
  table (`docs/07`). Not a table of asserted facts: the official CFS
  database was unreachable and the public lists disagree by up to 2×, so
  every row carries its source and confidence and the owner's corrections
  are permanent.
- **Modifiers (點食法)** — 少飯 / 瘦 / 走汁 / 半糖 / 走牛油 as first-class
  ± deltas rather than a range. The owner asked whether portion and modifier
  choices explain the published spread; reconstructing the dishes from
  composition values says they largely do (`docs/07` §6), so the variation
  is modelled instead of averaged. The running total shows its own
  arithmetic — seeing 走汁 −100 is the coaching, not clutter.
- Manual MET-based exercise logging, with the double-counting rule in
  `docs/04` that stops it inflating the day's target.
- Water tracking (35 ml/kg heuristic, owner to confirm).
- Streak (跟足計劃日數) — a factual count, no engagement mechanics.
- In-app 數據來源 page explaining, in Cantonese, where every number comes
  from and how much to trust it.

- **Exit test**: owner navigates entirely by the bottom bar, adds a food
  from the library and sees its 「估算」 badge, corrects it and sees the
  correction persist, logs a 45-minute session and sees the target rise
  sensibly rather than double, and finds the 數據來源 page.

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
