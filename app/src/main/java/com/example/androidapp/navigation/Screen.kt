package com.example.androidapp.navigation

sealed class Screen(val route: String) {
    // Nhóm màn hình chính (Bottom Navigation)
    object Home : Screen("home")
    object Search : Screen("search")
    object Profile : Screen("profile")

    // Nhóm màn hình Quiz (Chi tiết, Chơi, Kết quả)
    object QuizDetail : Screen("quiz/{id}") {
        fun createRoute(id: String) = "quiz/$id"
    }
    object TakeQuiz : Screen("quiz/{id}/play") {
        fun createRoute(id: String) = "quiz/$id/play"
    }
    object QuizResult : Screen("quiz/{id}/result") {
        fun createRoute(id: String) = "quiz/$id/result"
    }

    // Nhóm quản lý Quiz
    object CreateQuiz : Screen("quiz/create")
    object EditQuiz : Screen("quiz/{id}/edit") {
        fun createRoute(id: String) = "quiz/$id/edit"
    }

    // Các màn hình chức năng khác
    object Settings : Screen("settings")
    object History : Screen("history")
    object RecycleBin : Screen("trash")

    // Màn hình đăng nhập (nếu cần điều hướng riêng)
    object Login : Screen("login")
}