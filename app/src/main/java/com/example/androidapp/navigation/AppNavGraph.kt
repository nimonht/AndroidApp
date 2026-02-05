package com.example.androidapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.navArgument

// Import các màn hình đã làm
import com.example.androidapp.ui.home.HomeScreen
import com.example.androidapp.ui.search.SearchScreen
import com.example.androidapp.ui.profile.ProfileScreen
import com.example.androidapp.ui.quiz.QuizDetailScreen
import com.example.androidapp.ui.quiz.TakeQuizScreen

@Composable
fun AppNavGraph(navController: NavHostController) {
    // Theo thiết kế: Màn hình bắt đầu là Home (Dashboard)
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        // --- CÁC MÀN HÌNH CHÍNH (Đã làm xong) ---
        composable(Screen.Home.route) {
            HomeScreen(navController)
        }
        composable(Screen.Search.route) {
            SearchScreen(navController)
        }
        composable(Screen.Profile.route) {
            ProfileScreen(navController)
        }

        // --- CÁC MÀN HÌNH QUIZ (CÓ THAM SỐ ID) ---

        // Nhiệm vụ 5: Màn hình chi tiết
        composable(
            route = Screen.QuizDetail.route,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { backStackEntry ->
            val quizId = backStackEntry.arguments?.getString("id") ?: ""
            QuizDetailScreen(navController = navController, quizId = quizId)
        }

        // Nhiệm vụ 6: Màn hình làm bài (Vừa thêm vào đây)
        composable(
            route = Screen.TakeQuiz.route,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { backStackEntry ->
            val quizId = backStackEntry.arguments?.getString("id") ?: ""
            TakeQuizScreen(navController = navController, quizId = quizId)
        }

        composable(
            route = Screen.QuizResult.route,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        )
        // Nhiệm vụ 7: Màn hình kết quả
        composable(
            route = Screen.QuizResult.route,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { backStackEntry ->
            val quizId = backStackEntry.arguments?.getString("id") ?: ""
            // GỌI MÀN HÌNH KẾT QUẢ:
            com.example.androidapp.ui.quiz.QuizResultScreen(navController = navController, quizId = quizId)
        }




        // --- CÁC MÀN HÌNH KHÁC ---
        composable(Screen.CreateQuiz.route) {
            com.example.androidapp.ui.create.CreateQuizScreen(navController)
        }
        composable(Screen.Settings.route) { }
        composable(Screen.History.route) { }
        composable(Screen.RecycleBin.route) { }
        composable(Screen.Login.route) {
            com.example.androidapp.ui.auth.LoginScreen(navController)
        }
    }
}