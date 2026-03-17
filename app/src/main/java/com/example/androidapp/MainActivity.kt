package com.example.androidapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.androidapp.ui.navigation.QuizzezNavHost
import com.example.androidapp.ui.navigation.Routes
import com.example.androidapp.ui.theme.QuizzezTheme

/**
 * Main Activity for the Quizzez application.
 *
 * Hosts the Compose-based [QuizzezNavHost] as the sole content.
 * On launch the activity checks whether a user session already exists:
 * - If the user is already authenticated the app navigates directly to the home screen.
 * - Otherwise the login screen is presented as the landing screen.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as QuizzezApplication).appContainer
        val startDestination = if (appContainer.authRepository.isLoggedIn) {
            Routes.HOME
        } else {
            Routes.LOGIN
        }

        setContent {
            QuizzezTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    QuizzezNavHost(startDestination = startDestination)
                }
            }
        }
    }
}

