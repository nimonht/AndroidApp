package com.example.androidapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.example.androidapp.ui.navigation.QuizzezNavHost
import com.example.androidapp.ui.screens.home.HomeFragment
import com.example.androidapp.ui.theme.QuizzezTheme

/**
 * Main Activity for the Quizzez application.
 * Hybrid implementation supporting both XML Fragments and Jetpack Compose screens.
 */
class MainActivity : AppCompatActivity() {

    private var composeNavHost: ComposeView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            // Start with HomeFragment
            showHomeFragment()
        }
    }

    /**
     * Show the XML-based HomeFragment
     */
    private fun showHomeFragment() {
        val homeFragment = HomeFragment().apply {
            setNavigationCallbacks(
                onNavigateToQuiz = { quizId ->
                    navigateToComposeScreen(startDestination = "quiz/$quizId")
                },
                onNavigateToSearch = {
                    navigateToComposeScreen(startDestination = "search")
                }
            )
        }

        supportFragmentManager.commit {
            replace(R.id.fragmentContainer, homeFragment)
        }
    }

    /**
     * Navigate from Fragment to Compose screens
     */
    private fun navigateToComposeScreen(startDestination: String) {
        val composeFragment = ComposeNavigationFragment.newInstance(startDestination)

        supportFragmentManager.commit {
            replace(R.id.fragmentContainer, composeFragment)
            addToBackStack(null)
        }
    }

    /**
     * Navigate back to HomeFragment from Compose screens
     */
    fun navigateBackToHome() {
        supportFragmentManager.popBackStack()
    }
}

/**
 * Fragment that hosts Compose Navigation for non-home screens.
 */
class ComposeNavigationFragment : Fragment() {

    companion object {
        private const val ARG_START_DESTINATION = "start_destination"

        fun newInstance(startDestination: String): ComposeNavigationFragment {
            return ComposeNavigationFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_START_DESTINATION, startDestination)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): android.view.View {
        val startDestination = arguments?.getString(ARG_START_DESTINATION) ?: "home"

        return ComposeView(requireContext()).apply {
            setContent {
                QuizzezTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        QuizzezNavHost(
                            startDestination = startDestination,
                            onNavigateToHome = {
                                (activity as? MainActivity)?.navigateBackToHome()
                            }
                        )
                    }
                }
            }
        }
    }
}
