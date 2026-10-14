# DeFAT — 減脂教練 App

Defeat the fat. A fat-loss coaching app built by a personal trainer &
nutrition professional for their students. Android first (Samsung-focused),
with wearable integration, AI meal estimation from photos, and a
progressive coaching engine.

## Status

**Phase 1.6 — UI revision + meal types.** Installable Android app,
local-first (no sign-in, no Firebase yet). Home groups the day's meals by
meal type, a navigation drawer holds weight logging, history and profile,
and the app carries its own pine/jade identity. The
critical decisions and plans live in [`docs/`](docs/):

1. [Blueprint](docs/00-blueprint.md) — what we're building and for whom
2. [Tech decisions](docs/01-tech-decisions.md) — stack choices and why
3. [Architecture](docs/02-architecture.md) — system design and data model
4. [Roadmap](docs/03-roadmap.md) — phases 0–5 with exit tests
5. [Metabolism rules](docs/04-health-metabolism.md) — BMR/TDEE formulas
6. [AI nutrition pipeline](docs/05-ai-nutrition.md) — photo/menu analysis
7. [Privacy & compliance](docs/06-privacy-compliance.md) — health-data rules
8. [Phase 1 plan](docs/plans/phase1-implementation-plan.md) — the walking
   skeleton and the nutrition math
9. [Phase 1.6 plan](docs/plans/phase1.6-ui-revision-plan.md) — implementation
   spec for the current phase

AI development sessions: read [`CLAUDE.md`](CLAUDE.md) first.

## Building

Requires JDK 17 or newer (CI uses 21); all modules target Java 17 bytecode.
Android Studio (or the Android SDK + `ANDROID_HOME`) is needed to build
`:app` and `:core-data`.

```
./gradlew :app:assembleDebug       # full app (needs the Android SDK)
./gradlew :core-domain:test        # math tests, with the SDK available

# math tests WITHOUT the Android SDK or Google's Maven repo:
./gradlew -c settings-domain.gradle.kts :core-domain:test
```

`:core-domain` is pure Kotlin/JVM, but the plain `:core-domain:test` above
still needs the Android toolchain, because Gradle evaluates `:app`'s and
`:core-data`'s build scripts during configuration and those need the Android
Gradle Plugin from Google's Maven repo. On a machine that cannot reach that
repo, use `settings-domain.gradle.kts`, which includes `:core-domain` alone.
All of the calorie, macro, BMR and TDEE math lives there, so this is the
check worth running before any push. CI uses the real `settings.gradle.kts`
and builds all three modules.

### Firebase config (`google-services.json`)

`:app` needs `app/google-services.json`, which is **gitignored** (docs/06) and
therefore missing from a fresh clone; the build stops with Google's "File
google-services.json is missing" error until it exists. Copy
[`app/google-services.example.json`](app/google-services.example.json) to
`app/google-services.json` and fill in your own Firebase project's values, or
ask the project owner for theirs. CI writes the file from the
`GOOGLE_SERVICES_JSON` repository secret, which GitHub does not give to pull
requests from forks.

The Firestore security rules (`firestore.rules`) have their own tests, which
run against the Firebase emulator and need Node and Java:
`cd firestore-tests && npm ci && npm run test:emulator`.

GitHub Actions (`.github/workflows/android-ci.yml`) builds and tests every
push/PR and uploads a debug APK artifact — no local Android SDK required
to get an installable build.

## Installing the app (no Play Store yet — this is a test build)

Every green CI run republishes the APK to the **`test-build`** pre-release,
so this link always points at the latest build and needs no GitHub login:

**https://github.com/kenwong041415-arch/DeFAT/releases/download/test-build/app-debug.apk**

1. Open that link on the phone — it downloads `app-debug.apk` directly (no
   zip, no sign-in).
2. Tap the downloaded file. Android will ask permission to install from
   your browser or file manager — allow it, then Install.
3. This is a debug build signed with the standard Android debug key, not
   from the Play Store — that's expected for now.

**Builds install over each other from 0.2.0 onward.** Every APK is signed
with the committed `app/debug.keystore`, so a new build updates the app in
place and your logged meals survive. Builds before 0.2.0 were each signed
with a different key generated on the CI runner, which is why the first
update to 0.2.0 may still refuse to install and force one uninstall — see
`docs/plans/phase1.6-ui-revision-plan.md` §3 R1.

The same APK is also attached to each workflow run as the
`defat-debug-apk` artifact, but Actions artifacts answer **404 unless you
are signed in to GitHub**, so prefer the release link above.
