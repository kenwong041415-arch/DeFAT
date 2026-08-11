# Phase 1 — Implementation Plan (Walking skeleton + core math)

Status: approved plan, ready for implementation.
Author: Opus planning pass (per `CLAUDE.md` model roles).
Implementer: Sonnet. Follow this document literally; where it says "exact",
copy the text verbatim. If something here contradicts `docs/`, stop and flag
it rather than silently choosing.

---

## 1. Scope summary

Phase 1 delivers an installable Android app that a single user (the owner)
can use end-to-end with no backend:

1. Onboarding profile wizard (sex, birth date, height, weight, body-fat %,
   activity level, goal, medical disclaimer).
2. `core-domain` — pure Kotlin, no Android imports: BMR (Mifflin-St Jeor +
   Katch-McArdle), TDEE (multiplier + wearable modes), calorie target with
   safety floors, macro split, day rollup, safe-rate check, MET table.
   Fully unit-tested against the reference vectors in `docs/04`.
3. Home screen: target-vs-intake ring, remaining kcal, protein progress,
   macro totals, today's meal list.
4. Manual meal entry (add / edit / delete) and manual weight + body-fat entry.
5. Day rollup computed reactively from local storage.
6. English + Traditional Chinese (HK) strings, metric units.
7. GitHub Actions CI that runs all tests and publishes a debug APK artifact.

**Out of scope for Phase 1:** Firebase, sign-in, Health Connect, camera, AI,
notifications, trainer dashboard, trend charts, plateau detection.

### 1.1 Exit test (from `docs/03`)

> Owner creates own profile, sees a sensible daily target, logs a manual meal.

Mapped to a concrete acceptance script in §11.

---

## 2. Deviations from `docs/03` / `docs/06` — and why

These are real deviations. They are listed here so the docs stay honest; the
implementing PR must also add a short note to `docs/03-roadmap.md` pointing at
this file (see §12, task D-1).

| # | Deviation | Reason |
|---|---|---|
| **D1** | **No Firebase Auth sign-in and no Firestore in Phase 1.** Phase 1 is local-first: profile in DataStore, meals/weights in Room. Auth + Firestore sync become **Phase 1.5**. | No Firebase project exists yet — only the owner can create one in the Firebase console. Adding the Google Services plugin without `google-services.json` makes the build fail, which would break CI and block the owner from getting *any* installable APK. Local-first also directly satisfies the offline-first NFR in `docs/02`. |
| **D2** | **No `users/{uid}/days/{date}` rollup cache.** The day rollup is computed on demand from the meals of that date. | With no sync, a cache is pure duplication and a source of staleness bugs. Recomputation is O(meals-per-day) ≈ 5 rows. |
| **D3** | **Firestore security-rules tests moved from Phase 1 to Phase 1.5** (`docs/06` places them in Phase 1). | There are no rules and no project to test them against yet. They ship with D1. |
| **D4** | **`docs/04` reference vector correction.** The doc states Mifflin male 80 kg / 175 cm / 30 y → 1780 kcal. The formula in the same doc gives `10×80 + 6.25×175 − 5×30 + 5 = 800 + 1093.75 − 150 + 5 = ` **1748.75**. The female vector (1311.5) matches its formula exactly, so the formula is authoritative and the male number is an arithmetic slip. Code implements the formula; the unit test asserts **1748.75**. | Cannot implement two mutually inconsistent "ground truths". **Owner must confirm** (see §13). The implementing PR updates `docs/04-health-metabolism.md` line for the male vector to `1748.75 kcal` and adds a one-line note that this was corrected in the Phase 1 PR. |
| **D5** | **MET table + safe-rate math implemented in Phase 1** although the roadmap places manual exercise in Phase 2 and rate/trend logic in Phase 4. **No UI is wired to them.** | They are ~40 lines of pure, testable arithmetic straight out of `docs/04`, and having them tested now de-risks Phase 2. Explicitly excluded: 7-day moving average, plateau detection, adaptive TDEE — those stay in Phase 4. |
| **D6** | **No in-app language switcher.** The app follows the system/per-app language; `res/xml/locales_config.xml` is declared so Android 13+ (all current Samsung flagships) exposes DeFAT in Settings → Apps → DeFAT → Language. | Avoids an `appcompat` dependency and a locale-restart edge case for zero Phase 1 value. Both string files ship from day one, so nothing is lost. |
| **D7** | **String-based Compose navigation routes**, not type-safe serialized routes. | One fewer dependency (`kotlinx-serialization`) and the most heavily documented pattern; Phase 1 has 6 destinations and 1 argument. |
| **D8** | **Room entities carry `remoteId` and `pendingSync` columns from v1**, unused in Phase 1. | Lets Phase 1.5 add Firestore sync without a Room migration on the owner's already-installed app. Cheap now, expensive later. |
| **D9** | **AGP is deliberately not declared in the root `build.gradle.kts` plugins block**, and `google()` is content-filtered. | The development container has no Android SDK **and** its proxy blocks `dl.google.com` (403). This structure is what makes `gradle --configure-on-demand :core-domain:test` runnable locally. See §4.4. |

---

## 3. Module structure

Matches `docs/02` "App module structure", minus `core-ai` (Phase 3).

```
:app          Android application — Compose UI, navigation, ViewModels, Hilt wiring
:core-data    Android library    — Room, DataStore, repository implementations, Hilt modules
:core-domain  Pure Kotlin/JVM    — models, calculators, repository interfaces, use cases
```

Dependency direction (strictly one-way):

```
:app  ──────►  :core-data  ──────►  :core-domain
  └────────────────────────────────────►  :core-domain
```

`:core-domain` has **no Android dependency of any kind** — its only external
dependency is `kotlinx-coroutines-core` (for `Flow` in repository interfaces),
which is Kotlin-Multiplatform-safe. This is the future KMP seed (`docs/01` D1).

**Date/time:** `:core-domain` uses `java.time` (`LocalDate`, `Instant`).
`minSdk = 28` ≥ API 26, so `java.time` is available natively with no
desugaring. When KMP happens, these become `kotlinx-datetime` — keep
`java.time` usage confined to model/calculator signatures so the swap is
mechanical.

---

## 4. Gradle setup

### 4.1 Toolchain

- Gradle **8.14.3** via committed wrapper (matches the dev container exactly).
- JDK **21** to run Gradle; **Java 17** bytecode target for all modules
  (`sourceCompatibility`/`targetCompatibility`/`jvmTarget = 17`).
  Do **not** use `jvmToolchain(...)` — toolchain auto-provisioning needs
  network access the container does not reliably have. `-release 17` under
  JDK 21 is sufficient.

### 4.2 Gradle wrapper — generate and commit

The wrapper is **committed to git, jar included**. Run once at the repo root:

```
gradle wrapper --gradle-version 8.14.3 --distribution-type bin
```

This creates `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`,
`gradle/wrapper/gradle-wrapper.properties`. All four are committed.
`gradle-wrapper.jar` must **not** be gitignored — CI (`gradle/actions/setup-gradle`)
invokes `./gradlew`. Ensure `gradlew` has the executable bit
(`git update-index --chmod=+x gradlew` if needed).

### 4.3 `gradle/libs.versions.toml` (exact content)

```toml
[versions]
agp = "8.12.0"
kotlin = "2.2.20"
ksp = "2.2.20-2.0.4"
hilt = "2.57.2"
hiltNavigationCompose = "1.2.0"
composeBom = "2025.06.01"
coreKtx = "1.16.0"
lifecycle = "2.9.1"
activityCompose = "1.10.1"
navigationCompose = "2.9.0"
room = "2.7.2"
datastore = "1.1.1"
coroutines = "1.10.2"
junit = "4.13.2"

[libraries]
androidx-core-ktx = { module = "androidx.core:core-ktx", version.ref = "coreKtx" }
androidx-lifecycle-runtime-ktx = { module = "androidx.lifecycle:lifecycle-runtime-ktx", version.ref = "lifecycle" }
androidx-lifecycle-runtime-compose = { module = "androidx.lifecycle:lifecycle-runtime-compose", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "activityCompose" }
androidx-navigation-compose = { module = "androidx.navigation:navigation-compose", version.ref = "navigationCompose" }

androidx-compose-bom = { module = "androidx.compose:compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { module = "androidx.compose.ui:ui" }
androidx-compose-ui-graphics = { module = "androidx.compose.ui:ui-graphics" }
androidx-compose-ui-tooling = { module = "androidx.compose.ui:ui-tooling" }
androidx-compose-ui-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview" }
androidx-compose-material3 = { module = "androidx.compose.material3:material3" }

androidx-room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
androidx-room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
androidx-room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }

androidx-datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }

hilt-android = { module = "com.google.dagger:hilt-android", version.ref = "hilt" }
hilt-compiler = { module = "com.google.dagger:hilt-android-compiler", version.ref = "hilt" }
androidx-hilt-navigation-compose = { module = "androidx.hilt:hilt-navigation-compose", version.ref = "hiltNavigationCompose" }

kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }
kotlinx-coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }

junit = { module = "junit:junit", version.ref = "junit" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
android-library = { id = "com.android.library", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

**SDK levels** (set directly in `app/build.gradle.kts` and
`core-data/build.gradle.kts`, not in the catalog):
`compileSdk = 36`, `minSdk = 28` (per `docs/01` D1), `targetSdk = 36`.

#### Version fallback protocol (important)

The dev container cannot reach Google's Maven repo, so androidx/AGP versions
above are pinned from documentation, not resolved. **If the first CI run fails
with "Could not find …" for any pinned artifact:**

1. Do **not** switch to dynamic versions (`+`, `latest.release`) — ever.
2. Bump only the failing coordinate to the nearest existing stable version,
   keeping the major/minor family (e.g. `composeBom` → next published
   `YYYY.MM.NN`; `agp` → nearest published `8.1x.y` that requires
   Gradle ≤ 8.14.3).
3. Keep Kotlin ↔ KSP paired: KSP version prefix must equal the Kotlin version
   (`2.2.20` ↔ `2.2.20-2.0.4`). Never bump one alone.
4. Record every changed pin in a short "Version pin changes" list appended to
   §14 of this file, so the plan stays true.

### 4.4 `settings.gradle.kts` (exact content)

```kotlin
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "DeFAT"
include(":app")
include(":core-data")
include(":core-domain")
```

The `content { … }` filters are load-bearing: they stop Gradle from querying
the (blocked) Google repo for `org.jetbrains.*` and `junit` artifacts when
building `:core-domain` in the dev container.

### 4.5 `build.gradle.kts` (root) — exact content

```kotlin
// Intentionally empty of plugin declarations.
//
// The Android Gradle Plugin is NOT declared here with `apply false`, because
// doing so resolves the AGP jar from Google's Maven repo during root-project
// configuration. The development container has no Android SDK and no access
// to dl.google.com, so keeping AGP out of the root build is what allows
//     gradle --configure-on-demand :core-domain:test
// to run locally. Each module declares the plugins it needs, with versions
// from gradle/libs.versions.toml.
```

(The file may be literally just this comment block.)

### 4.6 `gradle.properties` (exact content)

```properties
org.gradle.jvmargs=-Xmx3072m -XX:MaxMetaspaceSize=1024m -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true

android.useAndroidX=true
android.nonTransitiveRClass=true

kotlin.code.style=official
```

Note: `org.gradle.configureondemand` is **not** set here (AGP does not
officially support it, and CI must be clean). Instead, in the dev container
run domain tests with the flag on the command line:

```
gradle --configure-on-demand :core-domain:test
```

This configures only the root project and `:core-domain`, so AGP is never
resolved. CI runs `./gradlew` without the flag on a machine that has the SDK.

### 4.7 `core-domain/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        // core-domain must stay Android-free; fail the build on any android import
        allWarningsAsErrors.set(false)
    }
    sourceSets.all { languageSettings.progressiveMode = true }
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.withType<Test>().configureEach { useJUnit() }
```

Source dirs: `src/main/kotlin`, `src/test/kotlin` (the Kotlin JVM plugin
includes these by default).

### 4.8 `core-data/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.defat.core.data"
    compileSdk = 36
    defaultConfig { minSdk = 28 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }
    sourceSets["main"].kotlin.srcDir("src/main/kotlin")
    sourceSets["test"].kotlin.srcDir("src/test/kotlin")
}

ksp { arg("room.schemaLocation", "$projectDir/schemas") }

dependencies {
    api(project(":core-domain"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
```

The generated `core-data/schemas/com.defat.core.data.db.DefatDatabase/1.json`
is **committed** (needed for safe Phase 1.5 migrations).

### 4.9 `app/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.defat.app"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.defat.app"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-phase1"
        resourceConfigurations += setOf("en", "zh-rHK")
    }
    buildTypes {
        debug { isMinifyEnabled = false }
        release {
            isMinifyEnabled = false          // R8 config lands in Phase 5
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }
    lint {
        abortOnError = true
        warningsAsErrors = false
        error += listOf("MissingTranslation", "ExtraTranslation")
    }
    sourceSets["main"].kotlin.srcDir("src/main/kotlin")
    sourceSets["test"].kotlin.srcDir("src/test/kotlin")
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    implementation(project(":core-domain"))
    implementation(project(":core-data"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
```

`lint` treating `MissingTranslation` as an error is the enforcement mechanism
for the bilingual requirement — an untranslated string fails CI.

Icons: use only the **core** Material icon set (`Icons.Default.Add`,
`Delete`, `Edit`, `Settings`, `Check`, `Close`, `Icons.AutoMirrored.Filled.ArrowBack`).
Do **not** add `material-icons-extended`.

---

## 5. Complete file tree

Files marked `[new]` are created by this phase. Existing docs are untouched
except `docs/03-roadmap.md` and `docs/04-health-metabolism.md` (see §12).

```
DeFAT/
├── .github/
│   └── workflows/
│       └── android-ci.yml                                    [new]
├── .gitignore                                                [new]
├── build.gradle.kts                                          [new]
├── settings.gradle.kts                                       [new]
├── gradle.properties                                         [new]
├── gradlew                                                   [new, +x]
├── gradlew.bat                                               [new]
├── gradle/
│   ├── libs.versions.toml                                    [new]
│   └── wrapper/
│       ├── gradle-wrapper.jar                                [new, committed]
│       └── gradle-wrapper.properties                         [new]
├── README.md                                                 [edit: status → Phase 1]
├── CLAUDE.md
├── docs/
│   ├── 00-blueprint.md … 06-privacy-compliance.md
│   ├── 03-roadmap.md                                         [edit: note D1/D3]
│   ├── 04-health-metabolism.md                               [edit: D4 vector fix]
│   └── plans/
│       └── phase1-implementation-plan.md                     (this file)
│
├── core-domain/
│   ├── build.gradle.kts                                      [new]
│   └── src/
│       ├── main/kotlin/com/defat/core/domain/
│       │   ├── model/
│       │   │   ├── Sex.kt
│       │   │   ├── ActivityLevel.kt
│       │   │   ├── UserProfile.kt
│       │   │   ├── Goal.kt
│       │   │   ├── Meal.kt
│       │   │   ├── WeightEntry.kt
│       │   │   ├── MeasurementSource.kt
│       │   │   ├── MacroTargets.kt
│       │   │   ├── DailyTarget.kt
│       │   │   └── DayRollup.kt
│       │   ├── calc/
│       │   │   ├── NutritionConstants.kt
│       │   │   ├── BmrCalculator.kt
│       │   │   ├── TdeeCalculator.kt
│       │   │   ├── CalorieTargetCalculator.kt
│       │   │   ├── MacroCalculator.kt
│       │   │   ├── DayRollupCalculator.kt
│       │   │   ├── WeightRateCalculator.kt
│       │   │   ├── MetCalculator.kt
│       │   │   └── Rounding.kt
│       │   ├── repository/
│       │   │   ├── ProfileRepository.kt
│       │   │   ├── MealRepository.kt
│       │   │   └── WeightRepository.kt
│       │   └── usecase/
│       │       ├── ComputeDailyTargetUseCase.kt
│       │       ├── ObserveTodayDashboardUseCase.kt
│       │       ├── SaveMealUseCase.kt
│       │       └── LogWeightUseCase.kt
│       └── test/kotlin/com/defat/core/domain/
│           ├── calc/
│           │   ├── BmrCalculatorTest.kt
│           │   ├── TdeeCalculatorTest.kt
│           │   ├── CalorieTargetCalculatorTest.kt
│           │   ├── MacroCalculatorTest.kt
│           │   ├── DayRollupCalculatorTest.kt
│           │   ├── WeightRateCalculatorTest.kt
│           │   └── MetCalculatorTest.kt
│           ├── model/
│           │   └── UserProfileTest.kt
│           └── usecase/
│               ├── ComputeDailyTargetUseCaseTest.kt
│               └── GoldenProfileEndToEndTest.kt
│
├── core-data/
│   ├── build.gradle.kts                                      [new]
│   ├── schemas/com.defat.core.data.db.DefatDatabase/1.json    [generated, committed]
│   └── src/
│       ├── main/kotlin/com/defat/core/data/
│       │   ├── db/
│       │   │   ├── DefatDatabase.kt
│       │   │   ├── MealDao.kt
│       │   │   ├── WeightDao.kt
│       │   │   └── entity/
│       │   │       ├── MealEntity.kt
│       │   │       └── WeightEntryEntity.kt
│       │   ├── datastore/
│       │   │   ├── ProfileKeys.kt
│       │   │   └── ProfileLocalDataSource.kt
│       │   ├── mapper/
│       │   │   ├── MealMappers.kt
│       │   │   └── WeightMappers.kt
│       │   ├── repository/
│       │   │   ├── ProfileRepositoryImpl.kt
│       │   │   ├── MealRepositoryImpl.kt
│       │   │   └── WeightRepositoryImpl.kt
│       │   └── di/
│       │       ├── DatabaseModule.kt
│       │       ├── DataStoreModule.kt
│       │       ├── DispatcherModule.kt
│       │       └── RepositoryModule.kt
│       └── test/kotlin/com/defat/core/data/mapper/
│           ├── MealMappersTest.kt
│           └── WeightMappersTest.kt
│
└── app/
    ├── build.gradle.kts                                      [new]
    ├── proguard-rules.pro                                    [new, empty w/ header]
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── kotlin/com/defat/app/
        │   │   ├── DefatApplication.kt
        │   │   ├── MainActivity.kt
        │   │   ├── MainViewModel.kt
        │   │   ├── navigation/
        │   │   │   ├── Routes.kt
        │   │   │   └── DefatNavHost.kt
        │   │   └── ui/
        │   │       ├── theme/{Color.kt, Type.kt, Theme.kt}
        │   │       ├── component/
        │   │       │   ├── CalorieRing.kt
        │   │       │   ├── MacroBar.kt
        │   │       │   ├── DecimalField.kt
        │   │       │   ├── WizardScaffold.kt
        │   │       │   └── SectionCard.kt
        │   │       ├── onboarding/
        │   │       │   ├── OnboardingUiState.kt
        │   │       │   ├── OnboardingViewModel.kt
        │   │       │   ├── WelcomeScreen.kt
        │   │       │   ├── BasicsScreen.kt
        │   │       │   ├── BodyScreen.kt
        │   │       │   ├── ActivityScreen.kt
        │   │       │   ├── GoalScreen.kt
        │   │       │   └── SummaryScreen.kt
        │   │       ├── home/
        │   │       │   ├── HomeUiState.kt
        │   │       │   ├── HomeViewModel.kt
        │   │       │   └── HomeScreen.kt
        │   │       ├── meal/
        │   │       │   ├── MealEditorUiState.kt
        │   │       │   ├── MealEditorViewModel.kt
        │   │       │   └── MealEditorScreen.kt
        │   │       ├── weight/
        │   │       │   ├── LogWeightViewModel.kt
        │   │       │   └── LogWeightScreen.kt
        │   │       └── profile/
        │   │           ├── ProfileViewModel.kt
        │   │           └── ProfileScreen.kt
        │   └── res/
        │       ├── values/{strings.xml, themes.xml, colors.xml}
        │       ├── values-zh-rHK/strings.xml
        │       ├── xml/locales_config.xml
        │       ├── drawable/ic_launcher_foreground.xml
        │       └── mipmap-anydpi-v26/{ic_launcher.xml, ic_launcher_round.xml}
        └── test/kotlin/com/defat/app/
            ├── fake/{FakeProfileRepository.kt, FakeMealRepository.kt, FakeWeightRepository.kt}
            └── ui/
                ├── OnboardingViewModelTest.kt
                ├── HomeViewModelTest.kt
                └── MealEditorViewModelTest.kt
```

`.gitignore` content:

```
*.iml
.gradle/
/local.properties
/.idea/
.DS_Store
build/
/captures
.externalNativeBuild
.cxx
local.properties
# Secrets — never commit (docs/06)
google-services.json
*.jks
*.keystore
# NOTE: gradle/wrapper/gradle-wrapper.jar IS committed on purpose (CI needs it).
```

---

## 6. `core-domain` specification

All arithmetic is `Double`. **Rounding is a presentation concern** — never
round inside a calculator; round only in `Rounding.kt` helpers called by the
UI layer. Every public function validates its inputs with `require(...)`.

### 6.1 `model/`

```kotlin
enum class Sex { MALE, FEMALE }

enum class ActivityLevel(val multiplier: Double) {
    SEDENTARY(1.2), LIGHT(1.375), MODERATE(1.55), ACTIVE(1.725), ATHLETE(1.9)
}

enum class MeasurementSource { MANUAL, HEALTH_CONNECT }   // HC unused in P1

data class Goal(
    val targetWeightKg: Double,
    val targetBodyFatPct: Double? = null,
    val targetDate: LocalDate? = null,
)

data class UserProfile(
    val sex: Sex,
    val birthDate: LocalDate,
    val heightCm: Double,
    val weightKg: Double,
    val bodyFatPct: Double?,          // null = unknown → Mifflin path
    val activityLevel: ActivityLevel,
    val goal: Goal,
    val disclaimerAcceptedAt: Instant?,
) {
    fun ageYears(on: LocalDate): Int   // Period.between(birthDate, on).years
}

data class Meal(
    val id: String,                    // UUID string
    val loggedAt: Instant,
    val date: LocalDate,               // local date the meal counts toward
    val name: String,
    val kcal: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val source: MealSource = MealSource.MANUAL,
)
enum class MealSource { MANUAL, PHOTO, TEXT, LABEL }   // only MANUAL used in P1

data class WeightEntry(
    val date: LocalDate,
    val weightKg: Double,
    val bodyFatPct: Double?,
    val recordedAt: Instant,
    val source: MeasurementSource = MeasurementSource.MANUAL,
)

data class MacroTargets(
    val proteinG: Double,
    val fatG: Double,
    val carbsG: Double,
    val carbsClamped: Boolean,        // true when carbs would have been < 0
) { val energyKcal: Double }          // 4p + 4c + 9f

enum class TdeeSource { ACTIVITY_MULTIPLIER, WEARABLE }

data class DailyTarget(
    val bmrKcal: Double,
    val bmrMethod: BmrMethod,
    val tdeeKcal: Double,
    val tdeeSource: TdeeSource,
    val targetKcal: Double,
    val floorApplied: Boolean,
    val macros: MacroTargets,
)
enum class BmrMethod { KATCH_MCARDLE, MIFFLIN_ST_JEOR }

data class DayRollup(
    val date: LocalDate,
    val target: DailyTarget,
    val intakeKcal: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val mealCount: Int,
) {
    val remainingKcal: Double         // target.targetKcal - intakeKcal
    val remainingProteinG: Double     // (target.macros.proteinG - proteinG).coerceAtLeast(0.0)
    val isOverTarget: Boolean         // remainingKcal < 0
    val progressFraction: Float       // (intakeKcal / targetKcal) as Float, coerced 0f..1f for the ring
}
```

### 6.2 `calc/NutritionConstants.kt`

```kotlin
object NutritionConstants {
    const val KCAL_PER_G_PROTEIN = 4.0
    const val KCAL_PER_G_CARB    = 4.0
    const val KCAL_PER_G_FAT     = 9.0

    const val KATCH_BASE = 370.0
    const val KATCH_LBM_COEFFICIENT = 21.6

    const val NEAT_TEF_BASELINE = 1.1          // docs/04 wearable mode; tune with owner

    const val DEFAULT_DEFICIT_FRACTION = 0.20
    const val FLOOR_KCAL_MALE = 1500.0
    const val FLOOR_KCAL_FEMALE = 1200.0

    const val PROTEIN_G_PER_KG_BODYWEIGHT = 1.8
    const val PROTEIN_G_PER_KG_LBM = 2.2
    const val MIN_FAT_G_PER_KG = 0.6

    const val KCAL_PER_KG_FAT = 7700.0
    const val SAFE_WEEKLY_LOSS_PCT_MIN = 0.5
    const val SAFE_WEEKLY_LOSS_PCT_MAX = 1.0
}
```

Every constant carries a `// docs/04 — <section>` comment. No magic numbers
anywhere else in the codebase.

### 6.3 `calc/BmrCalculator.kt`

```kotlin
object BmrCalculator {
    fun leanBodyMassKg(weightKg: Double, bodyFatPct: Double): Double
    fun katchMcArdle(weightKg: Double, bodyFatPct: Double): Double
    fun mifflinStJeor(weightKg: Double, heightCm: Double, ageYears: Int, sex: Sex): Double
    fun bmr(profile: UserProfile, on: LocalDate): BmrResult
}
data class BmrResult(val kcal: Double, val method: BmrMethod)
```

Rules:
- `bmr()` selects **Katch-McArdle when `bodyFatPct != null`**, else Mifflin
  (`docs/04`).
- Validation: `weightKg > 0`, `heightCm > 0`, `ageYears >= 0`,
  `bodyFatPct in 0.0..75.0` (else `IllegalArgumentException` with a message
  naming the parameter).

### 6.4 `calc/TdeeCalculator.kt`

```kotlin
object TdeeCalculator {
    fun fromActivityLevel(bmrKcal: Double, level: ActivityLevel): Double
    fun fromWearable(
        bmrKcal: Double,
        activeKcal: Double,
        neatTefFactor: Double = NutritionConstants.NEAT_TEF_BASELINE,
    ): Double
    fun tdee(bmrKcal: Double, level: ActivityLevel, activeKcal: Double?): TdeeResult
}
data class TdeeResult(val kcal: Double, val source: TdeeSource)
```

`tdee()` uses the wearable branch when `activeKcal != null` (Phase 2 will pass
it; Phase 1 always passes `null`). A KDoc block on `fromWearable` restates the
**double-counting guard** from `docs/04`: only *active* energy may be passed —
never total-calories-burned, and never a steps-derived estimate when active
energy exists.

### 6.5 `calc/CalorieTargetCalculator.kt`

```kotlin
object CalorieTargetCalculator {
    fun target(
        tdeeKcal: Double,
        sex: Sex,
        deficitFraction: Double = NutritionConstants.DEFAULT_DEFICIT_FRACTION,
        trainerOverrideFloorKcal: Double? = null,
    ): TargetResult
    fun floorFor(sex: Sex): Double
}
data class TargetResult(val kcal: Double, val floorApplied: Boolean)
```

- Raw target = `tdee * (1 - deficitFraction)`.
- Floor = `trainerOverrideFloorKcal ?: floorFor(sex)`; if raw < floor, return
  floor with `floorApplied = true`.
- `deficitFraction` validated to `0.0..0.40`.

### 6.6 `calc/MacroCalculator.kt`

```kotlin
object MacroCalculator {
    fun macroTargets(targetKcal: Double, weightKg: Double, bodyFatPct: Double?): MacroTargets
}
```

- protein = `bodyFatPct != null ? 2.2 * LBM : 1.8 * weightKg`
- fat = `0.6 * weightKg`
- carbs = `(targetKcal - 4*protein - 9*fat) / 4`; if negative → `0.0` and
  `carbsClamped = true`.

### 6.7 `calc/DayRollupCalculator.kt`

```kotlin
object DayRollupCalculator {
    fun rollup(date: LocalDate, target: DailyTarget, meals: List<Meal>): DayRollup
}
```

Pure summation; empty list → all zeros, `remainingKcal == targetKcal`.

### 6.8 `calc/WeightRateCalculator.kt` (D5 — no UI in Phase 1)

```kotlin
object WeightRateCalculator {
    fun weeklyRatePctOfBodyweight(weeklyChangeKg: Double, bodyWeightKg: Double): Double
    fun assess(weeklyChangeKg: Double, bodyWeightKg: Double): RateAssessment
    fun projectedWeeklyLossKg(dailyDeficitKcal: Double): Double  // deficit*7/7700
}
enum class RateAssessment { GAINING, TOO_SLOW, SAFE, TOO_FAST }
```

`weeklyChangeKg` is signed (negative = loss). Percentage is on the magnitude
of loss; a positive change returns `GAINING`.

### 6.9 `calc/MetCalculator.kt` (D5 — no UI in Phase 1)

```kotlin
object MetCalculator {
    fun kcal(met: Double, weightKg: Double, hours: Double): Double   // met * kg * h
}
data class MetActivity(val key: String, val met: Double)
object MetTable {
    val ACTIVITIES: List<MetActivity>   // exactly docs/04's list, in this order
    fun metFor(key: String): Double?
}
```

Table (keys are stable string ids, also used as string-resource suffixes):
`walking 3.5`, `jogging 7.0`, `cycling_moderate 6.8`, `swimming 6.0`,
`weight_training 5.0` (docs/04 gives a 3.5–6 range; use the midpoint 5.0 and
KDoc the range for owner review), `hiit 8.0`, `hiking 6.0`, `badminton 5.5`,
`basketball 6.5`, `yoga 2.5`.

### 6.10 `calc/Rounding.kt`

```kotlin
fun Double.roundKcal(): Int          // kotlin.math.roundToInt()
fun Double.roundGrams(): Int
fun Double.roundKg1dp(): Double      // 1 decimal place, half-up
fun Double.roundPct1dp(): Double
```

### 6.11 `repository/` (interfaces only)

```kotlin
interface ProfileRepository {
    val profile: Flow<UserProfile?>
    val onboardingComplete: Flow<Boolean>
    suspend fun save(profile: UserProfile)
    suspend fun updateCurrentBody(weightKg: Double, bodyFatPct: Double?)
    suspend fun clear()
}

interface MealRepository {
    fun mealsOn(date: LocalDate): Flow<List<Meal>>
    suspend fun mealById(id: String): Meal?
    suspend fun upsert(meal: Meal)
    suspend fun delete(id: String)
}

interface WeightRepository {
    val latest: Flow<WeightEntry?>
    fun between(from: LocalDate, to: LocalDate): Flow<List<WeightEntry>>
    suspend fun upsert(entry: WeightEntry)
}
```

### 6.12 `usecase/`

```kotlin
class ComputeDailyTargetUseCase {
    operator fun invoke(
        profile: UserProfile,
        on: LocalDate,
        activeKcal: Double? = null,
    ): DailyTarget
}
```
Composes BMR → TDEE → target → macros. This is **the** single entry point the
UI uses; no screen may call the individual calculators directly.

```kotlin
class ObserveTodayDashboardUseCase(
    private val profileRepository: ProfileRepository,
    private val mealRepository: MealRepository,
    private val computeDailyTarget: ComputeDailyTargetUseCase,
) {
    operator fun invoke(date: LocalDate): Flow<DayRollup?>   // null while no profile
}
```
Implemented with `profileRepository.profile.flatMapLatest { … combine(mealsOn(date)) … }`.

```kotlin
class SaveMealUseCase(private val mealRepository: MealRepository) {
    suspend operator fun invoke(meal: Meal)
}

class LogWeightUseCase(
    private val weightRepository: WeightRepository,
    private val profileRepository: ProfileRepository,
) {
    // Writes the measurement AND updates the profile's current weight/fat%
    // so targets recompute immediately. This coupling is intentional.
    suspend operator fun invoke(entry: WeightEntry)
}
```

---

## 7. `core-data` specification

### 7.1 Room

`DefatDatabase` — `@Database(entities = [MealEntity, WeightEntryEntity], version = 1, exportSchema = true)`.
DB file name: `defat.db`.

```kotlin
@Entity(tableName = "meals", indices = [Index("date")])
data class MealEntity(
    @PrimaryKey val id: String,
    val loggedAtMillis: Long,
    val date: String,            // ISO-8601 "yyyy-MM-dd", local date
    val name: String,
    val kcal: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val source: String,          // MealSource.name
    val updatedAtMillis: Long,
    val remoteId: String? = null,   // D8 — Phase 1.5 sync
    val pendingSync: Boolean = true // D8
)

@Entity(tableName = "weight_entries")
data class WeightEntryEntity(
    @PrimaryKey val date: String,   // one canonical entry per day; re-log replaces
    val weightKg: Double,
    val bodyFatPct: Double?,
    val recordedAtMillis: Long,
    val source: String,             // MeasurementSource.name
    val updatedAtMillis: Long,
    val remoteId: String? = null,
    val pendingSync: Boolean = true
)
```

No `TypeConverters` needed — all columns are primitives, dates stored as ISO
strings, enums as `.name`. Conversion happens in `mapper/`.

```kotlin
@Dao interface MealDao {
    @Query("SELECT * FROM meals WHERE date = :date ORDER BY loggedAtMillis ASC")
    fun observeByDate(date: String): Flow<List<MealEntity>>
    @Query("SELECT * FROM meals WHERE id = :id") suspend fun byId(id: String): MealEntity?
    @Upsert suspend fun upsert(meal: MealEntity)
    @Query("DELETE FROM meals WHERE id = :id") suspend fun delete(id: String)
}

@Dao interface WeightDao {
    @Query("SELECT * FROM weight_entries ORDER BY date DESC LIMIT 1")
    fun observeLatest(): Flow<WeightEntryEntity?>
    @Query("SELECT * FROM weight_entries WHERE date BETWEEN :from AND :to ORDER BY date ASC")
    fun observeBetween(from: String, to: String): Flow<List<WeightEntryEntity>>
    @Upsert suspend fun upsert(entry: WeightEntryEntity)
}
```

### 7.2 DataStore (profile)

Preferences DataStore, file `profile.preferences_pb`, created via
`preferencesDataStore(name = "profile")` on the application context, provided
as a singleton from `DataStoreModule`.

Keys (`ProfileKeys.kt`):

| Key | Type | Notes |
|---|---|---|
| `sex` | String | `Sex.name` |
| `birth_date` | String | ISO `yyyy-MM-dd` |
| `height_cm` | Double | |
| `weight_kg` | Double | current weight; updated by `LogWeightUseCase` |
| `body_fat_pct` | Double | absent = unknown |
| `activity_level` | String | `ActivityLevel.name` |
| `goal_target_weight_kg` | Double | |
| `goal_target_fat_pct` | Double | optional |
| `goal_target_date` | String | optional ISO date |
| `disclaimer_accepted_at` | Long | epoch millis; absent = not accepted |
| `onboarding_complete` | Boolean | gate for the start destination |

`ProfileLocalDataSource` exposes `Flow<UserProfile?>` (null when any required
key is missing) and `suspend fun save(profile)`, `suspend fun updateBody(...)`,
`suspend fun clear()`. Reads must catch `IOException` and emit
`emptyPreferences()` (standard DataStore pattern).

### 7.3 Repositories & DI

`*RepositoryImpl` classes are `@Singleton`, constructor-injected with the DAO /
data source and an `@IoDispatcher CoroutineDispatcher`; all suspend work wrapped
in `withContext(io)`, all Flows `.flowOn(io)`.

Hilt modules (all `@InstallIn(SingletonComponent::class)`):
- `DatabaseModule` — provides `DefatDatabase` (`Room.databaseBuilder`, no
  destructive migration; `fallbackToDestructiveMigration` is **forbidden** —
  the owner's real data lives here), `MealDao`, `WeightDao`.
- `DataStoreModule` — provides `DataStore<Preferences>`.
- `DispatcherModule` — `@IoDispatcher`, `@DefaultDispatcher` qualifiers.
- `RepositoryModule` — `@Binds` interface → impl for the three repositories;
  `@Provides` for the four use cases (they are plain classes, so `@Provides`
  keeps `:core-domain` free of javax.inject if desired — **decision:** allow
  `javax.inject.Inject` constructors in `:core-domain` use cases;
  `javax.inject` is a plain JVM annotation library with no Android
  dependency, and it keeps DI boilerplate down. Add
  `implementation("javax.inject:javax.inject:1")` to `:core-domain`).

**Day rollup logic location:** computation lives in
`DayRollupCalculator` + `ObserveTodayDashboardUseCase` (domain). `core-data`
only stores rows. Nothing is cached or persisted (D2).

**Date boundary rule:** "today" = `LocalDate.now()` in the device's default
zone, resolved in the ViewModel, never inside the domain (keeps calculators
deterministic and testable). A meal's `date` is computed at save time from its
`loggedAt` instant in the device zone.

---

## 8. App layer — screens, navigation, UX

### 8.1 Navigation graph

`Routes.kt`:

```kotlin
object Routes {
    const val ONBOARDING_GRAPH = "onboarding"
    const val ONBOARDING_WELCOME  = "onboarding/welcome"
    const val ONBOARDING_BASICS   = "onboarding/basics"
    const val ONBOARDING_BODY     = "onboarding/body"
    const val ONBOARDING_ACTIVITY = "onboarding/activity"
    const val ONBOARDING_GOAL     = "onboarding/goal"
    const val ONBOARDING_SUMMARY  = "onboarding/summary"

    const val HOME        = "home"
    const val MEAL_ADD    = "meal/add"
    const val MEAL_EDIT   = "meal/edit/{mealId}"
    fun mealEdit(id: String) = "meal/edit/$id"
    const val WEIGHT_LOG  = "weight/log"
    const val PROFILE     = "profile"
}
```

Flow:

```
MainActivity
  └─ MainViewModel.startDestination : StateFlow<StartDestination?>   (null = loading)
        ├─ NEEDS_ONBOARDING → nav graph "onboarding" (start: welcome)
        └─ HOME             → "home"

welcome →(accept disclaimer)→ basics → body → activity → goal → summary
   summary →(Start / 開始)→ saves profile, then
        navigate(HOME) { popUpTo(ONBOARDING_GRAPH) { inclusive = true } }
   every step has Back; Back on welcome exits the app

home ──FAB "Add meal"──► meal/add ──save/cancel──► home
home ──tap meal row────► meal/edit/{mealId} ──save/delete/cancel──► home
home ──button "Log weight"──► weight/log ──save/cancel──► home
home ──top-bar icon────► profile ──back──► home
```

While `startDestination == null`, `MainActivity` shows a centered
`CircularProgressIndicator` (no splash-screen library).

`OnboardingViewModel` is scoped to the **onboarding nav graph** so wizard state
survives step navigation:

```kotlin
val parentEntry = remember(backStackEntry) {
    navController.getBackStackEntry(Routes.ONBOARDING_GRAPH)
}
val vm: OnboardingViewModel = hiltViewModel(parentEntry)
```

### 8.2 Screen specs

All screens are **stateless composables** taking `uiState` + lambdas; a thin
`XxxRoute` composable in the same file collects the ViewModel state with
`collectAsStateWithLifecycle()` and passes it down. Previews (`@Preview`) for
each screen with a fake state, in both a light and a dark variant.

#### S1 — `WelcomeScreen`
- App name, one-line pitch, and the **medical disclaimer** (`docs/06`): the
  app gives general fitness/nutrition guidance, is not medical advice, and
  people with medical conditions (diabetes, eating-disorder history,
  pregnancy) should consult a doctor.
- A checkbox "I understand" gating the Next button. Acceptance timestamp is
  stored in the profile (`disclaimerAcceptedAt`).

#### S2 — `BasicsScreen`
- Sex: two large segmented options (Male / Female). Helper text explains it is
  used for the metabolic formula.
- Birth date: `DatePickerDialog` (Material3 `DatePicker` in a dialog);
  displays the computed age. Validation: age 13–100.
- Height: numeric field, **cm**, 100–250.

#### S3 — `BodyScreen`
- Current weight: numeric, **kg**, 30–300, one decimal.
- Body fat %: numeric, 3–75, optional, with a switch "I don't know my body
  fat %". Helper text: knowing it makes the estimate more accurate
  (Katch-McArdle vs Mifflin-St Jeor) — say it in plain language, not by
  formula name.

#### S4 — `ActivityScreen`
- Five selectable cards, each with a title and a concrete example, e.g.
  Sedentary = "Desk job, little exercise"; Light = "Light exercise 1–3
  days/week"; Moderate = "3–5 days/week"; Active = "6–7 days/week";
  Athlete = "Twice-a-day training or physical job". Shows the multiplier as
  small secondary text (owner is a professional and will want to see it).

#### S5 — `GoalScreen`
- Target weight (kg, required), target body-fat % (optional), target date
  (optional date picker).
- Live inline feedback: total kg to lose and, if a target date is set, the
  implied weekly rate as a % of bodyweight, colour-coded via
  `WeightRateCalculator.assess(...)` — green SAFE, amber TOO_SLOW,
  red TOO_FAST with the text "Faster than 1% of bodyweight per week is not
  recommended". This is a warning, not a block.

#### S6 — `SummaryScreen`
- The payoff screen. Shows, from `ComputeDailyTargetUseCase`:
  BMR (with which method was used, in plain words: "based on your body fat %"
  / "based on height, weight and age"), TDEE, **daily calorie target**, and
  protein / fat / carbs grams.
- If `floorApplied`, show a notice: "We raised your target to the safe
  minimum (1500 / 1200 kcal)."
- Primary button: Start.

#### S7 — `HomeScreen` (the main screen)
Top bar: today's date, settings icon → Profile.
Body, in order:
1. **`CalorieRing`** — a `Canvas` composable: background arc (surfaceVariant)
   + progress arc (primary; error colour once `isOverTarget`), 270° sweep
   starting at 135°, rounded caps, stroke ~20.dp, animated with
   `animateFloatAsState`. Centre content: big `remainingKcal` number, label
   "kcal left" (or "over" when negative), and small "eaten X / target Y".
2. **Protein row** — `MacroBar` (LinearProgressIndicator + label)
   "Protein 70 / 132 g". Protein gets visual priority per `docs/04`
   (muscle retention during a cut).
3. **Carbs / Fat** — two smaller `MacroBar`s.
4. **Metabolism line** — "BMR 1666 · TDEE 2582 kcal" small text, tappable to
   an info bottom sheet explaining in plain language where these come from.
5. **Today's meals** — list of rows (name, kcal, macro chips, time). Tap →
   edit. Empty state: "No meals logged yet — tap + to add your first one."
6. **Weight card** — latest weight + fat % and its date, with a "Log weight"
   button.
FAB: Add meal.

#### S8 — `MealEditorScreen` (add + edit, one screen)
- Fields: name (text, required), kcal, protein g, carbs g, fat g, time
  (time picker, default now), date (defaults today).
- Helper action **"Calculate kcal from macros"** — fills kcal with
  `4p + 4c + 9f`; and a mismatch hint when entered kcal differs from the macro
  sum by more than 15 % ("Your calories and macros don't match — check the
  numbers"), mirroring the label sanity-check rule in `docs/05`.
- Save (validates: name non-blank, kcal ≥ 0, macros ≥ 0), Cancel; in edit mode
  also Delete with a confirm dialog.

#### S9 — `LogWeightScreen`
- Weight (kg, required), body fat % (optional), date (default today).
- On save: writes the entry **and** updates the profile's current weight/fat %
  (`LogWeightUseCase`), so the home target recomputes immediately. The screen
  states this: "This updates your daily target."

#### S10 — `ProfileScreen`
- Read-only summary of the profile plus an Edit action that reuses the
  onboarding step screens in "edit one field" mode — **simplification for
  Phase 1: instead of reusing the wizard, render the same inputs inline in a
  single scrolling form** with a Save button. (Fewer moving parts; the wizard
  stays a one-time flow.)
- Also shows: computed daily target block (same content as S6), the medical
  disclaimer text, app version, and a line telling the owner how to change
  the app language in system settings (D6).

### 8.3 Theme

Material 3. `Theme.kt` supports dynamic colour on API 31+ and falls back to a
brand scheme. Brand seed: a deep teal-green primary (health/progress) with an
amber secondary for warnings; define explicit light and dark
`ColorScheme`s in `Color.kt`. Typography: default Material 3 type scale with
an enlarged `displayMedium` used for the ring's centre number.

### 8.4 Strings — bilingual requirement

- `res/values/strings.xml` — English (default).
- `res/values-zh-rHK/strings.xml` — Traditional Chinese, Hong Kong.
- **Every** user-visible string is a resource. No string literals in
  composables (lint `MissingTranslation` is an error, so a missing zh-rHK
  entry fails CI).
- Numbers/units are formatted with `stringResource(R.string.x, value)` and
  `%1$d` / `%1$.1f` placeholders — never string concatenation.
- Tone for 繁中: written Traditional Chinese as used in HK fitness coaching;
  supportive, never shaming (`docs/05` tone guide).

Required string keys (non-exhaustive but covering every screen; Sonnet adds
what else it needs, always in both files):

| Key | English | 繁中 (zh-rHK) |
|---|---|---|
| `app_name` | DeFAT | DeFAT |
| `common_next` | Next | 下一步 |
| `common_back` | Back | 返回 |
| `common_save` | Save | 儲存 |
| `common_cancel` | Cancel | 取消 |
| `common_delete` | Delete | 刪除 |
| `common_start` | Start | 開始 |
| `unit_kg` | kg | 公斤 |
| `unit_cm` | cm | 厘米 |
| `unit_kcal` | kcal | 千卡 |
| `unit_g` | g | 克 |
| `onboarding_welcome_title` | Welcome to DeFAT | 歡迎使用 DeFAT |
| `onboarding_welcome_body` | Set up your profile and we'll work out your daily calorie and protein targets. | 設定你嘅個人資料，我哋會計出你每日嘅卡路里同蛋白質目標。 |
| `disclaimer_title` | Before we start | 開始之前 |
| `disclaimer_body` | DeFAT provides general fitness and nutrition guidance. It is not medical advice and does not diagnose or treat any condition. If you have a medical condition (including diabetes, a history of eating disorders, or pregnancy), please consult a doctor first. | DeFAT 只提供一般健身及營養建議，並非醫療意見，亦不會診斷或治療任何疾病。如你有任何健康狀況（包括糖尿病、飲食失調病史或懷孕），請先諮詢醫生。 |
| `disclaimer_accept` | I understand | 我明白 |
| `basics_title` | About you | 關於你 |
| `basics_sex` | Sex | 性別 |
| `sex_male` | Male | 男 |
| `sex_female` | Female | 女 |
| `basics_birth_date` | Date of birth | 出生日期 |
| `basics_age_years` | %1$d years old | %1$d 歲 |
| `basics_height` | Height (cm) | 身高（厘米） |
| `body_title` | Your body right now | 你現時嘅身體數據 |
| `body_weight` | Weight (kg) | 體重（公斤） |
| `body_fat_pct` | Body fat % | 體脂率 % |
| `body_fat_unknown` | I don't know my body fat % | 我唔知自己嘅體脂率 |
| `body_fat_hint` | If you know your body fat %, your estimate will be more accurate. | 如果知道體脂率，估算會更準確。 |
| `activity_title` | How active are you? | 你嘅日常活動量？ |
| `activity_sedentary` / `_desc` | Sedentary / Desk job, little or no exercise | 久坐 / 文職，幾乎冇運動 |
| `activity_light` / `_desc` | Lightly active / Light exercise 1–3 days a week | 輕度活躍 / 每週運動 1–3 日 |
| `activity_moderate` / `_desc` | Moderately active / Exercise 3–5 days a week | 中度活躍 / 每週運動 3–5 日 |
| `activity_active` / `_desc` | Very active / Exercise 6–7 days a week | 高度活躍 / 每週運動 6–7 日 |
| `activity_athlete` / `_desc` | Athlete / Twice-daily training or a physical job | 運動員 / 每日兩練或體力勞動 |
| `goal_title` | Your goal | 你嘅目標 |
| `goal_target_weight` | Target weight (kg) | 目標體重（公斤） |
| `goal_target_fat` | Target body fat % (optional) | 目標體脂率 %（可選） |
| `goal_target_date` | Target date (optional) | 目標日期（可選） |
| `goal_rate_safe` | About %1$.1f%% of bodyweight per week — a healthy pace. | 大約每星期減體重嘅 %1$.1f%%，速度健康。 |
| `goal_rate_fast` | That's %1$.1f%% of bodyweight per week. Losing faster than 1%% a week is not recommended. | 即係每星期減 %1$.1f%%。每星期超過 1%% 並唔建議。 |
| `goal_rate_slow` | That's %1$.1f%% per week — slower than the usual 0.5–1%% range. | 即係每星期 %1$.1f%%，比一般 0.5–1%% 慢。 |
| `summary_title` | Your daily plan | 你嘅每日計劃 |
| `summary_bmr` | Basal metabolic rate | 基礎代謝率 |
| `summary_tdee` | Total daily energy | 每日總消耗 |
| `summary_target` | Daily calorie target | 每日卡路里目標 |
| `summary_method_katch` | Based on your body fat % | 根據你嘅體脂率計算 |
| `summary_method_mifflin` | Based on your height, weight and age | 根據你嘅身高、體重同年齡計算 |
| `summary_floor_applied` | We raised your target to the safe minimum of %1$d kcal. | 已將目標調高至安全下限 %1$d 千卡。 |
| `home_kcal_left` | kcal left | 千卡可食 |
| `home_kcal_over` | kcal over | 超出千卡 |
| `home_eaten_of_target` | Eaten %1$d of %2$d kcal | 已食 %1$d / %2$d 千卡 |
| `home_protein_progress` | Protein %1$d / %2$d g | 蛋白質 %1$d / %2$d 克 |
| `home_carbs_progress` | Carbs %1$d / %2$d g | 碳水 %1$d / %2$d 克 |
| `home_fat_progress` | Fat %1$d / %2$d g | 脂肪 %1$d / %2$d 克 |
| `home_metabolism_line` | BMR %1$d · TDEE %2$d kcal | 基礎代謝 %1$d · 總消耗 %2$d 千卡 |
| `home_meals_title` | Today's meals | 今日嘅餐 |
| `home_meals_empty` | No meals logged yet — tap + to add your first one. | 仲未記錄任何一餐，撳 + 加入第一餐。 |
| `home_add_meal` | Add meal | 加一餐 |
| `home_log_weight` | Log weight | 記錄體重 |
| `home_weight_none` | No weight logged yet | 仲未記錄體重 |
| `meal_title_add` | Add meal | 加一餐 |
| `meal_title_edit` | Edit meal | 修改餐點 |
| `meal_name` | What did you eat? | 你食咗咩？ |
| `meal_kcal` | Calories (kcal) | 卡路里（千卡） |
| `meal_protein` | Protein (g) | 蛋白質（克） |
| `meal_carbs` | Carbs (g) | 碳水化合物（克） |
| `meal_fat` | Fat (g) | 脂肪（克） |
| `meal_time` | Time | 時間 |
| `meal_calc_from_macros` | Calculate calories from macros | 由營養素計算卡路里 |
| `meal_macro_mismatch` | Your calories and macros don't match — please check. | 卡路里同營養素對唔上，請檢查一下。 |
| `meal_delete_confirm` | Delete this meal? | 確定刪除呢一餐？ |
| `weight_title` | Log weight | 記錄體重 |
| `weight_updates_target` | This updates your daily target. | 呢個會更新你嘅每日目標。 |
| `profile_title` | Profile | 個人資料 |
| `profile_language_hint` | To change the app language, open Android Settings → Apps → DeFAT → Language. | 想轉語言，可以去 Android 設定 → 應用程式 → DeFAT → 語言。 |
| `profile_version` | Version %1$s | 版本 %1$s |
| `error_required` | Required | 必填 |
| `error_out_of_range` | Enter a value between %1$s and %2$s | 請輸入 %1$s 至 %2$s 之間嘅數值 |

`res/xml/locales_config.xml`:

```xml
<locale-config xmlns:android="http://schemas.android.com/apk/res/android">
    <locale android:name="en"/>
    <locale android:name="zh-HK"/>
</locale-config>
```
referenced from `<application android:localeConfig="@xml/locales_config">`.

### 8.5 Manifest

- `<application>` with `android:name=".DefatApplication"`, `android:theme`,
  `android:icon="@mipmap/ic_launcher"`, `android:localeConfig`,
  `android:allowBackup="false"` (health data — `docs/06` on-device
  minimisation; auto-backup to Google Drive is not something Phase 1 should
  silently do).
- Single `MainActivity` with `android:exported="true"` and the LAUNCHER
  intent filter, `android:windowSoftInputMode="adjustResize"`.
- **No permissions at all** in Phase 1 (no internet, no camera). This is a
  deliberate, checkable property: the APK the owner installs cannot leak
  anything.

### 8.6 ViewModels

Pattern for all: `@HiltViewModel`, constructor-injected use cases, expose a
single `StateFlow<XUiState>` built with `stateIn(viewModelScope,
SharingStarted.WhileSubscribed(5_000), XUiState.Loading)`; one-shot events
(navigate-back, error snackbar) via a `Channel<Event>.receiveAsFlow()`.
Input parsing (`String` → `Double?`) happens in the ViewModel, never in the
composable; the UI state holds raw text plus a validation error resource id.

---

## 9. Unit tests (mandatory)

All values below are exact; assert `Double`s with `assertEquals(expected,
actual, 1e-6)` unless a looser delta is stated. Test method names use
backticks and describe behaviour.

### 9.1 `BmrCalculatorTest` — reference vectors from `docs/04`

| Test | Input | Expected |
|---|---|---|
| `katch-McArdle reference vector` | 80 kg, 25 % fat | LBM `60.0`, BMR **`1666.0`** |
| `mifflin male reference vector` | 80 kg, 175 cm, 30 y, MALE | **`1748.75`** (see D4 — `docs/04` printed 1780, which is an arithmetic error; the test comment must cite D4 and show `800 + 1093.75 − 150 + 5`) |
| `mifflin female reference vector` | 60 kg, 162 cm, 28 y, FEMALE | **`1311.5`** |
| `bmr() prefers Katch-McArdle when body fat is known` | profile with `bodyFatPct = 25.0` | method `KATCH_MCARDLE` |
| `bmr() falls back to Mifflin when body fat is null` | profile with `bodyFatPct = null` | method `MIFFLIN_ST_JEOR` |
| `rejects non-positive weight` | 0 kg | `IllegalArgumentException` |
| `rejects body fat outside 0..75` | 80 %, and −1 % | `IllegalArgumentException` |

### 9.2 `TdeeCalculatorTest`

| Test | Input | Expected |
|---|---|---|
| `activity multipliers match docs/04` | each `ActivityLevel` | 1.2 / 1.375 / 1.55 / 1.725 / 1.9 |
| `moderate multiplier on reference BMR` | 1666.0, MODERATE | **`2582.3`** |
| `sedentary on reference BMR` | 1666.0, SEDENTARY | `1999.2` |
| `wearable mode uses 1.1 baseline plus active energy` | 1666.0, active 500 | **`2332.6`** |
| `tdee() reports ACTIVITY_MULTIPLIER when no wearable data` | activeKcal = null | source `ACTIVITY_MULTIPLIER` |
| `tdee() reports WEARABLE when active energy present` | activeKcal = 500.0 | source `WEARABLE`, `2332.6` |

### 9.3 `CalorieTargetCalculatorTest`

| Test | Input | Expected |
|---|---|---|
| `default deficit is 20 percent` | tdee 2582.3, MALE | kcal **`2065.84`**, `floorApplied = false` |
| `male floor applies at 1500` | tdee 1800.0, MALE | `1500.0`, `floorApplied = true` |
| `female floor applies at 1200` | tdee 1400.0, FEMALE | `1200.0`, `floorApplied = true` |
| `trainer override can go below the floor` | tdee 1400, FEMALE, override 1000 | `1120.0`, `floorApplied = false` |
| `rejects absurd deficit fractions` | 0.6 | `IllegalArgumentException` |

### 9.4 `MacroCalculatorTest`

| Test | Input | Expected |
|---|---|---|
| `uses 2.2 g per kg LBM when body fat known` | target 2065.84, 80 kg, 25 % | protein **`132.0`**, fat **`48.0`**, carbs **`276.46`**, `carbsClamped = false` |
| `uses 1.8 g per kg bodyweight when body fat unknown` | target 2065.84, 80 kg, null | protein **`144.0`**, fat `48.0`, carbs `(2065.84−576−432)/4 = 264.46` |
| `fat floor is 0.6 g per kg` | any | fat == `0.6 * weightKg` |
| `carbs clamp to zero and flag when protein+fat exceed target` | target 800, 100 kg, 20 % | carbs `0.0`, `carbsClamped = true` |
| `energyKcal recomposes to the target when not clamped` | first row | `132*4 + 276.46*4 + 48*9 == 2065.84` (delta 1e-6) |

### 9.5 `DayRollupCalculatorTest`

| Test | Input | Expected |
|---|---|---|
| `empty day leaves the full target remaining` | no meals, target 2065.84 | intake 0, remaining `2065.84`, `mealCount 0`, `isOverTarget false` |
| `sums meals` | (600,30,80,15) + (450,40,30,12) | intake `1050.0`, protein `70.0`, carbs `110.0`, fat `27.0`, remaining `1015.84`, remaining protein `62.0` |
| `flags going over target` | one meal of 2500 kcal | remaining `−434.16`, `isOverTarget true`, `progressFraction == 1f` |
| `progressFraction is clamped to 0..1` | above | `1f` |

### 9.6 `WeightRateCalculatorTest`

| Test | Input | Expected |
|---|---|---|
| `0.6 kg per week on 80 kg is safe` | −0.6, 80 | `0.75 %`, `SAFE` |
| `1.2 kg per week on 80 kg is too fast` | −1.2, 80 | `1.5 %`, `TOO_FAST` |
| `0.2 kg per week on 80 kg is too slow` | −0.2, 80 | `0.25 %`, `TOO_SLOW` |
| `weight gain is reported as gaining` | +0.3, 80 | `GAINING` |
| `projected weekly loss uses 7700 kcal per kg` | 516.46 kcal/day | `0.4695…` kg (delta 1e-3) |

### 9.7 `MetCalculatorTest`

| Test | Input | Expected |
|---|---|---|
| `walking one hour at 70 kg` | MET 3.5, 70 kg, 1.0 h | `245.0` |
| `jogging half an hour at 70 kg` | MET 7.0, 70 kg, 0.5 h | `245.0` |
| `MET table matches docs/04` | `MetTable.ACTIVITIES` | contains exactly the 10 keys with the documented MET values |

### 9.8 `UserProfileTest`

| Test | Expected |
|---|---|
| `age is 29 the day before the birthday` | birth 1996-03-15, on 2026-03-14 → `29` |
| `age is 30 on the birthday` | on 2026-03-15 → `30` |

### 9.9 `ComputeDailyTargetUseCaseTest` + `GoldenProfileEndToEndTest`

**The golden profile** (the number the owner will eyeball in the exit test):

> Male, 80 kg, 175 cm, 30 years old, 25 % body fat, MODERATE activity.

| Stage | Value |
|---|---|
| BMR (Katch-McArdle) | `1666.0` |
| TDEE (×1.55) | `2582.3` |
| Target (−20 %) | `2065.84` → displayed **2066 kcal** |
| Protein (2.2 × 60 LBM) | `132.0 g` |
| Fat (0.6 × 80) | `48.0 g` |
| Carbs | `276.46 g` → displayed **276 g** |
| Implied daily deficit | `516.46 kcal` |
| Projected weekly loss | `≈ 0.47 kg` = `0.59 %` bodyweight → `SAFE` |

`GoldenProfileEndToEndTest` asserts this whole chain in one test, including
the display-rounded integers, so a regression anywhere in the pipeline fails
one obvious test. A second golden case with `bodyFatPct = null` asserts the
Mifflin path end to end.

### 9.10 `core-data` tests (JVM, no Robolectric)

- `MealMappersTest` — `Meal ↔ MealEntity` round-trip preserves every field;
  `LocalDate`/`Instant` ↔ ISO string / epoch millis conversions;
  enum name round-trip; unknown enum name falls back to `MANUAL` rather than
  throwing.
- `WeightMappersTest` — same, including `bodyFatPct = null`.

(Room DAO behaviour is verified by the CI `assembleDebug` compile + the
owner's device test in Phase 1; instrumented DAO tests are Phase 2.)

### 9.11 `app` tests (JVM, fakes, `kotlinx-coroutines-test`)

- `OnboardingViewModelTest` — validation gates (Next disabled until a step is
  valid); "I don't know my body fat" clears the value; completing the wizard
  calls `ProfileRepository.save` exactly once with the expected `UserProfile`;
  the summary state exposes the golden-profile numbers.
- `HomeViewModelTest` — with a fake profile + two fake meals, the emitted
  `HomeUiState` carries intake `1050`, remaining `1016` (rounded), protein
  `70/132`; with no profile it stays in `Loading`/`NeedsOnboarding`.
- `MealEditorViewModelTest` — "calculate kcal from macros" fills `4p+4c+9f`;
  the >15 % mismatch warning triggers and clears; blank name blocks save;
  edit mode loads the existing meal and saves with the same id.

---

## 10. CI workflow spec

`.github/workflows/android-ci.yml`:

```yaml
name: Android CI

on:
  push:
    branches: [ main ]
  pull_request:
  workflow_dispatch:

permissions:
  contents: read

concurrency:
  group: android-ci-${{ github.ref }}
  cancel-in-progress: true

jobs:
  build:
    name: Test and build debug APK
    runs-on: ubuntu-latest
    timeout-minutes: 40
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'

      - name: Set up Android SDK
        uses: android-actions/setup-android@v3

      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Domain unit tests (pure Kotlin)
        run: ./gradlew --no-daemon :core-domain:test

      - name: Android unit tests
        run: ./gradlew --no-daemon :core-data:testDebugUnitTest :app:testDebugUnitTest

      - name: Lint (fails on missing translations)
        run: ./gradlew --no-daemon :app:lintDebug

      - name: Build debug APK
        run: ./gradlew --no-daemon :app:assembleDebug

      - name: Upload debug APK
        uses: actions/upload-artifact@v4
        with:
          name: defat-debug-apk
          path: app/build/outputs/apk/debug/app-debug.apk
          if-no-files-found: error
          retention-days: 30

      - name: Upload test and lint reports
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: reports
          path: |
            core-domain/build/reports/tests/**
            app/build/reports/**
            core-data/build/reports/**
          if-no-files-found: ignore
          retention-days: 14
```

Notes:
- Domain tests run **first and as a separate step** so that a formula
  regression is visible at a glance in the Actions UI without opening logs.
- `android-actions/setup-android@v3` guarantees `cmdline-tools` and accepted
  licences; AGP downloads platform 36 / build-tools automatically from there.
- No secrets, no signing config, no `google-services.json` — the workflow must
  run on a fork/clean checkout with zero configuration. Verify this.
- The APK is debug-signed with the standard debug keystore, so it installs on
  the owner's Samsung after allowing "install unknown apps" for the browser or
  file manager.

---

## 11. Definition of Done

Phase 1 is done when **all** of the following hold:

**Automated**
1. `gradle --configure-on-demand :core-domain:test` passes in the dev
   container (no Android SDK required).
2. The `Android CI` workflow is green on `main`: domain tests, `core-data`
   and `app` unit tests, `lintDebug`, `assembleDebug`.
3. `defat-debug-apk` is downloadable from the workflow run.
4. Every reference vector in §9.1–9.4 is asserted by a named test, and
   `GoldenProfileEndToEndTest` passes.
5. Lint reports no `MissingTranslation` — both `values/strings.xml` and
   `values-zh-rHK/strings.xml` are complete.
6. `:core-domain` contains zero `android.*` / `androidx.*` imports
   (grep-checkable; also add a one-line note in the PR description confirming
   it).

**Manual — the roadmap exit test, run by the owner on a Samsung phone**
7. Install the APK from the CI artifact; the app opens with no crash and no
   sign-in.
8. Complete the onboarding wizard with the owner's real numbers; the summary
   screen shows a BMR/TDEE/target the owner (a nutrition professional) judges
   sensible. For the golden profile the target must read **2066 kcal,
   132 g protein, 48 g fat, 276 g carbs**.
9. On Home, the ring shows the full target remaining.
10. Add a manual meal; the ring, remaining kcal, protein bar and meal list all
    update immediately. Edit it; delete it.
11. Log a weight; the latest-weight card updates and the daily target
    recomputes from the new weight.
12. Kill and relaunch the app: the profile and the logged meal are still
    there, and the app opens directly on Home (not onboarding).
13. Switch the phone's app language to 繁體中文 (Android Settings → Apps →
    DeFAT → Language); every screen is translated, with no English left
    behind and no layout overflow.

**Docs**
14. `docs/03-roadmap.md` notes D1/D3 (Firebase → Phase 1.5) and links to this
    plan; `docs/04-health-metabolism.md` male Mifflin vector is corrected per
    D4 with a one-line rationale; `README.md` status says Phase 1 with build
    and install instructions.

---

## 12. Implementation order (task list for Sonnet)

Work in this order; each group should compile/test before moving on.
Branch: `phase1/walking-skeleton`. Keep commits grouped as below so the Opus
review pass can read the history.

1. **B-1 Build skeleton** — `.gitignore`, `settings.gradle.kts`, root
   `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`,
   generate + commit the wrapper (§4.2).
2. **B-2 `core-domain` module** — build file, models, constants.
3. **B-3 Calculators** — §6.3–6.10, each with its tests written **in the same
   commit** (§9.1–9.8). Run `gradle --configure-on-demand :core-domain:test`
   until green. *This is the highest-value commit in the phase; do not rush
   it and do not proceed until every reference vector passes.*
4. **B-4 Repository interfaces + use cases** + `ComputeDailyTargetUseCaseTest`
   and `GoldenProfileEndToEndTest`.
5. **B-5 CI workflow** — add `.github/workflows/android-ci.yml` now, before
   any Android code, so the very first Android compile is validated on a
   machine that actually has the SDK. Push and confirm the domain-test step is
   green.
6. **B-6 `core-data`** — build file, entities, DAOs, DB, DataStore, mappers
   (+ mapper tests), repository impls, Hilt modules. Commit the generated
   `schemas/…/1.json`.
7. **B-7 `app` shell** — build file, manifest, Application, MainActivity,
   MainViewModel, theme, launcher icon, `locales_config.xml`, both
   `strings.xml` files (write the full key set up front; it is far easier
   than retrofitting translations).
8. **B-8 Navigation + onboarding** — routes, NavHost, `OnboardingViewModel`
   and the six wizard screens, `OnboardingViewModelTest`.
9. **B-9 Home** — `CalorieRing`, `MacroBar`, `HomeViewModel`, `HomeScreen`,
   `HomeViewModelTest`.
10. **B-10 Meal editor + weight logging** — screens, ViewModels,
    `MealEditorViewModelTest`.
11. **B-11 Profile screen.**
12. **D-1 Docs** — update `docs/03-roadmap.md`, `docs/04-health-metabolism.md`
    (D4 fix), `README.md`; add the "how the owner installs the APK" section to
    the README.
13. **B-12 Final CI run + PR** — PR description lists: the D4 formula
    correction with the arithmetic shown, the Firebase deferral (D1), the
    link to the green workflow run and the APK artifact.

---

## 13. Owner actions needed

Written for a non-programmer; the implementer should reproduce this list in
the PR description.

1. **Turn on GitHub Actions** for the repository (Settings → Actions → General
   → "Allow all actions"). Without this, no APK gets built.
2. **Download and install the app**: open the repo on GitHub → *Actions* tab →
   click the newest green run → scroll to *Artifacts* → download
   `defat-debug-apk` → it arrives as a `.zip`; unzip it on the phone → tap
   `app-debug.apk` → Android will ask to allow installing unknown apps from
   the file manager/browser → allow → Install. (This is a test build, not from
   the Play Store; that is expected and normal for now.)
3. **Confirm the metabolism-formula correction (D4).** `docs/04` lists the
   Mifflin-St Jeor example for a male 80 kg / 175 cm / 30 y as 1780 kcal, but
   the formula in the same document gives
   `10×80 + 6.25×175 − 5×30 + 5 = 800 + 1093.75 − 150 + 5 = 1748.75`.
   The code follows the formula (1748.75) and the doc will be corrected.
   Please confirm this is right — you are the nutrition authority here.
4. **Review the MET table** in `core-domain/.../MetCalculator.kt`. `docs/04`
   gives weight training as a 3.5–6 range; the code uses 5.0. Tell us if you
   want a different value, and add any activities your students actually do.
5. **Sanity-check your own daily target** on the summary screen during the
   exit test. If it looks wrong for a real client, say so before Phase 2 — the
   deficit % (20 %) and the calorie floors (1500 M / 1200 F) are single
   constants we can tune.
6. **Later (Phase 1.5), create the Firebase project** — this needs your Google
   account and cannot be done for you. We will write
   `docs/guides/firebase-setup.md` with click-by-click steps before you need
   it. In short: create a project named DeFAT, choose region `asia-east2`
   (Hong Kong), add an Android app with package name `com.defat.app`,
   download `google-services.json` and send it to us privately (it must never
   be committed to GitHub — `docs/06`).
7. **Nothing to do about the Play Store yet.** The Health Connect declaration
   and privacy policy come in Phase 2/5.

---

## 14. Phase 1.5 preview (not in scope, listed so nothing is forgotten)

When the owner has created the Firebase project:
- Add `google-services.json` (gitignored) + a committed
  `google-services.example.json`, and a CI strategy that keeps the debug build
  working without secrets (e.g. a placeholder file generated in CI, or a
  product flavour that skips Firebase).
- Firebase Auth (email + Google) with a sign-in screen; the local profile is
  claimed by the signing-in user.
- Firestore mirrors of the Room data using the `remoteId` / `pendingSync`
  columns already present (D8) — one-time upload of existing local data on
  first sign-in, then bidirectional sync.
- Firestore security rules + emulator rule tests (`docs/06`, moved from
  Phase 1 per D3).
- `docs/guides/firebase-setup.md` for the owner.

**Version pin changes** (append here if the fallback protocol in §4.3 is used):
*none yet.*
