package com.example.androidapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.navArgument

@Composable
fun AppNavGraph(navController: NavHostController) {
    // Theo thiết kế: Màn hình bắt đầu là Home (Dashboard)
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        // --- CÁC MÀN HÌNH CHÍNH ---
        composable(Screen.Home.route) {
            // HomeScreen(...) -> Sau này sẽ bỏ comment và code vào đây
        }
        composable(Screen.Search.route) {
            // SearchScreen(...)
        }
        composable(Screen.Profile.route) {
            // ProfileScreen(...)
        }

        // --- CÁC MÀN HÌNH QUIZ (CÓ THAM SỐ ID) ---
        composable(
            route = Screen.QuizDetail.route,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { backStackEntry ->
            val quizId = backStackEntry.arguments?.getString("id")
            com.example.androidapp.ui.quiz.QuizDetailScreen(navController = navController, quizId = quizId)
        }


        composable(
            route = Screen.TakeQuiz.route,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) {
            // TakeQuizScreen(...)
        }

        composable(
            route = Screen.QuizResult.route,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) {
            // QuizResultScreen(...)
        }

        // --- CÁC MÀN HÌNH KHÁC ---
        composable(Screen.CreateQuiz.route) { }
        composable(Screen.Settings.route) { }
        composable(Screen.History.route) { }
        composable(Screen.RecycleBin.route) { }
        composable(Screen.Login.route) { }
    }
}