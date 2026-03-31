package com.example.androidapp.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidapp.di.LocalAppContainer
import com.example.androidapp.R
import com.example.androidapp.data.error.ErrorEvent
import com.example.androidapp.ui.components.feedback.OfflineBanner
import com.example.androidapp.ui.components.navigation.BottomNavBar
import com.example.androidapp.ui.components.navigation.CreateQuizFAB
import com.example.androidapp.ui.components.common.LoginPromptDialog
import com.example.androidapp.ui.navigation.Routes.Args
import com.example.androidapp.ui.screens.auth.LoginScreen
import com.example.androidapp.ui.screens.auth.RegisterScreen
import com.example.androidapp.ui.screens.attempt.AttemptDetailScreen
import com.example.androidapp.ui.screens.create.CreateQuizScreen
import com.example.androidapp.ui.screens.create.CsvImportScreen
import com.example.androidapp.ui.screens.create.EditQuizScreen
import com.example.androidapp.ui.screens.create.QuizPreviewScreen
import com.example.androidapp.ui.screens.history.HistoryScreen
import com.example.androidapp.ui.screens.home.HomeScreen
import com.example.androidapp.ui.screens.profile.EditProfileScreen
import com.example.androidapp.ui.screens.profile.ProfileScreen
import com.example.androidapp.ui.screens.quiz.QuizDetailScreen
import com.example.androidapp.ui.screens.quiz.QuizResultScreen
import com.example.androidapp.ui.screens.quiz.TakeQuizScreen
import com.example.androidapp.ui.screens.review.AnswerReviewScreen
import com.example.androidapp.ui.screens.search.SearchScreen
import com.example.androidapp.ui.screens.settings.SettingsScreen
import com.example.androidapp.ui.screens.trash.TrashScreen

/**
 * Main navigation host for the Quizzez application.
 * Sets up all navigation routes and handles navigation between screens.
 *
 * @param navController The NavHostController for managing navigation state.
 * @param startDestination The initial route to display (default: Home).
 */
@Composable
fun QuizzezNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Routes.HOME
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val currentUser by LocalAppContainer.authRepository.currentUser
        .collectAsStateWithLifecycle(initialValue = null)

    val isOnline by LocalAppContainer.networkMonitor.isOnline
        .collectAsStateWithLifecycle(initialValue = true)
    val isOffline = !isOnline

    val guestSessionManager = LocalAppContainer.guestSessionManager
    val isGuest = currentUser == null && guestSessionManager.isGuest

    var showLoginPrompt by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        if (prefs.getBoolean("crashed_previously", false)) {
            prefs.edit().putBoolean("crashed_previously", false).apply()
            ErrorEvent.post(context.getString(R.string.error_default_message))
        }

        ErrorEvent.errors.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    if (showLoginPrompt) {
        LoginPromptDialog(
            onDismiss = { showLoginPrompt = false },
            onLoginClick = {
                showLoginPrompt = false
                navController.navigate(Routes.LOGIN)
            },
            onRegisterClick = {
                showLoginPrompt = false
                navController.navigate(Routes.REGISTER)
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { OfflineBanner(isOffline = isOffline) },
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
        },
        floatingActionButton = {
            // Show create quiz FAB only on the Home screen.
            // If the user is not logged in, redirect to the login screen.
            if (currentRoute == Routes.HOME) {
                CreateQuizFAB(
                    onClick = {
                        if (isGuest) {
                            showLoginPrompt = true
                        } else if (currentUser == null) {
                            navController.navigate(Routes.LOGIN)
                        } else {
                            navController.navigate(Routes.QUIZ_CREATE)
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
                    },
                    onNavigateToEditQuiz = { quizId ->
                        if (isGuest) showLoginPrompt = true else navController.navigate(Routes.quizEdit(quizId))
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
                    onNavigateToHistory = { if (isGuest) showLoginPrompt = true else navController.navigate(Routes.HISTORY) },
                    onNavigateToTrash = { if (isGuest) showLoginPrompt = true else navController.navigate(Routes.TRASH) },
                    onNavigateToEditProfile = { if (isGuest) showLoginPrompt = true else navController.navigate(Routes.PROFILE_EDIT) }
                )
            }

            composable(Routes.PROFILE_EDIT) {
                EditProfileScreen(
                    onNavigateBack = { navController.popBackStack() }
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
                    onQuizComplete = { attemptId ->
                        navController.navigate(Routes.quizResult(quizId, attemptId)) {
                            popUpTo(Routes.quizDetail(quizId)) { inclusive = false }
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
                        navController.navigate(Routes.answerReview(quizId, attemptId))
                    }
                )
            }

            composable(Routes.QUIZ_CREATE) {
                CreateQuizScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onSaveComplete = { navController.popBackStack() },
                    onNavigateToCsvImport = { navController.navigate(Routes.CSV_IMPORT) }
                )
            }

            composable(Routes.CSV_IMPORT) {
                CsvImportScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onQuestionsImported = { _ ->
                        // Questions are delivered via the callback.
                        // Full cross-screen wiring requires a SharedViewModel;
                        // navigate back so the caller can retrieve the result.
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = Routes.QUIZ_EDIT,
                arguments = listOf(navArgument(Args.QUIZ_ID) { type = NavType.StringType })
            ) { backStackEntry ->
                val quizId = backStackEntry.arguments?.getString(Args.QUIZ_ID) ?: return@composable
                EditQuizScreen(
                    quizId = quizId,
                    onNavigateBack = { navController.popBackStack() },
                    onSaveComplete = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.QUIZ_PREVIEW,
                arguments = listOf(navArgument(Args.QUIZ_ID) { type = NavType.StringType })
            ) { backStackEntry ->
                val quizId = backStackEntry.arguments?.getString(Args.QUIZ_ID) ?: return@composable
                QuizPreviewScreen(
                    quizId = quizId,
                    onNavigateBack = { navController.popBackStack() },
                    onPublish = {
                        // After publishing, navigate to the quiz detail screen so the user
                        // can see the published quiz, clearing the preview from the back stack.
                        navController.navigate(Routes.quizDetail(quizId)) {
                            popUpTo(Routes.quizPreview(quizId)) { inclusive = true }
                        }
                    }
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
                        navController.navigate(Routes.attemptDetail(attemptId))
                    }
                )
            }

            composable(Routes.TRASH) {
                TrashScreen(onNavigateBack = { navController.popBackStack() })
            }

            // ==================== Review & Detail Screens ====================
            composable(
                route = Routes.ANSWER_REVIEW,
                arguments = listOf(
                    navArgument(Args.QUIZ_ID) { type = NavType.StringType },
                    navArgument(Args.ATTEMPT_ID) { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val quizId = backStackEntry.arguments?.getString(Args.QUIZ_ID) ?: return@composable
                val attemptId = backStackEntry.arguments?.getString(Args.ATTEMPT_ID) ?: return@composable
                AnswerReviewScreen(
                    quizId = quizId,
                    attemptId = attemptId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.ATTEMPT_DETAIL,
                arguments = listOf(navArgument(Args.ATTEMPT_ID) { type = NavType.StringType })
            ) { backStackEntry ->
                val attemptId = backStackEntry.arguments?.getString(Args.ATTEMPT_ID) ?: return@composable
                AttemptDetailScreen(
                    attemptId = attemptId,
                    onNavigateBack = { navController.popBackStack() },
                    onReviewAnswers = { quizId, aId ->
                        navController.navigate(Routes.answerReview(quizId, aId))
                    },
                    onRetryQuiz = { quizId ->
                        navController.navigate(Routes.quizPlay(quizId))
                    }
                )
            }

            // ==================== Auth Screens ====================
            composable(Routes.LOGIN) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = { navController.navigate(Routes.REGISTER) },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Routes.REGISTER) {
                RegisterScreen(
                    onRegisterSuccess = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.REGISTER) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = { navController.popBackStack() },
                    onNavigateBack = { navController.popBackStack() }
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
