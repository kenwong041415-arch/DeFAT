# 06 — Privacy & Compliance Checklist

Health data is the most sensitive category we touch. These rules bind
every phase; new data collection requires updating this doc first.

## Principles

1. **Minimum collection** — request only the Health Connect data types the
   current phase actually uses (`docs/04` lists them per purpose).
2. **On-device first** — raw Health Connect records stay on the phone.
   Only daily derived numbers (steps total, active kcal, weight entries the
   user logs) sync to Firestore.
3. **User owns the data** — provide in-app: export my data (JSON/CSV) and
   delete my account (wipes Firestore docs + Storage photos). Required by
   Google Play and basic decency.
4. **Trainer access is consented** — students explicitly accept trainer
   visibility during onboarding; the consent text says exactly what the
   trainer sees (logs, weight trend, photos only if shared).
5. **Photos** — meal/check-in photos are private per user in Storage with
   rules enforcing owner+trainer read only. Check-in body photos are
   student-shareable, default OFF.

## Hong Kong PDPO (個人資料（私隱）條例)

- Publish a bilingual (EN/繁中) Personal Information Collection Statement
  and privacy policy page (host on Firebase Hosting) before closed testing.
- State: what is collected, purpose (fitness coaching), who sees it
  (student + their trainer), retention (until account deletion), and that
  data is stored on Google Cloud `asia-east2`.
- No selling/sharing data with third parties, ever. AI processing: meal
  photos/text are sent to Anthropic's API for estimation — this must be in
  the policy; Anthropic API data is not used for model training by default.

## Google Play requirements (budget real calendar time for these)

- **Health Connect declaration**: apps requesting Health Connect
  permissions must complete Google's health apps declaration form and be
  approved. Apply early in Phase 2 — approval can take weeks.
- **Data-safety form**: must exactly match actual collection; mismatches
  cause rejections.
- Health Connect policy forbids using health data for ads — we don't have
  ads; keep it that way.
- Prominent disclosure + runtime permission flow before first Health
  Connect read; app must function (manual mode) if permissions denied.
- Camera permission: photos only on explicit user action.

## Security

- Firestore security rules: user reads/writes own subtree; trainer role
  read-only on assigned students; deny-all default. Rules get their own
  tests (Firebase emulator) in Phase 1.
- Claude API key in Secret Manager only; Cloud Functions enforce auth +
  quota (`docs/05`).
- No secrets, keystores, or `google-services.json` with production keys
  committed to git (`.gitignore` them; keep a `google-services.example.json`).

## Medical disclaimer

Onboarding + settings show: the app provides general fitness and nutrition
guidance, is not medical advice, and users with medical conditions
(diabetes, eating-disorder history, pregnancy, etc.) should consult a
doctor. Target floors in `docs/04` are a hard guard, not a suggestion.
