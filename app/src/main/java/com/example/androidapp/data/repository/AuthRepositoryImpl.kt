package com.example.androidapp.data.repository

import com.example.androidapp.data.local.dao.UserDao
import com.example.androidapp.data.local.toDomain
import com.example.androidapp.data.local.toEntity
import com.example.androidapp.data.remote.firebase.FirestoreCascadeHelper
import com.example.androidapp.data.remote.firebase.UserRemoteDataSource
import com.example.androidapp.data.remote.model.UserDto
import com.google.firebase.Timestamp
import com.example.androidapp.domain.model.User
import com.example.androidapp.domain.model.UserRole
import com.example.androidapp.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.UserProfileChangeRequest
import com.example.androidapp.data.remote.toDomain
import com.google.firebase.firestore.FirebaseFirestore
import android.net.Uri
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Implementation of [AuthRepository] that wraps [FirebaseAuth].
 * On login/register, the user profile is cached in Room via [UserDao].
 *
 * The [currentUser] flow is backed by a [MutableStateFlow] that is updated by
 * both [FirebaseAuth.AuthStateListener] (login/logout) and [updateProfile]
 * (display name / photo URL changes), ensuring the UI always reflects the
 * latest user data without requiring a re-login.
 */
class AuthRepositoryImpl(
    private val firebaseAuth: FirebaseAuth,
    private val userDao: UserDao,
    private val userRemoteDataSource: UserRemoteDataSource,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    companion object {
        // Error codes used as Exception messages. ViewModels map these to UiError values.
        const val ERROR_LOGIN_FAILED = "auth_login_failed"
        const val ERROR_ACCOUNT_BANNED = "auth_account_banned"
        const val ERROR_INVALID_CREDENTIALS = "auth_invalid_credentials"
        const val ERROR_REGISTER_FAILED = "auth_register_failed"
        const val ERROR_EMAIL_IN_USE = "auth_email_in_use"
        const val ERROR_WEAK_PASSWORD = "auth_weak_password"
        const val ERROR_RESET_PASSWORD_FAILED = "auth_reset_password_failed"
        const val ERROR_NO_USER = "auth_no_user"
        const val ERROR_DELETE_ACCOUNT_FAILED = "auth_delete_account_failed"
        const val ERROR_REFRESH_SESSION_FAILED = "auth_refresh_session_failed"
        const val ERROR_UPDATE_PROFILE_FAILED = "auth_update_profile_failed"
    }

    /**
     * Coroutine scope for background operations triggered by listeners.
     * Uses [SupervisorJob] so individual failures don't cancel siblings.
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Backing mutable state that holds the currently authenticated [User].
     * Updated from three sources:
     * 1. [FirebaseAuth.AuthStateListener] — fires on login / logout / token refresh.
     * 2. [updateProfile] — fires after a successful display-name or photo-URL change.
     * 3. [register] — fires after account creation (includes auto-fetched avatar).
     */
    private val _currentUser = MutableStateFlow<User?>(null)

    /**
     * When true, the [FirebaseAuth.AuthStateListener] skips updating [_currentUser].
     *
     * This flag is set during [login], [register], and [updateProfile] to prevent
     * the listener from overwriting [_currentUser] with stale or incomplete data
     * from [FirebaseAuth.currentUser] before those methods have finished building
     * the authoritative [User] object (e.g. with an auto-fetched avatar URL or
     * Room-enriched fields like `username`).
     */
    @Volatile
    private var _suppressAuthStateUpdates = false

    init {
        firebaseAuth.addAuthStateListener { auth ->
            // Skip if a login/register/updateProfile operation is in progress;
            // those methods will set _currentUser manually with richer data.
            if (_suppressAuthStateUpdates) return@addAuthStateListener

            val firebaseUser = auth.currentUser
            if (firebaseUser != null) {
                val capturedUid = firebaseUser.uid
                // Fetch full profile (including role) from Firestore/Room
                scope.launch {
                    val user = fetchFullUserProfile(capturedUid, firebaseUser)
                    // Guard against stale writes: the user may have signed out
                    // (or switched accounts) while the coroutine was in-flight.
                    if (firebaseAuth.currentUser?.uid != capturedUid) return@launch
                    // If user was banned while app was closed, force sign-out
                    if (user.isBanned) {
                        firebaseAuth.signOut()
                        _currentUser.value = null
                        return@launch
                    }
                    _currentUser.value = user
                }
            } else {
                _currentUser.value = null
            }
        }
    }

    /**
     * Emits the currently authenticated [User] by observing Firebase Auth state
     * changes **and** manual profile updates. Collectors receive the latest
     * value immediately (replay = 1 via [StateFlow] semantics).
     */
    override val currentUser: Flow<User?> = _currentUser.asStateFlow()

    override val isLoggedIn: Boolean
        get() = firebaseAuth.currentUser != null

    override suspend fun login(email: String, password: String): Result<User> {
        _suppressAuthStateUpdates = true
        try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
                ?: return Result.failure(Exception(ERROR_LOGIN_FAILED))

            // Fetch full profile (including role) from Firestore, falling back to Room
            val user = fetchFullUserProfile(firebaseUser.uid, firebaseUser)

            // Reject banned users — sign out immediately so they cannot use the app
            if (user.isBanned) {
                firebaseAuth.signOut()
                return Result.failure(Exception(ERROR_ACCOUNT_BANNED))
            }

            // Update the shared flow so all collectors get the latest data
            _currentUser.value = user
            return Result.success(user)
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            return Result.failure(Exception(ERROR_INVALID_CREDENTIALS))
        } catch (e: Exception) {
            return Result.failure(e)
        } finally {
            _suppressAuthStateUpdates = false
        }
    }

    override suspend fun register(email: String, password: String, username: String): Result<User> {
        _suppressAuthStateUpdates = true
        try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
                ?: return Result.failure(Exception(ERROR_REGISTER_FAILED))

            // Set displayName on the Firebase Auth profile so that
            // AuthStateListener (currentUser Flow) emits the correct name
            // immediately after registration.
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(username)
                .build()
            firebaseUser.updateProfile(profileUpdates).await()

            // Auto-fetch a random avatar for new users (non-fatal if it fails)
            var avatarUrl: String? = null
            try {
                avatarUrl = fetchRandomAvatarUrl()
                if (avatarUrl != null) {
                    val avatarUpdate = UserProfileChangeRequest.Builder()
                        .setPhotoUri(Uri.parse(avatarUrl))
                        .build()
                    firebaseUser.updateProfile(avatarUpdate).await()
                }
            } catch (_: Exception) {
                // Avatar fetch failure is non-fatal; proceed with null photoUrl
            }

            val user = User(
                id = firebaseUser.uid,
                email = email,
                displayName = username,
                username = username,
                photoUrl = avatarUrl
            )

            // Persist to Room and Firestore BEFORE emitting _currentUser.
            // The _currentUser emission triggers the AuthViewModel init collector
            // which sets AuthUiState.Authenticated. When the XML AuthFragment
            // observes that state it navigates away and the Fragment (plus its
            // ViewModel scope) is destroyed. If the Firestore write has not
            // finished by then the coroutine is cancelled and the user document
            // never lands in the 'users' collection.
            //
            // withContext(NonCancellable) ensures the writes run to completion
            // even if the caller's scope is cancelled mid-flight (e.g. Fragment
            // removal racing with coroutine dispatch).
            withContext(NonCancellable) {
                // Cache locally
                userDao.insertUser(user.toEntity())
                // Persist to Firestore
                try {
                    userRemoteDataSource.saveUser(
                        UserDto(
                            id = user.id,
                            email = email,
                            displayName = username,
                            username = username,
                            photoUrl = avatarUrl,
                            createdAt = Timestamp.now(),
                            updatedAt = Timestamp.now()
                        )
                    )
                } catch (_: Exception) {
                    // Firestore sync failure is non-fatal; fetchFullUserProfile
                    // will back-fill the document on the next login / app start.
                }
            }

            // NOW propagate the authenticated user to all UI collectors.
            // All persistence is already done, so it is safe for the UI to
            // navigate away (destroying the Fragment / ViewModel scope).
            _currentUser.value = user

            return Result.success(user)
        } catch (e: FirebaseAuthUserCollisionException) {
            return Result.failure(Exception(ERROR_EMAIL_IN_USE))
        } catch (e: FirebaseAuthWeakPasswordException) {
            return Result.failure(Exception(ERROR_WEAK_PASSWORD))
        } catch (e: Exception) {
            return Result.failure(e)
        } finally {
            _suppressAuthStateUpdates = false
        }
    }

    override suspend fun logout() {
        firebaseAuth.signOut()
        // AuthStateListener will set _currentUser to null
    }

    override suspend fun getCurrentUser(): User? {
        val firebaseUser = firebaseAuth.currentUser ?: return null
        return fetchFullUserProfile(firebaseUser.uid, firebaseUser)
    }

    /**
     * Sends a password reset email to [email].
     * @return [Result.success] on success, or [Result.failure] with the error.
     */
    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(ERROR_RESET_PASSWORD_FAILED, e))
        }
    }

    /**
     * Deletes the current user's account from Firestore, Room, and Firebase Auth.
     * @return [Result.success] on success, or [Result.failure] with the error.
     */
    override suspend fun deleteAccount(): Result<Unit> {
        return try {
            val firebaseUser = firebaseAuth.currentUser
                ?: return Result.failure(Exception(ERROR_NO_USER))
            val uid = firebaseUser.uid

            // Cascade-delete all user data from Firestore
            try {
                FirestoreCascadeHelper.cascadeDeleteUserData(firestore, uid)
            } catch (_: Exception) {
                // Firestore cascade cleanup failure is non-fatal;
                // proceed with Auth deletion so the user is not stuck
            }

            // Remove local Room cache
            userDao.deleteUserById(uid)

            // Delete Firebase Auth account
            firebaseUser.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(ERROR_DELETE_ACCOUNT_FAILED, e))
        }
    }

    /**
     * Generates and returns a unique guest identifier.
     * @return A new guest UUID string prefixed with "guest_".
     */
    override fun generateGuestId(): String {
        return "guest_${UUID.randomUUID()}"
    }

    /**
     * Refreshes the current authentication session by forcing a token refresh.
     * @return [Result.success] on success, or [Result.failure] with the error.
     */
    override suspend fun refreshSession(): Result<Unit> {
        return try {
            firebaseAuth.currentUser?.getIdToken(true)?.await()
                ?: return Result.failure(Exception(ERROR_NO_USER))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(ERROR_REFRESH_SESSION_FAILED, e))
        }
    }

    /**
     * Updates the current user's display name and/or photo URL.
     * Applies the change to Firebase Auth, then syncs to Firestore and the local Room cache.
     * Finally, updates [_currentUser] so all UI collectors immediately reflect the change
     * without requiring a re-login.
     *
     * @param displayName The new display name to set.
     * @param photoUrl The new photo URL to set, or null to leave unchanged.
     * @return [Result.success] on success, or [Result.failure] with the error.
     */
    override suspend fun updateProfile(displayName: String, photoUrl: String?): Result<Unit> {
        _suppressAuthStateUpdates = true
        try {
            val firebaseUser = firebaseAuth.currentUser
                ?: return Result.failure(Exception(ERROR_NO_USER))

            // Build and apply the Firebase Auth profile update
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .apply { if (photoUrl != null) setPhotoUri(Uri.parse(photoUrl)) }
                .build()
            firebaseUser.updateProfile(profileUpdates).await()

            val uid = firebaseUser.uid

            // Update local Room cache - preserve existing fields where possible
            val existing = userDao.getUserById(uid)
            val updatedPhotoUrl = photoUrl ?: existing?.photoUrl
            if (existing != null) {
                userDao.updateUser(
                    existing.copy(
                        displayName = displayName,
                        photoUrl = updatedPhotoUrl
                    )
                )
            }

            // Sync updated profile to Firestore (non-fatal if offline)
            try {
                val currentDto = userRemoteDataSource.getUserById(uid)
                userRemoteDataSource.saveUser(
                    UserDto(
                        id = uid,
                        email = currentDto?.email ?: firebaseUser.email ?: "",
                        displayName = displayName,
                        username = currentDto?.username ?: "",
                        photoUrl = updatedPhotoUrl,
                        role = currentDto?.role ?: "user",
                        createdAt = currentDto?.createdAt ?: Timestamp.now(),
                        updatedAt = Timestamp.now(),
                        deletedAt = currentDto?.deletedAt
                    )
                )
            } catch (_: Exception) {
                // Firestore sync failure is non-fatal; local cache already updated
            }

            // Immediately propagate profile changes to all UI collectors.
            // AuthStateListener does NOT fire for profile-only updates (displayName,
            // photoUrl), so we must update _currentUser manually here.
            val previousUser = _currentUser.value
            _currentUser.value = User(
                id = uid,
                email = previousUser?.email ?: firebaseUser.email ?: "",
                displayName = displayName,
                username = previousUser?.username ?: existing?.username ?: "",
                photoUrl = updatedPhotoUrl,
                role = previousUser?.role ?: UserRole.USER,
                isBanned = previousUser?.isBanned ?: false
            )

            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(Exception(ERROR_UPDATE_PROFILE_FAILED, e))
        } finally {
            _suppressAuthStateUpdates = false
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Fetches the full user profile (including role) from Firestore first,
     * falling back to the local Room cache, and finally to Firebase Auth data.
     * Also updates the Room cache when Firestore data is successfully fetched.
     *
     * @param uid The user's unique identifier.
     * @param firebaseUser The Firebase Auth user (for fallback data).
     * @return A [User] with the correct role and profile information.
     */
    private suspend fun fetchFullUserProfile(
        uid: String,
        firebaseUser: com.google.firebase.auth.FirebaseUser
    ): User {
        // 1. Try Firestore (authoritative source for role)
        try {
            val userDto = userRemoteDataSource.getUserById(uid)
            if (userDto != null) {
                val domainUser = userDto.toDomain()
                // Cache to Room for offline access
                userDao.insertUser(domainUser.toEntity())
                return domainUser
            }

            // Firestore is reachable but no user document exists.
            // This happens when the registration Firestore write failed, or
            // the account was created before the users collection was
            // introduced. Create the missing document so the profile is
            // discoverable by other features (admin panel, quiz author
            // display, etc.).
            val newUserDto = UserDto(
                id = uid,
                email = firebaseUser.email ?: "",
                displayName = firebaseUser.displayName ?: "",
                username = firebaseUser.displayName ?: "",
                photoUrl = firebaseUser.photoUrl?.toString(),
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
            userRemoteDataSource.saveUser(newUserDto)
            val domainUser = newUserDto.toDomain()
            userDao.insertUser(domainUser.toEntity())
            return domainUser
        } catch (_: Exception) {
            // Firestore unavailable (offline) — fall through to Room
        }

        // 2. Try Room cache
        val cached = userDao.getUserById(uid)
        if (cached != null) return cached.toDomain()

        // 3. Fallback to Firebase Auth data (role defaults to USER)
        return User(
            id = uid,
            email = firebaseUser.email ?: "",
            displayName = firebaseUser.displayName ?: "",
            photoUrl = firebaseUser.photoUrl?.toString()
        )
    }

    /**
     * Fetches a random anime/artwork image URL from the Wallhaven API.
     * Used to auto-assign a profile avatar to new users on registration.
     *
     * API: https://wallhaven.cc/api/v1/search
     * Parameters: categories=010 (anime), purity=100 (SFW), sorting=random,
     * atleast=400x400, ratios=1x1
     *
     * @return The small thumbnail URL of a random image, or null if the fetch fails.
     */
    private suspend fun fetchRandomAvatarUrl(): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL(
                "https://wallhaven.cc/api/v1/search?categories=010&purity=100&sorting=random&atleast=400x400&ratios=1x1"
            )
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000

            try {
                val responseCode = connection.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    return@withContext null
                }

                val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseBody)
                val dataArray = json.getJSONArray("data")

                if (dataArray.length() == 0) {
                    return@withContext null
                }

                val firstItem = dataArray.getJSONObject(0)
                val thumbs = firstItem.getJSONObject("thumbs")
                thumbs.getString("small")
            } finally {
                connection.disconnect()
            }
        } catch (_: Exception) {
            null
        }
    }
}
