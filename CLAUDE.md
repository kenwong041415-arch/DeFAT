# CLAUDE.md — Instructions for AI Development Sessions

DeFAT is a fat-loss coaching app built by a personal trainer / nutrition
professional for their students. Android first (primarily Samsung devices),
iOS later. The project owner is NOT a programmer — explain decisions in
plain language and never assume they can debug code themselves.

## Read these first

All critical decisions live in `docs/`. Do not re-litigate them casually;
if a decision must change, update the doc in the same PR and explain why.

| Doc | What it decides |
|---|---|
| `docs/00-blueprint.md` | Product vision, users, feature list, success criteria |
| `docs/01-tech-decisions.md` | Stack: Kotlin + Compose, Firebase, Health Connect, Claude API |
| `docs/02-architecture.md` | App layers, data model, backend shape |
| `docs/03-roadmap.md` | Phased delivery plan; what is in/out of each phase |
| `docs/04-health-metabolism.md` | BMR/TDEE formulas, wearable data rules, double-counting guard |
| `docs/05-ai-nutrition.md` | Food photo / label / menu AI pipeline, prompts, cost control |
| `docs/06-privacy-compliance.md` | Health-data privacy, PDPO, Google Play health policy |

## Model roles (set by the project owner)

- **Opus** — big-picture work: feature design, implementation plans for each
  phase, and periodic review of code and logic written by Sonnet. Start each
  phase with an Opus planning pass; end each phase with an Opus review pass.
- **Sonnet** — implementation: writing Kotlin/Compose code, Firebase config,
  tests, and other hands-on coding against an approved plan.
- Reviews should check: correctness of nutrition/metabolism math against
  `docs/04`, no health-data leaks, and adherence to the architecture doc.

## Working rules

- Language: code, commits, and docs in English. Chat replies to the owner in
  Cantonese (廣東話). User-facing app strings must support both English and
  Traditional Chinese (`values/` + `values-zh-rTW/` or `values-zh-rHK/`).
- Branch per feature, PR into `main`, keep PRs small enough to review.
- All calorie/macro math must live in pure, unit-tested Kotlin functions
  (no formulas buried inside UI code).
- Never hardcode API keys in the app. All Claude API calls go through the
  backend proxy (see `docs/02` and `docs/05`).
- Health data is sensitive: follow `docs/06` before adding any new
  collection, storage, or sharing of user health data.
