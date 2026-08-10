# 00 — Project Blueprint

## Vision

DeFAT ("Defeat Fat") is a coaching companion app. A personal trainer /
nutrition professional (the owner) uses it with their students to drive
sustainable weight and fat loss. It is not a generic calorie counter: the
trainer's methodology is built in, and the trainer can see and guide each
student's progress.

## Users

1. **Student** — wants to lose weight/fat. Logs meals with minimal friction
   (photo, text, or label scan), wears a smartwatch/band (Samsung, Xiaomi,
   Fitbit, or Apple), and receives daily targets, feedback, and reminders.
2. **Trainer (owner)** — monitors all students, adjusts targets, and sends
   encouragement. Served by a simple web dashboard in a later phase.

Primary market: Hong Kong. UI in Traditional Chinese and English.

## Core features (from the owner's brief)

1. **Personal profile** — age, sex, height, weight, body-fat %, activity
   level, goal weight/fat %, target date, dietary restrictions/allergies.
2. **Wearable integration** — Samsung, Xiaomi, Fitbit on Android via
   **Health Connect** (one API covers all three); Apple Health when the iOS
   version ships. Pulls steps, active calories, exercise sessions, sleep,
   heart rate, and weight from smart scales.
3. **Daily metabolism estimate (每日新陳代謝估算)** — BMR via
   Katch-McArdle when body-fat % is known, Mifflin-St Jeor otherwise;
   TDEE from activity level plus wearable data (see `docs/04`).
4. **Daily exercise burn (每日運動消耗)** — from wearable active-energy
   data, with manual entry fallback using MET values.
5. **Meal logging with AI estimation** — calories, protein, carbs, fat per
   meal, estimated from: (a) a food photo, (b) a text/voice description,
   (c) a nutrition-label photo (OCR + serving math), or (d) manual entry /
   food database search.
6. **AI restaurant helper** — photograph a menu; the app recommends the
   best choices for the student's remaining daily budget and goals.
7. **Progress engine** — daily/weekly trend of weight, fat %, calorie
   balance; moving-average smoothing; adaptive target adjustment; plateau
   detection; progressive comments and reminders (praise, warnings, nudges).

## Additional features (owner asked "anything else you can think of")

- **Trainer dashboard (web)** — student list, red-flag alerts (missed logs,
  plateau, under-eating), broadcast and 1-to-1 messages.
- **Weekly check-in** — guided weigh-in + progress photos + measurements
  (waist, hip), stored privately for before/after comparison.
- **Streaks & gentle gamification** — logging streaks, weekly goals met.
- **Water intake tracking** and protein-target emphasis (muscle retention
  during a cut).
- **Diet-break / refeed logic** — after sustained deficit, suggest
  maintenance weeks (trainer-approved).
- **Smart reminders** — meal-time logging nudges, weigh-in reminders,
  end-of-day summary notification.
- **Offline-first logging** — logs save locally and sync when online.

## Explicit non-goals (for now)

- No social feed / community features.
- No medical claims: the app coaches lifestyle, it does not diagnose or
  treat. Show a disclaimer; advise medical consultation for extreme cases.
- No barcode-database licensing in MVP (label photo + AI covers most cases;
  a barcode lookup can be added later if needed).

## Success criteria

- A student can go from install → profile → first AI-estimated meal log in
  under 5 minutes.
- Daily logging takes under 2 minutes total.
- The trainer can answer "how is student X doing this week?" in one glance.
