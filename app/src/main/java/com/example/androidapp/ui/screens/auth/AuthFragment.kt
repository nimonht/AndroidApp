package com.example.androidapp.ui.screens.auth

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.androidapp.QuizzezApplication
import com.example.androidapp.databinding.FragmentAuthBinding
import com.example.androidapp.domain.util.InputValidator
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

/**
 * XML-based landing screen for authentication.
 *
 * Displays a TabLayout that switches between Login and Register forms,
 * plus a "Continue as Guest" button. Delegates all auth logic to
 * [AuthViewModel] via the standard event pattern.
 *
 * Navigation callbacks are provided by the host (typically [MainActivity]):
 * - [onAuthSuccess] -- called after successful login or registration.
 * - [onGuestContinue] -- called when the user taps "Continue as Guest".
 */
class AuthFragment : Fragment() {

    // ---- View binding -------------------------------------------------------

    private var _binding: FragmentAuthBinding? = null

    /** Non-null accessor; only valid between onCreateView and onDestroyView. */
    private val binding get() = _binding!!

    // ---- ViewModel ----------------------------------------------------------

    private lateinit var viewModel: AuthViewModel

    // ---- Navigation callbacks (set by the host) -----------------------------

    /** Invoked when login or registration succeeds. */
    var onAuthSuccess: (() -> Unit)? = null

    /** Invoked when the user chooses to continue without an account. */
    var onGuestContinue: (() -> Unit)? = null

    // ---- Lifecycle ----------------------------------------------------------

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAuthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViewModel()
        setupTabs()
        setupLoginForm()
        setupRegisterForm()
        setupGuestButton()
        observeUiState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ---- ViewModel initialisation -------------------------------------------

    private fun initViewModel() {
        val appContainer =
            (requireActivity().application as QuizzezApplication).appContainer

        viewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                    AuthViewModel(
                        appContainer.authRepository,
                        appContainer.attemptRepository,
                        appContainer.guestSessionManager
                    ) as T
            }
        )[AuthViewModel::class.java]
    }

    // ---- Tab switching (Login / Register) ------------------------------------

    private fun setupTabs() {
        val tabLayout = binding.tabLayout
        tabLayout.addTab(
            tabLayout.newTab().setText(com.example.androidapp.R.string.auth_tab_login)
        )
        tabLayout.addTab(
            tabLayout.newTab().setText(com.example.androidapp.R.string.auth_tab_register)
        )

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                showFormForTab(tab.position)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })

        // In-form "switch" text links
        binding.tvSwitchToRegister.setOnClickListener {
            tabLayout.getTabAt(1)?.select()
        }
        binding.tvSwitchToLogin.setOnClickListener {
            tabLayout.getTabAt(0)?.select()
        }

        // Explicitly apply the initial state for tab 0 (Login).
        // TabLayout.OnTabSelectedListener.onTabSelected is only fired on *changes*,
        // not on the initial selection, so both containers would otherwise remain in
        // whatever visibility the XML inflater left them in until the user taps a tab.
        showFormForTab(0)
    }

    /**
     * Toggles visibility of login / register containers.
     *
     * The divider's position is handled automatically by the Barrier in the XML
     * layout, which tracks the bottom edge of whichever container is visible.
     * A GONE view contributes zero size to the Barrier, so no runtime constraint
     * patching is needed here.
     */
    private fun showFormForTab(position: Int) {
        val showLogin = position == 0
        binding.loginContainer.visibility = if (showLogin) View.VISIBLE else View.GONE
        binding.registerContainer.visibility = if (showLogin) View.GONE else View.VISIBLE
    }

    // ---- Login form ---------------------------------------------------------

    private fun setupLoginForm() {
        binding.etLoginEmail.doAfterTextChanged { updateLoginButtonState() }
        binding.etLoginPassword.doAfterTextChanged { updateLoginButtonState() }

        binding.btnLogin.setOnClickListener {
            val email = binding.etLoginEmail.text?.toString().orEmpty().trim()
            val password = binding.etLoginPassword.text?.toString().orEmpty()
            viewModel.onEvent(AuthEvent.Login(email, password))
        }

        updateLoginButtonState()
    }

    private fun updateLoginButtonState() {
        binding.btnLogin.isEnabled = isLoginFormValid()
    }

    // ---- Register form ------------------------------------------------------

    private fun setupRegisterForm() {
        binding.etRegisterUsername.doAfterTextChanged { updateRegisterButtonState() }
        binding.etRegisterEmail.doAfterTextChanged { 
            validateRegisterEmail()
            updateRegisterButtonState() 
        }
        binding.etRegisterPassword.doAfterTextChanged { 
            validateRegisterPassword()
            updateRegisterButtonState() 
        }
        binding.etRegisterConfirmPassword.doAfterTextChanged {
            validateConfirmPassword()
            updateRegisterButtonState()
        }

        binding.btnRegister.setOnClickListener {
            val email = binding.etRegisterEmail.text?.toString().orEmpty().trim()
            val password = binding.etRegisterPassword.text?.toString().orEmpty()
            val username = binding.etRegisterUsername.text?.toString().orEmpty().trim()
            viewModel.onEvent(AuthEvent.Register(email, password, username))
        }

        updateRegisterButtonState()
    }

    private fun updateRegisterButtonState() {
        binding.btnRegister.isEnabled = isRegisterFormValid()
    }

    private fun validateRegisterEmail() {
        val email = binding.etRegisterEmail.text?.toString().orEmpty().trim()
        if (email.isNotEmpty() && !InputValidator.isValidEmail(email)) {
            binding.tilRegisterEmail.error = getString(com.example.androidapp.R.string.auth_email_invalid)
        } else {
            binding.tilRegisterEmail.error = null
        }
    }

    private fun validateRegisterPassword() {
        val password = binding.etRegisterPassword.text?.toString().orEmpty()
        if (password.isNotEmpty() && !InputValidator.isPasswordValid(password)) {
            binding.tilRegisterPassword.error = getString(com.example.androidapp.R.string.auth_password_short)
        } else {
            binding.tilRegisterPassword.error = null
        }
    }

    private fun validateConfirmPassword() {
        val password = binding.etRegisterPassword.text?.toString().orEmpty()
        val confirm = binding.etRegisterConfirmPassword.text?.toString().orEmpty()
        if (confirm.isNotEmpty() && password != confirm) {
            binding.tilRegisterConfirmPassword.error =
                getString(com.example.androidapp.R.string.auth_password_mismatch)
        } else {
            binding.tilRegisterConfirmPassword.error = null
        }
    }

    // ---- Guest button -------------------------------------------------------

    private fun setupGuestButton() {
        binding.btnContinueGuest.setOnClickListener {
            onGuestContinue?.invoke()
        }
    }

    // ---- State observation --------------------------------------------------

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    handleUiState(state)
                }
            }
        }
    }

    private fun handleUiState(state: AuthUiState) {
        when (state) {
            is AuthUiState.Idle -> {
                setLoadingState(false)
            }

            is AuthUiState.Loading -> {
                setLoadingState(true)
            }

            is AuthUiState.Authenticated -> {
                setLoadingState(false)
                onAuthSuccess?.invoke()
            }

            is AuthUiState.Error -> {
                setLoadingState(false)
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                viewModel.onEvent(AuthEvent.ClearError)
            }
        }
    }

    /**
     * Toggles progress indicators and button enabled state for both forms.
     */
    private fun setLoadingState(loading: Boolean) {
        // Login form
        binding.loginProgressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !loading && isLoginFormValid()
        if (loading) {
            binding.btnLogin.text = ""
        } else {
            binding.btnLogin.setText(com.example.androidapp.R.string.login)
        }

        // Register form
        binding.registerProgressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !loading && isRegisterFormValid()
        if (loading) {
            binding.btnRegister.text = ""
        } else {
            binding.btnRegister.setText(com.example.androidapp.R.string.register)
        }

        // Disable guest button while loading
        binding.btnContinueGuest.isEnabled = !loading
    }

    private fun isLoginFormValid(): Boolean {
        val email = binding.etLoginEmail.text?.toString().orEmpty().trim()
        val password = binding.etLoginPassword.text?.toString().orEmpty()
        return InputValidator.isValidEmail(email) && password.isNotBlank()
    }

    private fun isRegisterFormValid(): Boolean {
        val username = binding.etRegisterUsername.text?.toString().orEmpty().trim()
        val email = binding.etRegisterEmail.text?.toString().orEmpty().trim()
        val password = binding.etRegisterPassword.text?.toString().orEmpty()
        val confirm = binding.etRegisterConfirmPassword.text?.toString().orEmpty()
        return username.isNotBlank()
                && InputValidator.isValidEmail(email)
                && InputValidator.isPasswordValid(password)
                && password == confirm
    }
}
