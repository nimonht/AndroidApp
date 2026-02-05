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
import com.example.androidapp.ui.components.navigation.BottomNavBar
import com.example.androidapp.ui.navigation.Routes.Args
import com.example.androidapp.ui.screens.auth.LoginScreen
import com.example.androidapp.ui.screens.auth.RegisterScreen
import com.example.androidapp.ui.screens.create.CreateQuizScreen
import com.example.androidapp.ui.screens.history.HistoryScreen
import com.example.androidapp.ui.screens.home.HomeScreen
import com.example.androidapp.ui.screens.profile.ProfileScreen
import com.example.androidapp.ui.screens.quiz.QuizDetailScreen
import com.example.androidapp.ui.screens.quiz.QuizResultScreen
import com.example.androidapp.ui.screens.quiz.TakeQuizScreen
import com.example.androidapp.ui.screens.search.SearchScreen
import com.example.androidapp.ui.screens.settings.SettingsScreen
import com.example.androidapp.ui.screens.trash.TrashScreen

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
                BottomNavBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(Routes.HOME) { saveState = true }
                            launchSingleTop = true
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
                HomeScreen(
                    onNavigateToQuiz = { quizId ->
                        navController.navigate(Routes.quizDetail(quizId))
                    },
                    onNavigateToSearch = {
                        navController.navigate(Routes.SEARCH)
                    }
                )
            }

            composable(Routes.SEARCH) {
                SearchScreen(
                    onNavigateToQuiz = { quizId ->
                        navController.navigate(Routes.quizDetail(quizId))
                    }
                )
            }

            composable(Routes.PROFILE) {
                ProfileScreen(
                    onNavigateToLogin = { navController.navigate(Routes.LOGIN) },
                    onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                    onNavigateToHistory = { navController.navigate(Routes.HISTORY) },
                    onNavigateToTrash = { navController.navigate(Routes.TRASH) }
                )
            }

            // ==================== Quiz Screens ====================
            composable(
                route = Routes.QUIZ_DETAIL,
                arguments = listOf(navArgument(Args.QUIZ_ID) { type = NavType.StringType })
            ) { backStackEntry ->
                val quizId = backStackEntry.arguments?.getString(Args.QUIZ_ID) ?: return@composable
                QuizDetailScreen(
                    quizId = quizId,
                    onNavigateBack = { navController.popBackStack() },
                    onStartQuiz = { navController.navigate(Routes.quizPlay(quizId)) }
                )
            }

            composable(
                route = Routes.QUIZ_PLAY,
                arguments = listOf(navArgument(Args.QUIZ_ID) { type = NavType.StringType })
            ) { backStackEntry ->
                val quizId = backStackEntry.arguments?.getString(Args.QUIZ_ID) ?: return@composable
                TakeQuizScreen(
                    quizId = quizId,
                    onNavigateBack = { navController.popBackStack() },
                    onQuizComplete = { completedQuizId ->
                        // Generate attempt ID (in real app, this comes from repository)
                        navController.navigate(Routes.quizResult(completedQuizId, "attempt_1")) {
                            popUpTo(Routes.QUIZ_DETAIL) { inclusive = false }
                        }
                    }
                )
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
                QuizResultScreen(
                    quizId = quizId,
                    attemptId = attemptId,
                    onNavigateHome = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    },
                    onRetryQuiz = {
                        navController.navigate(Routes.quizPlay(quizId)) {
                            popUpTo(Routes.QUIZ_DETAIL) { inclusive = false }
                        }
                    },
                    onReviewAnswers = {
                        // TODO: Navigate to review screen
                    }
                )
            }

            composable(Routes.QUIZ_CREATE) {
                CreateQuizScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onSaveComplete = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.QUIZ_EDIT,
                arguments = listOf(navArgument(Args.QUIZ_ID) { type = NavType.StringType })
            ) { backStackEntry ->
                val quizId = backStackEntry.arguments?.getString(Args.QUIZ_ID) ?: return@composable
                // TODO: Create EditQuizScreen (reuses CreateQuizScreen with pre-populated data)
                CreateQuizScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onSaveComplete = { navController.popBackStack() }
                )
            }

            // ==================== User Screens ====================
            composable(Routes.SETTINGS) {
                SettingsScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable(Routes.HISTORY) {
                HistoryScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onAttemptClick = { attemptId ->
                        // TODO: Navigate to attempt detail/review
                    }
                )
            }

            composable(Routes.TRASH) {
                TrashScreen(onNavigateBack = { navController.popBackStack() })
            }

            // ==================== Auth Screens ====================
            composable(Routes.LOGIN) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = { navController.navigate(Routes.REGISTER) }
                )
            }

            composable(Routes.REGISTER) {
                RegisterScreen(
                    onRegisterSuccess = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.REGISTER) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = { navController.popBackStack() }
                )
            }
        }
    }
}

/**
 * Determines whether the bottom navigation bar should be visible for the current route.
 */
private fun shouldShowBottomBar(currentRoute: String?): Boolean {
    return currentRoute in listOf(Routes.HOME, Routes.SEARCH, Routes.PROFILE)
}
