package com.example.androidapp.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.domain.model.User
import com.example.androidapp.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Possible authentication UI states.
 */
sealed class AuthUiState {
    /** Initial state, no action in progress. */
    data object Idle : AuthUiState()

    /** An auth operation (login/register/logout) is in progress. */
    data object Loading : AuthUiState()

    /** Auth succeeded with the given [user]. */
    data class Authenticated(val user: User) : AuthUiState()

    /** Auth failed with the given [message]. */
    data class Error(val message: String) : AuthUiState()
}

/** Events that can be dispatched to [AuthViewModel]. */
sealed class AuthEvent {
    data class Login(val email: String, val password: String) : AuthEvent()
    data class Register(val email: String, val password: String, val username: String) : AuthEvent()
    data object Logout : AuthEvent()
    data object ClearError : AuthEvent()
}

/**
 * ViewModel managing authentication state shared across Login, Register, and app-level auth checks.
 */
class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)

    /** Current authentication UI state. */
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                if (user != null && _uiState.value !is AuthUiState.Authenticated) {
                    _uiState.value = AuthUiState.Authenticated(user)
                } else if (user == null && _uiState.value is AuthUiState.Authenticated) {
                    _uiState.value = AuthUiState.Idle
                }
            }
        }
    }

    /**
     * Dispatches an [AuthEvent] to the ViewModel.
     */
    fun onEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.Login -> onLogin(event.email, event.password)
            is AuthEvent.Register -> onRegister(event.email, event.password, event.username)
            is AuthEvent.Logout -> onLogout()
            is AuthEvent.ClearError -> _uiState.value = AuthUiState.Idle
        }
    }

    private fun onLogin(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.login(email, password)
            _uiState.value = result.fold(
                onSuccess = { user -> AuthUiState.Authenticated(user) },
                onFailure = { e -> AuthUiState.Error(e.message ?: "Đăng nhập thất bại") }
            )
        }
    }

    private fun onRegister(email: String, password: String, username: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.register(email, password, username)
            _uiState.value = result.fold(
                onSuccess = { user -> AuthUiState.Authenticated(user) },
                onFailure = { e -> AuthUiState.Error(e.message ?: "Đăng ký thất bại") }
            )
        }
    }

    private fun onLogout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.value = AuthUiState.Idle
        }
    }
}

