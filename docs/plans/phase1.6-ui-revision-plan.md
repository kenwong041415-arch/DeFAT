# Phase 1.6 — Implementation Plan (UI revision + meal types)

Status: approved plan, ready for implementation.
Author: Opus planning pass (per `CLAUDE.md` model roles).
Implementer: Sonnet. Follow this document literally; where it says "exact",
copy the text verbatim. If something here contradicts `docs/`, stop and flag
it rather than silently choosing.

Predecessor: [`docs/plans/phase1-implementation-plan.md`](phase1-implementation-plan.md).
Everything in that plan still holds unless this file overrides it. Section
references written as "P1 §8.2" point at the Phase 1 plan.

Branch: `phase1.6/ui-revision`.

---

## 1. Scope summary

Phase 1 shipped and the owner installed it on a Samsung phone. They confirmed
the metabolism math is correct and gave UI feedback. Phase 1.6 acts on that
feedback. It adds **no new math**, **no backend**, **no new permissions**.

1. **Home rebuilt to the approved "coach" layout (Option B)** — calorie ring,
   then three compact macro tiles, then today's meals grouped by meal type
   with per-type subtotals, then a tappable metabolism footnote. The weight
   card leaves Home.
2. **Meal types** — exactly five (早餐 / 午餐 / 下午茶 / 晚餐 / 小食), stored
   on every meal, inferred from the clock when a meal is created, back-filled
   onto the owner's existing rows by a Room migration.
3. **Navigation drawer** — 今日 / 記錄體重 / 歷史記錄 / 進度圖表 (disabled) /
   個人資料.
4. **History screen** — pick a date, see that day's meals grouped by type and
   that day's totals against target.
5. **Meal editor** — meal-type chips, working date and time pickers, a
   常用食物 quick-add list, and a hard block on saving a 0-kcal meal.
6. **Fixed brand theme** — pine/jade Material 3 light + dark schemes,
   Material You dynamic colour removed.
7. **Room v1 → v2 migration** with a real, CI-runnable SQL test.

**Out of scope:** Firebase (still Phase 1.5), Health Connect, camera, AI,
charts (Phase 4), notifications, exercise logging, water, streaks, an in-app
settings screen, and any change to BMR/TDEE/target/macro formulas.

### 1.1 Exit test

> The owner opens Home, immediately understands where their protein/carbs/fat
> stand, sees today's meals grouped as 早餐/午餐/下午茶/晚餐/小食, logs a meal
> at the right meal type in under 20 seconds, cannot save it with 0 kcal, and
> finds yesterday's meals through 歷史記錄.

Mapped to a concrete acceptance script in §13.

---

## 2. Decisions and deviations — and why

Where the owner left something open, this plan chooses. One line of reasoning
each. Do not re-litigate; if one must change, update this file in the same PR.

| # | Decision | Reason |
|---|---|---|
| **E1** | **No 設定 drawer row.** The drawer has five rows: 今日 / 記錄體重 / 歷史記錄 / 進度圖表 (disabled) / 個人資料. | 個人資料 already holds everything a settings screen would (language hint, disclaimer, version, editable body numbers); a second row pointing at the same screen teaches the owner that the drawer lies. |
| **E2** | **The drawer belongs to Home only**, not to a global scaffold. Other destinations stay ordinary pushed routes with a back arrow. | Zero change to `DefatNavHost` structure or to the `MainActivity` start-destination latch (which was a bug fix in Phase 1); a global drawer would have to be threaded through the onboarding graph, which must not have one. |
| **E3** | **The disabled 進度圖表 row is a plain non-clickable `Row`, not a `NavigationDrawerItem`.** | `NavigationDrawerItem` has no `enabled` parameter; giving it `onClick = {}` still makes it a `Button` to TalkBack, which the owner explicitly ruled out. Static text is announced as text. |
| **E4** | **The drawer has no icons — labels only.** | Removes an entire class of CI failure (`material-icons-extended` is not a dependency and never will be; there is no core icon for "charts" or "weight"), and the coach layout reads cleaner without them. |
| **E5** | **Macro tiles are hand-built (`Box` + `Box` fill), not `LinearProgressIndicator`.** | The M3 linear indicator's track plus its stop-indicator dot is exactly what read as "full at zero" to the owner. A zero-width fill box draws nothing at zero, which is the whole point of the change. |
| **E6** | **Existing v1 meal rows get their meal type inferred from `loggedAtMillis`, not dumped in one bucket.** | The owner's real rows become immediately useful instead of all landing in 小食 and needing manual re-tagging; the same time rule is used everywhere so the result is predictable. |
| **E7** | **常用食物 is derived from the existing `meals` table by name**, not a new `foods` table. | No new sync surface for Phase 1.5, no new entity, no second source of truth. Cost is one cheap DAO query. |
| **E8** | **The frequent-food *ranking* is a pure `core-domain` function over raw rows**, not a SQL `GROUP BY`. | Ranking rules (normalisation, tie-breaks, how many) are the kind of thing the owner will want tuned, and `core-domain` is the only module whose tests run in the dev container. SQL does the one thing it is good at: `ORDER BY loggedAtMillis DESC LIMIT 200`. |
| **E9** | **Frequent-food figures come from the *most recent* matching entry**, not an average. | The owner's latest entry for a food is their best current estimate; averaging would drag a corrected value back toward the mistake. |
| **E10** | **Show 5 frequent foods.** | The row is horizontally scrollable, so 5 fits on a Samsung flagship without scrolling and the tail is still reachable; more than that turns a shortcut into a list to read. |
| **E11** | **Meal-type boundaries are half-open: a meal at exactly 10:30 is 午餐.** A meal at 03:00 is **早餐**, per the owner's rule as written ("before 10:30"). | Implemented literally rather than inventing a late-night branch the owner explicitly rejected. Flagged in §14 as a one-constant change if 3 a.m. should be 小食. |
| **E12** | **Auto-inference stops once the user touches the chips.** Changing the time afterwards does not re-infer. | Silently overriding an explicit choice is the worst kind of surprise; in edit mode the stored type always wins. |
| **E13** | **`Meal.mealType` is appended last with a default of `MealType.SNACK`.** | `:app` and `:core-data` cannot be compiled in the dev container, so a required parameter risks a whole CI round for one missed construction site. The default is unreachable in production code — `MealEditorViewModel` always supplies a value. |
| **E14** | **Jade is `secondary`, clay is `tertiary`, pine is `primary`, brick is `error`.** No new theming machinery, no `CompositionLocal`. | Every colour lands in a standard M3 slot, so `MaterialTheme.colorScheme.*` stays the only way to get a colour and the mapping is documented once in `Color.kt`. |
| **E15** | **`DefatTheme` loses its `dynamicColor` parameter entirely**, not just its default. | A parameter left at `false` is an invitation to flip it back; deleting it makes the decision structural. |
| **E16** | **The Room migration test is a plain-JVM SQL test using `org.xerial:sqlite-jdbc`, not Robolectric and not instrumented.** | CI has no emulator; this executes the *exact same SQL strings* the real migration executes, against real SQLite, in `:core-data:testDebugUnitTest`, with no new Android test infrastructure. See §6.5 for why this is genuinely strong and what it does not cover. |
| **E17** | **A stable debug keystore is committed at `app/debug.keystore`.** | See §3 — without it every CI build is signed with a different throwaway key and the owner must uninstall (wiping their data) to install each new build. This is the single highest-value change in the phase for data safety. |
| **E18** | **History computes a past day's target from the *current* profile.** | Per-day target snapshots are Phase 4 trend-engine work (`docs/04` "Trend engine"); adding a `days/{date}` cache now contradicts P1 D2. Documented as a known limitation, not hidden. |
| **E19** | **`ObserveTodayDashboardUseCase` is reused for History and keeps its name.** | It already takes a `date` parameter; renaming touches Hilt-provided call sites for zero user value. A KDoc line notes it is no longer today-only. |

---

## 3. Risk register — read this before writing any code

### R1 (CRITICAL) — the owner will probably lose their existing data on this one install, for a reason unrelated to the migration

The Phase 1 APK was debug-signed on a GitHub runner. GitHub-hosted runners do
not carry a persistent `~/.android/debug.keystore`; AGP generates one on demand
with a **randomly generated key pair**. `gradle/actions/setup-gradle` caches the
Gradle home, not `~/.android`. So **every CI run signs the APK with a different
key**, and Android refuses to install an APK over an app signed by a different
key (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`, shown to the owner as
"App not installed"). The only way through is uninstall → which deletes
`defat.db` and `profile.preferences_pb`. `android:allowBackup="false"`
(P1 §8.5, correct per `docs/06`) means there is no adb-backup rescue either.

Consequences, stated plainly:

- The Room v1→v2 migration may well never run on the owner's phone, because
  their v1 database may not survive to meet it.
- **The migration is still mandatory.** It protects every install from 1.6
  onward, and it must exist before the app has data worth keeping.
- **This is the cheapest moment in the project's life to change the schema.**
  Do the schema work now.

Mitigation, in this PR:

1. Generate a stable debug keystore and commit it (E17):
   ```
   keytool -genkeypair -v -keystore app/debug.keystore \
     -storepass android -alias androiddebugkey -keypass android \
     -keyalg RSA -keysize 2048 -validity 10000 \
     -dname "CN=Android Debug,O=Android,C=US"
   ```
   Wire it in `app/build.gradle.kts`:
   ```kotlin
   signingConfigs {
       getByName("debug") {
           storeFile = file("debug.keystore")
           storePassword = "android"
           keyAlias = "androiddebugkey"
           keyPassword = "android"
       }
   }
   ```
   (`buildTypes.debug` already uses the `debug` signing config by default.)
2. `.gitignore`: keep `*.keystore` ignored, add the single exception
   `!app/debug.keystore` with the comment below it.
3. `docs/06-privacy-compliance.md`: under **Security**, amend the "no
   keystores committed" bullet to read that the *debug* keystore is
   deliberately committed (standard `android`/`android` credentials, no
   secret value, cannot sign a Play release) so test builds update in place,
   and that the **release** keystore must never be committed and will live in
   GitHub Actions secrets from Phase 5.
4. `README.md`: a short "Installing test builds" note saying that from 1.6
   onward the APK installs over the previous one without uninstalling.

Owner-facing instructions are in §14.

### R2 (HIGH) — the committed v1 schema JSON is fabricated

`core-data/schemas/com.defat.core.data.db.DefatDatabase/1.json` was
hand-authored in commit `4584d39`; its commit message says so, and its
`identityHash` (`3f1b1c6e9c9d4a5e8b6b6c9a1e7f2d3a`) is invented. **CI has not
regenerated it in the repository** — KSP writes the real file into
`core-data/schemas/` inside the runner's workspace on every build, but nothing
ever uploads or commits it, so `main` still carries the fabricated file.

This never reached the phone (the device's `room_master_table` holds the hash
Room computed at runtime), and Room does not read `1.json` at build time
because we are writing a manual migration rather than an `@AutoMigration`. But
a fabricated `createSql` bundle is a bad starting point for a migration test
and a landmine for the first Phase 1.5 auto-migration.

**Exact recipe to get an authentic v1 schema** (tasks M-1/M-2 in §12):

1. On the branch, *before* touching the entity or the DB version, add to
   `.github/workflows/android-ci.yml`, immediately after the "Build debug APK"
   step:
   ```yaml
      - name: Upload exported Room schemas
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: room-schemas
          path: core-data/schemas/**
          if-no-files-found: error
          retention-days: 14
   ```
2. Push. CI builds the still-v1 code; KSP regenerates
   `.../DefatDatabase/1.json` in the workspace; the artifact captures it.
3. Download `room-schemas`, and commit the contained `1.json` **verbatim**
   (byte-for-byte, no reformatting) over the fabricated one. Commit message:
   `M-2: replace the hand-authored v1 Room schema with the CI-generated one`.
4. Only then do the v2 work. After the v2 build goes green, download the
   artifact again and commit `2.json` the same way.
5. Once both files are authentic, add this permanent guard as the CI step
   right after the schema upload:
   ```yaml
      - name: Fail if the exported Room schema drifts from the committed one
        run: git diff --exit-code -- core-data/schemas
   ```
   From then on, any entity change that is not accompanied by a committed
   schema fails CI instead of failing on the owner's phone.

### R3 (HIGH) — `fallbackToDestructiveMigration` must not appear

It is forbidden (P1 §7.3, and the comment is already in `DatabaseModule.kt`).
If the migration is wrong, the correct outcome is a **crash on launch** that we
fix, not a silent wipe. Do not add it "temporarily". Do not add
`.fallbackToDestructiveMigrationOnDowngrade()` either.

### R4 (MEDIUM) — the entity default and the SQL default must match exactly

`ALTER TABLE … ADD COLUMN … DEFAULT 'SNACK'` puts a column default into the
SQLite schema. Room compares the *entity's* declared default against the
database's. If `MealEntity.mealType` is not annotated
`@ColumnInfo(defaultValue = "SNACK")`, Room throws
`IllegalStateException: Migration didn't properly handle: meals(...)` with an
`Expected / Found` diff on `defaultValue` — on the owner's phone, at launch,
after a successful-looking build. This is the single most common Room
migration bug. See §6.2.

### R5 (LOW) — new column and the Phase 1.5 sync surface

`mealType` must be part of the Firestore `users/{uid}/meals/{mealId}` document
when sync lands. Add the field to the data model bullet in
`docs/02-architecture.md` in this PR (§12, task D-1) so Phase 1.5 does not
have to rediscover it.

---

## 4. Complete file tree

`[new]` created, `[edit]` modified, `[del]` deleted. Unlisted files are
untouched.

```
DeFAT/
├── .github/workflows/android-ci.yml                              [edit: schema artifact + drift guard]
├── .gitignore                                                    [edit: !app/debug.keystore]
├── README.md                                                     [edit: status 1.6, install-over note]
├── gradle/libs.versions.toml                                     [edit: sqlite-jdbc]
├── docs/
│   ├── 02-architecture.md                                        [edit: meal.mealType in the data model]
│   ├── 03-roadmap.md                                             [edit: Phase 1.6 section + link]
│   ├── 06-privacy-compliance.md                                  [edit: debug-keystore exception]
│   └── plans/phase1.6-ui-revision-plan.md                        (this file)                          [new]
│
├── core-domain/
│   └── src/
│       ├── main/kotlin/com/defat/core/domain/
│       │   ├── model/
│       │   │   ├── MealType.kt                                   [new]
│       │   │   ├── MealTypeGroup.kt                              [new]
│       │   │   ├── FrequentFood.kt                               [new]
│       │   │   └── Meal.kt                                       [edit: + mealType, last param]
│       │   ├── calc/
│       │   │   ├── MealTypeInference.kt                          [new]
│       │   │   ├── MealGrouping.kt                               [new]
│       │   │   └── FrequentFoodRanker.kt                         [new]
│       │   ├── repository/MealRepository.kt                      [edit: + recentMeals]
│       │   └── usecase/ObserveTodayDashboardUseCase.kt           [edit: KDoc only]
│       └── test/kotlin/com/defat/core/domain/calc/
│           ├── MealTypeInferenceTest.kt                          [new]
│           ├── MealGroupingTest.kt                               [new]
│           └── FrequentFoodRankerTest.kt                         [new]
│
├── core-data/
│   ├── build.gradle.kts                                          [edit: sqlite-jdbc test dep, TZ]
│   ├── schemas/com.defat.core.data.db.DefatDatabase/
│   │   ├── 1.json                                                [edit: replaced with CI-generated]
│   │   └── 2.json                                                [new, CI-generated]
│   └── src/
│       ├── main/kotlin/com/defat/core/data/
│       │   ├── db/
│       │   │   ├── DefatDatabase.kt                              [edit: version = 2]
│       │   │   ├── MealMigrations.kt                             [new]
│       │   │   ├── MealDao.kt                                    [edit: + observeRecent]
│       │   │   └── entity/MealEntity.kt                          [edit: + mealType]
│       │   ├── mapper/MealMappers.kt                             [edit: mealType both ways]
│       │   ├── repository/MealRepositoryImpl.kt                  [edit: + recentMeals]
│       │   └── di/DatabaseModule.kt                              [edit: .addMigrations]
│       └── test/kotlin/com/defat/core/data/
│           ├── mapper/MealMappersTest.kt                         [edit: + 2 tests]
│           └── db/MealTypeMigrationSqlTest.kt                    [new]
│
└── app/
    ├── build.gradle.kts                                          [edit: version, signingConfig]
    ├── debug.keystore                                            [new, committed on purpose]
    └── src/
        ├── main/
        │   ├── kotlin/com/defat/app/
        │   │   ├── navigation/Routes.kt                          [edit: + HISTORY]
        │   │   ├── navigation/DefatNavHost.kt                    [edit: + history, drawer callbacks]
        │   │   └── ui/
        │   │       ├── theme/Color.kt                            [edit: full rewrite]
        │   │       ├── theme/Theme.kt                            [edit: full rewrite]
        │   │       ├── component/
        │   │       │   ├── MacroBar.kt                           [del]
        │   │       │   ├── MacroTile.kt                          [new]
        │   │       │   ├── MealTypeSections.kt                   [new]
        │   │       │   ├── DefatDrawer.kt                        [new]
        │   │       │   └── CalorieRing.kt                        [edit: arc colour → secondary]
        │   │       ├── home/{HomeUiState,HomeViewModel,HomeScreen}.kt   [edit: all three]
        │   │       ├── history/
        │   │       │   ├── HistoryUiState.kt                     [new]
        │   │       │   ├── HistoryViewModel.kt                   [new]
        │   │       │   └── HistoryScreen.kt                      [new]
        │   │       └── meal/{MealEditorUiState,MealEditorViewModel,MealEditorScreen}.kt [edit: all three]
        │   └── res/
        │       ├── values/strings.xml                            [edit]
        │       ├── values-zh-rHK/strings.xml                     [edit]
        │       ├── values/colors.xml                             [edit]
        │       ├── values/themes.xml                             [edit]
        │       └── values-night/themes.xml                       [new]
        └── test/kotlin/com/defat/app/
            ├── fake/FakeMealRepository.kt                        [edit: + recentMeals]
            └── ui/
                ├── HomeViewModelTest.kt                          [edit]
                ├── MealEditorViewModelTest.kt                    [edit: + 4 tests]
                └── HistoryViewModelTest.kt                       [new]
```

`.gitignore` — replace the secrets block with:

```
# Secrets — never commit (docs/06)
google-services.json
*.jks
*.keystore
# Exception: the debug keystore is committed on purpose so every CI build is
# signed with the same key and test APKs install over the previous one instead
# of forcing an uninstall (which would wipe the owner's logged meals).
# Standard android/android credentials; it can never sign a Play release.
!app/debug.keystore
```

---

## 5. `core-domain` specification

Still pure Kotlin: no `android.*` / `androidx.*` imports (the
`checkNoAndroidImports` Gradle task enforces this). All new logic lives here
and is verifiable in the dev container with
`gradle --configure-on-demand :core-domain:test`.

### 5.1 `model/MealType.kt`

```kotlin
package com.defat.core.domain.model

/**
 * The five meal types the owner uses with students. Declaration order is the
 * display order everywhere (Home, History, the editor's chip row) — do not
 * sort this list anywhere else.
 *
 * The owner explicitly decided on exactly these five and rejected a
 * late-night (宵夜) type. Adding one is a schema-visible change: it needs a
 * new string pair and no migration (the column is free text), but the
 * inference boundaries in [com.defat.core.domain.calc.MealTypeInference]
 * would have to change too.
 */
enum class MealType { BREAKFAST, LUNCH, AFTERNOON_TEA, DINNER, SNACK }
```

### 5.2 `model/Meal.kt` (edit)

Append **last**, with a default (E13):

```kotlin
data class Meal(
    val id: String,
    val loggedAt: Instant,
    val date: LocalDate,
    val name: String,
    val kcal: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val source: MealSource = MealSource.MANUAL,
    /**
     * Which of the five daily meals this belongs to. Defaults to SNACK only so
     * that test and preview construction sites keep compiling; production code
     * always sets it explicitly (MealEditorViewModel) or reads it back from the
     * database (mappers).
     */
    val mealType: MealType = MealType.SNACK,
)
```

### 5.3 `calc/MealTypeInference.kt`

```kotlin
package com.defat.core.domain.calc

import com.defat.core.domain.model.MealType
import java.time.LocalTime

/**
 * Maps a clock time to the meal type it most likely belongs to. Boundaries are
 * half-open — a meal at exactly 10:30 is LUNCH, not BREAKFAST.
 *
 * Owner-set boundaries (docs/plans/phase1.6-ui-revision-plan.md §2 E11).
 * Times before 10:30 are BREAKFAST, including the small hours: a 03:00 meal is
 * logged as 早餐. If the owner wants a different cut-off, change
 * [BREAKFAST_FROM_MINUTE] and nothing else.
 */
object MealTypeInference {

    const val BREAKFAST_FROM_MINUTE: Int = 0        // 00:00
    const val LUNCH_FROM_MINUTE: Int = 10 * 60 + 30 // 10:30
    const val AFTERNOON_TEA_FROM_MINUTE: Int = 14 * 60 + 30 // 14:30
    const val DINNER_FROM_MINUTE: Int = 17 * 60 + 30 // 17:30
    const val SNACK_FROM_MINUTE: Int = 21 * 60       // 21:00

    fun forTime(time: LocalTime): MealType =
        forMinuteOfDay(time.hour * 60 + time.minute)

    /**
     * The single source of truth for the rule. The Room v1 -> v2 migration
     * back-fills existing rows with the same boundaries, and
     * MealTypeMigrationSqlTest asserts SQL and Kotlin agree for all 1440
     * minutes of the day.
     */
    fun forMinuteOfDay(minuteOfDay: Int): MealType {
        require(minuteOfDay in 0..1439) {
            "minuteOfDay must be in 0..1439 but was $minuteOfDay"
        }
        return when {
            minuteOfDay < LUNCH_FROM_MINUTE -> MealType.BREAKFAST
            minuteOfDay < AFTERNOON_TEA_FROM_MINUTE -> MealType.LUNCH
            minuteOfDay < DINNER_FROM_MINUTE -> MealType.AFTERNOON_TEA
            minuteOfDay < SNACK_FROM_MINUTE -> MealType.DINNER
            else -> MealType.SNACK
        }
    }
}
```

### 5.4 `model/MealTypeGroup.kt` + `calc/MealGrouping.kt`

```kotlin
package com.defat.core.domain.model

data class MealTypeGroup(
    val type: MealType,
    /** Ascending by [Meal.loggedAt]. */
    val meals: List<Meal>,
    val kcal: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
) {
    val isEmpty: Boolean get() = meals.isEmpty()
    val mealCount: Int get() = meals.size
}
```

```kotlin
package com.defat.core.domain.calc

object MealGrouping {
    /**
     * Always returns exactly five groups, one per [MealType], in enum
     * declaration order, including empty ones — the UI shows an empty meal type
     * as a prompt to log, so the empty groups are load-bearing, not padding.
     */
    fun groupByType(meals: List<Meal>): List<MealTypeGroup>
}
```

Implementation notes: single pass into a `Map<MealType, MutableList<Meal>>`,
then `MealType.entries.map { … }`; within a group `sortedBy { it.loggedAt }`;
totals are plain summation, no rounding (rounding stays a presentation
concern, P1 §6).

### 5.5 `model/FrequentFood.kt` + `calc/FrequentFoodRanker.kt`

```kotlin
package com.defat.core.domain.model

data class FrequentFood(
    /** Display name — the spelling used in the most recent matching entry. */
    val name: String,
    val timesLogged: Int,
    val kcal: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
)
```

`mealType` is deliberately **not** carried: tapping a frequent food fills the
numbers only and leaves the user's current chip choice alone (§8.3).

```kotlin
package com.defat.core.domain.calc

object FrequentFoodRanker {

    const val DEFAULT_LIMIT: Int = 5

    /**
     * Trim, collapse internal whitespace runs to a single space, lowercase with
     * root-locale semantics. Chinese food names are case-less so this is mostly
     * a whitespace fix; English ones ("Chicken Rice" / "chicken rice") merge.
     */
    fun normaliseName(raw: String): String =
        raw.trim().replace(WHITESPACE_RUN, " ").lowercase()

    /**
     * Groups [meals] by [normaliseName], drops blank names, and returns at most
     * [limit] foods ordered by: times logged descending, then most recently
     * logged first, then normalised name ascending (so the order is total and
     * the UI never reshuffles for no reason).
     *
     * Numbers and display spelling come from the most recent entry in the group
     * (plan §2 E9), never an average.
     */
    fun rank(meals: List<Meal>, limit: Int = DEFAULT_LIMIT): List<FrequentFood>
}
```

`private val WHITESPACE_RUN = Regex("\\s+")` at file scope.
`require(limit > 0)`.
Kotlin's no-arg `String.lowercase()` is already locale-independent — do **not**
use the deprecated `toLowerCase()`.

### 5.6 `repository/MealRepository.kt` (edit)

```kotlin
interface MealRepository {
    fun mealsOn(date: LocalDate): Flow<List<Meal>>
    /**
     * The [limit] most recently logged meals, newest first. Feeds the
     * frequent-food list; the ranking itself is [FrequentFoodRanker.rank].
     */
    fun recentMeals(limit: Int): Flow<List<Meal>>
    suspend fun mealById(id: String): Meal?
    suspend fun upsert(meal: Meal)
    suspend fun delete(id: String)
}
```

### 5.7 `usecase/ObserveTodayDashboardUseCase.kt` (edit — KDoc only)

Add one line: it is used for any date, not just today (History passes a past
date); the target it computes always reflects the *current* profile (E18).

---

## 6. `core-data` specification and the migration

### 6.1 `DefatDatabase.kt`

```kotlin
@Database(
    entities = [MealEntity::class, WeightEntryEntity::class],
    version = 2,
    exportSchema = true,
)
```

### 6.2 `entity/MealEntity.kt` (edit)

```kotlin
@Entity(tableName = "meals", indices = [Index("date")])
data class MealEntity(
    @PrimaryKey val id: String,
    val loggedAtMillis: Long,
    val date: String,
    val name: String,
    val kcal: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val source: String,
    val updatedAtMillis: Long,
    val remoteId: String? = null,
    val pendingSync: Boolean = true,
    /**
     * MealType.name. The column default MUST stay in sync with the DEFAULT in
     * MealMigrations.MIGRATION_1_2_STATEMENTS — Room compares the declared
     * default against the database's and throws at launch if they differ
     * (plan §3 R4).
     */
    @ColumnInfo(defaultValue = "SNACK")
    val mealType: String = "SNACK",
)
```

Import `androidx.room.ColumnInfo`.
Note the literal `"SNACK"`: an annotation argument must be a compile-time
constant, so `MealType.SNACK.name` is not allowed there. Add the comment above
so nobody "improves" it.

The new column goes **last** so the generated `CREATE TABLE` column order in
`2.json` is a clean append.

### 6.3 `db/MealMigrations.kt` (new)

```kotlin
package com.defat.core.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 -> v2: adds `meals.mealType` and back-fills existing rows from the time
 * of day they were logged, using the same boundaries as
 * com.defat.core.domain.calc.MealTypeInference (plan §2 E6).
 *
 * fallbackToDestructiveMigration is forbidden here and everywhere — the
 * owner's real logged meals live in this database (plan §3 R3).
 */
object MealMigrations {

    /**
     * Single source of truth. The Migration below runs these, and
     * MealTypeMigrationSqlTest runs these same strings against real SQLite via
     * the JDBC driver, so the test cannot drift from the shipped SQL.
     */
    val MIGRATION_1_2_STATEMENTS: List<String> = listOf(
        "ALTER TABLE `meals` ADD COLUMN `mealType` TEXT NOT NULL DEFAULT 'SNACK'",
        """
        UPDATE `meals` SET `mealType` = CASE
            WHEN CAST(strftime('%H%M', `loggedAtMillis` / 1000, 'unixepoch', 'localtime') AS INTEGER) < 1030 THEN 'BREAKFAST'
            WHEN CAST(strftime('%H%M', `loggedAtMillis` / 1000, 'unixepoch', 'localtime') AS INTEGER) < 1430 THEN 'LUNCH'
            WHEN CAST(strftime('%H%M', `loggedAtMillis` / 1000, 'unixepoch', 'localtime') AS INTEGER) < 1730 THEN 'AFTERNOON_TEA'
            WHEN CAST(strftime('%H%M', `loggedAtMillis` / 1000, 'unixepoch', 'localtime') AS INTEGER) < 2100 THEN 'DINNER'
            ELSE 'SNACK'
        END
        """.trimIndent(),
    )

    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            MIGRATION_1_2_STATEMENTS.forEach(db::execSQL)
        }
    }
}
```

Why this SQL shape:

- `'localtime'` uses the device's zone, which for the owner is
  `Asia/Hong_Kong` — the same zone that produced `loggedAtMillis`, so the
  back-filled type matches what the owner remembers eating.
- `CAST(strftime('%H%M', …) AS INTEGER)` gives `930` for 09:30 and `1430` for
  14:30, so the comparisons are plain integer comparisons and the boundaries
  read the same as `MealTypeInference`.
- `loggedAtMillis / 1000` is SQLite integer division on an INTEGER column.
- `ADD COLUMN` with `NOT NULL` requires a `DEFAULT` — supplied.

**CI trap:** Room 2.7 introduced a KMP `Migration.migrate(connection: SQLiteConnection)`.
The Android artifact still exposes `migrate(db: SupportSQLiteDatabase)` and the
override above is correct. If CI reports that it does not override anything,
change the single method to
`override fun migrate(connection: androidx.sqlite.SQLiteConnection)` and call
`connection.execSQL(it)` — do not change anything else.

### 6.4 `di/DatabaseModule.kt` (edit)

```kotlin
Room.databaseBuilder(context, DefatDatabase::class.java, DefatDatabase.DATABASE_NAME)
    .addMigrations(MealMigrations.MIGRATION_1_2)
    // fallbackToDestructiveMigration is forbidden — the owner's real data
    // lives here (plan §3 R3). A wrong migration must crash and be fixed,
    // never silently wipe.
    .build()
```

### 6.5 The migration test — what is feasible, and what it does and does not prove

**Not feasible in CI:** an instrumented `androidx.room:room-testing`
`MigrationTestHelper` test. CI has no emulator, and adding
`reactivecircus/android-emulator-runner` would add ~10 minutes and a new
flakiness surface to a UI phase.

**Rejected:** Robolectric. It would work, but it means a new dependency, test
assets wiring for the schema directory, and an API surface
(`MigrationTestHelper` constructors moved between Room 2.6 and 2.7) that
cannot be checked in the dev container — exactly the class of guess that cost
Phase 1 five CI rounds.

**Chosen (E16):** a plain-JVM test in `:core-data/src/test` that drives real
SQLite through `org.xerial:sqlite-jdbc`, creating the v1 `meals` table and
running `MealMigrations.MIGRATION_1_2_STATEMENTS` verbatim.

What this **does** prove:
- the SQL parses and executes on real SQLite;
- the column is added with the right type, nullability and default;
- every one of the 1440 minutes of a day maps to the same `MealType` that
  `MealTypeInference.forMinuteOfDay` returns — i.e. the SQL `CASE` and the
  Kotlin `when` can never drift;
- pre-existing column values survive the migration untouched.

What it does **not** prove:
- that Room's runtime schema validation accepts the result (the
  `@ColumnInfo(defaultValue = …)` question in R4). That is covered instead by
  the committed-schema drift guard in CI (§3 R2 step 5) plus the owner's
  install test (§13 item 12), and it is the one thing to look at first if the
  app crashes on launch after update.

**Timezone handling.** `strftime(…, 'localtime')` in the SQLite C library reads
the process `TZ` environment variable, not the JVM default. Pin it for
`:core-data` tests so the JVM and SQLite agree:

```kotlin
// core-data/build.gradle.kts, after the android { } block
tasks.withType<Test>().configureEach {
    // MealTypeMigrationSqlTest exercises SQLite's strftime(..., 'localtime'),
    // which reads the OS TZ, while the expected values come from java.time.
    // Pin both to the owner's zone so the test is deterministic on any runner.
    environment("TZ", "Asia/Hong_Kong")
    systemProperty("user.timezone", "Asia/Hong_Kong")
}
```

### 6.6 `db/MealDao.kt` (edit)

```kotlin
    /** Newest first; the frequent-food ranking is done in :core-domain. */
    @Query("SELECT * FROM meals ORDER BY loggedAtMillis DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<MealEntity>>
```

### 6.7 `mapper/MealMappers.kt` (edit)

```kotlin
// Meal -> MealEntity
    mealType = mealType.name,

// MealEntity -> Meal
    // Unknown enum name falls back to SNACK rather than throwing, matching the
    // existing MealSource behaviour.
    mealType = runCatching { MealType.valueOf(mealType) }.getOrDefault(MealType.SNACK),
```

Watch the shadowing: inside `MealEntity.toDomain()` the receiver property
`mealType` is a `String`; write
`runCatching { MealType.valueOf(this.mealType) }` if the compiler complains.

### 6.8 `repository/MealRepositoryImpl.kt` (edit)

```kotlin
    override fun recentMeals(limit: Int): Flow<List<Meal>> =
        mealDao.observeRecent(limit)
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(io)
```

### 6.9 `core-data/build.gradle.kts` (edit)

```kotlin
    testImplementation(libs.sqlite.jdbc)
```

`gradle/libs.versions.toml`:

```toml
sqliteJdbc = "3.50.1.0"
...
sqlite-jdbc = { module = "org.xerial:sqlite-jdbc", version.ref = "sqliteJdbc" }
```

`org.xerial:sqlite-jdbc` is on Maven Central, which the container's proxy does
**not** block, and it is a `testImplementation` only — it never ships in the
APK. If CI cannot resolve `3.50.1.0`, apply the P1 §4.3 fallback protocol
(nearest published `3.5x.y.z`) and record it in §16.

---

## 7. Theme specification

Material You dynamic colour is removed (E15). `Theme.kt` becomes:

```kotlin
@Composable
fun DefatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = DefatTypography,
        content = content,
    )
}
```

Delete the imports `android.os.Build`, `dynamicDarkColorScheme`,
`dynamicLightColorScheme`, `androidx.compose.ui.platform.LocalContext`. All
existing call sites (`MainActivity`, every `@Preview`) use the trailing lambda
only, so the signature change is safe.

### 7.1 Slot mapping (E14) — document this table verbatim at the top of `Color.kt`

| M3 slot | Meaning in DeFAT | Used by |
|---|---|---|
| `primary` | pine — brand, emphasis | FAB, filled buttons, top bar, **protein tile** |
| `secondary` | jade — progress / done | calorie ring arc, carbs & fat meter fills |
| `tertiary` | clay — caution, not an error | macro-mismatch hint, over-budget nudges |
| `error` | brick — genuinely wrong | validation errors, ring when over target |
| `outlineVariant` | hairline | tile borders, dividers, meter tracks |
| `onSurfaceVariant` | muted text | labels, subtotals, footnotes |

### 7.2 Light scheme

| Role | Hex | Contrast | Against |
|---|---|---|---|
| `primary` | `#14574A` | — | — |
| `onPrimary` | `#FFFFFF` | **8.43 : 1** | on `primary` |
| `primaryContainer` | `#CFE7E0` | — | — |
| `onPrimaryContainer` | `#0A3229` | **12.8 : 1** | on `primaryContainer` |
| `secondary` | `#2E9E7B` | — | — |
| `onSecondary` | `#08251C` | **4.87 : 1** | on `secondary` |
| `secondaryContainer` | `#CDEDE0` | — | — |
| `onSecondaryContainer` | `#0B3B2C` | **11.2 : 1** | on `secondaryContainer` |
| `tertiary` | `#B4762A` | — | — |
| `onTertiary` | `#FFFFFF` | 3.77 : 1 (fills/large text only) | on `tertiary` |
| `tertiaryContainer` | `#F7E4C6` | — | — |
| `onTertiaryContainer` | `#5A3A0E` | **8.6 : 1** | on `tertiaryContainer` |
| `error` | `#A8402F` | — | — |
| `onError` | `#FFFFFF` | **6.11 : 1** | on `error` |
| `errorContainer` | `#F7DAD4` | — | — |
| `onErrorContainer` | `#5A1E14` | **10.4 : 1** | on `errorContainer` |
| `background` | `#EFF3F1` | — | — |
| `onBackground` | `#16211E` | **15.6 : 1** | on `background` |
| `surface` | `#FFFFFF` | — | — |
| `onSurface` | `#16211E` | **16.5 : 1** | on `surface` |
| `surfaceVariant` | `#DFE7E3` | — | — |
| `onSurfaceVariant` | `#61756F` | **4.90 : 1** | on `surface` |
| `outline` | `#8FA29C` | — | — |
| `outlineVariant` | `#C9D5D0` | — | — |

Ratios are WCAG 2.x relative-luminance ratios, computed for this plan; Sonnet
does not need to recompute them, only to use these exact hexes.

Two honest caveats, both deliberate:

1. **`onSurfaceVariant` (`#61756F`) is 4.90 : 1 on white but 4.38 : 1 on the
   `#EFF3F1` ground.** Rule for implementers: muted text always sits on a
   `surface` (white card / drawer sheet / dialog), never directly on
   `background`. Home's content is inside cards and tiles, so this holds; if a
   layout ever needs muted text straight on the ground, use `onBackground`.
2. **`onTertiary` white-on-clay is 3.77 : 1**, which passes the 3 : 1 bar for
   large text and non-text graphics but not the 4.5 : 1 body-text bar. Clay is
   therefore never used behind small text: caution *text* uses
   `onTertiaryContainer` on `tertiaryContainer`.

### 7.3 Dark scheme

| Role | Hex | Contrast | Against |
|---|---|---|---|
| `primary` | `#8FC9BC` | **8.89 : 1** | on `surface` |
| `onPrimary` | `#06322A` | **7.51 : 1** | on `primary` |
| `primaryContainer` | `#1E4A41` | — | — |
| `onPrimaryContainer` | `#CFE7E0` | **8.9 : 1** | on `primaryContainer` |
| `secondary` | `#4FC49E` | **7.68 : 1** | on `surface` |
| `onSecondary` | `#00382B` | **6.07 : 1** | on `secondary` |
| `secondaryContainer` | `#17453A` | — | — |
| `onSecondaryContainer` | `#CDEDE0` | **9.4 : 1** | on `secondaryContainer` |
| `tertiary` | `#F0B860` | **9.27 : 1** | on `surface` |
| `onTertiary` | `#3B2400` | **8.7 : 1** | on `tertiary` |
| `tertiaryContainer` | `#553A12` | — | — |
| `onTertiaryContainer` | `#F7E4C6` | **8.2 : 1** | on `tertiaryContainer` |
| `error` | `#F5A092` | **8.16 : 1** | on `surface` |
| `onError` | `#5F160A` | **8.3 : 1** | on `error` |
| `errorContainer` | `#77291B` | — | — |
| `onErrorContainer` | `#F7DAD4` | **7.5 : 1** | on `errorContainer` |
| `background` | `#0C1614` | — | — |
| `onBackground` | `#E3EBE8` | **15.2 : 1** | on `background` |
| `surface` | `#14211E` | — | — |
| `onSurface` | `#E3EBE8` | **13.68 : 1** | on `surface` |
| `surfaceVariant` | `#1E2E2A` | — | — |
| `onSurfaceVariant` | `#8CA39C` | **6.19 : 1** | on `surface` |
| `outline` | `#5C716B` | — | — |
| `outlineVariant` | `#2A3B36` | — | — |

Dark `primary` is a *muted* pine tint and dark `secondary` a *bright* jade
tint, so the protein tile stays visibly distinct from the progress fills in
dark mode — using tints of the same hue would have collapsed them together.

### 7.4 Meter contrast — the accessibility argument

The 3 dp meter fill (jade `#2E9E7B`) against its track (`#C9D5D0`) is 2.21 : 1,
below the 3 : 1 WCAG 1.4.11 bar for adjacent graphical objects. This is
accepted deliberately: **the meter is redundant**, never the sole carrier of
information. Every tile always shows the literal "used / target" figure next
to it, and the tile exposes
`stringResource(R.string.home_protein_progress, used, target)` as its merged
content description, so a screen reader reads "Protein 70 of 132 grams". The
fill against the white surface is 3.34 : 1, which does clear the bar; the
track is a low-contrast affordance, which is standard.

### 7.5 XML resources

`res/values/colors.xml`:

```xml
<resources>
    <color name="ic_launcher_background">#14574A</color>
    <color name="window_background">#EFF3F1</color>
    <color name="window_background_dark">#0C1614</color>
</resources>
```

`res/values/themes.xml` — `<item name="android:windowBackground">@color/window_background</item>`.

`res/values-night/themes.xml` (new):

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.DeFAT" parent="android:Theme.Material.NoActionBar">
        <item name="android:windowBackground">@color/window_background_dark</item>
    </style>
</resources>
```

This kills the white flash before Compose draws in dark mode.

### 7.6 `CalorieRing.kt` (edit)

One line: the progress arc becomes
`if (isOverTarget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary`,
and the track becomes `MaterialTheme.colorScheme.outlineVariant` (the current
`surfaceVariant` track is nearly invisible against a white card in the new
palette). Nothing else changes.

---

## 8. App layer — screens and UX

All screens stay stateless composables driven by a `uiState` + lambdas, with a
thin `XxxRoute` collecting the ViewModel (P1 §8.2). Every new screen and
component gets `@Preview` in a light and a dark variant.

### 8.1 Navigation

`Routes.kt` — add one constant:

```kotlin
    const val HISTORY = "history"
```

`DefatNavHost.kt`:

- `HomeRoute` gains `onOpenHistory: () -> Unit`; it keeps `onLogWeight` and
  `onOpenProfile` (now driven from the drawer instead of the top bar and a
  card).
- New destination:
  ```kotlin
  composable(Routes.HISTORY) {
      HistoryRoute(
          onEditMeal = { mealId -> navController.navigate(Routes.mealEdit(mealId)) },
          onBack = { navController.popBackStack() },
      )
  }
  ```
- Nothing else moves. The onboarding graph, the start-destination latch in
  `MainActivity` and the meal-edit argument are untouched.

### 8.2 S7 — `HomeScreen` (rebuilt)

Structure:

```
ModalNavigationDrawer(drawerState, drawerContent = { DefatDrawerContent(...) }) {
    Scaffold(topBar = …, floatingActionButton = …) { padding -> HomeContent(…) }
}
```

`drawerState = rememberDrawerState(DrawerValue.Closed)` and a
`rememberCoroutineScope()` live in `HomeScreen` (UI-only state, no ViewModel).
Drawer item taps `scope.launch { drawerState.close() }` and then invoke the
navigation lambda.

**Top bar.** `navigationIcon` = `IconButton` with `Icons.Filled.Menu`,
`contentDescription = stringResource(R.string.nav_open_menu)`.
`title` = `stringResource(R.string.home_title_today)` ("Today" / "今日").
`actions` = **none** — the settings icon is removed; the drawer is the single
navigation surface. This also removes the `LocalDate.now()` call that was
sitting inside the composable.

**Body**, in order, inside the existing `verticalScroll` Column:

1. **`CalorieRing`** — unchanged call site apart from the theme change.
   Big remaining number, "kcal left"/"kcal over", and
   `home_eaten_of_target` ("已食 1,050 / 2,267 千卡") beneath.
2. **`MacroTileRow`** — three tiles in a `Row`, `Arrangement.spacedBy(8.dp)`,
   each `Modifier.weight(1f)`. Protein first.
3. **Meal-type sections** — `Text(home_meals_title)` as a section heading,
   then, when the whole day is empty, one muted `home_meals_empty` line, then
   `MealTypeSections(groups, onEditMeal)`.
4. **Metabolism footnote** — `home_metabolism_footnote`
   ("基礎代謝 1,828 · 總消耗 2,833 千卡 · 撳睇解釋"), `bodySmall`,
   `onSurfaceVariant`, `Modifier.clickable { showMetabolismInfo = true }`
   opening the existing `ModalBottomSheet`. Add
   `Modifier.semantics { role = Role.Button }` so it is announced as tappable.
5. `Spacer(80.dp)` for FAB clearance.
6. **The weight card is deleted.** Along with it: the `onLogWeight` parameter
   on `HomeContent`, the `SectionCard` import if unused, and the three weight
   string keys (§9).

**`HomeUiState`** becomes:

```kotlin
sealed interface HomeUiState {
    data object Loading : HomeUiState
    data object NeedsOnboarding : HomeUiState

    data class Content(
        val dayRollup: DayRollup,
        /** All five types, always, in enum order (MealGrouping.groupByType). */
        val mealGroups: List<MealTypeGroup>,
    ) : HomeUiState
}
```

`meals` and `latestWeight` are gone.

**`HomeViewModel`** drops the `WeightRepository` injection and maps meals into
groups:

```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    observeTodayDashboard: ObserveTodayDashboardUseCase,
    mealRepository: MealRepository,
) : ViewModel() {

    private val today = LocalDate.now()

    val uiState: StateFlow<HomeUiState> = combine(
        observeTodayDashboard(today),
        mealRepository.mealsOn(today),
    ) { rollup, meals ->
        if (rollup == null) HomeUiState.NeedsOnboarding
        else HomeUiState.Content(rollup, MealGrouping.groupByType(meals))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)
}
```

### 8.3 S8 — `MealEditorScreen` (extended)

Field order top to bottom:

1. **Meal type** — label `meal_type_label` ("Which meal?" / "邊一餐？"), then a
   `Row(Modifier.horizontalScroll(rememberScrollState()), Arrangement.spacedBy(8.dp))`
   of five `FilterChip`s, `selected = uiState.mealType == type`,
   `onClick = { onMealTypeChange(type) }`.
   **Do not use `FlowRow`** — it needs `@ExperimentalLayoutApi`, which is not
   opted into. `horizontalScroll` handles "Afternoon tea" on a narrow phone.
2. **常用食物** — shown only when `uiState.frequentFoods.isNotEmpty()`:
   `Text(meal_frequent_title)` then a horizontally scrolling `Row` of
   `AssistChip`s, one per food, labelled with the food's name and
   `stringResource(R.string.kcal_value, food.kcal.roundKcal())` as supporting
   text inside the same label, e.g. `雞胸飯 · 520 千卡`. Build that label with
   a single formatted resource, not concatenation:
   `stringResource(R.string.meal_frequent_chip, food.name, food.kcal.roundKcal())`.
   Content description: `stringResource(R.string.meal_frequent_add, food.name)`.
   Tapping fills name + kcal + protein + carbs + fat and **leaves the meal-type
   chips alone** (E9/§5.5).
3. Name (existing).
4. Calories — `DecimalField(..., errorText = if (uiState.showKcalError) stringResource(R.string.meal_kcal_required) else null)`.
5. Protein / Carbs / Fat (existing).
6. "Calculate calories from macros" (existing) and the mismatch hint
   (existing, now `colorScheme.tertiary` rather than `error` — a mismatch is a
   caution, not a blocker).
7. **Date** — `Text(common_date)` label + `OutlinedButton` showing
   `uiState.date.toString()`; tapping opens `DatePickerDialog`. Copy the
   BasicsScreen pattern verbatim, including the `ZoneOffset.UTC` round-trip:
   ```kotlin
   val initialMillis = uiState.date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
   val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
   … onDateSelected(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
   ```
   This is what lets the owner back-fill yesterday's meals.
8. **Time** — `Text(meal_time)` label + `OutlinedButton` showing
   `uiState.time.format(DateTimeFormatter.ofPattern("HH:mm"))`.
   **CI trap:** there is no `TimePickerDialog` composable in Material3 1.3.x
   (the version compose-bom 2025.06.01 resolves). Build it by hand:
   ```kotlin
   if (showTimePicker) {
       val state = rememberTimePickerState(
           initialHour = uiState.time.hour,
           initialMinute = uiState.time.minute,
           is24Hour = true,
       )
       AlertDialog(
           onDismissRequest = { showTimePicker = false },
           confirmButton = {
               TextButton(onClick = {
                   onTimeSelected(LocalTime.of(state.hour, state.minute))
                   showTimePicker = false
               }) { Text(stringResource(R.string.common_save)) }
           },
           dismissButton = {
               TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.common_cancel)) }
           },
           text = { TimePicker(state = state) },
       )
   }
   ```
   `TimePicker` and `rememberTimePickerState` are `@ExperimentalMaterial3Api`,
   already covered by the module-wide opt-in in `app/build.gradle.kts`.
   24-hour display is chosen deliberately: unambiguous and locale-independent.
9. Save (existing) / Delete (existing).

**`MealEditorUiState`** additions:

```kotlin
    val mealType: MealType = MealType.SNACK,
    val mealTypeManuallySet: Boolean = false,
    val kcalTouched: Boolean = false,
    val frequentFoods: List<FrequentFood> = emptyList(),
```

and the validation change:

```kotlin
    /** Macros stay optional; calories do not. A 0-kcal meal made Home read
     *  "已食 0 / 2267" and look broken, so it is not saveable. */
    val isKcalValid: Boolean get() = (kcal ?: 0.0) > 0.0

    val showKcalError: Boolean get() = kcalTouched && !isKcalValid

    val isSaveEnabled: Boolean
        get() = isNameValid && isKcalValid && protein >= 0.0 && carbs >= 0.0 && fat >= 0.0
```

`showKcalError` is gated on `kcalTouched` so a freshly opened "add meal" screen
is not red before the owner has typed anything; the Save button is disabled
either way.

**`MealEditorViewModel`** additions:

- Initial state built explicitly so time and type agree:
  ```kotlin
  private val now: LocalTime = LocalTime.now()
  private val _uiState = MutableStateFlow(
      MealEditorUiState(
          mealId = mealId,
          isEditMode = mealId != null,
          time = now,
          date = LocalDate.now(),
          mealType = MealTypeInference.forTime(now),
      ),
  )
  ```
- `init` also collects frequent foods:
  ```kotlin
  viewModelScope.launch {
      mealRepository.recentMeals(FREQUENT_FOOD_SCAN_LIMIT).collect { recent ->
          _uiState.update { it.copy(frequentFoods = FrequentFoodRanker.rank(recent)) }
      }
  }
  ```
  with `private const val FREQUENT_FOOD_SCAN_LIMIT = 200` at file scope.
- Edit mode load sets `mealType = meal.mealType, mealTypeManuallySet = true`.
- `fun setMealType(type: MealType)` → `copy(mealType = type, mealTypeManuallySet = true)`.
- `setTime` re-infers only when the user has not chosen:
  ```kotlin
  fun setTime(time: LocalTime) = update { state ->
      state.copy(
          time = time,
          mealType = if (state.mealTypeManuallySet) state.mealType
                     else MealTypeInference.forTime(time),
      )
  }
  ```
- `setKcal` also sets `kcalTouched = true`.
- `fun applyFrequentFood(food: FrequentFood)` fills
  `nameText = food.name`, `kcalText`/`proteinText`/`carbsText`/`fatText` from
  its numbers (format with `roundKcal()`/`roundGrams()` then `.toString()` so
  the field does not show `520.0`), and sets `kcalTouched = true`.
- `save()` passes `mealType = state.mealType` into the `Meal`.

### 8.4 S11 — `HistoryScreen` (new)

`ui/history/HistoryUiState.kt`:

```kotlin
sealed interface HistoryUiState {
    data object Loading : HistoryUiState
    data object NeedsOnboarding : HistoryUiState
    data class Content(
        val date: LocalDate,
        val dayRollup: DayRollup,
        val mealGroups: List<MealTypeGroup>,
        val canGoForward: Boolean,   // false when date == today
    ) : HistoryUiState
}
```

`HistoryViewModel`:

```kotlin
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val observeDashboard: ObserveTodayDashboardUseCase,
    private val mealRepository: MealRepository,
) : ViewModel() {

    private val today = LocalDate.now()
    private val selectedDate = MutableStateFlow(today)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HistoryUiState> = selectedDate.flatMapLatest { date ->
        combine(observeDashboard(date), mealRepository.mealsOn(date)) { rollup, meals ->
            if (rollup == null) HistoryUiState.NeedsOnboarding
            else HistoryUiState.Content(
                date = date,
                dayRollup = rollup,
                mealGroups = MealGrouping.groupByType(meals),
                canGoForward = date.isBefore(today),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState.Loading)

    fun selectDate(date: LocalDate) { selectedDate.value = minOf(date, today) }
    fun previousDay() { selectedDate.value = selectedDate.value.minusDays(1) }
    fun nextDay() { if (selectedDate.value.isBefore(today)) selectedDate.value = selectedDate.value.plusDays(1) }
}
```

`@OptIn(ExperimentalCoroutinesApi::class)` on `flatMapLatest` is required —
this is exactly the opt-in that already appears in
`ObserveTodayDashboardUseCase` and it will fail the build if omitted.

`HistoryScreen` body:

1. Top bar: back arrow, title `history_title`.
2. A date row: `IconButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, cd = history_prev_day)`,
   a centre `TextButton` showing `date.toString()` (cd `history_pick_date`)
   that opens the same `DatePickerDialog` pattern,
   `IconButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, cd = history_next_day, enabled = canGoForward)`.
   `DatePickerDialog` gets a `SelectableDates` that rejects future dates — or,
   simpler and with no experimental surface, clamp in `selectDate` (already
   done above) and leave the picker permissive.
3. A `SectionCard(title = stringResource(R.string.history_totals_title))`
   containing the `home_eaten_of_target` line and the same `MacroTileRow`.
4. `MealTypeSections(groups, onEditMeal)` — identical component to Home, so
   the two screens cannot drift.

No FAB: adding a meal to a past day is done from Home's FAB plus the editor's
date picker, which keeps one add-flow rather than two.

### 8.5 New shared components

**`ui/component/MacroTile.kt`**

```kotlin
data class MacroTileData(
    val label: String,
    val usedG: Int,
    val targetG: Int,
    val emphasised: Boolean,       // protein
    val contentDescription: String,
)

@Composable fun MacroTileRow(protein: MacroTileData, carbs: MacroTileData, fat: MacroTileData, modifier: Modifier = Modifier)

@Composable fun MacroTile(data: MacroTileData, modifier: Modifier = Modifier)
```

Tile rendering:

```kotlin
val accent = if (data.emphasised) MaterialTheme.colorScheme.primary
             else MaterialTheme.colorScheme.secondary
val border = if (data.emphasised) MaterialTheme.colorScheme.primary
             else MaterialTheme.colorScheme.outlineVariant
val fraction = if (data.targetG <= 0) 0f
               else (data.usedG.toFloat() / data.targetG).coerceIn(0f, 1f)

Column(
    modifier = modifier
        .clip(RoundedCornerShape(12.dp))
        .border(1.dp, border, RoundedCornerShape(12.dp))
        .background(if (data.emphasised) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else Color.Transparent)
        .padding(horizontal = 10.dp, vertical = 8.dp)
        .clearAndSetSemantics { contentDescription = data.contentDescription },
) {
    Text(data.label, style = MaterialTheme.typography.labelMedium,
         color = if (data.emphasised) accent else MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(2.dp))
    Text(usedOfTargetText, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(6.dp))
    Box(
        Modifier.fillMaxWidth().height(3.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(MaterialTheme.colorScheme.outlineVariant),
    ) {
        if (fraction > 0f) {
            Box(Modifier.fillMaxWidth(fraction).fillMaxHeight()
                .clip(RoundedCornerShape(2.dp)).background(accent))
        }
    }
}
```

The `if (fraction > 0f)` guard is the fix for the owner's complaint: at zero,
nothing is drawn at all. `usedOfTargetText` is
`stringResource(R.string.macro_used_of_target, data.usedG, data.targetG)`,
computed inside the composable — `MacroTileData` carries the raw ints so the
component owns its formatting.

**`ui/component/MealTypeSections.kt`**

```kotlin
@Composable
fun MealTypeSections(
    groups: List<MealTypeGroup>,
    onEditMeal: (String) -> Unit,
    modifier: Modifier = Modifier,
)

@Composable fun mealTypeLabel(type: MealType): String   // when(type) -> stringResource(...)
```

Per group:

- Header `Row`: `Text(mealTypeLabel(group.type), titleSmall)`,
  `Spacer(Modifier.weight(1f))`, then either
  `Text(stringResource(R.string.kcal_value, group.kcal.roundKcal()), labelMedium, onSurfaceVariant)`
  when non-empty, or `Text(stringResource(R.string.value_none), …)` when empty.
- Then either the meal rows or
  `Text(stringResource(R.string.home_group_empty), bodySmall, fontStyle = FontStyle.Italic, color = onSurfaceVariant)`.
- Meal row: `Row(Modifier.fillMaxWidth().clickable { onEditMeal(meal.id) }.padding(vertical = 8.dp))`
  with time (`labelMedium`, `Modifier.width(48.dp)`), name (`bodyMedium`,
  `Modifier.weight(1f)`, `maxLines = 1`, `overflow = TextOverflow.Ellipsis`),
  kcal (`bodyMedium`).
  Time string: `meal.loggedAt.atZone(ZoneId.systemDefault()).toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))`,
  wrapped in `remember(meal.loggedAt) { … }`.
- `HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)` between
  groups. Use `HorizontalDivider`, **not** the deprecated `Divider`.

**`ui/component/DefatDrawer.kt`**

```kotlin
@Composable
fun DefatDrawerContent(
    dailyTargetKcal: Int?,
    onToday: () -> Unit,
    onLogWeight: () -> Unit,
    onHistory: () -> Unit,
    onProfile: () -> Unit,
)
```

`ModalDrawerSheet { … }` containing:

- Header `Column(Modifier.padding(24.dp))`:
  `Text(stringResource(R.string.app_name), headlineSmall)` and, when
  `dailyTargetKcal != null`,
  `Text(stringResource(R.string.drawer_target_line, dailyTargetKcal), bodyMedium, onSurfaceVariant)`.
- `HorizontalDivider()`.
- Four `NavigationDrawerItem`s, no icons (E4):
  今日 (`home_title_today`, `selected = true`), 記錄體重 (`weight_title`),
  歷史記錄 (`history_title`), 個人資料 (`profile_title`), each with
  `modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)`.
- Between 歷史記錄 and 個人資料, the **disabled** charts row (E3), a plain
  non-clickable `Row`:
  ```kotlin
  Row(
      modifier = Modifier
          .fillMaxWidth()
          .padding(NavigationDrawerItemDefaults.ItemPadding)
          .padding(horizontal = 12.dp, vertical = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
  ) {
      Text(
          text = stringResource(R.string.drawer_charts),
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
      )
      Spacer(Modifier.weight(1f))
      Text(
          text = stringResource(R.string.drawer_coming_soon),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant)
              .padding(horizontal = 8.dp, vertical = 2.dp),
      )
  }
  ```
  It has no `clickable`, no `onClick`, no `Role.Button` — TalkBack announces
  two pieces of static text. **Do not** "tidy" this into a
  `NavigationDrawerItem`.

---

## 9. Strings — full bilingual table

Every key below must exist in **both** `app/src/main/res/values/strings.xml`
and `app/src/main/res/values-zh-rHK/strings.xml`. Lint treats
`MissingTranslation` **and** `ExtraTranslation` as errors, so additions and
deletions must be mirrored in the same commit.

繁中 is Hong Kong written Chinese in the supportive coaching tone of `docs/05`
— never shaming, second person, no simplified characters.

### 9.1 New keys

| Key | English | 繁中 (zh-rHK) |
|---|---|---|
| `home_title_today` | Today | 今日 |
| `nav_open_menu` | Open menu | 開啟選單 |
| `drawer_target_line` | Daily target %1$d kcal | 每日目標 %1$d 千卡 |
| `drawer_charts` | Progress charts | 進度圖表 |
| `drawer_coming_soon` | Coming soon | 稍後推出 |
| `history_title` | History | 歷史記錄 |
| `history_pick_date` | Pick a date | 揀日期 |
| `history_prev_day` | Previous day | 前一日 |
| `history_next_day` | Next day | 下一日 |
| `history_totals_title` | That day's total | 嗰日總計 |
| `meal_type_breakfast` | Breakfast | 早餐 |
| `meal_type_lunch` | Lunch | 午餐 |
| `meal_type_afternoon_tea` | Afternoon tea | 下午茶 |
| `meal_type_dinner` | Dinner | 晚餐 |
| `meal_type_snack` | Snack | 小食 |
| `meal_type_label` | Which meal? | 邊一餐？ |
| `macro_protein` | Protein | 蛋白質 |
| `macro_carbs` | Carbs | 碳水 |
| `macro_fat` | Fat | 脂肪 |
| `macro_used_of_target` | %1$d / %2$d g | %1$d / %2$d 克 |
| `kcal_value` | %1$d kcal | %1$d 千卡 |
| `value_none` | — | — |
| `home_group_empty` | Nothing logged yet | 仲未記錄 |
| `home_metabolism_footnote` | BMR %1$d · TDEE %2$d kcal · tap to see how | 基礎代謝 %1$d · 總消耗 %2$d 千卡 · 撳睇解釋 |
| `meal_frequent_title` | Foods you log often | 常用食物 |
| `meal_frequent_chip` | %1$s · %2$d kcal | %1$s · %2$d 千卡 |
| `meal_frequent_add` | Fill in %1$s | 填入 %1$s |
| `meal_kcal_required` | Enter the calories — it has to be more than 0. | 請輸入卡路里，一定要大過 0。 |

### 9.2 Keys deleted from both files

Removed with the Home weight card:

- `home_log_weight`
- `home_weight_none`
- `home_weight_as_of`

The drawer's 記錄體重 row reuses the existing `weight_title`
("Log weight" / "記錄體重"), so nothing is lost.

### 9.3 Keys reused unchanged (do not duplicate)

`app_name`, `common_save`, `common_cancel`, `common_back`, `common_delete`,
`common_date`, `unit_kcal`, `unit_g`, `weight_title`, `profile_title`,
`home_meals_title`, `home_meals_empty`, `home_add_meal`, `home_kcal_left`,
`home_kcal_over`, `home_eaten_of_target`, `home_metabolism_line` (still used by
`ProfileScreen` and `SummaryScreen` — **do not delete it**),
`home_metabolism_info_title`, `home_metabolism_info_body`,
`home_protein_progress` / `home_carbs_progress` / `home_fat_progress` (now the
macro tiles' content descriptions), `meal_time`, `meal_name`, `meal_kcal`,
`meal_protein`, `meal_carbs`, `meal_fat`, `meal_calc_from_macros`,
`meal_macro_mismatch`, `meal_delete_confirm`, `meal_title_add`,
`meal_title_edit`.

### 9.4 String rules that fail the build if broken

- English apostrophes must be escaped: `day\'s`, `don\'t`.
- Any literal `%` in a string that also has format args must be doubled `%%`.
  None of the new strings need this — check again if you add one.
- Never build a user-visible sentence by concatenation; add a format string.

---

## 10. Unit tests — exact expected values

Assert `Double`s with `assertEquals(expected, actual, 1e-6)`. Test names use
backticks and describe behaviour. **The existing 49 `core-domain` tests must
still pass**; run `gradle --configure-on-demand :core-domain:test` after every
domain commit.

### 10.1 `MealTypeInferenceTest` (core-domain, 5 tests)

| Test | Input → Expected |
|---|---|
| `midnight and the small hours are breakfast` | 00:00 → `BREAKFAST`; 03:00 → `BREAKFAST`; 07:30 → `BREAKFAST` |
| `boundaries are half open` | 10:29 → `BREAKFAST`; **10:30 → `LUNCH`**; 14:29 → `LUNCH`; **14:30 → `AFTERNOON_TEA`**; 17:29 → `AFTERNOON_TEA`; **17:30 → `DINNER`**; 20:59 → `DINNER`; **21:00 → `SNACK`**; 23:59 → `SNACK` |
| `midday and evening land in the obvious buckets` | 12:00 → `LUNCH`; 16:00 → `AFTERNOON_TEA`; 19:00 → `DINNER`; 22:30 → `SNACK` |
| `forMinuteOfDay agrees with forTime for every minute` | loop `0..1439`: `forMinuteOfDay(m) == forTime(LocalTime.of(m / 60, m % 60))` |
| `rejects a minute outside the day` | `-1` and `1440` → `IllegalArgumentException` |

### 10.2 `MealGroupingTest` (core-domain, 5 tests)

Fixture meals (all on `2026-08-11`, kcal / protein / carbs / fat):

- `b1` 08:00 `BREAKFAST` 300 / 20 / 40 / 8
- `l1` 13:00 `LUNCH` 600 / 30 / 80 / 15
- `l2` 12:30 `LUNCH` 450 / 40 / 30 / 12

| Test | Expected |
|---|---|
| `an empty day still returns all five groups in order` | `size == 5`; types `[BREAKFAST, LUNCH, AFTERNOON_TEA, DINNER, SNACK]`; every group `isEmpty`, `kcal == 0.0` |
| `sums each type` | `LUNCH`: kcal **1050.0**, protein **70.0**, carbs **110.0**, fat **27.0**, `mealCount == 2`; `BREAKFAST`: kcal **300.0**, `mealCount == 1`; `AFTERNOON_TEA`/`DINNER`/`SNACK` all `kcal == 0.0` and `isEmpty` |
| `orders meals inside a group oldest first` | `LUNCH.meals.map { it.id } == listOf("l2", "l1")` (input order `l1, l2`) |
| `group totals reconcile with the day rollup` | `groups.sumOf { it.kcal } == DayRollupCalculator.rollup(date, target, meals).intakeKcal` == **1350.0** |
| `types with no meals still appear` | `groups.single { it.type == MealType.DINNER }.isEmpty` is `true` |

### 10.3 `FrequentFoodRankerTest` (core-domain, 7 tests)

| Test | Input → Expected |
|---|---|
| `normalises case, ends and internal runs of whitespace` | `normaliseName("  Chicken   Rice ")` → `"chicken rice"`; `normaliseName("CHICKEN RICE")` → `"chicken rice"` |
| `merges the same food logged with different spelling` | three meals named `" Chicken Rice"`, `"chicken rice"`, `"CHICKEN  RICE"` → `rank(...).size == 1`, `timesLogged == 3` |
| `ranks by times logged descending` | A×3, B×2, C×1 → names in order `[A, B, C]`, `timesLogged` `[3, 2, 1]` |
| `takes numbers and spelling from the most recent entry` | `雞胸飯` at 10:00 = 500/45/50/8, then at 12:00 = **520**/**48**/52/9 → `kcal 520.0`, `proteinG 48.0`, `timesLogged 2`, `name == "雞胸飯"` (the 12:00 spelling) |
| `limits the list` | 8 distinct foods, `limit = 5` → `size == 5` |
| `ignores blank and whitespace-only names` | meals named `""` and `"   "` → excluded; a day of only blanks → `emptyList()` |
| `ties break by most recent then name` | A logged twice, latest 10:00; B logged twice, latest 12:00 → `[B, A]` |

### 10.4 `MealTypeMigrationSqlTest` (core-data, JVM + sqlite-jdbc, 4 tests)

Setup per test: `DriverManager.getConnection("jdbc:sqlite::memory:")`; create
the v1 table using **the exact `createSql` from the committed
`schemas/…/1.json`** (paste it as a constant in the test, with a comment
pointing at the file); insert rows; run
`MealMigrations.MIGRATION_1_2_STATEMENTS` in order.

| Test | Expected |
|---|---|
| `adds a non-null mealType column defaulting to SNACK` | after migration, `PRAGMA table_info(meals)` contains `mealType`, `type = TEXT`, `notnull = 1`, `dflt_value = 'SNACK'`; a row inserted afterwards without `mealType` reads back `"SNACK"` |
| `back-fills every minute of the day to match MealTypeInference` | insert 1440 v1 rows, row *m* logged at `LocalDate.of(2026, 3, 5).atTime(m / 60, m % 60).atZone(ZoneId.of("Asia/Hong_Kong")).toInstant().toEpochMilli()`; after migration each row's `mealType` equals `MealTypeInference.forMinuteOfDay(m).name` — assert all 1440, not a sample |
| `preserves every other column` | a row with a known `id`, `name = "雞胸飯"`, `kcal = 520.0`, `remoteId = null`, `pendingSync = 1` is byte-identical after migration |
| `a row logged at exactly 10:30 becomes LUNCH` | the boundary the owner will notice first, asserted explicitly rather than only inside the loop |

Note in the test file's KDoc: this runs the same SQL strings the app runs, but
it does not exercise Room's runtime schema validation — see §6.5.

### 10.5 `MealMappersTest` (core-data, +2 tests)

| Test | Expected |
|---|---|
| `mealType round-trips` | `Meal(mealType = AFTERNOON_TEA).toEntity().toDomain().mealType == AFTERNOON_TEA`; entity string is `"AFTERNOON_TEA"` |
| `an unknown mealType name falls back to SNACK` | `MealEntity(mealType = "宵夜").toDomain().mealType == MealType.SNACK` |

### 10.6 `HomeViewModelTest` (app, edited — 3 tests)

The constructor loses `weightRepository`; update both existing tests. Add:

| Test | Expected |
|---|---|
| `emits intake, remaining and protein for two fake meals` (existing, updated) | intake `1050`, remaining `1016`, protein `70`, protein target `132`; `mealGroups.size == 5` |
| `groups today's meals by type` (new) | one breakfast (300 kcal) + two lunches (600, 450) → the `LUNCH` group's `kcal.roundKcal() == 1050`, `mealCount == 2`; the `DINNER` group `isEmpty` |
| `with no profile it needs onboarding` (existing) | unchanged |

### 10.7 `MealEditorViewModelTest` (app, +4 tests)

| Test | Expected |
|---|---|
| `zero or blank calories block save` | name `"雞胸飯"` + kcal `"0"` → `isSaveEnabled` false and `showKcalError` true; kcal `""` → false; kcal `"520"` → true and `showKcalError` false |
| `the kcal error only shows after the field is touched` | fresh state → `showKcalError` false even though `isKcalValid` is false |
| `saves the meal type the user picked` | `setMealType(DINNER)`, save → `lastUpserted!!.mealType == MealType.DINNER` |
| `the meal type follows the time until the user picks one` | `setTime(19:00)` → state `mealType == DINNER`; then `setMealType(SNACK)`; then `setTime(08:00)` → still `SNACK` |
| `a frequent food fills the name and the numbers but not the meal type` | `setMealType(BREAKFAST)`, then `applyFrequentFood(FrequentFood("雞胸飯", 3, 520.0, 48.0, 52.0, 9.0))` → `nameText == "雞胸飯"`, `kcalText == "520"`, `proteinText == "48"`, `mealType == BREAKFAST` |

`FakeMealRepository` gains
`override fun recentMeals(limit: Int) = meals.map { it.sortedByDescending { m -> m.loggedAt }.take(limit) }`.

### 10.8 `HistoryViewModelTest` (app, new — 3 tests)

| Test | Expected |
|---|---|
| `shows the selected day's meals grouped by type` | two meals on `today.minusDays(1)`; `previousDay()` → `Content.date == today.minusDays(1)`, group subtotals match |
| `cannot go past today` | initial state `canGoForward == false`; `nextDay()` leaves `date == today` |
| `an empty past day still shows five groups` | `previousDay()` with no meals → `mealGroups.size == 5`, all `isEmpty`, `dayRollup.intakeKcal == 0.0` |

Use the same `Dispatchers.setMain(UnconfinedTestDispatcher())` +
`backgroundScope.launch { vm.uiState.collect {} }` + `runCurrent()` pattern as
`HomeViewModelTest` — `stateIn(WhileSubscribed)` does not run without a
collector, which cost a fix in Phase 1.

**Test count after this phase:** `core-domain` 49 → **66**, `core-data` 7 →
**13**, `app` 10 → **18**.

---

## 11. CI notes and the trap list

The workflow keeps its Phase 1 shape. Changes:

1. Add the **Upload exported Room schemas** step (§3 R2 step 1).
2. After both schema files are authentic, add the **schema drift guard**
   (§3 R2 step 5).
3. Nothing else. No emulator job, no new runner, no secrets.

### 11.1 Traps specific to this work

Phase 1 burned five CI rounds on missing imports, `Column` vs `ColumnScope`,
a missing `kotlin("test")`, and experimental opt-ins. The equivalents here,
in the order they are most likely to bite:

| # | Trap | Avoidance |
|---|---|---|
| T1 | `@ColumnInfo(defaultValue = "SNACK")` omitted → the app compiles, CI is green, and the owner's phone crashes at launch with `Migration didn't properly handle: meals`. | §6.2. This is the one to double-check by eye before pushing. |
| T2 | Room 2.7 `Migration.migrate` signature. | §6.3 gives the fallback in one line — apply it without investigating. |
| T3 | `TimePickerDialog` does not exist in Material3 1.3.x. | §8.3 step 8 gives the `AlertDialog` + `TimePicker` construction. |
| T4 | `FlowRow` needs `@ExperimentalLayoutApi`, which is **not** opted in. | Use `Modifier.horizontalScroll(rememberScrollState())` everywhere chips could overflow. |
| T5 | Icons outside the core set (`ShowChart`, `Restaurant`, `MonitorWeight`, `History`, …) — `material-icons-extended` is not a dependency and must not become one. | Drawer has no icons (E4). The only icons used anywhere new: `Icons.Filled.Menu`, `Icons.AutoMirrored.Filled.KeyboardArrowLeft`, `Icons.AutoMirrored.Filled.KeyboardArrowRight`. If `AutoMirrored.KeyboardArrow*` does not resolve, fall back to `Icons.Filled.KeyboardArrowLeft/Right` (deprecation is a warning, and `warningsAsErrors = false`). |
| T6 | `Divider` is deprecated in M3 1.3. | Use `HorizontalDivider`. |
| T7 | `flatMapLatest` in `HistoryViewModel` without `@OptIn(ExperimentalCoroutinesApi::class)`. | Copy the annotation from `ObserveTodayDashboardUseCase`. |
| T8 | Component composables that take a `content` lambda must declare `@Composable ColumnScope.() -> Unit` (the `SectionCard` precedent) or callers cannot use `Modifier.weight`. | `MacroTileRow` places its children itself and takes no content lambda — keep it that way. |
| T9 | `MissingTranslation` / `ExtraTranslation` are lint **errors**. Deleting `home_log_weight` from only one file fails the build. | Do the string edits as one commit touching both files, and diff the two files' key lists before pushing. |
| T10 | `stringResource` cannot be called outside a `@Composable`. Meal-type labels are resolved by the `mealTypeLabel(type)` composable helper, not by a `MealType.toLabel()` extension in domain. | `core-domain` never sees a resource id — that is the module boundary. |
| T11 | Adding `mealType` to `Meal` breaks positional constructor calls. | It is appended last with a default (E13); still, grep `Meal(` across all three modules before pushing. |
| T12 | `sqlite-jdbc` must be `testImplementation` in `:core-data` only. | Never `implementation` — it would land in the APK. |
| T13 | The `TZ` environment pin (§6.5) applies to all `:core-data` tests. | The existing mapper tests use explicit instants and ISO strings, so they are unaffected; verify their assertions still pass in the first CI round. |
| T14 | Unused imports after deleting the weight card, `MacroBar` and the dynamic-colour branch. | Warnings only, but clean them: `SectionCard`, `Button`, `WeightEntry`, `Build`, `LocalContext`, `dynamic*ColorScheme`. |
| T15 | `Modifier.fillMaxWidth(0f)` in the meter. | Guarded with `if (fraction > 0f)`. |

### 11.2 Front-loading

Everything in §5 and §10.1–10.3 is verifiable in the dev container. Do all of
it, green, **before** writing a single line of Android code. That is 17 of the
26 new tests, and it is the part that encodes the owner's rules.

---

## 12. Implementation order

Each group compiles/tests before the next. Keep the commits grouped so the
Opus review pass can read the history.

1. **P-0 — domain.** `MealType`, `Meal.mealType`, `MealTypeInference`,
   `MealTypeGroup` + `MealGrouping`, `FrequentFood` + `FrequentFoodRanker`,
   `MealRepository.recentMeals`, and all of §10.1–10.3 **in the same commit**.
   Run `gradle --configure-on-demand :core-domain:test` until green, including
   the pre-existing 49. *Highest-value commit of the phase.*
2. **M-1 — CI schema artifact.** Add only the `Upload exported Room schemas`
   step. Push. Confirm the workflow is green (the code is still v1).
3. **M-2 — authentic v1 schema.** Download `room-schemas`, commit the
   generated `1.json` verbatim over the fabricated one. No other change.
4. **P-1 — strings.** Both `strings.xml` files in one commit: all §9.1
   additions, all §9.2 deletions. Nothing references them yet; this is
   deliberate, because retrofitting translations is what goes wrong.
5. **P-2 — data layer + migration.** `MealEntity`, `DefatDatabase` version 2,
   `MealMigrations`, `DatabaseModule.addMigrations`, `MealDao.observeRecent`,
   mappers, `MealRepositoryImpl.recentMeals`, `sqlite-jdbc` catalog entry, the
   TZ pin, `MealTypeMigrationSqlTest`, and the two mapper tests.
6. **M-3 — authentic v2 schema + drift guard.** After P-2's CI run, commit the
   generated `2.json` and add the `git diff --exit-code -- core-data/schemas`
   step.
7. **P-3 — theme.** `Color.kt`, `Theme.kt`, `colors.xml`, `themes.xml`,
   `values-night/themes.xml`, the `CalorieRing` colour change.
8. **P-4 — shared components.** `MacroTile.kt`, `MealTypeSections.kt`,
   `DefatDrawer.kt`; delete `MacroBar.kt`. Previews for each.
9. **P-5 — Home.** `HomeUiState`, `HomeViewModel`, `HomeScreen`; update
   `HomeViewModelTest`.
10. **P-6 — meal editor.** State, ViewModel, screen, pickers, chips, frequent
    foods, the kcal block; `FakeMealRepository.recentMeals`; the four new
    editor tests.
11. **P-7 — History.** State, ViewModel, screen, route, `HistoryViewModelTest`.
12. **P-8 — signing + install safety.** Generate and commit
    `app/debug.keystore`, wire `signingConfigs.debug`, `.gitignore` exception,
    `versionCode = 2`, `versionName = "0.2.0-phase1.6"`.
13. **D-1 — docs.** `docs/02` (add `mealType` to the meals data-model bullet),
    `docs/03` (new "Phase 1.6" section linking here), `docs/06` (debug-keystore
    exception under Security), `README.md` (status + install-over-the-top
    note).
14. **P-9 — final CI run + PR.** The PR description must state, in plain
    language for the owner: the signing-key problem and its fix (§3 R1), what
    the migration does to their existing meals, the five meal types and their
    time boundaries, and a link to the green run and the `test-build` release
    asset.

---

## 13. Definition of Done

**Automated**

1. `gradle --configure-on-demand :core-domain:test` passes in the dev
   container — 66 tests, including the original 49.
2. `Android CI` is green on the branch and on `main`: domain tests,
   `:core-data:testDebugUnitTest` (13), `:app:testDebugUnitTest` (18),
   `lintDebug`, `assembleDebug`.
3. `lintDebug` reports no `MissingTranslation` and no `ExtraTranslation`.
4. `core-data/schemas/…/1.json` and `2.json` are byte-identical to what CI
   generates — the drift-guard step passes.
5. `MealTypeMigrationSqlTest` asserts all 1440 minutes.
6. `:core-domain` still contains zero `android.*` / `androidx.*` imports
   (`checkNoAndroidImports` passes).
7. Grepping `app/src/main` for `LinearProgressIndicator` returns nothing on
   Home, and `dynamicColor` / `dynamicLightColorScheme` return nothing at all.

**Manual — the owner, on their Samsung phone**

8. The new APK installs **over** the previous build without uninstalling, or,
   if it refuses, the owner uninstalls once and confirms that the *next* build
   after this one installs over the top.
9. Home shows: ring on top with the big remaining number and "已食 X / Y
   千卡"; three bordered macro tiles in a row with 蛋白質 emphasised; the meal
   sections; the footnote.
10. **With nothing logged, the macro meters are visibly empty**, not full.
    This is the specific complaint being fixed.
11. All five meal types 早餐 / 午餐 / 下午茶 / 晚餐 / 小食 appear on Home in
    that order, each with its own kcal subtotal, and an unlogged type shows
    "—" and an italic "仲未記錄".
12. **If their existing meals survived the install**, each one has been given a
    sensible meal type based on when it was logged.
13. Adding a meal at, say, 19:15 pre-selects 晚餐; the owner can change it; the
    time and date pickers both open and both stick.
14. Saving a meal with the calories left blank or 0 is impossible — the Save
    button is greyed and the field shows "請輸入卡路里，一定要大過 0。"
15. After logging the same food twice, it appears under 常用食物 and one tap
    fills the name and all the numbers.
16. The hamburger opens the drawer; the header shows DeFAT and the daily
    target; 記錄體重, 歷史記錄 and 個人資料 all open; **進度圖表 is greyed out,
    tagged 稍後推出, and does nothing when tapped**.
17. 歷史記錄 opens on today, steps back to yesterday, shows that day's meals
    grouped by type and that day's totals, and will not step into the future.
18. The app no longer looks like a generic Material demo: pine and jade in
    light mode, and the same identity in dark mode, with no white flash on
    launch in dark mode.
19. Switching the phone's app language to 繁體中文 leaves no English behind on
    Home, the editor, History or the drawer, and nothing overflows.

**Docs**

20. `docs/02`, `docs/03`, `docs/06` and `README.md` updated per D-1.

---

## 14. Owner actions needed

Written for a non-programmer; the implementer must reproduce this list in the
PR description, in Cantonese.

1. **Before you install the new build — expect one bump, once.** Every test
   build so far was signed with a different throwaway key, which Android
   treats as a different app. So this one time, when you tap the new
   `app-debug.apk`, Android may say **"App not installed"**. If that happens
   you have to delete DeFAT first and install fresh, and **the meals you have
   already logged will be gone**. Sorry — that is our mistake, not yours.
   Write down anything you want to keep (there should only be a handful of
   entries), and re-enter it after installing. **This is the last time this
   will happen**: from this build onward every APK is signed with the same key
   and will install straight over the previous one, keeping your data.
2. **Check the meal times are right for you and your students.** We use:
   before **10:30** = 早餐, **10:30–14:30** = 午餐, **14:30–17:30** = 下午茶,
   **17:30–21:00** = 晚餐, after **21:00** = 小食. These are only the *default*
   when you add a meal — you can always tap a different one. If any boundary
   is wrong for how Hong Kong actually eats, tell us the number and we change
   one line.
3. **One odd case to confirm:** a meal logged at 3 a.m. is currently labelled
   早餐, because our rule says "anything before 10:30 is breakfast". You told
   us not to add 宵夜, so this is the honest consequence. If you would rather
   the small hours (say midnight to 5 a.m.) counted as 小食, say so — it is a
   one-line change.
4. **Look at the app in dark mode too** (phone Settings → Display → Dark). The
   dark greens are new; tell us if anything is hard to read on your screen.
5. **Tell us whether 5 常用食物 is the right number.** We show your five
   most-logged foods; more than that starts to feel like a list to read.
6. **Confirm the drawer order** — 今日 / 記錄體重 / 歷史記錄 / 進度圖表 /
   個人資料. There is deliberately **no 設定 row**: everything a settings page
   would hold is already inside 個人資料, and a second row going to the same
   place would be misleading.
7. **進度圖表 is greyed out on purpose.** It is Phase 4 work. It is visible so
   you can see it is planned, not forgotten.
8. **Still nothing to do about Firebase or the Play Store.** Phase 1.5 and
   Phase 5 respectively.

---

## 15. Deferred, listed so nothing is forgotten

- Per-day target snapshots so History shows the target that actually applied
  that day (E18) — belongs with the Phase 4 trend engine.
- A date picker on `LogWeightScreen`: `LogWeightViewModel.setDate` exists and
  is still unused, exactly like `MealEditorViewModel.setTime`/`setDate` were
  before this phase. Out of scope here; note it in the PR so it does not rot
  for another phase.
- Reordering / renaming meal types, and a 宵夜 type — owner rejected for now.
- Weekly and monthly views on History; only single-day is in scope.
- `mealType` in the Firestore document shape — Phase 1.5, flagged in `docs/02`
  by D-1.
- An in-app settings screen (E1).

---

## 16. Version pin changes

Append here if the P1 §4.3 fallback protocol is used.

- `sqliteJdbc = "3.50.1.0"` — new pin, unverified in the dev container
  (Maven Central; not blocked by the proxy). If CI cannot resolve it, move to
  the nearest published `3.5x.y.z` and record it here.
