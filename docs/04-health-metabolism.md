# 04 — Metabolism & Wearable Data Rules

The owner is a nutrition professional: these formulas are the app's ground
truth and must be implemented exactly, as pure unit-tested Kotlin in
`core-domain`. AI never computes these numbers.

## BMR (basal metabolic rate, 基礎代謝率)

- **If body-fat % is known → Katch-McArdle** (preferred; owner measures
  clients' body fat):
  `LBM = weightKg × (1 − fatPct/100)`
  `BMR = 370 + 21.6 × LBM`
- **Otherwise → Henry (2005) "Oxford" equations**, banded by sex and age,
  weight in kg and height in cm, result in kcal/day:

  | Sex | Age | BMR |
  |---|---|---|
  | M | 18–30 | `14.4×W + 3.13×H + 113` |
  | M | 30–60 | `11.4×W + 5.41×H − 137` |
  | M | 60+ | `11.4×W + 5.41×H − 256` |
  | F | 18–30 | `10.4×W + 6.15×H − 282` |
  | F | 30–60 | `8.18×W + 5.02×H − 11.6` |
  | F | 60+ | `8.52×W + 4.21×H + 10.7` |

  Bands are **half-open on the lower bound**: age 30 uses the 30–60 row,
  age 60 uses the 60+ row. The app is for adults only; a profile under 18
  is rejected in the wizard rather than computed with an adult equation.

Reference test vectors (must pass in unit tests):
- Katch-McArdle: 80 kg @ 25% fat → LBM 60 → BMR **1666 kcal**.
- Henry male 18–30: 80 kg, 175 cm, 25 y → **1812.75 kcal**
  (`14.4×80 + 3.13×175 + 113 = 1152 + 547.75 + 113`).
- Henry male 30–60: 80 kg, 175 cm, 35 y → **1721.75 kcal**
  (`11.4×80 + 5.41×175 − 137 = 912 + 946.75 − 137`).
- Henry female 18–30: 60 kg, 162 cm, 28 y → **1338.3 kcal**
  (`10.4×60 + 6.15×162 − 282 = 624 + 996.3 − 282`).
- Henry female 30–60: 58 kg, 160 cm, 38 y → **1266.04 kcal**
  (`8.18×58 + 5.02×160 − 11.6 = 474.44 + 803.2 − 11.6`).

### Why Henry, and what it costs us

Set by the owner (Phase 1.7) after a competitor app was found using the
Oxford/Henry equations and claiming better accuracy for Asian populations.
Three things to record honestly:

1. **The accuracy claim is unverified.** A search of the literature found no
   study directly comparing Henry with Mifflin-St Jeor in a Hong Kong or
   Chinese population, and the general literature still broadly favours
   Mifflin-St Jeor. The owner is the nutrition professional and made this
   call as a professional judgement, not on evidence we could produce.
2. **The coefficients are second-hand.** Henry (2005), *Public Health
   Nutrition*, is the primary source and could not be opened from the
   development container (egress policy blocks the publisher). The kcal
   coefficients above come from two independent secondary sources that
   agree — a PENG clinical presentation (weight-only forms) and a
   published conversion of the weight-and-height table — and were
   cross-checked by converting each back to MJ/day, where every one lands
   on a clean three-significant-figure value (e.g. `14.4 → 0.0600`,
   `3.13 → 1.31`, `113 → 0.473`). That is good evidence of a faithful unit
   conversion rather than a fabricated table, but it is **not** primary
   verification. Re-check against the paper when it is reachable.
3. **The age bands are discontinuous, and students will see it.** Henry's
   banded structure makes BMR step down at the band edge: a man at 80 kg /
   175 cm drops 91 kcal of BMR (1813 → 1722) the day he turns 30, and a
   woman at 60 kg / 162 cm drops 46. After the activity multiplier and the
   20% deficit that is roughly a 50 kcal target step for no physiological
   reason. We implement the published equation **exactly** (per CLAUDE.md)
   and accept the step; smoothing it by interpolating across the boundary
   would be our invention, not Henry's, and is the owner's call if the step
   ever confuses a student.

Practical effect of the switch, measured across realistic profiles: Henry
runs **0–2% above Mifflin-St Jeor for under-30s and up to 6% above for
50-plus** (e.g. F 50 y / 70 kg / 158 cm: 1354 vs 1276). Targets therefore
move up slightly, mostly for older students — a marginally slower planned
rate of loss, not a different plan.

**Katch-McArdle was deliberately kept** for the body-fat-known path, which
is most of the owner's students. The competitor app's own displayed figure
(1,828 kcal at 90 kg / 25% fat) equals `370 + 21.6 × 67.5` to the kcal,
i.e. it is using Katch-McArdle there too; the other common lean-mass
formula, Cunningham (`500 + 22 × LBM`), gives 1,985 and does not match. So
on the path that matters most, following the competitor meant changing
nothing.

Mifflin-St Jeor stays implemented and unit-tested in `core-domain` as the
alternative formula (reference vectors: male 80 kg / 175 cm / 30 y →
**1748.75**; female 60 kg / 162 cm / 28 y → **1311.5**). It is not used by
default. Keeping it is what makes a future "formula" setting, or a
re-evaluation if the Henry claim is ever tested in a Chinese population, a
one-line change.

## TDEE (每日總消耗)

Two modes, chosen automatically per day:

1. **No wearable data** — classic multiplier:
   sedentary 1.2, light 1.375, moderate 1.55, active 1.725, athlete 1.9.
   `TDEE = BMR × multiplier`.
2. **Wearable data available** — build up from parts:
   `TDEE = BMR × 1.1 (NEAT/TEF baseline) + activeEnergyFromWearable`
   The 1.1 baseline covers digestion + non-tracked movement; tune this
   constant with the owner once real data exists.

### Manually logged exercise and the activity multiplier (Phase 1.7)

The activity multiplier **already contains** the student's usual exercise —
that is what "light" or "moderate" means. Adding a logged session on top of
`BMR × multiplier` counts the same workout twice and hands the student a
target that is too high, which is the direction a fat-loss app must never
get wrong. So on a day with manually logged exercise:

```
TDEE(day) = max(
    BMR × activityMultiplier,              // the profile's stated baseline
    BMR × 1.1 + loggedExerciseKcal         // the day as actually logged
)
```

Properties this guarantees, each a required unit test:

- **No exercise logged → identical to the multiplier-only result.** This is
  a regression guard: adding the feature must not move any existing day.
- A small session on a "moderate" profile does not move the target — that
  session is already what "moderate" assumed.
- A big session raises the target only by the part the multiplier did not
  already assume.
- The target never drops below `BMR × activityMultiplier`.

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
