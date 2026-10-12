package com.defat.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.defat.app.StartDestination
import com.defat.app.ui.history.HistoryRoute
import com.defat.app.ui.home.HomeRoute
import com.defat.app.ui.meal.MealEditorRoute
import com.defat.app.ui.onboarding.ActivityRoute
import com.defat.app.ui.onboarding.BasicsRoute
import com.defat.app.ui.onboarding.BodyRoute
import com.defat.app.ui.onboarding.GoalRoute
import com.defat.app.ui.onboarding.OnboardingViewModel
import com.defat.app.ui.onboarding.SummaryRoute
import com.defat.app.ui.onboarding.WelcomeRoute
import com.defat.app.ui.profile.ProfileRoute
import com.defat.app.ui.weight.LogWeightRoute

@Composable
fun DefatNavHost(
    startDestination: StartDestination,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = if (startDestination == StartDestination.HOME) Routes.HOME else Routes.ONBOARDING_GRAPH,
    ) {
        navigation(startDestination = Routes.ONBOARDING_WELCOME, route = Routes.ONBOARDING_GRAPH) {
            composable(Routes.ONBOARDING_WELCOME) { entry ->
                val vm = onboardingViewModel(navController, entry)
                WelcomeRoute(viewModel = vm, onNext = { navController.navigate(Routes.ONBOARDING_BASICS) })
            }
            composable(Routes.ONBOARDING_BASICS) { entry ->
                val vm = onboardingViewModel(navController, entry)
                BasicsRoute(
                    viewModel = vm,
                    onNext = { navController.navigate(Routes.ONBOARDING_BODY) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.ONBOARDING_BODY) { entry ->
                val vm = onboardingViewModel(navController, entry)
                BodyRoute(
                    viewModel = vm,
                    onNext = { navController.navigate(Routes.ONBOARDING_ACTIVITY) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.ONBOARDING_ACTIVITY) { entry ->
                val vm = onboardingViewModel(navController, entry)
                ActivityRoute(
                    viewModel = vm,
                    onNext = { navController.navigate(Routes.ONBOARDING_GOAL) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.ONBOARDING_GOAL) { entry ->
                val vm = onboardingViewModel(navController, entry)
                GoalRoute(
                    viewModel = vm,
                    onNext = { navController.navigate(Routes.ONBOARDING_SUMMARY) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.ONBOARDING_SUMMARY) { entry ->
                val vm = onboardingViewModel(navController, entry)
                SummaryRoute(
                    viewModel = vm,
                    onStart = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.ONBOARDING_GRAPH) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() },
                )
            }
        }

        composable(Routes.HOME) {
            HomeRoute(
                onAddMeal = { navController.navigate(Routes.MEAL_ADD) },
                onEditMeal = { mealId -> navController.navigate(Routes.mealEdit(mealId)) },
                onLogWeight = { navController.navigate(Routes.WEIGHT_LOG) },
                onOpenHistory = { navController.navigate(Routes.HISTORY) },
                onOpenProfile = { navController.navigate(Routes.PROFILE) },
            )
        }
        composable(Routes.HISTORY) {
            HistoryRoute(
                onEditMeal = { mealId -> navController.navigate(Routes.mealEdit(mealId)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.MEAL_ADD) {
            MealEditorRoute(mealId = null, onDone = { navController.popBackStack() })
        }
        composable(
            route = Routes.MEAL_EDIT,
            arguments = listOf(navArgument("mealId") { type = NavType.StringType }),
        ) { entry ->
            MealEditorRoute(
                mealId = entry.arguments?.getString("mealId"),
                onDone = { navController.popBackStack() },
            )
        }
        composable(Routes.WEIGHT_LOG) {
            LogWeightRoute(onDone = { navController.popBackStack() })
        }
        composable(Routes.PROFILE) {
            ProfileRoute(onBack = { navController.popBackStack() })
        }
    }
}

/** Scopes [OnboardingViewModel] to the onboarding nav graph (plan §8.1). */
@Composable
private fun onboardingViewModel(
    navController: NavController,
    entry: androidx.navigation.NavBackStackEntry,
): OnboardingViewModel {
    val parentEntry = remember(entry) { navController.getBackStackEntry(Routes.ONBOARDING_GRAPH) }
    return hiltViewModel(parentEntry)
}
