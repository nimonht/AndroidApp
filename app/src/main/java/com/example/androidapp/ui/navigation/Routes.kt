package com.example.androidapp.ui.navigation

import android.net.Uri

/**
 * Defines all navigation routes for the Quizzez application.
 * Routes follow the pattern defined in the frontend design document.
 */
object Routes {
    // Bottom Navigation Routes
    const val HOME = "home"
    const val SEARCH = "search"
    const val SEARCH_WITH_TAG = "search?tag={tag}"
    const val PROFILE = "profile"

    // Quiz Routes
    const val QUIZ_DETAIL = "quiz/{quizId}"
    const val QUIZ_PLAY = "quiz/{quizId}/play"
    const val QUIZ_RESULT = "quiz/{quizId}/result/{attemptId}"
    const val QUIZ_CREATE = "quiz/create"
    const val QUIZ_EDIT = "quiz/{quizId}/edit"
    const val QUIZ_PREVIEW = "quiz/{quizId}/preview"

    // User Routes
    const val SETTINGS = "settings"
    const val HISTORY = "history"
    const val TRASH = "trash"
    const val PROFILE_EDIT = "profile/edit"

    // CSV Import
    const val CSV_IMPORT = "csv_import"

    // Review & Detail Routes
    const val ANSWER_REVIEW = "quiz/{quizId}/review/{attemptId}"
    const val ATTEMPT_DETAIL = "attempt/{attemptId}"

    // Auth Routes
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val QUESTION_POOL = "question_pool"

    // Admin Routes
    const val ADMIN_DASHBOARD = "admin/dashboard"
    const val ADMIN_USERS = "admin/users"
    const val ADMIN_QUIZZES = "admin/quizzes"
    const val ADMIN_REPORTS = "admin/reports"

    // Helper functions to build routes with arguments
    object Args {
        const val QUIZ_ID = "quizId"
        const val ATTEMPT_ID = "attemptId"
        const val TAG = "tag"
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

    /**
     * Build the quiz preview route with a specific quiz ID.
     */
    fun quizPreview(quizId: String): String = "quiz/$quizId/preview"

    /**
     * Build the answer review route with quiz and attempt IDs.
     */
    fun answerReview(quizId: String, attemptId: String): String = "quiz/$quizId/review/$attemptId"

    /**
     * Build the attempt detail route with an attempt ID.
     */
    fun attemptDetail(attemptId: String): String = "attempt/$attemptId"

    /**
     * Build the search route pre-filtered by a specific tag.
     */
    fun searchWithTag(tag: String): String = "search?tag=${Uri.encode(tag)}"
}

/**
 * Sealed class representing navigation destinations.
 * Provides type-safe navigation with required arguments.
 */
sealed class NavigationDestination(val route: String) {
    // Bottom Navigation Destinations
    data object Home : NavigationDestination(Routes.HOME)
    data object Search : NavigationDestination(Routes.SEARCH)
    data class SearchWithTag(val tag: String) : NavigationDestination(Routes.searchWithTag(tag))
    data object Profile : NavigationDestination(Routes.PROFILE)

    // Quiz Destinations
    data object QuizCreate : NavigationDestination(Routes.QUIZ_CREATE)
    data class QuizDetail(val quizId: String) : NavigationDestination(Routes.quizDetail(quizId))
    data class QuizPlay(val quizId: String) : NavigationDestination(Routes.quizPlay(quizId))
    data class QuizResult(val quizId: String, val attemptId: String) :
        NavigationDestination(Routes.quizResult(quizId, attemptId))

    data class QuizEdit(val quizId: String) : NavigationDestination(Routes.quizEdit(quizId))
    data class QuizPreview(val quizId: String) : NavigationDestination(Routes.quizPreview(quizId))

    // User Destinations
    data object Settings : NavigationDestination(Routes.SETTINGS)
    data object History : NavigationDestination(Routes.HISTORY)
    data object Trash : NavigationDestination(Routes.TRASH)
    data object QuestionPool : NavigationDestination(Routes.QUESTION_POOL)

    // Review & Detail Destinations
    data class AnswerReview(val quizId: String, val attemptId: String) :
        NavigationDestination(Routes.answerReview(quizId, attemptId))

    data class AttemptDetail(val attemptId: String) :
        NavigationDestination(Routes.attemptDetail(attemptId))

    // CSV Import Destination
    data object CsvImport : NavigationDestination(Routes.CSV_IMPORT)

    // Profile Edit Destination
    data object ProfileEdit : NavigationDestination(Routes.PROFILE_EDIT)

    // Auth Destinations
    data object Login : NavigationDestination(Routes.LOGIN)
    data object Register : NavigationDestination(Routes.REGISTER)

    // Admin Destinations
    data object AdminDashboard : NavigationDestination(Routes.ADMIN_DASHBOARD)
    data object AdminUsers : NavigationDestination(Routes.ADMIN_USERS)
    data object AdminQuizzes : NavigationDestination(Routes.ADMIN_QUIZZES)
    data object AdminReports : NavigationDestination(Routes.ADMIN_REPORTS)
}
