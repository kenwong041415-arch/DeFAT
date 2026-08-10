# DeFAT — 減脂教練 App

Defeat the fat. A fat-loss coaching app built by a personal trainer &
nutrition professional for their students. Android first (Samsung-focused),
with wearable integration, AI meal estimation from photos, and a
progressive coaching engine.

## Status

**Phase 1 — Walking skeleton + core math.** Installable Android app,
local-first (no sign-in, no Firebase yet — see
[`docs/plans/phase1-implementation-plan.md`](docs/plans/phase1-implementation-plan.md)
for the full plan and its deviations from the original roadmap). The
critical decisions and plans live in [`docs/`](docs/):

1. [Blueprint](docs/00-blueprint.md) — what we're building and for whom
2. [Tech decisions](docs/01-tech-decisions.md) — stack choices and why
3. [Architecture](docs/02-architecture.md) — system design and data model
4. [Roadmap](docs/03-roadmap.md) — phases 0–5 with exit tests
5. [Metabolism rules](docs/04-health-metabolism.md) — BMR/TDEE formulas
6. [AI nutrition pipeline](docs/05-ai-nutrition.md) — photo/menu analysis
7. [Privacy & compliance](docs/06-privacy-compliance.md) — health-data rules
8. [Phase 1 plan](docs/plans/phase1-implementation-plan.md) — implementation
   spec for the current phase

AI development sessions: read [`CLAUDE.md`](CLAUDE.md) first.

## Building

Requires JDK 21. Android Studio (or the Android SDK + `ANDROID_HOME`) is
needed to build `:app` and `:core-data`; `:core-domain` is pure Kotlin/JVM
and builds without the SDK:

```
./gradlew :core-domain:test        # pure-Kotlin nutrition/metabolism math
./gradlew :app:assembleDebug       # full app (needs the Android SDK)
```

GitHub Actions (`.github/workflows/android-ci.yml`) builds and tests every
push/PR and uploads a debug APK artifact — no local Android SDK required
to get an installable build.

## Installing the app (no Play Store yet — this is a test build)

1. On GitHub, make sure Actions are enabled: **Settings → Actions →
   General → "Allow all actions"**.
2. Open the repo's **Actions** tab → click the newest green run →
   scroll down to **Artifacts** → download `defat-debug-apk`.
3. It downloads as a `.zip`. Unzip it on your phone (or unzip on a
   computer and copy `app-debug.apk` over).
4. Tap `app-debug.apk` on the phone. Android will ask permission to
   install from that app (file manager/browser) — allow it, then Install.
5. This is a debug build signed with the standard Android debug key, not
   from the Play Store — that's expected for now.
