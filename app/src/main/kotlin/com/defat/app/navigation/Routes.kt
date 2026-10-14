package com.defat.app.navigation

object Routes {
    // Phase 1.5: shown ahead of onboarding whenever nobody is signed in.
    const val AUTH_GRAPH = "auth"
    const val AUTH_SIGN_IN = "auth/sign_in"
    const val AUTH_SIGN_UP = "auth/sign_up"
    const val AUTH_FORGOT = "auth/forgot"

    /** Signed in but the first sync never finished; see StartDestinationPolicy. Outside AUTH_GRAPH on purpose. */
    const val AUTH_FINISH = "auth_finish"

    private val publicAuthRoutes = setOf(AUTH_SIGN_IN, AUTH_SIGN_UP, AUTH_FORGOT)
    fun isPublicAuthRoute(route: String?): Boolean = route in publicAuthRoutes

    const val ONBOARDING_GRAPH = "onboarding"
    const val ONBOARDING_WELCOME = "onboarding/welcome"
    const val ONBOARDING_BASICS = "onboarding/basics"
    const val ONBOARDING_BODY = "onboarding/body"
    const val ONBOARDING_ACTIVITY = "onboarding/activity"
    const val ONBOARDING_GOAL = "onboarding/goal"
    const val ONBOARDING_SUMMARY = "onboarding/summary"

    const val HOME = "home"
    const val HISTORY = "history"
    const val PROGRESS = "progress"
    const val MEAL_ADD = "meal/add"
    const val MEAL_EDIT = "meal/edit/{mealId}"
    fun mealEdit(id: String) = "meal/edit/$id"
    const val WEIGHT_LOG = "weight/log"
    const val PROFILE = "profile"
    const val FOOD_LIBRARY = "profile/foods"
    const val SOURCES = "profile/sources"
}
