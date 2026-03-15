package com.example.androidapp.ui.screens.auth

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.androidapp.BuildConfig
import com.example.androidapp.R
import com.example.androidapp.di.LocalAppContainer
import com.example.androidapp.domain.util.GoogleSignInHelper
import com.example.androidapp.ui.components.forms.GoogleSignInButton
import com.example.androidapp.ui.components.navigation.AppTopBar
import com.example.androidapp.ui.theme.FullShape
import kotlinx.coroutines.launch

/**
 * Login screen with email/password fields.
 * Stateless composable; all state is owned by [AuthViewModel].
 *
 * @param onLoginSuccess Callback when login is successful.
 * @param onNavigateToRegister Callback to navigate to registration screen.
 * @param onNavigateBack Callback to navigate back.
 * @param modifier Modifier for styling.
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container = LocalAppContainer
    val viewModel: AuthViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                AuthViewModel(container.authRepository) as T
        }
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isGoogleSignInLoading by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Authenticated) onLoginSuccess()
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Error) {
            snackbarHostState.showSnackbar((uiState as AuthUiState.Error).message)
            viewModel.onEvent(AuthEvent.ClearError)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = stringResource(R.string.login),
                canNavigateBack = true,
                navigateUp = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Title
            Text(
                text = stringResource(R.string.auth_welcome_back),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.auth_login_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Email field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(R.string.email)) },
                leadingIcon = {
                    Icon(Icons.Default.Email, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.password)) },
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = null)
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) 
                                Icons.Default.Visibility 
                            else 
                                Icons.Default.VisibilityOff,
                            contentDescription = null
                        )
                    }
                },
                visualTransformation = if (passwordVisible) 
                    VisualTransformation.None 
                else 
                    PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Login button
            Button(
                onClick = { viewModel.onEvent(AuthEvent.Login(email, password)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = FullShape,
                enabled = email.isNotBlank() && password.isNotBlank()
                        && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
                        && uiState !is AuthUiState.Loading
            ) {
                if (uiState is AuthUiState.Loading && !isGoogleSignInLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text(
                        text = stringResource(R.string.login),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Divider with "OR"
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.auth_or_divider),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Google Sign-In button
            GoogleSignInButton(
                onClick = {
                    coroutineScope.launch {
                        handleGoogleSignIn(
                            context = context,
                            viewModel = viewModel,
                            onLoadingChange = { isGoogleSignInLoading = it }
                        )
                    }
                },
                isLoading = isGoogleSignInLoading,
                enabled = uiState !is AuthUiState.Loading
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sign up link
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.auth_no_account))
                Text(
                    text = stringResource(R.string.register),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onNavigateToRegister() }
                )
            }
        }
    }
}

/**
 * Handles Google Sign-In flow using Credential Manager.
 *
 * @param context Android context.
 * @param viewModel AuthViewModel to dispatch GoogleSignIn event.
 * @param onLoadingChange Callback to update loading state.
 */
private suspend fun handleGoogleSignIn(
    context: Context,
    viewModel: AuthViewModel,
    onLoadingChange: (Boolean) -> Unit
) {
    // TODO: Replace with your actual Google Web Client ID from Google Cloud Console
    // Get it from: https://console.cloud.google.com/apis/credentials
    // Format: "YOUR_CLIENT_ID.apps.googleusercontent.com"
    val serverClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID.ifEmpty {
        // Fallback for development - this should be configured in build.gradle.kts
        "YOUR_GOOGLE_WEB_CLIENT_ID.apps.googleusercontent.com"
    }

    onLoadingChange(true)
    val result = GoogleSignInHelper.signIn(context, serverClientId)
    onLoadingChange(false)

    result.fold(
        onSuccess = { idToken ->
            viewModel.onEvent(AuthEvent.GoogleSignIn(idToken))
        },
        onFailure = { error ->
            // Error is already handled in GoogleSignInHelper with Vietnamese messages
            viewModel.onEvent(AuthEvent.ClearError)
            // The error will be shown via SnackbarHost from AuthUiState.Error
        }
    )
}
