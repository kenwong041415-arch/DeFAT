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
import com.defat.app.ui.component.DefatTab
import com.defat.app.ui.food.FoodLibraryEditorRoute
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
import com.defat.app.ui.progress.ProgressRoute
import com.defat.app.ui.sources.DataSourcesScreen
import com.defat.app.ui.weight.LogWeightRoute

@Composable
fun DefatNavHost(
    startDestination: StartDestination,
    navController: NavHostController = rememberNavController(),
) {
    // Bottom-bar navigation (plan §6.1). Popping to HOME (by route, because after
    // onboarding the graph's start destination is no longer on the stack) and
    // saving/restoring state keeps each tab as the student left it. Tapping the
    // tab already on screen is a no-op through launchSingleTop.
    val navigateToTab: (DefatTab) -> Unit = { tab ->
        navController.navigate(tab.route) {
            popUpTo(Routes.HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    // The centre add button opens the editor for the selected day; the editor
    // reads the selected day itself (SelectedDayHolder), so no argument is needed.
    val addMeal: () -> Unit = { navController.navigate(Routes.MEAL_ADD) }

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
                onAddMeal = addMeal,
                onEditMeal = { mealId -> navController.navigate(Routes.mealEdit(mealId)) },
                onLogWeight = { navController.navigate(Routes.WEIGHT_LOG) },
                onTabSelected = navigateToTab,
            )
        }
        composable(Routes.HISTORY) {
            HistoryRoute(
                onOpenToday = { navigateToTab(DefatTab.TODAY) },
                onAddMeal = addMeal,
                onTabSelected = navigateToTab,
            )
        }
        composable(Routes.PROGRESS) {
            ProgressRoute(onTabSelected = navigateToTab, onAddMeal = addMeal)
        }
        composable(Routes.PROFILE) {
            ProfileRoute(
                onLogWeight = { navController.navigate(Routes.WEIGHT_LOG) },
                onOpenFoodLibrary = { navController.navigate(Routes.FOOD_LIBRARY) },
                onOpenSources = { navController.navigate(Routes.SOURCES) },
                onAddMeal = addMeal,
                onTabSelected = navigateToTab,
            )
        }
        // The owner's 食物庫 editor. Visible to every user until the trainer role
        // exists (Phase 5); the role check will be one guard on this entry.
        composable(Routes.FOOD_LIBRARY) {
            FoodLibraryEditorRoute(onBack = { navController.popBackStack() })
        }
        composable(Routes.SOURCES) {
            DataSourcesScreen(onBack = { navController.popBackStack() })
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
