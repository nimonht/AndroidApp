package com.example.androidapp.domain.util

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import java.security.MessageDigest
import java.util.UUID

/**
 * Helper object for Google Sign-In using Credential Manager API.
 *
 * This utility provides a modern, secure way to integrate Google Sign-In
 * using the Android Credential Manager API (Android 14+) with backward
 * compatibility through Google Play Services.
 */
object GoogleSignInHelper {

    /**
     * Initiates Google Sign-In flow using Credential Manager.
     *
     * @param context Android context (typically Activity context).
     * @param serverClientId The OAuth 2.0 web client ID from Google Cloud Console.
     *                       This is different from your Android client ID.
     *                       Format: "YOUR_CLIENT_ID.apps.googleusercontent.com"
     * @return Result containing the Google ID token on success, or error on failure.
     *
     * @throws GetCredentialCancellationException if user cancels the sign-in.
     * @throws NoCredentialException if no Google account is available.
     * @throws GetCredentialException for other credential errors.
     */
    suspend fun signIn(
        context: Context,
        serverClientId: String
    ): Result<String> {
        return try {
            val credentialManager = CredentialManager.create(context)

            // Generate a nonce for security (prevents replay attacks)
            val nonce = generateNonce()
            val hashedNonce = hashNonce(nonce)

            // Build GetGoogleIdOption
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false) // Show all Google accounts
                .setServerClientId(serverClientId)
                .setNonce(hashedNonce)
                .build()

            // Build GetCredentialRequest
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            // Get credential from user
            val result = credentialManager.getCredential(
                request = request,
                context = context
            )

            // Extract ID token
            val idToken = handleSignInResult(result)
            Result.success(idToken)

        } catch (e: GetCredentialCancellationException) {
            // User cancelled the sign-in flow
            Result.failure(GoogleSignInCancelledException("Người dùng đã hủy đăng nhập"))
        } catch (e: NoCredentialException) {
            // No Google account available
            Result.failure(NoGoogleAccountException("Không tìm thấy tài khoản Google"))
        } catch (e: GetCredentialException) {
            // Other credential errors
            Result.failure(GoogleSignInException("Lỗi đăng nhập Google: ${e.message}"))
        } catch (e: Exception) {
            // Unexpected errors
            Result.failure(GoogleSignInException("Lỗi không xác định: ${e.message}"))
        }
    }

    /**
     * Extracts Google ID token from credential response.
     */
    private fun handleSignInResult(result: GetCredentialResponse): String {
        val credential = result.credential

        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            try {
                val googleIdTokenCredential = GoogleIdTokenCredential
                    .createFrom(credential.data)
                return googleIdTokenCredential.idToken
            } catch (e: GoogleIdTokenParsingException) {
                throw GoogleSignInException("Lỗi phân tích token Google: ${e.message}")
            }
        } else {
            throw GoogleSignInException("Loại credential không hợp lệ")
        }
    }

    /**
     * Generates a random nonce for Google Sign-In security.
     */
    private fun generateNonce(): String {
        return UUID.randomUUID().toString()
    }

    /**
     * Hashes a nonce using SHA-256.
     */
    private fun hashNonce(nonce: String): String {
        val bytes = nonce.toByteArray(Charsets.UTF_8)
        val digest = MessageDigest.getInstance("SHA-256")
        val hashedBytes = digest.digest(bytes)
        return hashedBytes.joinToString("") { "%02x".format(it) }
    }
}

/**
 * Exception thrown when user cancels Google Sign-In.
 */
class GoogleSignInCancelledException(message: String) : Exception(message)

/**
 * Exception thrown when no Google account is available.
 */
class NoGoogleAccountException(message: String) : Exception(message)

/**
 * General Google Sign-In exception.
 */
class GoogleSignInException(message: String) : Exception(message)
