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

**b) The KOL / media lists disagree with each other, badly — but the
disagreement is mostly *real variation*, not error.** See §6: the owner
pointed out that portion and modifier choices (份量 / 少飯 / 半糖 / 走糖)
could explain the spread, and reconstructing the dishes from composition
values confirms it. That finding changes the data model, so read §6 before
treating any range below as uncertainty.

Measured across the sources listed in §5:

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

Causes, in order of size — all three quantified in §6: **modifier choices**
(走汁 / 少飯 / 瘦 / 半糖 / 走牛油), **counting unit** (per 件 vs per 籠), and
**genuine portion size** (one 碟頭飯 can be 450 g or 700 g). Several articles
also recycle each other's numbers, so "three sources agree" often means one
original estimate — but that residual sloppiness is the *smallest* of the
causes, not the largest.

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
   **Superseded in part by §6.3:** where the range is explained by modifier
   choices, we ship a base plus modifier deltas instead of a range, and the
   row's confidence rises. A range survives only for genuinely unexplained
   spread.
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

## 6. Modifiers — why the ranges exist, and the model that follows

The owner's question: could 份量 / 少飯 / 半糖 / 走糖 account for the spread
in §1b? Reconstructing each dish from composition values says **yes, for most
of it** — and that moves the spread from "uncertainty we must average away"
to "variation we can model exactly".

### 6.1 The reconstruction

Built up from standard composition estimates (white bread 70 kcal/slice, egg
72, butter 100/tbsp, peanut butter 95/tbsp, syrup 60/tbsp, sugar 16/tsp,
evaporated milk 80/60 ml, rice 260/bowl, lean 叉燒 200/100 g, 半肥瘦
320/100 g, sauce 100/2 tbsp, luncheon meat 144/slice, frying oil 88/10 g).
These are estimates, and the owner should correct them.

| Dish | Reconstructed | Published |
|---|---|---|
| 餐蛋麵, 1 slice luncheon meat / 2 slices + oil | **614** / **788** | 600 / 785 |
| 港式奶茶, hot 走糖 / iced 全糖 | **104** / **253** | 112 / ~250 |
| 西多士, 走醬走牛油走糖漿 / +PB+butter / full | **300** / **495** / **710** | 388 / 522 / 850 |
| 叉燒飯, 瘦+少飯+走汁 / 半肥瘦+汁 / 加飯+多汁 | **590** / **939** / **1068** | 652 / 720 / 1000 |

餐蛋麵 and 奶茶 land almost exactly. 西多士 and 叉燒飯 reproduce the ordering
and the magnitude with the base running low, most likely because pan-frying
absorbs more fat than the 10 g assumed. The conclusion holds: **西多士 at 388
vs 850 kcal is not one source being wrong — it is 走花生醬走牛油走糖漿 versus
the full build. The 460 kcal gap is entirely peanut butter, butter and
syrup**, all fat and sugar, all low-volume and invisible.

### 6.2 Three distinct causes, three different fixes

| Cause | Dishes | Fix |
|---|---|---|
| **Modifiers** — 少飯 / 瘦 / 走汁 / 半糖 / 走牛油 | 西多士, 叉燒飯, 奶茶, 餐蛋麵 | Model as first-class ± deltas (§6.3) |
| **Counting unit** — per 件 vs per 籠 | 鳳爪, 蝦餃, 燒賣 | Always store per 件; ask quantity |
| **Portion size** — 大包/小包, 450 g vs 700 g 碟頭飯 | 叉燒包, 碟頭飯 | Separate rows with `portionGrams` |

蒸鳳爪 is the clean counter-example and the reason the three must not be
conflated: 63 kcal ≈ 2 隻 (2 × 35 + sauce ≈ 78), 240 kcal ≈ 一籠 of 6
(6 × 35 + sauce ≈ 230). Nothing to do with 走汁 — the sources were counting
different things.

### 6.3 The model change this forces

§2 originally said a `LOW` row shows its published range and pre-fills the
midpoint. **That is now wrong** for modifier-driven spreads: it averages away
a quantity we can compute. Replace it with:

```
FoodModifier
  id
  nameZh, nameEn
  groupId                 mutually exclusive within a group
  appliesTo               List<FoodCategory> or an explicit item list
  kcalDelta, proteinDelta, carbsDelta, fatDelta     signed; may be negative
  source, confidence      same tiers as FoodItem (§3)
```

Starting groups and deltas — **all draft, all for the owner to overrule**:

| Group | Options (kcal delta) | Applies to |
|---|---|---|
| 飯量 | 少飯 −130 · 正常 0 · 加飯 +130 | ROAST_RICE, TEA_RESTAURANT rice dishes |
| 肉款 | 瘦 −120 · 半肥瘦 0 | 叉燒 dishes |
| 皮 | 去皮 −90 · 正常 0 | 油雞, 白切雞, 燒鵝 |
| 醬汁 | 走汁 −100 · 少汁 −50 · 正常 0 | ROAST_RICE, NOODLE |
| 甜度 | 走糖 −48 · 半糖 −24 · 正常 0 | DRINK |
| 奶 | 走奶 −80 · 少奶 −40 · 正常 0 | DRINK |
| 牛油 | 走牛油 −100 · 正常 0 | 西多士, 多士, 菠蘿油 |
| 花生醬 | 加花生醬 +95 · 無 0 | 西多士, 多士 |
| 加配料 | 加蛋 +72 · 加午餐肉 +144 · 加菜 +15 | NOODLE, TEA_RESTAURANT |

Rules that follow:

1. **A seed row's stored numbers are the standard as-served configuration**,
   not a midpoint and not the lightest version. Deltas are relative to that,
   so 走汁 is −100 from a row that already includes sauce. Every row must
   state its baseline in `portionLabel` (e.g. `1 碟（正常飯、半肥瘦、有汁）`).
2. **`kcalLow`/`kcalHigh` stay only for genuinely unexplained spread.** Where
   §6.1 accounts for the range, drop the range and ship the base plus
   modifiers; the row's confidence rises accordingly.
3. **Dim sum is stored per 件**, with a quantity stepper in the UI. No row
   may mean "一籠" unless `portionLabel` says so and `portionGrams` backs it.
4. **Modifier deltas are owner-editable exactly like food rows**, with the
   same permanence (`editedByOwner`).

### 6.4 Why this is worth building rather than shipping a range

- **The number matches what the student actually ate.** A midpoint is wrong
  for everyone; 走汁 + 少飯 + 瘦 is right for the student who ordered it.
- **It teaches.** The student sees `走汁 −100` on screen. Repeating the
  owner's own coaching is the product, and the competitor app does not do it.
- **It changes what the owner has to supply, and shrinks it.** Instead of ~50
  absolute numbers with ambiguous portions, the owner supplies roughly 15
  modifier deltas — the figures a trainer already knows cold — plus a
  judgement on the base portions. Composition tables can carry the bases.
- **It collapses the `LOW` tier.** Variance that is modelled is no longer
  uncertainty, so fewer rows need an 「估算」 badge at all.

## 7. Open items for the owner

§6 reduced this list. The original ask was ~50 absolute numbers with
ambiguous portions; the modifier model needs roughly 15 deltas and a
judgement on base portions instead, and composition tables can carry most of
the bases.

**Highest value first:**

1. **The modifier deltas in §6.3.** 走汁 saves how many? 少飯? 瘦 vs 半肥瘦?
   去皮? These are the figures a trainer states every day, and each one
   corrects a whole category of dishes at once rather than a single row.
2. **Base portion convention.** What counts as "正常飯" in a 碟頭飯 — 1.5
   bowls, 1.75, 2? One answer fixes every rice dish in the table. Likewise
   the standard meat weight.
3. **Protein columns** (§4A/§4C). Still mostly empty in every public source,
   and still the app's main coaching lever.
4. **Dim sum counting convention** — the app will store per 件 and ask for
   quantity (§6.2 rule 3); confirm that matches how the owner's students
   actually order.
5. **Review Tier 4C** for anything the modifier model does not already
   explain — those rows are still the most likely to be wrong, and the
   owner's numbers replace them outright (`source = TRAINER`).
6. **The 16-item dietitian set** (§4B) — if the owner can open the 星島
   article, reading off the remaining 15 rows gives us the best
   single-methodology block in the whole library.
7. **Sanity-check the composition estimates in §6.1** (bread, butter, peanut
   butter, evaporated milk, rice per bowl, 叉燒 per 100 g). They are the
   foundation under every reconstructed base.
