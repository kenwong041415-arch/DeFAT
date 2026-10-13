// Domain-only build, for running the nutrition/metabolism tests without the
// Android toolchain:
//
//     ./gradlew -c settings-domain.gradle.kts :core-domain:test
//
// Why this file exists: the main build includes :app and :core-data, whose
// build scripts need the Android Gradle Plugin from Google's Maven repo.
// Development containers for this project routinely cannot reach that repo,
// and Gradle evaluates every included project's build script during
// configuration — so a single unreachable plugin makes even a pure-Kotlin
// `:core-domain:test` impossible to run. All of the calorie, macro and BMR
// math lives in :core-domain, which is the code most worth testing before a
// push, so it gets a settings file that stands alone.
//
// This is a developer convenience only. CI uses the real settings.gradle.kts
// and builds all three modules; nothing here changes what ships.

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "DeFAT-domain"
include(":core-domain")
