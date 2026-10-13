# Phase 1.7 — Implementation Plan (CaLoMei-aligned structure)

Status: approved plan, ready for implementation.
Author: Opus planning pass (per `CLAUDE.md` model roles).
Implementer: Sonnet. Follow this document literally; where it says "exact",
copy the text verbatim. If something here contradicts `docs/`, stop and flag
it rather than silently choosing.

Predecessors: [`phase1-implementation-plan.md`](phase1-implementation-plan.md)
("P1") and [`phase1.6-ui-revision-plan.md`](phase1.6-ui-revision-plan.md)
("P1.6"). Everything in those still holds unless this file overrides it.

**This phase ships as two PRs.** `CLAUDE.md` requires PRs small enough to
review, and the combined scope below is not. The split is in §2.

- **PR A** — navigation, theme, BMR formula, week strip, day-start-time
  setting.
- **PR B** — food library and modifiers, exercise logging, water, streak,
  data-sources page.

**Branch reality (deviation from the original §2 wording).** The AI
development sessions for this project are pinned to a single designated
branch, `claude/fitness-weight-loss-app-wubj4k`, and may not push elsewhere.
PR A therefore lands on that branch and rides in the **same pull request as
these planning docs** (PR #4) rather than getting a `phase1.7a/structure`
branch of its own. PR B becomes a separate pull request on the same branch
once PR #4 is merged. The review-size intent is unchanged — PR A's code plus
docs is still a reviewable unit, and PR B's four tables stay out of it.

### Running the tests before you push

The math tests can be run in a container with no Android SDK and no access
to Google's Maven repo:

```
./gradlew -c settings-domain.gradle.kts :core-domain:test
```

`settings-domain.gradle.kts` includes `:core-domain` alone. The plain
`:core-domain:test` does **not** work without the Android toolchain, because
Gradle evaluates `:app`'s build script during configuration — the README
claimed otherwise and was wrong until this phase. Baseline before any
Phase 1.7 work: **66 tests, 0 failures.** Maven Central occasionally answers
`429 Too Many Requests` through the shared proxy; that is transient, so retry
rather than treating it as a failure.

`:core-data` and `:app` still cannot be compiled locally at all. CI is the
only validator for those, so every Kotlin change outside `:core-domain` is
unverified until CI runs. Budget for that: Phase 1 took five red CI rounds.

---

## 1. Why this phase exists

The owner found a competitor app (CaLoMei), studied it, and asked for the
same structure with one explicit exception: **語氣唔跟** — the structure is
copied, the snarky tone is not. They then made three decisions, recorded in
§3, and asked for the Hong Kong food numbers to be researched rather than
supplied by hand. That research is `docs/07-hk-food-library.md` and its
negative result shapes §8 of this plan.

Nothing here needs Firebase, Health Connect, the camera, or the network. The
app stays local-first and offline, exactly as P1 left it.

### 1.1 Exit test

> The owner opens DeFAT, moves between 今日 / 歷史 / 進度 / 我 with the bottom
> bar and never looks for a hamburger menu; taps a day in the week strip and
> sees that day; adds 叉燒飯 from the food library in two taps and sees the
> 「估算」 badge on it; corrects its calories and sees the correction stick on
> the next app launch; logs a 45-minute weight-training session and watches
> the day's target rise by a sensible amount, not double; logs 8 cups of
> water; sees a streak count that matches the days they actually logged; and
> finds a 數據來源 page that explains, in Cantonese, where every number comes
> from.

---

## 2. Scope, split across the two PRs

### PR A — structure (no new Room tables, no new logging features)

| # | Item | §  |
|---|---|---|
| A1 | **Henry (2005) BMR** replaces Mifflin-St Jeor on the no-body-fat path. Katch-McArdle unchanged. | §5 |
| A2 | **Bottom navigation** — 今日 / 歷史 / [+] / 進度 / 我. `DefatDrawer` deleted. | §6 |
| A3 | **Week date strip** on Home; Home can show any of the last 7 days. | §6.3 |
| A4 | **Day-start time** setting (default 04:00) so a 2 a.m. snack lands on the previous day. | §7 |
| A5 | **Dark-first theme** — dark is the default scheme, DeFAT's pine/jade kept, no amber. | §9 |
| A6 | **Minimum age 18** in the profile wizard. | §5.4 |

### PR B — logging features (Room v2 → v3)

| # | Item | §  |
|---|---|---|
| B1 | **食物庫** — seeded, searchable, source/confidence badges, owner-editable. | §8 |
| B2 | **Manual exercise logging** with the MET table, and the TDEE rule that stops it double-counting. | §10 |
| B3 | **Water tracking** with a weight-derived goal. | §11 |
| B4 | **Streak (連續日數)** — consecutive days with at least one meal. | §12 |
| B5 | **數據來源 page** — plain-Cantonese provenance for every number. | §13 |

### Out of scope (unchanged from P1.6 unless listed)

Firebase (Phase 1.5), Health Connect (Phase 2), camera and AI estimation
(Phase 3), charts and the trend engine (Phase 4) — 進度 remains a placeholder
tab — notifications, trainer dashboard, and **CaLoMei's 語氣模式 / 認罪 / 案底
/ 受刑日數 wording, which is deliberately not copied** (§4).

---

## 3. The owner's decisions

Asked three questions on the published design proposal, the owner answered:

| # | Question | Answer | Consequence |
|---|---|---|---|
| 1 | Bottom tabs, or keep the P1.6 drawer? | **底部制 跟** — bottom tabs | §6. This reverses a P1.6 decision; the drawer is deleted, not hidden. |
| 2 | Keep Mifflin-St Jeor, switch to Oxford/Henry, or make it selectable? | **跟佢** — follow the competitor | §5. Henry on the no-fat path; Katch-McArdle stays. |
| 3 | Dark theme, keeping DeFAT green rather than the competitor's amber? | **ok** | §9. Dark default, pine/jade kept. |

Decision 2 was made after being told plainly that the accuracy claim behind
it could not be verified and that the call belonged to the owner as the
nutrition professional. That exchange is recorded in `docs/04` so nobody
later mistakes it for an evidence-based change.

---

## 4. Tone translation — binding wording table

CaLoMei's structure is copied; its wording is not. Use **exactly** these
strings. Do not invent a middle ground, and do not reintroduce the
competitor's framing anywhere, including in code comments.

| CaLoMei | DeFAT (use this) |
|---|---|
| 認罪 | 搵食物 |
| 案底 | 今日記錄 |
| 受刑日數 | 跟足計劃日數 |
| 你嘅戰績 | 進度 |
| 你嘅配額 | 每日目標 |
| 記低佢！ | 記錄 |
| BMI commentary / body-shaming lines | **removed entirely** |
| 語氣模式 (毒舌／溫柔 selector) | **not built** |

Rule for all new copy: state the number, state what to do, stop. No
judgement words about the student's body or discipline. The app is the
owner's professional voice talking to their own students.

---

## 5. A1 — Henry (2005) BMR

Ground truth is the table in `docs/04-health-metabolism.md`. Read that
section before writing code, including its three honesty notes.

### 5.1 `model/BmrMethod.kt` (edit)

Add one entry. Do **not** remove `MIFFLIN_ST_JEOR`.

```kotlin
enum class BmrMethod {
    KATCH_MCARDLE,
    HENRY_OXFORD,
    MIFFLIN_ST_JEOR,
}
```

### 5.2 `calc/NutritionConstants.kt` (edit)

Add the Henry coefficients as named constants. No magic numbers in the
calculator body.

```kotlin
// Henry (2005) "Oxford" equations — docs/04. kcal/day, W in kg, H in cm.
// Secondary-sourced coefficients; see docs/04 note 2 before changing them.
const val HENRY_M_18_30_W = 14.4
const val HENRY_M_18_30_H = 3.13
const val HENRY_M_18_30_C = 113.0
const val HENRY_M_30_60_W = 11.4
const val HENRY_M_30_60_H = 5.41
const val HENRY_M_30_60_C = -137.0
const val HENRY_M_60_PLUS_W = 11.4
const val HENRY_M_60_PLUS_H = 5.41
const val HENRY_M_60_PLUS_C = -256.0
const val HENRY_F_18_30_W = 10.4
const val HENRY_F_18_30_H = 6.15
const val HENRY_F_18_30_C = -282.0
const val HENRY_F_30_60_W = 8.18
const val HENRY_F_30_60_H = 5.02
const val HENRY_F_30_60_C = -11.6
const val HENRY_F_60_PLUS_W = 8.52
const val HENRY_F_60_PLUS_H = 4.21
const val HENRY_F_60_PLUS_C = 10.7

/** Lower bound of the adult band Henry's adult equations cover. */
const val HENRY_MIN_ADULT_AGE = 18
```

### 5.3 `calc/BmrCalculator.kt` (edit)

Add `henryOxford`, switch `bmr()` to use it, keep `mifflinStJeor` untouched.

```kotlin
/**
 * Henry (2005) "Oxford" equations — docs/04. Banded by sex and age;
 * bands are half-open on the lower bound (age 30 -> the 30..59 band,
 * age 60 -> the 60+ band).
 *
 * The bands are discontinuous by design: BMR steps down at each boundary
 * (see docs/04). We implement the published equation exactly and do not
 * interpolate; smoothing would be our invention, not Henry's.
 *
 * @throws IllegalArgumentException if [ageYears] < HENRY_MIN_ADULT_AGE —
 *   DeFAT is an adults-only app and the wizard rejects minors (§5.4).
 */
fun henryOxford(weightKg: Double, heightCm: Double, ageYears: Int, sex: Sex): Double
```

Implementation requirements:

- `require(weightKg > 0)`, `require(heightCm > 0)` with the same message
  style as `mifflinStJeor`.
- `require(ageYears >= NutritionConstants.HENRY_MIN_ADULT_AGE)` with the
  message `"ageYears must be >= 18 (adults only), was $ageYears"`.
- Band selection by `when`, on `ageYears` only — never on a `Double` age.
- Return `w * weightKg + h * heightCm + c` with the constants above. One
  expression, no intermediate rounding.

`bmr()` changes only in its else-branch:

```kotlin
fun bmr(profile: UserProfile, on: LocalDate): BmrResult {
    val fatPct = profile.bodyFatPct
    return if (fatPct != null) {
        BmrResult(katchMcArdle(profile.weightKg, fatPct), BmrMethod.KATCH_MCARDLE)
    } else {
        val age = profile.ageYears(on)
        BmrResult(
            henryOxford(profile.weightKg, profile.heightCm, age, profile.sex),
            BmrMethod.HENRY_OXFORD,
        )
    }
}
```

Also update the KDoc on `bmr()` and the `UserProfile.bodyFatPct` comment
(`// null = unknown -> Mifflin path` is now wrong; make it
`// null = unknown -> Henry path`).

### 5.4 A6 — minimum age 18

`BasicsScreen` / `OnboardingViewModel`: a birth date giving an age under 18
on today's date is a validation error, not a silent clamp.

- String key `error_min_age_18`.
- EN: `"DeFAT is for adults (18+). Please ask your trainer for guidance."`
- zh-rHK: `"DeFAT 只適用於 18 歲以上。請直接向你嘅教練查詢。"`
- The Next button stays disabled while the error shows.

Rationale: Henry's adult equations do not apply to minors, and `docs/06`'s
medical-disclaimer posture does not cover coaching a child. Rejecting is
honest; computing an adult target for a 15-year-old is not.

### 5.5 The BMR method label the user sees

`HomeScreen`'s metabolism footnote and `ProfileScreen` must name the method
in use. String keys and exact text:

| Key | EN | zh-rHK |
|---|---|---|
| `bmr_method_katch` | `Katch-McArdle (uses your body fat %)` | `Katch-McArdle（用你嘅體脂率）` |
| `bmr_method_henry` | `Henry / Oxford (2005)` | `Henry／Oxford（2005）` |
| `bmr_method_note_henry` | `Add your body fat %% for a more personal number.` | `加入體脂率，個數字會更貼你。` |

**Correction (review pass): use a bare `%`, not `%%`.** The original text here
told the implementer to escape it. That was wrong. `stringResource(id)` calls
`getString(id)` with no arguments, which does no format substitution, so `%%`
would render on screen as two literal percent signs. The repo already ships
three no-argument strings containing a bare `%` — `body_fat_pct`,
`body_fat_hint`, `summary_method_katch` — and they pass lint. §14's identical
advice is wrong for the same reason; escape `%` only in strings that actually
take format arguments.

---

## 6. A2 — bottom navigation

### 6.1 Structure

Five slots. The centre is a raised add button, not a destination.

| Slot | Label (EN / zh-rHK) | Route | Icon |
|---|---|---|---|
| 1 | Today / 今日 | `home` | `Icons.Outlined.Home` |
| 2 | History / 歷史 | `history` | `Icons.Outlined.DateRange` |
| 3 | — (centre FAB) | opens `meal_editor` for the selected day | `Icons.Filled.Add` |
| 4 | Progress / 進度 | `progress` | `Icons.Outlined.ShowChart` |
| 5 | Me / 我 | `profile` | `Icons.Outlined.Person` |

Requirements:

- `NavigationBar` + four `NavigationBarItem`s, with the FAB in a
  `Scaffold(floatingActionButton = …, floatingActionButtonPosition = Center)`
  docked above the bar. Do not fake the centre slot with a fifth
  `NavigationBarItem`; a disabled-looking tab that is really a button fails
  TalkBack.
- The FAB carries `contentDescription = stringResource(R.string.action_add_meal)`.
- 進度 is **enabled and navigable** but its screen is a single centred
  placeholder (§6.4). P1.6 showed it as a disabled drawer row; a disabled
  bottom tab reads as a bug.
- Tab state survives rotation and process death: hoist the selected route
  from the `NavController`'s current back-stack entry, not from a local
  `remember`.
- Re-tapping the current tab pops that tab's stack to its root.

### 6.2 Deletions

- `app/src/main/kotlin/com/defat/app/ui/component/DefatDrawer.kt` — delete.
- All `ModalNavigationDrawer`, `DrawerState`, `rememberDrawerState` and
  `scope.launch { drawerState.close() }` usages in `HomeScreen.kt` — delete.
  The P1.6 fix that moved navigation inside the drawer coroutine goes with
  it; the FAB and tabs navigate directly.
- The hamburger `IconButton` in Home's `TopAppBar` — delete. Home's top bar
  keeps only the title and the date.
- 記錄體重 was a drawer row. It moves to **today's 我 tab** as a row, and to
  Home as a secondary action under the ring. Route `log_weight` is unchanged.

### 6.3 A3 — week date strip

A horizontal row of seven days at the top of Home, under the top bar.

- Shows the selected day's week, Monday-first (Hong Kong convention).
- Each cell: weekday initial (一 二 三 四 五 六 日 / M T W T F S S) above the
  day number.
- The selected day is a filled pill in `primary`; today, when not selected,
  gets a 2 dp `primary` outline; days with at least one logged meal get a
  small dot under the number; **future days are disabled**, not hidden.
- Tapping a day sets Home's `selectedDate`. Everything on Home — ring, macro
  tiles, meal sections, exercise, water — follows `selectedDate`, not
  `today`.
- Swiping the strip left/right moves by one week, back to the earliest logged
  day and forward to the current week only.
- `HomeUiState` gains `selectedDate: LocalDate` and
  `weekDays: List<WeekDayCell>` where
  `WeekDayCell(date, isSelected, isToday, hasEntries, isFuture)`.

This makes Home and History overlap on purpose: the strip covers "the last
few days", the 歷史 tab covers "any day, with totals". Do not delete
`HistoryScreen`; retarget it as the tab (§6.5).

### 6.4 `progress` placeholder screen

Centred icon, a heading and one line of body text. No fake charts.

| Key | EN | zh-rHK |
|---|---|---|
| `progress_title` | `Progress` | `進度` |
| `progress_soon_heading` | `Charts are coming` | `圖表稍後推出` |
| `progress_soon_body` | `Once you have a couple of weeks of logs, this tab will show your weight trend and weekly rate.` | `累積幾個星期記錄之後，呢一頁會顯示你嘅體重趨勢同每週變化。` |

### 6.5 `HistoryScreen` becomes the 歷史 tab

- Keep the existing date picker, add a reverse-chronological list of days
  that have any entry: date, total kcal vs target, and a 達標 / 超標 chip.
- Tapping a row opens that day *on Home* by setting `selectedDate` — one
  place renders a day, not two.

---

## 7. A4 — day-start time

### 7.1 Why

A student eating at 01:30 is still on "yesterday". Without this, a late
snack silently lands on a fresh day, both days read wrong, and the streak
breaks. CaLoMei exposes the same setting; the owner's students keep late
hours.

### 7.2 Domain

New pure helper, `core-domain/calc/DayBoundary.kt`:

```kotlin
/**
 * Maps an instant to the logical day it belongs to, given the user's
 * day-start time. A dayStart of 04:00 means 2026-10-13T01:30 local is still
 * logged against 2026-10-12.
 */
object DayBoundary {
    val DEFAULT_DAY_START: LocalTime = LocalTime.of(4, 0)

    fun logicalDate(at: Instant, zone: ZoneId, dayStart: LocalTime): LocalDate
    fun startOfLogicalDay(date: LocalDate, zone: ZoneId, dayStart: LocalTime): Instant
    fun endOfLogicalDayExclusive(date: LocalDate, zone: ZoneId, dayStart: LocalTime): Instant
}
```

`logicalDate` = take the local date-time; if its `LocalTime` is before
`dayStart`, subtract one day. Half-open on the lower bound: a meal exactly at
`dayStart` belongs to the new day.

### 7.3 Blast radius — all of this must move to `DayBoundary`

Every place that currently derives a date from an instant:

- `ObserveTodayDashboardUseCase` — the day window it queries.
- `DayRollupCalculator` callers — which meals belong to the day.
- `MealRepository`'s day-range queries (the SQL window becomes
  `loggedAtMillis >= :startMillis AND loggedAtMillis < :endMillis`, computed
  by `DayBoundary`, instead of any date-string comparison).
- `HistoryViewModel`, `HomeViewModel`.
- Streak counting (§12).

**`MealTypeInference`'s frozen path does not change — but it needs a new
day-start-aware overload.**

The original wording here was: *"A 01:30 snack is 小食 on yesterday's list.
Leave `MealTypeInference` and its tests exactly as they are."* **That was
wrong**, and the Sonnet implementation pass caught it. The frozen rule maps
everything before 10:30 to `BREAKFAST`, including 01:30 — so with a 04:00 day
start a late-night bowl of noodles would be inferred as 早餐 and then merged
into the previous day's 早餐 section next to that day's real 08:00 breakfast.
The student would see two breakfasts and no late-night snack.

The fix, applied in the Opus review pass:

- `forMinuteOfDay(minute)` and `forTime(time)` are **frozen**. The shipped
  Room v1 → v2 migration back-fills rows with exactly these boundaries and
  `MealTypeMigrationSqlTest` proves SQL and Kotlin agree for all 1440
  minutes. Touching them would make the migration disagree with the code.
- A new `forTime(time, dayStart)` returns `SNACK` when `time < dayStart` and
  delegates to the frozen rule otherwise. A midnight `dayStart` reproduces
  the frozen rule exactly, so nothing changes for a user who never moves the
  setting — asserted over all 1440 minutes.
- `MealEditorViewModel` uses the overload at all three inference points
  (initial state, the post-load re-infer, and `setTime`), and still respects
  `mealTypeManuallySet`.

### 7.4 Setting UI and storage

- Stored in DataStore next to the profile: key `day_start_minutes`, an
  `Int` count of minutes from midnight, default `240`.
- Edited on the 我 tab: a row 「一日幾點開始」 opening a time picker limited to
  00:00–08:00 in 30-minute steps.
- Helper text, EN: `"Meals logged before this time count towards the previous day."`
  zh-rHK: `"喺呢個時間之前記錄嘅餐，會算落前一日。"`
- **Changing it does not rewrite stored rows.** Meals keep their instant;
  only the bucketing changes. Say so in the helper text's second line:
  EN `"Past records are not moved; only how days are grouped changes."`
  zh-rHK `"舊記錄唔會搬，只係分日嘅方式會變。"`

---

## 8. B1 — 食物庫 (food library)

Read `docs/07-hk-food-library.md` first. Its §2 is binding: we ship sourced,
confidence-tagged, owner-editable rows, **not** a table of asserted facts.
**Read §6 as well** — the owner asked whether 份量 / 少飯 / 半糖 / 走糖 explain
the published spreads, the reconstruction says they largely do, and that
changed the data model: modifiers are first-class rows with signed deltas,
not a range with a midpoint.

### 8.1 Domain model

`core-domain/model/FoodItem.kt`:

```kotlin
enum class FoodSource { OFFICIAL, COMPOSITION_TABLE, DIETITIAN, MEDIA, TRAINER, USER }
enum class FoodConfidence { HIGH, MEDIUM, LOW }
enum class FoodCategory {
    TEA_RESTAURANT, DIM_SUM, ROAST_RICE, NOODLE, FAST_FOOD, DRINK, STAPLE, SNACK
}

data class FoodItem(
    val id: String,
    val nameZh: String,
    val nameEn: String,
    val portionLabel: String,
    val portionGrams: Double?,
    val kcal: Double,
    val proteinG: Double?,
    val carbsG: Double?,
    val fatG: Double?,
    val kcalLow: Double?,
    val kcalHigh: Double?,
    val source: FoodSource,
    val confidence: FoodConfidence,
    val category: FoodCategory,
    val isSeed: Boolean,
    val editedByOwner: Boolean,
)
```

`core-domain/model/FoodModifier.kt` (per `docs/07` §6.3):

```kotlin
data class FoodModifier(
    val id: String,
    val nameZh: String,
    val nameEn: String,
    val groupId: String,          // options within a group are exclusive
    val appliesTo: Set<FoodCategory>,
    val kcalDelta: Double,        // signed
    val proteinDelta: Double?,
    val carbsDelta: Double?,
    val fatDelta: Double?,
    val source: FoodSource,
    val confidence: FoodConfidence,
    val editedByOwner: Boolean,
)
```

`core-domain/calc/ModifiedFoodCalculator.kt` — pure, tested. Applies a chosen
set of modifiers to a base `FoodItem`:

```kotlin
/**
 * docs/07 §6.3. A FoodItem's stored numbers are the standard as-served
 * configuration; deltas are relative to that, so 走汁 is -100 from a row
 * that already includes sauce.
 */
fun apply(base: FoodItem, modifiers: List<FoodModifier>, quantity: Int = 1): MacroTotals
```

Requirements:

- `require(quantity >= 1)`.
- **At most one modifier per `groupId`** — two options from the same group is
  a programming error, not a silent last-wins: `require` it.
- A modifier whose `appliesTo` does not contain `base.category` is rejected
  by `require`, so a 走糖 cannot be attached to 叉燒飯.
- Macro deltas are **nullable and independent**: a null `proteinDelta` on an
  applied modifier makes the result's protein null, not unchanged. Pretending
  protein is unaffected when the source never published it is the same
  false-precision this whole doc exists to avoid.
- **Clamp the result at zero**, never negative. 走汁 on an already light row
  must not produce −20 kcal.
- Quantity multiplies *after* modifiers: `(base + deltas) × quantity`.

`core-domain/calc/FoodConfidenceRules.kt` — pure, tested:

```kotlin
/** docs/07 §3. Confidence is derived from source and spread, then stored. */
fun confidenceFor(source: FoodSource, kcal: Double, kcalLow: Double?, kcalHigh: Double?): FoodConfidence
```

Rules, in order:
1. `TRAINER` or `OFFICIAL` → `HIGH`.
2. `MEDIA` → `LOW`.
3. A range exists and `(kcalHigh - kcalLow) / kcal > 0.25` → `LOW`.
4. `COMPOSITION_TABLE` or `DIETITIAN` → `MEDIUM`.
5. `USER` → `MEDIUM`.

### 8.2 Room (v2 → v3)

New tables `food_items` and `food_modifiers`, columns mirroring the models.
`food_modifiers.appliesTo` is stored as a comma-separated category list —
a join table for nine rows of static data is not worth its migration.
`id` is the primary key
and for seed rows it is a stable slug (`"cha_siu_rice_lean"`), never an
autoincrement — a stable id is what lets a later app version update a seed
row the owner has *not* edited while leaving their edits alone.

Migration `MIGRATION_2_3`, same discipline as P1.6 §6.3:

- Exposed as `MIGRATION_2_3_STATEMENTS: List<String>` so the plain-JVM
  sqlite-jdbc test runs the exact shipped SQL.
- `CREATE TABLE` + `CREATE INDEX food_items_category_idx` + the seed
  `INSERT`s.
- `fallbackToDestructiveMigration` remains forbidden (P1.6 R3).
- Entity `@ColumnInfo(defaultValue = …)` must match the SQL `DEFAULT`
  exactly (P1.6 R4) — this is the trap that bit us on `mealType`.

Seeding: generate the `INSERT` statements from two checked-in CSVs under
`core-data/src/main/resources/`, by a small Gradle task, so the owner can
edit a spreadsheet rather than Kotlin.

`food_seed.csv` columns exactly:
`id,nameZh,nameEn,portionLabel,portionGrams,kcal,proteinG,carbsG,fatG,kcalLow,kcalHigh,source,category`

`food_modifier_seed.csv` columns exactly:
`id,nameZh,nameEn,groupId,appliesTo,kcalDelta,proteinDelta,carbsDelta,fatDelta,source`

Blank cell = SQL `NULL`. Confidence is **not** a CSV column in either file —
it is computed by `confidenceFor` at seed-generation time so the rule has one
home.

Populate `food_seed.csv` from `docs/07` §4A/4B/4C and
`food_modifier_seed.csv` from §6.3. Do not invent a number that is not in
that doc, and do not fill a blank protein cell with a guess.

Two consequences of `docs/07` §6 for the seed data, both required:

- Every row that §6.1 reconstructed gets its `portionLabel` rewritten to
  state the baseline configuration (`1 碟（正常飯、半肥瘦、有汁）`), and its
  `kcalLow`/`kcalHigh` cleared, because the spread is now modelled.
- Every `DIM_SUM` row is restated **per 件** with `portionLabel` `1 件`,
  per §6.2 rule 3. 蒸鳳爪 ships as ~40 kcal per 件, not 63 or 240.

### 8.3 Re-seeding on later updates

`MIGRATION_3_4` and beyond will want to correct seed rows. The rule, written
into a KDoc on the DAO now so it is not forgotten:

```
UPDATE food_items SET ... WHERE id = :id AND editedByOwner = 0
```

An owner edit is permanent. Never `INSERT OR REPLACE` over the table.

### 8.4 UI — 搵食物 sheet

Replaces the P1.6 常用食物 quick-add list inside `MealEditorScreen`, and the
`FrequentFoodRanker` output becomes its first section.

Layout, top to bottom:
1. Search field, `label = 搵食物` (the §4 translation of 認罪).
2. 常用 — the existing `FrequentFoodRanker` results, unchanged logic.
3. Category chips: 茶餐廳 / 點心 / 燒味飯 / 粉麵 / 快餐 / 飲品 / 主食 / 小食.
4. Results list. Each row: name, portion label, kcal, and a badge per
   `docs/07` §3 — none for `HIGH`, 「約」 for `COMPOSITION_TABLE`, 「估算」
   for `DIETITIAN`/`MEDIA`, 「教練」 for `TRAINER`, 「自訂」 for `USER`.
5. Tapping a row opens the **modifier step** (§8.6) when the item has any
   applicable modifier group, otherwise it fills the editor directly.
6. A `LOW`-confidence row that still carries an unexplained range shows it as
   `850–970 kcal` on a second line and pre-fills the **midpoint**, rounded to
   10 kcal, with the editor field focused. Per `docs/07` §6.3 rule 2 this is
   now the exception, not the norm — a range explained by modifiers must be
   replaced by base + deltas instead.
7. Last row, always: 「搵唔到？自己加」 → the manual entry fields.

Search matches `nameZh` by substring and `nameEn` case-insensitively. No
fuzzy matching, no network.

### 8.5 Owner edit path

On the 我 tab, a row 「食物庫」 opens a list of all seed rows. Editing one
writes `source = TRAINER`, `confidence = HIGH`, `editedByOwner = true`, and
clears `kcalLow`/`kcalHigh`. A short note at the top of that screen, exact
text:

- EN: `"These are starting estimates from public sources. Your numbers replace them permanently."`
- zh-rHK: `"呢啲係公開資料嘅初步估算。你改過嘅數字會永久取代佢哋。"`

This screen is visible to every user in Phase 1.7 — there is no trainer role
yet (that is Phase 5). Do not build a role check now; build the screen so a
role check is a single guard later.

### 8.6 The modifier step — 點食法

Shown after tapping a food row that has applicable modifier groups. This is
the screen that makes the whole food library honest, so it gets real design
attention rather than a dialog.

Layout:

1. Dish name and the **baseline** it is stored at, from `portionLabel` —
   e.g. `叉燒飯 · 正常飯、半肥瘦、有汁`. The student must be able to see what
   they are adjusting *from*.
2. One row of chips per modifier group, in the §6.3 order (飯量, 肉款, 皮,
   醬汁, 甜度, 奶, 牛油, 花生醬, 加配料). Single-select within a group; the
   `正常`/`0` option is pre-selected and visibly the default.
3. A quantity stepper for `DIM_SUM` items, labelled in 件.
4. **A live running total that shows its own arithmetic**, not just a number:

   ```
   939  基礎（正常飯、半肥瘦、有汁）
   −120 瘦
   −130 少飯
   −100 走汁
   ─────
   589 kcal
   ```

   This is the point of the feature. The owner is a trainer teaching
   students; seeing 走汁 −100 is the lesson, and `docs/07` §6.4 is the
   argument for it. **Do not collapse this into a single figure** to save
   vertical space — scroll instead.
5. 記錄 confirms into the meal editor with the computed values, and the
   chosen modifier ids stored on the meal so the row can be re-opened and
   re-adjusted rather than retyped.

Accessibility: each chip group is a `Row` with
`Modifier.semantics { }` carrying the group name, so TalkBack announces
「飯量：少飯，已選」 rather than a bare 「少飯」. Do **not** use
`clearAndSetSemantics` on the chips — that clears their click action, which
is the accessibility regression already made and reverted once in Phase 1.6.

### 8.7 Meal storage for modifiers

`meals` gains `baseFoodId TEXT` (nullable) and `modifierIds TEXT` (nullable,
a comma-separated list of modifier ids). Both nullable because a manually
typed meal has neither.

- Written in the same `MIGRATION_2_3` as the new tables — one migration, one
  version bump.
- **Never recompute a saved meal's macros from the current base and
  modifiers.** The meal stores its own final numbers, exactly like
  `ExerciseEntry.kcal` (§10.2). If the owner later corrects 叉燒飯 from 939
  to 880, last week's logged meals must not silently change — that would
  rewrite the student's history and the trend engine's inputs. The ids are
  kept only so the row can be re-opened for editing.
- `MealType` inference, grouping, and every existing test stay untouched.

---

## 9. A5 — dark-first theme

- `DefatTheme`'s default becomes the dark scheme. Follow the system setting
  when one is set; when the app is given no signal, dark.
- The pine/jade palette from P1.6 §7 is kept **unchanged**, including the
  green-biased surface-container ramp that P1.6 had to set explicitly
  because M3 does not derive those slots from `surface`. Do not regenerate
  the scheme from a seed colour; do not introduce amber.
- Light scheme stays shipped and correct; a student who wants light gets it.
- `values-night/` already exists; re-check every string and colour reference
  resolves in both, since lint treats `MissingTranslation` as an error.
- Re-run the P1.6 §7.4 contrast argument for the ring and macro tiles on
  dark as the default rather than the alternative. Any slot that drops below
  4.5:1 for text or 3:1 for a meter gets fixed in `Color.kt`, not worked
  around in a composable.

---

## 10. B2 — manual exercise logging

### 10.1 The double-counting rule — read this before coding

`docs/04` already forbids adding a wearable's "total calories burned" on top
of our BMR. Manual exercise has a **different** double-count: the activity
multiplier (1.375 "light", 1.55 "moderate"…) *already contains* the
student's usual exercise. Adding a logged session on top counts the same
workout twice and inflates the day's target.

The rule for Phase 1.7, to be added to `docs/04`:

```
TDEE(day) = max(
    BMR × activityMultiplier,                 // the profile's stated baseline
    BMR × NEAT_TEF_BASELINE + exerciseKcal    // the day as actually logged
)
```

with `NEAT_TEF_BASELINE = 1.1` from `docs/04`. Properties, each a unit test:

- No exercise logged → the first term wins → today behaves exactly as it
  does in P1.6. **This is a regression guard, not a nicety.**
- A small session on a "moderate" profile → the first term still wins → the
  target does not move. Correct: that session is what "moderate" means.
- A big session → the second term wins → the target rises by the part of the
  session the multiplier did not already assume.
- The target can never fall below `BMR × activityMultiplier` on a day with
  exercise logged.

### 10.2 Domain

- `model/ExerciseEntry.kt`: `id, loggedAt: Instant, activityKey: String, met: Double, minutes: Int, weightKgAtTime: Double, kcal: Double, note: String?`.
- `kcal` is computed by the existing `MetCalculator` (`MET × weightKg ×
  hours`, P1 §) and **stored**, so a later weight change does not silently
  rewrite history.
- `calc/DayTdeeCalculator.kt` — new, pure, implements §10.1 exactly.
- `DayRollup` gains `exerciseKcal` and `tdeeKcal`; `DayRollupCalculator`
  takes the exercise list for the logical day.

### 10.3 Data

Room table `exercise_entries` in the same `MIGRATION_2_3` as the food table —
one migration, one version bump. Index on `loggedAtMillis`.

### 10.4 UI

- A 運動 section on Home under the meals, with the day's total and a 加運動
  row.
- The sheet: activity picker from the `docs/04` MET table (walking 3.5,
  jogging 7, cycling moderate 6.8, swimming 6, weight training 3.5–6, HIIT 8,
  hiking 6, badminton 5.5, basketball 6.5, yoga 2.5), a minutes field, and a
  live kcal preview.
- **Show the formula**, exactly: `MET × 體重(kg) × 小時 = kcal`, with the
  student's own numbers substituted underneath. The owner is a trainer
  teaching students; a visible formula is the product, not clutter.
- Weight training's 3.5–6 range needs one number: offer 輕 (3.5) / 中 (5) /
  重 (6) chips rather than silently picking.

---

## 11. B3 — water tracking

- Goal: `35 ml × weightKg`, rounded down to the nearest 100 ml, floored at
  1500 ml. Pure function `calc/WaterGoalCalculator.kt`, tested.
- Stored as one row per logical day: `water_log(dateEpochDay, ml)`. Same
  migration.
- UI on Home: a row of eight 250 ml cup icons plus a 自訂 field for other
  amounts. Tapping a cup fills up to it; tapping the filled cup at the end
  removes one. Show `1,750 / 2,400 ml`.
- 35 ml/kg is a common coaching heuristic, not a clinical requirement. Label
  it 建議 and put it in the §13 data-sources page as such, flagged for the
  owner to overrule.

---

## 12. B4 — streak (跟足計劃日數)

- `calc/StreakCalculator.kt`, pure: given the sorted set of logical dates
  that have at least one meal, and today's logical date, return the count of
  consecutive days ending at today or yesterday.
- Ending at **yesterday** still counts, so the streak does not read 0 all
  morning before the first meal. Test this explicitly.
- A gap of one or more days resets it. No "freezes", no grace days — those
  are engagement mechanics, and the owner's framing is 跟足計劃日數, a factual
  count.
- Displayed as a small chip next to Home's date. No flames, no
  congratulation copy beyond the number and the label.

---

## 13. B5 — 數據來源 page

A scrollable page on the 我 tab, 「數據來源」. Plain Cantonese, no jargon. Four
sections, each stating what the number is, where it comes from, and how
confident to be:

1. **基礎代謝（BMR）** — Katch-McArdle when body fat is known, Henry/Oxford
   otherwise. Says plainly that the Henry switch was the owner's
   professional decision and that the published comparison for Chinese
   populations does not exist yet (`docs/04`).
2. **每日消耗（TDEE）** — the multiplier, and §10.1's rule in one sentence:
   「有記錄運動嘅日子，我哋取兩種算法較高嘅一個，唔會重複計同一次運動。」
3. **食物數字** — `docs/07` in three sentences: the official government
   database was not reachable, the public lists disagree by up to double,
   so everything is tagged 估算 / 約 / 教練 and the owner's own numbers
   override them.
4. **飲水建議** — 35 ml/kg is a coaching rule of thumb, not a medical
   target.

Then the existing medical disclaimer from `docs/06`, linked rather than
duplicated.

This page is also the honest answer to "where did 850 kcal come from?", and
it is cheaper to maintain than a confidence badge on every screen.

---

## 14. Strings

Both `values/strings.xml` and `values-zh-rHK/strings.xml`, every key in both,
or the build fails (`MissingTranslation` is an error — P1.6 §9.4).

New key groups: `nav_*` (5), `week_*` (2), `progress_*` (3), `bmr_method_*`
(3), `error_min_age_18` (1), `day_start_*` (4), `food_*` (14), `exercise_*`
(12), `water_*` (5), `streak_*` (2), `sources_*` (10).

Deleted with the drawer: every `drawer_*` key. Remove from **both** files —
a key left in only one trips `ExtraTranslation`, also an error.

Rules unchanged from P1.6 §9.4: no hardcoded user-facing text in
composables; `%` escaped as `%%` in any string without format args;
quantities via `plurals` not string concatenation.

---

## 15. Tests — exact expected values

### 15.1 `BmrCalculatorTest` (+8)

| Case | Expected |
|---|---|
| Henry M 25 y, 80 kg, 175 cm | `1812.75` |
| Henry M 35 y, 80 kg, 175 cm | `1721.75` |
| Henry M 65 y, 80 kg, 175 cm | `1602.75` |
| Henry F 28 y, 60 kg, 162 cm | `1338.3` |
| Henry F 38 y, 58 kg, 160 cm | `1266.04` |
| Henry F 65 y, 70 kg, 158 cm | `1272.28` |
| Band edge: M 30 y vs M 29 y, 80 kg / 175 cm | `1721.75` vs `1812.75` — asserts the step is real and intentional |
| `henryOxford(age = 17)` | throws `IllegalArgumentException` |

Tolerance `0.01`. Keep every existing Mifflin test passing unchanged.

`bmr()` selection tests: `bodyFatPct != null` → `KATCH_MCARDLE`;
`bodyFatPct == null` → `HENRY_OXFORD` (was `MIFFLIN_ST_JEOR` — update, do not
delete).

### 15.2 `DayBoundaryTest` (new, 8)

Zone pinned to `Asia/Hong_Kong`, day start 04:00:

| Instant (local) | Logical date |
|---|---|
| 2026-10-13 03:59 | 2026-10-12 |
| 2026-10-13 04:00 | 2026-10-13 |
| 2026-10-13 04:01 | 2026-10-13 |
| 2026-10-13 23:59 | 2026-10-13 |
| 2026-10-14 00:30 | 2026-10-13 |

Plus: day start 00:00 behaves as plain local date; `startOfLogicalDay` and
`endOfLogicalDayExclusive` are exactly 24 h apart; the window is half-open
(an instant equal to the end belongs to the next day).

### 15.3 `DayTdeeCalculatorTest` (new, 6)

BMR 1800, multiplier 1.55 (moderate) → baseline 2790.

| Exercise kcal | Expected TDEE | Why |
|---|---|---|
| 0 | `2790.0` | multiplier wins — P1.6 regression guard |
| 200 | `2790.0` | 1980 + 200 = 2180 < 2790 |
| 900 | `2880.0` | 1980 + 900 beats the baseline |
| 1500 | `3480.0` | big day |

Plus: sedentary (1.2) with 600 kcal logged → `max(2160, 1980 + 600) = 2580`;
negative exercise kcal → throws.

### 15.4 `FoodConfidenceRulesTest` (new, 8)

One per rule in §8.1, plus: `MEDIA` with a tight range is still `LOW` (rule 2
beats rule 3); `DIETITIAN` with a 30% spread is `LOW` (rule 3 beats rule 4);
`TRAINER` with a wide range is `HIGH` (rule 1 beats everything).

### 15.4a `ModifiedFoodCalculatorTest` (new, 10)

Base 叉燒飯: 939 kcal, 42 g P, 118 g C, 30 g F, category `ROAST_RICE`.

| Modifiers | Expected kcal |
|---|---|
| none | `939` |
| 瘦 (−120) | `819` |
| 瘦 + 少飯 (−130) + 走汁 (−100) | `589` |
| 走汁, quantity 2 | `1678` |

Plus, each its own test:

- Two options from the same `groupId` → throws.
- A modifier whose `appliesTo` excludes `ROAST_RICE` (e.g. 走糖) → throws.
- `quantity = 0` → throws.
- A modifier with a null `proteinDelta` makes the result's protein **null**,
  not 42 — the false-precision guard.
- Clamping: a 120 kcal base with a −200 modifier yields `0.0`, never
  negative.
- Order independence: applying 瘦, 少飯, 走汁 in any permutation gives `589`.

### 15.5 `StreakCalculatorTest` (new, 7)

Empty set → 0. Today only → 1. Yesterday only → 1. Today + yesterday → 2.
A five-day run ending today → 5. A run ending two days ago → 0. A gap in the
middle → only the tail counts.

### 15.6 `WaterGoalCalculatorTest` (new, 4)

80 kg → `2800`; 62 kg → `2100` (2170 floored to the hundred); 40 kg → `1500`
(floor); 0 kg → throws.

### 15.7 `FoodItemMigrationSqlTest` (new, core-data, 5)

Same plain-JVM sqlite-jdbc approach as `MealTypeMigrationSqlTest`, `TZ`
pinned to `Asia/Hong_Kong`:

1. v2 schema + `MIGRATION_2_3_STATEMENTS` applies with no error.
2. `food_items` and `food_modifiers` have every expected column with the
   expected nullability, and `meals` has gained `baseFoodId` and
   `modifierIds` as nullable.
3. The seed row counts match each CSV's row count.
4. Existing `meals` and `weight_entries` rows survive untouched.
5. `UPDATE … WHERE editedByOwner = 0` leaves an owner-edited row alone.

Watch the `.use { }` lambda bodies — a broken statement block here is what
failed CI during P1.6. Give the lambda a named parameter rather than relying
on `it`.

### 15.8 ViewModel tests

`HomeViewModelTest` (+6): week strip builds seven cells Monday-first; future
days disabled; selecting a day re-queries; exercise and water totals land on
the selected day, not today; streak shown. Use `runTest(testDispatcher)` +
`runCurrent()` — reading `.value` with no collector is what broke P1
(`HomeViewModelTest`), and a `StateFlow` with `WhileSubscribed` will read as
its initial value otherwise.

New: `FoodLibraryViewModelTest` (5), `ExerciseEntryViewModelTest` (4),
`SettingsViewModelTest` (3, day-start persistence and the 00:00–08:00 clamp).

---

## 16. Implementation order

**PR A**

1. `BmrMethod`, `NutritionConstants`, `BmrCalculator` + tests. Run
   `:core-domain:test` — this is the only module that builds in the dev
   container.
2. `DayBoundary` + tests.
3. Thread `DayBoundary` through the use cases and repositories; fix every
   call site; re-run domain tests.
4. Day-start setting: DataStore key, 我-tab row, time picker.
5. Theme flip to dark-first; re-check contrast.
6. Bottom nav + FAB; delete `DefatDrawer` and the drawer wiring.
7. Week strip; Home follows `selectedDate`.
8. `progress` placeholder; retarget `HistoryScreen`.
9. Age-18 validation.
10. Strings for both locales; `versionName` `0.3.0-phase1.7a`, `versionCode 3`.
11. Push, let CI compile `:app` and `:core-data`, fix, repeat.

**PR B**

12. `FoodItem`, `FoodModifier`, `FoodConfidenceRules`,
    `ModifiedFoodCalculator`, `StreakCalculator`, `WaterGoalCalculator`,
    `DayTdeeCalculator`, `ExerciseEntry` + tests. `ModifiedFoodCalculator`
    before any UI — the running total in §8.6 is its output.
13. `food_seed.csv` and `food_modifier_seed.csv` from `docs/07` §4 and §6.3;
    the Gradle seed-generation task. Restate dim sum per 件 and rewrite the
    reconstructed rows' `portionLabel` while doing it.
14. Room v3: four tables plus the two `meals` columns,
    `MIGRATION_2_3_STATEMENTS`, the SQL test.
15. Repositories + DAOs + mappers.
16. 搵食物 sheet; the 點食法 modifier step (§8.6) with the visible arithmetic;
    the owner's 食物庫 editor, covering modifier deltas too.
17. Exercise sheet with the visible formula; Home's 運動 section.
18. Water row; streak chip.
19. 數據來源 page.
20. Strings; `versionName` `0.3.0-phase1.7b`, `versionCode 4`.
21. Push, CI, fix, repeat.

---

## 17. Risk register

### R1 (CRITICAL) — the day-start change can silently move the owner's existing data

Flipping bucketing from "local date of the instant" to "logical day" moves
any meal already logged between 00:00 and 04:00 onto the previous day. The
owner has real data on their phone. Totals they remember will change.

Mitigation: the default is 04:00 from the start, the change is announced in
the 數據來源 page and the setting's helper text, and **no stored row is
rewritten** (§7.4). Verify on the owner's own install before PR B goes out:
if they have a late-night meal in the log, tell them which day it moved to
rather than letting them find it.

### R2 (HIGH) — the Henry coefficients are not primary-verified

`docs/04` note 2. Two independent secondary sources agree and the MJ
round-trip is clean, but the paper itself was unreachable. Mitigation: the
coefficients are isolated in `NutritionConstants` with a pointer to that
note, the test vectors in §15.1 are computed from the published equations so
a coefficient typo fails immediately, and Mifflin stays implemented so
reverting is one line.

### R3 (HIGH) — manual exercise double-counting

§10.1. The `max()` rule exists precisely because the naive
`TDEE = BMR × multiplier + exercise` is wrong and would hand students a
too-high target — the opposite of what a fat-loss app must get wrong safely.
The zero-exercise regression test in §15.3 is mandatory.

### R4 (MEDIUM) — food numbers presented as fact

`docs/07` §1b. If the 估算 badge or the range is dropped for visual tidiness,
the app starts asserting that 西多士 is 522 kcal. The badge is not decoration;
it is the thing that keeps the owner's professional name safe. Any PR that
removes it is rejected.

### R5 (MEDIUM) — Room v2 → v3 and the schema drift guard

`fallbackToDestructiveMigration` stays forbidden. The authentic `2.json` /
`3.json` schema files are still a TODO from P1.6 (the fabricated `1.json`
was deleted rather than re-guessed); CI uploads real schemas as an artifact.
Before PR B merges, pull that artifact and commit `2.json` and `3.json` for
real, then turn the drift guard on. This is the last phase where skipping it
is tolerable — three tables land in one migration.

### R5a (MEDIUM, accepted) — a profile already stored under 18 now gets an adult target instead of crashing

A6 rejects a minor at the wizard, so no *new* profile can be under 18. But
`henryOxford` throws under 18, and Home and History open arbitrary past days,
so a 19-year-old browsing back three years would have asked for a
16-year-old's BMR and crashed the app. `TargetReferenceDate` (added in the
implementation pass, tested) fixes that by evaluating any day before the
student's 18th birthday as of that birthday.

The side effect: a profile created *before* PR A that is still under 18 today
now shows an adult target where it previously crashed. That contradicts A6's
own rationale — computing an adult cut target for a 15-year-old is exactly
what A6 exists to prevent.

**Accepted, not fixed, for PR A.** Such a profile can only exist on an
install created before this phase, and the only such install is the owner's,
who is an adult. The alternative — routing a stored minor back through
onboarding — adds a lockout path that could strand a real user's logged data
for a case that cannot currently occur. If DeFAT ever reaches students
directly (Phase 5 closed testing), this must be closed first: a profile whose
age *today* is under 18 should be sent back to onboarding with
`error_min_age_18`, not given a target.

### R6 (LOW) — bottom nav reverses a P1.6 decision

P1.6 §8.1 chose a drawer and shipped it. This is a deliberate reversal on the
owner's instruction (§3), not an oversight. P1.6's plan is **not** rewritten;
`docs/03` records the reversal so the history stays legible.

---

## 18. Definition of Done

- [ ] `:core-domain:test` green locally; CI green on all three modules.
- [ ] Lint clean, `MissingTranslation` and `ExtraTranslation` still errors.
- [ ] Every new user-facing string in both `values/` and `values-zh-rHK/`.
- [ ] No `drawer_*` string key and no `DefatDrawer` reference anywhere.
- [ ] `fallbackToDestructiveMigration` absent from the whole repo.
- [ ] Authentic `2.json` and `3.json` committed; drift guard enabled.
- [ ] `docs/03-roadmap.md` has the Phase 1.7 entry.
- [ ] `docs/04` carries the Henry table, the three honesty notes, and the
      §10.1 TDEE rule.
- [ ] `docs/07` referenced from `CLAUDE.md`'s doc table.
- [ ] Zero-exercise day produces a byte-identical target to P1.6.
- [ ] Installs over the owner's existing build with the committed debug
      keystore — no uninstall, no data loss (`docs/06`).
- [ ] The §1.1 exit test passes on the owner's Samsung device.

---

## 19. Owner actions needed

1. **Review `docs/07` §4C.** Those are the rows most likely wrong. Your
   numbers replace them permanently.
2. **Portion weights.** A 碟頭飯 kcal means nothing without "約 500 g". Your
   portion convention is worth more than any published figure.
3. **Protein columns** in `docs/07` — the app's main coaching lever, and
   mostly blank in every public source.
4. **Confirm 35 ml/kg** for the water goal, or give the number you use.
5. **Confirm the weight-training MET split** (輕 3.5 / 中 5 / 重 6).
6. **Tell us if the 30th-birthday BMR step** (§5, −91 kcal for a man) is
   acceptable. We implement Henry exactly; smoothing it is your call.
7. Still blocked and still waiting: **the Firebase project** (Phase 1.5).

---

## 20. Deferred, listed so nothing is forgotten

- Charts and the trend engine — Phase 4; `progress` is a placeholder.
- Health Connect, so exercise stops being manual — Phase 2.
- AI photo estimation, which is the real answer to the food-database
  problem — Phase 3.
- Re-seeding the food library from the official CFS database once it is
  reachable — Phase 4 task, tracked in `docs/07` §2 rule 6.
- A trainer role gate on the 食物庫 editor — Phase 5.
- A selectable BMR formula in settings, if the Henry question is ever
  settled by evidence — one line, because Mifflin stays implemented.
