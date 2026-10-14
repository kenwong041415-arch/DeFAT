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
   Google Play and basic decency. Implemented in Phase 1.5: export is a local
   JSON file; delete re-checks the password, empties every subcollection in
   `SYNCED_SUBCOLLECTIONS` (the list sync also uses, so a new collection
   cannot be forgotten), verifies with the server that nothing is left,
   deletes the Auth user, then clears the phone. Photos join the delete in
   the phase that adds them.
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
  read-only on assigned students; deny-all default. Rules have their own
  tests against the Firebase emulator (`firestore-tests/`, CI workflow
  `firestore-rules.yml`), most of them *negative*: another signed-in user,
  an unauthenticated client, a path outside `users/`, and a payload that
  claims someone else's uid are each shown to be denied. Ownership is always
  the uid in the document **path**, never a field in the payload.
  `firestore.rules` is deployed by the owner (console or CLI), not by CI.
- **The Firebase API key is public by design.** The repository is public and
  the test APK is a login-free release asset, so the key in `google-services.json`
  ships to anyone. Security rules are therefore the *only* control on the data
  until App Check exists.
- **Accepted gap — App Check is not enabled (Phase 1.5 decision D5).** Its Play
  Integrity provider wants Play Store distribution and the debug provider
  needs a token registered per install; that friction buys little while the
  owner is the only user. **Closing condition: App Check must be enabled
  before any student other than the owner uses the app.**
- **No `role` field is written, on purpose (Phase 1.5 review pass).** The plan
  had the client write `role: "student"` into the profile document so Phase 5
  would need no migration. It was removed: the rules let a user write their own
  profile, so a client-written `role` is a field the user controls, and a
  Phase 5 that granted trainer access from it would hand that access out on
  request. Leaving it out means there is nothing there to mistake for an
  authority. **Phase 5 must set the role from something a client cannot
  write** — a custom auth claim, or a document only the Admin SDK writes.
- **Accepted gap — sign-up is open, and the API key is public.** Anyone who
  pulls the key out of the APK can create an account and write into their own
  tree. The rules keep them out of *other* people's data, which is the part
  that matters, but they can still consume the project's free-tier quota and so
  degrade the service for real users. Spark offers no way to close this; the
  real controls are App Check and a Blaze budget alert. **Same closing
  condition as App Check: before any student other than the owner uses the
  app.**
- **Accepted gap — a deleted row keeps its contents in the cloud until the
  account is deleted.** Deletes sync as tombstones (`deletedAt`) rather than
  hard deletes, because a hard delete on one phone is indistinguishable from a
  row the other phone has not seen yet, and the row would come back. The
  tombstone keeps the original fields, so a meal the student deleted is still
  stored — in their own private tree, readable only by them, and removed by
  "delete my account". Stripping the fields on upload was considered and not
  done in this phase: applying a field-less tombstone needs a new port
  operation on every collection, in the two modules this container cannot
  compile, for a modest gain on data that never leaves the owner's tree.
  **Closing condition: before Phase 5 closed testing**, either strip tombstone
  payloads on upload or add a purge of tombstones older than 30 days.
- **Account data on the phone.** Firestore's own local cache is set to memory
  only, so cloud sync adds no second on-device copy of health data beyond
  Room. The "Export my data" file is written to the app's private cache,
  shared through FileProvider, and deleted at the next app start.
- Claude API key in Secret Manager only; Cloud Functions enforce auth +
  quota (`docs/05`).
- No secrets, release keystores, or `google-services.json` with production
  keys committed to git (`.gitignore` them; keep a
  `google-services.example.json`).
- **Deliberate exception:** `app/debug.keystore` *is* committed. It holds the
  standard `android`/`android` debug credentials, carries no secret value and
  can never sign a Play release. Without it, every CI run signs the test APK
  with a fresh random key, Android refuses to install over the previous build,
  and the tester has to uninstall — wiping their logged health data, which
  `android:allowBackup="false"` makes unrecoverable. Committing it protects
  user data rather than exposing it. The **release** keystore is a different
  matter: it must never be committed and will live in GitHub Actions secrets
  from Phase 5.

## Medical disclaimer

Onboarding + settings show: the app provides general fitness and nutrition
guidance, is not medical advice, and users with medical conditions
(diabetes, eating-disorder history, pregnancy, etc.) should consult a
doctor. Target floors in `docs/04` are a hard guard, not a suggestion.
