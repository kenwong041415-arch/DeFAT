# 01 — Critical Technology Decisions

These are the load-bearing choices. Each lists the decision, why, and what
was rejected. Changing any of these later is expensive — challenge them in
a doc update, not silently in code.

## D1. App platform: Native Android — Kotlin + Jetpack Compose

- **Why**: First target is Android (mainly Samsung). Health Connect,
  CameraX, and notifications are all first-class in native Android.
  Kotlin + Compose is Google's current standard, has the best Claude/AI
  tooling support, and biggest talent/documentation pool.
- **iOS path**: business logic (calorie math, models) should be kept in
  plain Kotlin modules so a future move to Kotlin Multiplatform can share
  it with iOS. UI would be rebuilt in SwiftUI at that point.
- **Rejected**: Flutter/React Native — weaker Health Connect and camera
  integration, an extra abstraction layer to debug, and no real benefit
  until iOS is actually in scope.
- Min SDK: **API 28 (Android 9)**; Health Connect requires API 28+, and it
  covers essentially all active Samsung devices.

## D2. Wearables: Android Health Connect as the single integration point

- **Why**: Samsung Health, Fitbit, and Xiaomi Mi Fitness all sync their
  data INTO Health Connect on Android. Integrating Health Connect once
  gives us all three brands (plus Garmin, Oura, etc.) with one API and one
  permission model. We never talk to each vendor's cloud API directly.
- **Consequence**: Students must install/enable the vendor's own app and
  turn on its Health Connect sync — onboarding must include per-brand
  setup guides (Samsung / Xiaomi / Fitbit) with screenshots.
- **Apple Health**: only reachable from an iOS app; deferred to iOS phase.
- **Rejected**: per-vendor cloud APIs (Fitbit Web API, Samsung Health SDK
  partnership, Xiaomi open platform) — three integrations, approval
  processes, OAuth servers, and rate limits, for data Health Connect
  already delivers on-device.

## D3. Backend: Firebase (Auth + Firestore + Cloud Functions + Storage + FCM)

- **Why**: no server to operate (owner is not a programmer), generous free
  tier, offline-first sync built into Firestore, push notifications via
  FCM, and Cloud Functions gives us the required server-side proxy for the
  Claude API. Fastest credible path to a working product.
- **Region**: use an Asia region (e.g. `asia-east2`, Hong Kong) for data
  residency and latency.
- **Rejected**: custom Node/Go backend (operational burden), Supabase
  (viable, but weaker Android offline sync and FCM integration).

## D4. AI: Claude API via a Cloud Functions proxy — never from the app

- **Why a proxy**: an API key shipped inside an APK can be extracted and
  abused. All AI calls go app → Cloud Function (authenticated via Firebase
  Auth) → Claude API. The function enforces per-user daily quotas.
- **Model routing** (cost control):
  - Food photo / label / menu analysis (vision): **Claude Sonnet** class.
  - Text-description meal estimation, daily progress comments:
    **Claude Haiku** class.
  - Always request structured JSON output against a fixed schema.
- Details, prompts, and cost estimates: `docs/05-ai-nutrition.md`.

## D5. Nutrition math is deterministic code, not AI

- BMR, TDEE, calorie targets, macro splits, trend smoothing, and plateau
  detection are pure Kotlin functions with unit tests (`docs/04`). AI only
  estimates what's on the plate; the arithmetic is always ours. This keeps
  results reproducible and reviewable by the owner.

## D6. Language & units

- App strings: English + Traditional Chinese (Hong Kong) from day one.
- Units: metric default (kg, cm, kcal); imperial toggle later if requested.

## D7. Distribution

- Google Play, closed testing track first (the owner invites students by
  email), then open. Note: Google Play requires apps using Health Connect
  to complete a health-apps declaration — budget time for review
  (`docs/06`).
