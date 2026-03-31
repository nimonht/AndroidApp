package com.example.androidapp

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.androidapp.ui.navigation.QuizzezNavHost
import com.example.androidapp.ui.screens.auth.AuthFragment
import com.example.androidapp.ui.screens.auth.AuthUiState
import com.example.androidapp.ui.screens.auth.AuthViewModel
import com.example.androidapp.ui.theme.QuizzezTheme
import kotlinx.coroutines.launch

/**
 * Main Activity for the Quizzez application.
 *
 * Hosts two top-level views inside a FrameLayout (defined in `activity_main.xml`):
 * 1. **FragmentContainerView** -- displays the XML-based [AuthFragment] (login / register / guest).
 * 2. **ComposeView** -- displays the Compose-based [QuizzezNavHost] (the rest of the app).
 *
 * On launch the activity checks whether a user session already exists:
 * - If the user is already authenticated the auth screen is skipped and
 *   the Compose content is shown immediately.
 * - Otherwise the [AuthFragment] is presented as the landing screen.
 *
 * Transitions between the two are driven by callbacks set on [AuthFragment]
 * ([AuthFragment.onAuthSuccess] and [AuthFragment.onGuestContinue]).
 */
class MainActivity : FragmentActivity() {

    private lateinit var composeView: ComposeView
    private lateinit var fragmentContainer: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        composeView = findViewById(R.id.composeView)
        fragmentContainer = findViewById(R.id.fragmentContainer)

        // Set up the Compose content once (it stays ready but hidden until needed).
        composeView.setContent {
            QuizzezTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    QuizzezNavHost()
                }
            }
        }

        val appContainer = (application as QuizzezApplication).appContainer

        // Check for an existing logged-in session. If the user is already
        // authenticated we skip the auth landing entirely.
        if (appContainer.authRepository.isLoggedIn) {
            showComposeContent()
        } else {
            showAuthFragment()
        }
    }

    // ---- Screen switching ----------------------------------------------------

    /**
     * Displays the XML-based [AuthFragment] and hides the Compose content.
     */
    private fun showAuthFragment() {
        fragmentContainer.visibility = View.VISIBLE
        composeView.visibility = View.GONE

        val existingFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        if (existingFragment == null) {
            val authFragment = AuthFragment().apply {
                onAuthSuccess = { showComposeContent() }
                onGuestContinue = {
                    appContainer.guestSessionManager.startGuestSession()
                    showComposeContent()
                }
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, authFragment)
                .commit()
        } else if (existingFragment is AuthFragment) {
            // Re-attach callbacks after configuration change.
            existingFragment.onAuthSuccess = { showComposeContent() }
            existingFragment.onGuestContinue = {
                appContainer.guestSessionManager.startGuestSession()
                showComposeContent()
            }
        }
    }

    /**
     * Hides the [AuthFragment] and shows the Compose-based app content.
     * The fragment is removed from the back-stack so the user cannot
     * navigate back to it with the system back button.
     */
    private fun showComposeContent() {
        fragmentContainer.visibility = View.GONE
        composeView.visibility = View.VISIBLE

        // Remove any auth fragment still attached.
        val fragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        if (fragment != null) {
            supportFragmentManager.beginTransaction()
                .remove(fragment)
                .commitAllowingStateLoss()
        }
    }
}
