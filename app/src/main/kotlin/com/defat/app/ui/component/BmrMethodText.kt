package com.defat.app.ui.component

import com.defat.app.R
import com.defat.core.domain.model.BmrMethod

/**
 * The string naming the BMR formula in use (plan §5.5). Exhaustive on purpose:
 * adding a [BmrMethod] without a label must fail the build, not show the wrong
 * formula name.
 */
fun bmrMethodLabelRes(method: BmrMethod): Int = when (method) {
    BmrMethod.KATCH_MCARDLE -> R.string.bmr_method_katch
    BmrMethod.HENRY_OXFORD -> R.string.bmr_method_henry
    BmrMethod.MIFFLIN_ST_JEOR -> R.string.bmr_method_mifflin
}
