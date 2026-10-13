package com.defat.core.domain.calc

import com.defat.core.domain.model.UserProfile
import java.time.LocalDate

/**
 * The date to hand to the target calculation when viewing [day].
 *
 * Henry's equations are adults-only and `BmrCalculator.henryOxford` throws for
 * an age under 18. Home and History let the student look at any past day, and
 * the target for a past day is computed from the *current* profile (Phase 1.6
 * plan §2 E18), so a 19-year-old browsing to a day three years back would ask
 * for the BMR of a 16-year-old and crash. The rule: a day before the student's
 * 18th birthday is evaluated as of that birthday. This never fires for the
 * adults the wizard admits, and it never changes a day on or after the 18th
 * birthday.
 */
object TargetReferenceDate {

    fun forDay(profile: UserProfile, day: LocalDate): LocalDate {
        val eighteenth = profile.birthDate.plusYears(NutritionConstants.HENRY_MIN_ADULT_AGE.toLong())
        return if (day.isBefore(eighteenth)) eighteenth else day
    }
}
