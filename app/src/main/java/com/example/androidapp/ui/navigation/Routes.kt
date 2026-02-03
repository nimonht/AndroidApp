package com.example.androidapp.ui.navigation

/**
 * Defines all navigation routes for the QuizCode application.
 * Routes follow the pattern defined in the frontend design document.
 */
object Routes {
    // Bottom Navigation Routes
    const val HOME = "home"
    const val SEARCH = "search"
    const val PROFILE = "profile"

    // Quiz Routes
    const val QUIZ_DETAIL = "quiz/{quizId}"
    const val QUIZ_PLAY = "quiz/{quizId}/play"
    const val QUIZ_RESULT = "quiz/{quizId}/result/{attemptId}"
    const val QUIZ_CREATE = "quiz/create"
    const val QUIZ_EDIT = "quiz/{quizId}/edit"

    // User Routes
    const val SETTINGS = "settings"
    const val HISTORY = "history"
    const val TRASH = "trash"

    // Auth Routes
    const val LOGIN = "login"
    const val REGISTER = "register"

    // Helper functions to build routes with arguments
    object Args {
        const val QUIZ_ID = "quizId"
        const val ATTEMPT_ID = "attemptId"
    }

    /**
     * Build the quiz detail route with a specific quiz ID.
     */
    fun quizDetail(quizId: String): String = "quiz/$quizId"

    /**
     * Build the quiz play route with a specific quiz ID.
     */
    fun quizPlay(quizId: String): String = "quiz/$quizId/play"

    /**
     * Build the quiz result route with quiz and attempt IDs.
     */
    fun quizResult(quizId: String, attemptId: String): String = "quiz/$quizId/result/$attemptId"

    /**
     * Build the quiz edit route with a specific quiz ID.
     */
    fun quizEdit(quizId: String): String = "quiz/$quizId/edit"
}

/**
 * Sealed class representing navigation destinations.
 * Provides type-safe navigation with required arguments.
 */
sealed class NavigationDestination(val route: String) {
    // Bottom Navigation Destinations
    data object Home : NavigationDestination(Routes.HOME)
    data object Search : NavigationDestination(Routes.SEARCH)
    data object Profile : NavigationDestination(Routes.PROFILE)

    // Quiz Destinations
    data object QuizCreate : NavigationDestination(Routes.QUIZ_CREATE)
    data class QuizDetail(val quizId: String) : NavigationDestination(Routes.quizDetail(quizId))
    data class QuizPlay(val quizId: String) : NavigationDestination(Routes.quizPlay(quizId))
    data class QuizResult(val quizId: String, val attemptId: String) : 
        NavigationDestination(Routes.quizResult(quizId, attemptId))
    data class QuizEdit(val quizId: String) : NavigationDestination(Routes.quizEdit(quizId))

    // User Destinations
    data object Settings : NavigationDestination(Routes.SETTINGS)
    data object History : NavigationDestination(Routes.HISTORY)
    data object Trash : NavigationDestination(Routes.TRASH)

    // Auth Destinations
    data object Login : NavigationDestination(Routes.LOGIN)
    data object Register : NavigationDestination(Routes.REGISTER)
}
