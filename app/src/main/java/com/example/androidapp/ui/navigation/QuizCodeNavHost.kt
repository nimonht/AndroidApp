package com.example.androidapp.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.androidapp.ui.navigation.Routes.Args

/**
 * Main navigation host for the QuizCode application.
 * Sets up all navigation routes and handles navigation between screens.
 *
 * @param navController The NavHostController for managing navigation state.
 * @param startDestination The initial route to display (default: Home).
 */
@Composable
fun QuizCodeNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Routes.HOME
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            // Show bottom navigation bar only on main screens
            if (shouldShowBottomBar(currentRoute)) {
                QuizCodeBottomNavBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            // Pop up to the start destination to avoid building up a large back stack
                            popUpTo(Routes.HOME) {
                                saveState = true
                            }
                            // Avoid multiple copies of the same destination
                            launchSingleTop = true
                            // Restore state when reselecting a previously selected item
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            // ==================== Bottom Navigation Screens ====================
            composable(Routes.HOME) {
                // TODO: Replace with actual HomeScreen
                PlaceholderScreen(screenName = "Home")
            }

            composable(Routes.SEARCH) {
                // TODO: Replace with actual SearchScreen
                PlaceholderScreen(screenName = "Search")
            }

            composable(Routes.PROFILE) {
                // TODO: Replace with actual ProfileScreen
                PlaceholderScreen(screenName = "Profile")
            }

            // ==================== Quiz Screens ====================
            composable(
                route = Routes.QUIZ_DETAIL,
                arguments = listOf(
                    navArgument(Args.QUIZ_ID) { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val quizId = backStackEntry.arguments?.getString(Args.QUIZ_ID) ?: return@composable
                // TODO: Replace with actual QuizDetailScreen
                PlaceholderScreen(screenName = "Quiz Detail: $quizId")
            }

            composable(
                route = Routes.QUIZ_PLAY,
                arguments = listOf(
                    navArgument(Args.QUIZ_ID) { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val quizId = backStackEntry.arguments?.getString(Args.QUIZ_ID) ?: return@composable
                // TODO: Replace with actual TakeQuizScreen
                PlaceholderScreen(screenName = "Take Quiz: $quizId")
            }

            composable(
                route = Routes.QUIZ_RESULT,
                arguments = listOf(
                    navArgument(Args.QUIZ_ID) { type = NavType.StringType },
                    navArgument(Args.ATTEMPT_ID) { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val quizId = backStackEntry.arguments?.getString(Args.QUIZ_ID) ?: return@composable
                val attemptId = backStackEntry.arguments?.getString(Args.ATTEMPT_ID) ?: return@composable
                // TODO: Replace with actual QuizResultScreen
                PlaceholderScreen(screenName = "Quiz Result: $quizId / $attemptId")
            }

            composable(Routes.QUIZ_CREATE) {
                // TODO: Replace with actual CreateQuizScreen
                PlaceholderScreen(screenName = "Create Quiz")
            }

            composable(
                route = Routes.QUIZ_EDIT,
                arguments = listOf(
                    navArgument(Args.QUIZ_ID) { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val quizId = backStackEntry.arguments?.getString(Args.QUIZ_ID) ?: return@composable
                // TODO: Replace with actual EditQuizScreen
                PlaceholderScreen(screenName = "Edit Quiz: $quizId")
            }

            // ==================== User Screens ====================
            composable(Routes.SETTINGS) {
                // TODO: Replace with actual SettingsScreen
                PlaceholderScreen(screenName = "Settings")
            }

            composable(Routes.HISTORY) {
                // TODO: Replace with actual HistoryScreen
                PlaceholderScreen(screenName = "History")
            }

            composable(Routes.TRASH) {
                // TODO: Replace with actual TrashScreen (Recycle Bin)
                PlaceholderScreen(screenName = "Trash")
            }

            // ==================== Auth Screens ====================
            composable(Routes.LOGIN) {
                // TODO: Replace with actual LoginScreen
                PlaceholderScreen(screenName = "Login")
            }

            composable(Routes.REGISTER) {
                // TODO: Replace with actual RegisterScreen
                PlaceholderScreen(screenName = "Register")
            }
        }
    }
}

/**
 * Determines whether the bottom navigation bar should be visible for the current route.
 */
private fun shouldShowBottomBar(currentRoute: String?): Boolean {
    return currentRoute in listOf(
        Routes.HOME,
        Routes.SEARCH,
        Routes.PROFILE
    )
}
