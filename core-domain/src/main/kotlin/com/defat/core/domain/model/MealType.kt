package com.defat.core.domain.model

/**
 * The five meal types the owner uses with students. Declaration order is the
 * display order everywhere (Home, History, the editor's chip row) — do not
 * sort this list anywhere else.
 *
 * The owner explicitly decided on exactly these five and rejected a
 * late-night (宵夜) type. Adding one is a schema-visible change: it needs a
 * new string pair and no migration (the column is free text), but the
 * inference boundaries in [com.defat.core.domain.calc.MealTypeInference]
 * would have to change too.
 */
enum class MealType { BREAKFAST, LUNCH, AFTERNOON_TEA, DINNER, SNACK }
