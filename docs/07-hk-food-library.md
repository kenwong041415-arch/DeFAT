# 07 — Hong Kong Food Library (食物庫)

This doc decides **where the app's food numbers come from, how much we
trust each one, and who is allowed to change them**. It exists because the
research below produced a clear negative result: there is no single
trustworthy public list of Hong Kong restaurant-food macros that we can
ship as fact.

## 1. What the research found

Asked to build a 40–60 item Hong Kong food list from online sources
(茶餐廳 / 酒樓 / 快餐店 / 連鎖店), the honest finding is:

**a) The authoritative source exists but we cannot reach it.**
The Centre for Food Safety (食物安全中心) runs the official
*Nutrient Information Inquiry* / Food Composition Database, which has
covered 1000+ local foods since 2007 and is what every dietitian cites.
`cfs.gov.hk` is **blocked by this development container's egress policy**
(the proxy returns `403` on CONNECT), as is every restaurant chain's own
site. So the numbers below are *not* sourced from CFS directly; they are
sourced from media and KOL articles that quote it, second-hand.

**b) The KOL / media lists disagree with each other, badly.**
Not by rounding — by up to 2×. Measured across the sources listed in §5:

| Item | Published values found | Spread |
|---|---|---|
| 西多士 | 388 / 522 / 850 kcal | **2.2×** |
| 餐蛋麵 | 600 / 750 / 785 kcal | 1.3× |
| 叉燒飯 | 652 (瘦) / 720 / 1000 kcal | 1.5× |
| 乾炒牛河 | 850 / 970 kcal | 1.14× |
| 鳳爪 | 63 / 240 kcal | **3.8×** |
| 叉燒包 | 137 / 160 / 240 kcal | 1.75× |
| 港式奶茶 | 112 / ~250 kcal (全糖凍) | 2.2× |
| 譚仔清湯湯底 | 207 / 330 kcal | 1.6× |

Causes, in order of size: **portion is never stated** (one 碟頭飯 can be
450 g or 700 g), per-piece vs per-basket confusion on dim sum, sugar and
condensed-milk assumptions on drinks, and 走汁/去皮 variants counted
inconsistently. Several articles also recycle each other's numbers, so
"three sources agree" often means one original estimate.

**c) Protein is almost never published.** These articles are written for
weight-loss readers, so they print kcal and sometimes fat. DeFAT's whole
coaching value is the protein target — and the macro columns simply are
not there for most dishes.

**d) One source is usable as-is.** A dietitian pair (Gavin & Stephy, via
星島) published 16 茶餐廳 粉麵 with the **full macro set per serving**
(e.g. 牛腩湯河粉 670 kcal / 40 g P / 86 g C / 19 g F). Internally
consistent, one methodology. Worth more than ten calorie-only lists.

**e) Chain fast food is the one solid tier.** McDonald's HK publishes a
nutrition PDF with complete macros. Café de Coral / Fairwood / Tam Jai do
not publish per-dish macros, so their items are estimates like everything
else.

## 2. The decision this forces

**We do not hardcode a "correct" HK food table.** Shipping a single number
for 西多士 when the real published range is 388–850 kcal would be a
coaching error dressed up as precision, and it is the owner's professional
name on it.

Instead the food library is a **typed, sourced, owner-editable table**:

```
FoodItem
  id
  nameZh, nameEn
  portionLabel      "1 碗", "1 件", "1 碟 (約 500g)"
  portionGrams      nullable — null when the dish has no honest weight
  kcal, proteinG, carbsG, fatG
  kcalLow, kcalHigh nullable — the published spread, when sources disagree
  source            OFFICIAL | COMPOSITION_TABLE | DIETITIAN | MEDIA | TRAINER | USER
  confidence        HIGH | MEDIUM | LOW      (derived from source, stored)
  category          TEA_RESTAURANT | DIM_SUM | ROAST_RICE | NOODLE | FAST_FOOD |
                    DRINK | STAPLE | SNACK
  isSeed            true for shipped rows, false for owner/user additions
  editedByOwner     true once the owner overrides a seed row
```

Rules that follow from it:

1. **Every number on screen carries its confidence.** A `LOW` row shows a
   「估算」badge and, where we have one, the range. No silent precision.
2. **The owner can edit any seed row**, and their edit wins forever
   (`editedByOwner`) — a later app update never overwrites it. The owner is
   the nutrition authority; the seed is a starting point, not doctrine.
3. **Students can add their own items** (`source = USER`), private to them.
4. **A `LOW`-confidence item never silently drives a coaching comment.**
   The day rollup may use it, but plateau/compliance logic must treat a day
   dominated by `LOW` rows as low-evidence (Phase 4).
5. **Phase 3 AI estimation is the real answer for the long tail.** The seed
   list exists to make the first 20 seconds of logging fast for the dishes
   the owner's students actually eat every week — not to be a database.
6. **If CFS becomes reachable** (a session without the egress restriction, or
   the owner downloading the data once), we re-seed the `MEDIUM`/`LOW` rows
   from it and promote them. That is tracked as a Phase 4 task, not a
   blocker.

## 3. Confidence tiers

| Tier | Source | What it means | Shown as |
|---|---|---|---|
| HIGH | `OFFICIAL` — chain's own published nutrition data | Manufacturer-declared, full macros | no badge |
| HIGH | `TRAINER` — the owner entered or corrected it | The authority for this app | 「教練」 |
| MEDIUM | `COMPOSITION_TABLE` — standard food-composition values for single ingredients/staples | Stable, widely agreed; portion still estimated | 「約」 |
| MEDIUM | `DIETITIAN` — a registered dietitian's published set with full macros and one methodology | Best available for composite dishes | 「估算」 |
| LOW | `MEDIA` — calorie-only press/KOL list, or sources disagreeing >25% | Order-of-magnitude only | 「估算」+ range |
| — | `USER` — student's own entry | Their own number | 「自訂」 |

## 4. Seed list

**Status: DRAFT — awaiting the owner's review.** Protein/carb/fat are blank
where no source published them; those rows ship kcal-only and the editor
prompts for macros. The owner should expect to correct a large share of
Tier B and is the final word on all of it.

### 4A — Tier HIGH / MEDIUM (chains + staples)

| Food | Portion | kcal | P | C | F | Source |
|---|---|---|---|---|---|---|
| 巨無霸 Big Mac | 1 個 | 496 | 25.6 | 42.7 | 24.6 | OFFICIAL |
| 麥樂雞 (6 件) | 120 g | 316 | 18.3 | — | — | OFFICIAL |
| 麥樂雞 (4 件) | 4 件 | 211 | 12.2 | — | — | OFFICIAL |
| 麥香魚 | 1 個 | 321 | — | — | — | OFFICIAL |
| 薯條 (中) | 1 包 | 313 | — | — | — | OFFICIAL |
| 薯條 (細) | 1 包 | 225 | — | — | — | OFFICIAL |
| 蘋果批 | 1 件 | 228 | — | — | — | OFFICIAL |
| 粒粒粟米杯 | 1 杯 | 81 | — | — | — | OFFICIAL |
| 脆香雞翼 | 2 件 | 242 | — | — | — | OFFICIAL |
| 白飯 | 1 碗 (約 200 g 熟) | 260 | 5 | 58 | 0.6 | COMPOSITION_TABLE |
| 雞蛋 | 1 隻 (50 g) | 72 | 6.3 | 0.4 | 4.8 | COMPOSITION_TABLE |
| 雞胸肉 (熟, 無皮) | 100 g | 165 | 31 | 0 | 3.6 | COMPOSITION_TABLE |
| 午餐肉 | 1 片 | 144 | — | — | — | MEDIA |
| 米線 (熟) | 1 碗 (約 200 g) | 280 | — | — | — | MEDIA |
| 魚蛋 | 1 粒 | 21 | — | — | — | MEDIA |

### 4B — Tier DIETITIAN (full macros, one methodology)

From the Gavin & Stephy 16-item 粉麵 set. Only 牛腩湯河粉 came through the
research with all four numbers intact; **the remaining 15 rows of this set
must be read off the original article before they ship** — see §6.

| Food | Portion | kcal | P | C | F | Source |
|---|---|---|---|---|---|---|
| 牛腩湯河粉 | 1 碗 | 670 | 40 | 86 | 19 | DIETITIAN |
| 干燒伊麵 | 1 碟 | ~1300 | — | — | — | DIETITIAN |

### 4C — Tier LOW (estimate + published range)

Ship these with a range and an 「估算」 badge, or hold them until the owner
replaces the number.

| Food | Portion | kcal (range) | P | Note |
|---|---|---|---|---|
| 乾炒牛河 | 1 碟 | 850–970 | — | |
| 西炒飯 | 1 碟 | ~1000 | — | reported above 乾炒牛河 |
| 焗豬扒飯 | 1 碟 (約 650 g) | 1235–1284 | — | |
| 咖喱牛腩飯 | 1 碟 | ~1300 | — | |
| 叉燒飯 (半肥瘦) | 1 碟 | 720–1000 | — | |
| 叉燒飯 (瘦, 走汁) | 1 碟 | 500–652 | — | |
| 燒肉飯 | 1 碟 | ~930 | 36 | |
| 燒鵝飯 | 1 碟 | ~970 | — | |
| 豉油雞飯 | 1 碟 | ~770 | 27 | |
| 白切雞飯 | 1 碟 | 500–600 | — | 去皮 |
| 雲吞麵 (湯) | 1 碗 | 380–450 | 18–22 | |
| 沙嗲牛肉麵 | 1 碗 | 650–667 | — | |
| 五香肉丁公仔麵 | 1 碗 | 710 | — | |
| 午餐肉煎蛋公仔麵 | 1 碗 | 785 | — | |
| 餐蛋麵 | 1 碗 | 600–785 | — | |
| 火腿通粉 | 1 碗 | ~310 | — | low-cal option |
| 餐蛋治 | 1 份 | 565 | — | |
| 火腿蛋治 | 1 份 | 452–480 | — | |
| 西多士 | 1 份 | **388–850** | — | widest spread; depends on 花生醬/牛油 |
| 奶油豬 | 1 個 | 344 | — | |
| 牛油多士 | 1 份 | 168 | — | |
| 炒蛋 | 1 份 | 220 | — | |
| 煎蛋 | 1 隻 | 90 | — | |
| 蛋撻 | 1 個 | 217 | — | |
| 糯米雞 | 1 隻 | ~600 | — | 810 kcal quoted with 甜豆漿 |
| 港式奶茶 (熱, 少甜) | 1 杯 | ~112 | — | |
| 凍奶茶 (全糖) | 1 杯 | ~250 | — | |
| 蝦餃 | 1 粒 | 44–50 | — | per-piece vs per-basket unclear |
| 燒賣 | 1 粒 | ~62 | — | per-piece vs per-basket unclear |
| 叉燒包 | 1 個 | 137–240 | — | |
| 腸粉 | 1 碟 | 65–100 | — | filling-dependent |
| 炸春卷 | 1 件 | ~150 | — | |
| 蒸鳳爪 | 1 碟 | **63–240** | — | 3.8× spread; hold |
| 豉汁蒸排骨 | 1 碟 | ~260 | — | |
| 馬拉糕 | 1 件 | ~370 | — | |
| 炸兩 | 1 碟 | ~450 | — | |
| 譚仔 清湯湯底 | 1 碗 | 207–330 | — | |
| 譚仔 麻辣湯底 | 1 碗 | 436–525 | — | |
| 譚仔 腩肉 | 1 份 (38 g) | 140–160 | — | 脂肪 ~12.7 g |

## 5. Sources used

Second-hand aggregators, listed so every number above can be traced and
replaced. None of these is authoritative; all were reached through web
search summaries, not by opening the pages (see §1a).

- 星島頭條 — 20 款茶餐廳食物卡路里 · 16 款粉麵 (Gavin & Stephy) · 24/30 款點心
- 香港01 — 12 款茶餐廳食品 · 20 款點心 · 燒味 9 款 · 譚仔配料
- HKET TOPick — 21 款碟頭粉麵飯 · 6 款焗飯 · 10 款點心 · 茶餐廳早餐
- FWD / Bowtie / Greenutss / GOtrip / WeekendHK / Cosmopolitan HK — 茶餐廳、麥當勞、點心、譚仔 lists
- UFood — 5 種燒味飯 (quotes 食環署 per-100 g figures)
- loseweighthk / kamadelivery / Fit Options — McDonald's HK figures traced to the chain's own nutrition PDF
- **Not reached:** `cfs.gov.hk` (official Nutrient Information Inquiry), `mcdonalds.com.hk`, `cafedecoral.com`, `fairwood.com.hk` — all blocked by egress policy

## 6. Open items for the owner

1. **Review Tier 4C.** These are the rows most likely to be wrong. The
   owner's own numbers replace them outright (`source = TRAINER`).
2. **Portion weights.** Nearly every composite dish lacks one. A 碟頭飯
   number means nothing without "約 500 g". The owner's portion convention
   is worth more than any published kcal.
3. **Protein columns.** Mostly empty, and protein is the app's main coaching
   lever. Filling these is the single highest-value thing the owner can do
   to this table.
4. **The 16-item dietitian set** (§4B) — if the owner can open the 星島
   article, reading off the remaining 15 rows gives us the best
   single-methodology block in the whole library.
5. **Dim sum counting convention** — per 粒 or per 籠. Pick one; the app
   will state it on every row.
