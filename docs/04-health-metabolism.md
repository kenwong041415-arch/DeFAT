# 04 — Metabolism & Wearable Data Rules

The owner is a nutrition professional: these formulas are the app's ground
truth and must be implemented exactly, as pure unit-tested Kotlin in
`core-domain`. AI never computes these numbers.

## BMR (basal metabolic rate, 基礎代謝率)

- **If body-fat % is known → Katch-McArdle** (preferred; owner measures
  clients' body fat):
  `LBM = weightKg × (1 − fatPct/100)`
  `BMR = 370 + 21.6 × LBM`
- **Otherwise → Mifflin-St Jeor**:
  male: `BMR = 10×weightKg + 6.25×heightCm − 5×age + 5`
  female: `BMR = 10×weightKg + 6.25×heightCm − 5×age − 161`

Reference test vectors (must pass in unit tests):
- Katch-McArdle: 80 kg @ 25% fat → LBM 60 → BMR **1666 kcal**.
- Mifflin male: 80 kg, 175 cm, 30 y → **1748.75 kcal**.
  (Corrected in the Phase 1 PR — this line previously read 1780 kcal, which
  was an arithmetic slip; the formula above gives
  `10×80 + 6.25×175 − 5×30 + 5 = 800 + 1093.75 − 150 + 5 = 1748.75`. The
  female vector below already matched its formula, so the formula was kept
  as ground truth. See `docs/plans/phase1-implementation-plan.md` D4.)
- Mifflin female: 60 kg, 162 cm, 28 y → **1311.5 kcal**.

## TDEE (每日總消耗)

Two modes, chosen automatically per day:

1. **No wearable data** — classic multiplier:
   sedentary 1.2, light 1.375, moderate 1.55, active 1.725, athlete 1.9.
   `TDEE = BMR × multiplier`.
2. **Wearable data available** — build up from parts:
   `TDEE = BMR × 1.1 (NEAT/TEF baseline) + activeEnergyFromWearable`
   The 1.1 baseline covers digestion + non-tracked movement; tune this
   constant with the owner once real data exists.

### Double-counting guard (critical)

- Use Health Connect **Active Calories Burned / Exercise sessions** only —
  never add "total calories burned" (which already includes BMR) on top of
  our BMR.
- If a manual exercise entry overlaps in time with a wearable exercise
  session, count only the wearable session.
- Steps are display/motivation only; never convert steps to kcal when
  active-energy data exists.

## Manual exercise fallback (每日運動消耗)

`kcal = MET × weightKg × hours`. Ship a small curated MET table (walking
3.5, jogging 7, cycling moderate 6.8, swimming 6, weight training 3.5–6,
HIIT 8, hiking 6, badminton 5.5, basketball 6.5, yoga 2.5) — owner to
review and extend.

## Calorie target (削脂目標)

- Default deficit: **20% below TDEE**; floor at owner-approved minimums
  (never below ~1200 kcal female / ~1500 kcal male without trainer
  override).
- Safe rate: 0.5–1.0% bodyweight per week; warn (student + trainer flag)
  when faster.
- Macros for a cut: protein `1.8 g × kg` bodyweight (or `2.2 g × kg` LBM
  when fat% known), fat ≥ `0.6 g × kg`, carbs = remaining calories.
  (4 kcal/g protein & carbs, 9 kcal/g fat.)

## Trend engine

- Weight/fat% displayed as **7-day moving average**; single daily readings
  fluctuate with water and are never used for feedback directly.
- Weekly rate = this week's average − last week's average.
- **Plateau**: < 0.2% bodyweight change over 14 days while logging
  compliance ≥ 70% → suggest (to trainer): recheck intake accuracy, small
  target adjustment, or diet break.
- **Adaptive TDEE** (Phase 4+): compare logged average intake vs measured
  average weight change (7700 kcal ≈ 1 kg fat) to correct the TDEE
  estimate over rolling 3–4 weeks. Trainer approves target changes.

## Health Connect data types to request (read)

`Steps`, `ActiveCaloriesBurned`, `ExerciseSession`, `Weight`,
`BodyFat`, `SleepSession`, `HeartRate` (sleep & HR are context for
comments only in early phases). Request the minimum set per phase —
each extra permission adds friction and review burden (`docs/06`).
