package com.example.androidapp.ui.navigation

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.example.androidapp.ui.screens.auth.LoginScreen
import com.example.androidapp.ui.screens.auth.SignupScreen
import com.example.androidapp.ui.screens.history.HistoryScreen
import com.example.androidapp.ui.screens.home.HomeScreen
import com.example.androidapp.ui.screens.quiz.QuizDetailScreen // Đảm bảo bạn đã tạo màn hình này
import com.example.androidapp.ui.screens.settings.SettingsScreen
import com.example.androidapp.ui.screens.trash.TrashScreen

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "login", // Màn hình đầu tiên khi mở app
        modifier = modifier
    ) {
        // 1. Màn hình Đăng nhập
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true } // Xóa login khỏi lịch sử để không back lại được
                    }
                },
                onNavigateToSignup = { navController.navigate("signup") }
            )
        }

        // 2. Màn hình Đăng ký
        composable("signup") {
            SignupScreen(
                onSignupSuccess = { navController.navigate("login") },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        // 3. Màn hình Trang chủ (Home)
        composable("home") {
            HomeScreen(
                onNavigateToQuizDetail = { quizId -> navController.navigate("quiz_detail/$quizId") },
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToHistory = { navController.navigate("history") }
            )
        }

        // 4. Màn hình Chi tiết Quiz (CÓ DEEP LINK)
        composable(
            route = "quiz_detail/{quizId}",
            arguments = listOf(navArgument("quizId") { type = NavType.StringType }),
            // --- CẤU HÌNH DEEP LINK TẠI ĐÂY ---
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = "https://quizcode.app/quiz/{quizId}"
                    action = Intent.ACTION_VIEW
                }
            )
        ) { backStackEntry ->
            val quizId = backStackEntry.arguments?.getString("quizId")
            // Gọi màn hình QuizDetailScreen (Bạn cần tạo màn hình này trong ui/screens/quiz/)
            QuizDetailScreen(
                quizId = quizId ?: "",
                onNavigateBack = { navController.popBackStack() },
                onStartQuiz = { /* Logic bắt đầu làm bài */ }
            )
        }

        // 5. Các màn hình khác
        composable("settings") {
            SettingsScreen(
                navigateUp = { navController.popBackStack() },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }

        composable("history") {
            HistoryScreen(navigateUp = { navController.popBackStack() })
        }

        composable("trash") {
            TrashScreen(navigateUp = { navController.popBackStack() })
        }
    }
}