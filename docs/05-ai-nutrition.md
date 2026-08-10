# 05 — AI Nutrition Pipeline

All AI calls run server-side in Cloud Functions (`docs/01` D4). The app
never holds the Claude API key. Every endpoint checks Firebase Auth and a
per-user daily quota (default: 30 AI calls/day) before spending tokens.

## Endpoints

### `/estimateMeal`

Input: one of `photo` (Storage ref), `text` (free description, EN/廣東話),
`label` (nutrition label photo + claimed servings).
Output (strict JSON schema — reject/retry on parse failure):

```json
{
  "items": [
    {"name": "叉燒飯", "portion": "1 碟 (~450g)",
     "kcal": 780, "proteinG": 32, "carbsG": 105, "fatG": 24}
  ],
  "totalKcal": 780, "totalProteinG": 32, "totalCarbsG": 105,
  "totalFatG": 24,
  "confidence": "medium",
  "assumptions": ["standard restaurant portion", "skin-on char siu"],
  "clarifyingQuestion": null
}
```

- Prompt must state: Hong Kong context (cha chaan teng, dim sum, dai pai
  dong portions), estimate cooked weights, when unsure prefer a single
  `clarifyingQuestion` (e.g. "有冇走飯？") over wild guessing.
- Label mode: OCR the panel, multiply by servings, sanity-check that
  macros × energy densities ≈ stated kcal (flag mismatch > 15%).
- UI always shows estimates as **editable**; user confirmation produces
  `finalValues`. Store user corrections — they are future tuning data.

### `/analyzeMenu`

Input: menu photo(s) + student context (remaining kcal & protein today,
goal, dietary flags). Output: top 3 ranked picks + items to avoid, each
with a one-line reason and modification tips (走汁/少飯/轉蒸). Reasoning
language follows the student's locale.

### `/dailyDigest` (scheduled)

For each active student at ~21:00 HKT: summarize the day (balance, protein
hit/miss, streak) into one short encouraging comment (Haiku-class model).
Tone guide: supportive coach, never shaming; celebrate compliance, frame
misses as tomorrow's plan.

## Model routing & cost control

| Task | Model class | Why |
|---|---|---|
| Food photo, label, menu | Sonnet (vision) | accuracy matters most |
| Text-description estimate | Haiku | cheap, adequate |
| Daily comments | Haiku | high volume, low complexity |

- Downscale photos client-side to ≤ 1280 px longest edge before upload
  (halves vision token cost, no accuracy loss for plates/menus).
- Cache label results by image hash (same product rescanned = free).
- Log tokens per call to Firestore for cost monitoring; alert the owner if
  monthly projection exceeds a set budget.

## Accuracy expectations (set honestly with students)

Photo estimation is ±20–30% at best — good enough for consistent trend
tracking, not lab analysis. The app should say so ("估算值，可以修改"),
and the trend engine (`docs/04`) is designed around consistent logging
rather than perfect absolute numbers.

## Failure modes

- Function timeout / model error → app falls back to manual entry, queues
  nothing silently, tells the user.
- Non-food photo → model must return an `"error": "not_food"` marker, app
  shows a friendly retry message.
- Never store AI raw responses containing anything but the JSON result.
