package com.example.androidapp.ui.navigation

import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.androidapp.R
import com.example.androidapp.di.LocalAppContainer
import com.example.androidapp.ui.components.navigation.BottomNavBar
import com.example.androidapp.ui.components.navigation.CreateQuizFAB
import com.example.androidapp.ui.components.common.LoginPromptDialog
import com.example.androidapp.ui.navigation.Routes.Args
import com.example.androidapp.ui.screens.admin.dashboard.AdminDashboardScreen
import com.example.androidapp.ui.screens.admin.dashboard.AdminDashboardViewModel
import com.example.androidapp.ui.screens.admin.users.AdminUserManagementScreen
import com.example.androidapp.ui.screens.admin.users.AdminUserManagementViewModel
import com.example.androidapp.ui.screens.admin.quizzes.AdminQuizManagementScreen
import com.example.androidapp.ui.screens.admin.quizzes.AdminQuizManagementViewModel
import com.example.androidapp.ui.screens.admin.reports.AdminReportsScreen
import com.example.androidapp.ui.screens.admin.reports.AdminReportsViewModel
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
import com.example.androidapp.ui.screens.pool.QuestionPoolScreen
import com.example.androidapp.ui.screens.settings.SettingsScreen
import com.example.androidapp.ui.screens.settings.SettingsViewModel
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

    // Normalize the route pattern for BottomNavBar so the Search tab stays
    // selected even when the route includes optional query parameters.
    val bottomBarRoute = when {
        currentRoute?.startsWith("search") == true -> Routes.SEARCH
        else -> currentRoute
    }

    val currentUser by LocalAppContainer.authRepository.currentUser
        .collectAsStateWithLifecycle(initialValue = null)

    var showLoginPrompt by remember { mutableStateOf(false) }

    if (showLoginPrompt) {
        LoginPromptDialog(
            onLogin = {
                showLoginPrompt = false
                navController.navigate(Routes.LOGIN)
            },
            onDismiss = { showLoginPrompt = false }
        )
    }

    Scaffold(
        bottomBar = {
            // Show bottom navigation bar only on main screens
            if (shouldShowBottomBar(currentRoute)) {
                BottomNavBar(
                    currentRoute = bottomBarRoute,
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
                        if (currentUser == null) {
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
                        navController.navigate(Routes.SEARCH) {
                            popUpTo(Routes.HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToEditQuiz = { quizId ->
                        navController.navigate(Routes.quizEdit(quizId))
                    },
                    onNavigateToSearchWithTag = { tag ->
                        navController.navigate(Routes.searchWithTag(tag)) {
                            popUpTo(Routes.HOME) { saveState = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(
                route = Routes.SEARCH_WITH_TAG,
                arguments = listOf(
                    navArgument(Args.TAG) {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { backStackEntry ->
                val initialTag = backStackEntry.arguments?.getString(Args.TAG)
                SearchScreen(
                    onNavigateToQuiz = { quizId ->
                        navController.navigate(Routes.quizDetail(quizId))
                    },
                    initialTag = initialTag?.ifBlank { null }
                )
            }

            composable(Routes.PROFILE) {
                val container = LocalAppContainer
                ProfileScreen(
                    onNavigateToLogin = { navController.navigate(Routes.LOGIN) },
                    onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                    onNavigateToHistory = {
                        if (currentUser != null) {
                            navController.navigate(Routes.HISTORY)
                        } else {
                            showLoginPrompt = true
                        }
                    },
                    onNavigateToTrash = {
                        if (currentUser != null) {
                            navController.navigate(Routes.TRASH)
                        } else {
                            showLoginPrompt = true
                        }
                    },
                    onNavigateToEditProfile = {
                        if (currentUser != null) {
                            navController.navigate(Routes.PROFILE_EDIT)
                        } else {
                            showLoginPrompt = true
                        }
                    },
                    onNavigateToQuestionPool = {
                        if (currentUser != null) {
                            navController.navigate(Routes.QUESTION_POOL)
                        } else {
                            showLoginPrompt = true
                        }
                    },
                    onNavigateToAdminPanel = {
                        if (currentUser != null && currentUser!!.isAdmin()) {
                            if (container.networkMonitor.isOnline.value) {
                                navController.navigate(Routes.ADMIN_DASHBOARD)
                            } else {
                                Toast.makeText(
                                    navController.context,
                                    navController.context.getString(R.string.admin_network_required),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                )
            }

            composable(Routes.PROFILE_EDIT) {
                if (currentUser == null) {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                    showLoginPrompt = true
                } else {
                    EditProfileScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
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
                    onStartQuiz = { navController.navigate(Routes.quizPlay(quizId)) },
                    onEditQuiz = { id -> navController.navigate(Routes.quizEdit(id)) },
                    onTagClick = { tag ->
                        navController.navigate(Routes.searchWithTag(tag)) {
                            popUpTo(Routes.HOME) { saveState = true }
                            launchSingleTop = true
                        }
                    }
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
                    onQuestionsImported = { questions ->
                        navController.previousBackStackEntry?.savedStateHandle?.set(
                            "imported_questions_json",
                            com.google.gson.Gson().toJson(questions)
                        )
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
                val container = LocalAppContainer
                val settingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel<SettingsViewModel>(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            return SettingsViewModel(
                                settingsPreferences = container.settingsPreferences,
                                authRepository = container.authRepository
                            ) as T
                        }
                    }
                )
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onAccountDeleted = {
                        // Navigate to Home and clear entire back stack after account deletion.
                        navController.navigate(Routes.HOME) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.HISTORY) {
                if (currentUser == null) {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                    showLoginPrompt = true
                } else {
                    HistoryScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onAttemptClick = { attemptId ->
                            navController.navigate(Routes.attemptDetail(attemptId))
                        }
                    )
                }
            }

            composable(Routes.TRASH) {
                if (currentUser == null) {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                    showLoginPrompt = true
                } else {
                    TrashScreen(onNavigateBack = { navController.popBackStack() })
                }
            }

            composable(Routes.QUESTION_POOL) {
                if (currentUser == null) {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                    showLoginPrompt = true
                } else {
                    QuestionPoolScreen(onNavigateBack = { navController.popBackStack() })
                }
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
                            popUpTo(Routes.HOME) { inclusive = true }
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
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = { navController.popBackStack() },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // ==================== Admin Screens ====================
            composable(Routes.ADMIN_DASHBOARD) {
                // Guard: redirect non-admin users back
                if (currentUser?.isAdmin() != true) {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                    return@composable
                }

                val container = LocalAppContainer
                val viewModel: AdminDashboardViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T =
                            AdminDashboardViewModel(container.adminRepository, container.networkMonitor) as T
                    }
                )

                AdminDashboardScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToUsers = { navController.navigate(Routes.ADMIN_USERS) },
                    onNavigateToQuizzes = { navController.navigate(Routes.ADMIN_QUIZZES) },
                    onNavigateToReports = { navController.navigate(Routes.ADMIN_REPORTS) }
                )
            }

            composable(Routes.ADMIN_USERS) {
                if (currentUser?.isAdmin() != true) {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                    return@composable
                }

                val container = LocalAppContainer
                val viewModel: AdminUserManagementViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T =
                            AdminUserManagementViewModel(
                                container.adminRepository,
                                container.networkMonitor,
                                container.authRepository
                            ) as T
                    }
                )

                AdminUserManagementScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Routes.ADMIN_QUIZZES) {
                if (currentUser?.isAdmin() != true) {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                    return@composable
                }

                val container = LocalAppContainer
                val viewModel: AdminQuizManagementViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T =
                            AdminQuizManagementViewModel(
                                container.adminRepository,
                                container.authRepository,
                                container.networkMonitor
                            ) as T
                    }
                )

                AdminQuizManagementScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onQuizClick = { quizId -> navController.navigate(Routes.quizDetail(quizId)) }
                )
            }

            composable(Routes.ADMIN_REPORTS) {
                if (currentUser?.isAdmin() != true) {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                    return@composable
                }

                val container = LocalAppContainer
                val viewModel: AdminReportsViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T =
                            AdminReportsViewModel(container.adminRepository, container.networkMonitor) as T
                    }
                )

                AdminReportsScreen(
                    viewModel = viewModel,
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
    return currentRoute in listOf(Routes.HOME, Routes.SEARCH_WITH_TAG, Routes.PROFILE)
}
