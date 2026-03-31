package com.example.androidapp

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.FragmentActivity
import com.example.androidapp.data.preferences.SettingsPreferences
import com.example.androidapp.ui.navigation.QuizzezNavHost
import com.example.androidapp.ui.screens.auth.AuthFragment
import com.example.androidapp.ui.theme.QuizzezTheme

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
        val settingsPreferences = (application as QuizzezApplication).appContainer.settingsPreferences

        composeView.setContent {
            val themeMode by settingsPreferences.darkThemeMode.collectAsStateWithLifecycle(
                initialValue = SettingsPreferences.THEME_MODE_SYSTEM
            )
            val darkTheme = when (themeMode) {
                SettingsPreferences.THEME_MODE_LIGHT -> false
                SettingsPreferences.THEME_MODE_DARK -> true
                else -> isSystemInDarkTheme()
            }

            QuizzezTheme(darkTheme = darkTheme) {
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
                onGuestContinue = { showComposeContent() }
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, authFragment)
                .commit()
        } else if (existingFragment is AuthFragment) {
            // Re-attach callbacks after configuration change.
            existingFragment.onAuthSuccess = { showComposeContent() }
            existingFragment.onGuestContinue = { showComposeContent() }
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
