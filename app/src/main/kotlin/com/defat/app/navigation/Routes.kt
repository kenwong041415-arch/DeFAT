package com.defat.app.navigation

object Routes {
    const val ONBOARDING_GRAPH = "onboarding"
    const val ONBOARDING_WELCOME = "onboarding/welcome"
    const val ONBOARDING_BASICS = "onboarding/basics"
    const val ONBOARDING_BODY = "onboarding/body"
    const val ONBOARDING_ACTIVITY = "onboarding/activity"
    const val ONBOARDING_GOAL = "onboarding/goal"
    const val ONBOARDING_SUMMARY = "onboarding/summary"

    const val HOME = "home"
    const val HISTORY = "history"
    const val MEAL_ADD = "meal/add"
    const val MEAL_EDIT = "meal/edit/{mealId}"
    fun mealEdit(id: String) = "meal/edit/$id"
    const val WEIGHT_LOG = "weight/log"
    const val PROFILE = "profile"
}
