package com.example.androidapp.data.repository

import com.example.androidapp.data.local.dao.UserDao
import com.example.androidapp.data.local.toDomain
import com.example.androidapp.data.local.toEntity
import com.example.androidapp.data.remote.firebase.UserRemoteDataSource
import com.example.androidapp.data.remote.model.UserDto
import com.example.androidapp.domain.model.User
import com.example.androidapp.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import android.net.Uri
import java.util.UUID
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await
import com.example.androidapp.data.network.retryWithBackoff
import com.example.androidapp.data.error.ErrorEvent

/**
 * Implementation of [AuthRepository] that wraps [FirebaseAuth].
 * On login/register, the user profile is cached in Room via [UserDao].
 */
class AuthRepositoryImpl(
    private val firebaseAuth: FirebaseAuth,
    private val userDao: UserDao,
    private val userRemoteDataSource: UserRemoteDataSource
) : AuthRepository {

    private var isManualLogout = false

    /**
     * Emits the currently authenticated [User] by listening to [FirebaseAuth] state changes.
     */
    override val currentUser: Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val firebaseUser = auth.currentUser
            if (firebaseUser != null) {
                isManualLogout = false // reset on login
                val user = User(
                    id = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    displayName = firebaseUser.displayName ?: "",
                    photoUrl = firebaseUser.photoUrl?.toString()
                )
                trySend(user)
            } else {
                trySend(null)
            }
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    override val isLoggedIn: Boolean
        get() = firebaseAuth.currentUser != null

    override suspend fun login(email: String, password: String): Result<User> {
        return try {
            val result = retryWithBackoff(
                maxRetries = 2,
                shouldRetry = { it !is FirebaseAuthInvalidCredentialsException }
            ) {
                firebaseAuth.signInWithEmailAndPassword(email, password).await()
            }
            val firebaseUser = result.user ?: return Result.failure(Exception("Đăng nhập thất bại"))
            val user = User(
                id = firebaseUser.uid,
                email = firebaseUser.email ?: email,
                displayName = firebaseUser.displayName ?: "",
                photoUrl = firebaseUser.photoUrl?.toString()
            )
            // Attempt to load full profile from Firestore/Room
            val cached = userDao.getUserById(firebaseUser.uid)
            val enriched = if (cached != null) cached.toDomain() else user
            Result.success(enriched)
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(Exception("Email hoặc mật khẩu không đúng"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun register(email: String, password: String, username: String): Result<User> {
        return try {
            val result = retryWithBackoff(
                maxRetries = 2,
                shouldRetry = { it !is FirebaseAuthUserCollisionException && it !is FirebaseAuthWeakPasswordException }
            ) {
                firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            }
            val firebaseUser = result.user ?: return Result.failure(Exception("Đăng ký thất bại"))
            val user = User(
                id = firebaseUser.uid,
                email = email,
                displayName = username,
                username = username,
                photoUrl = null
            )
            // Cache locally
            userDao.insertUser(user.toEntity())
            // Persist to Firestore in background
            try {
                userRemoteDataSource.saveUser(
                    UserDto(id = user.id, email = email, displayName = username, username = username)
                )
            } catch (_: Exception) {
                // Firestore sync failure is non-fatal
            }
            Result.success(user)
        } catch (e: FirebaseAuthUserCollisionException) {
            Result.failure(Exception("Email này đã được sử dụng"))
        } catch (e: FirebaseAuthWeakPasswordException) {
            Result.failure(Exception("Mật khẩu quá yếu. Cần ít nhất 6 ký tự"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        isManualLogout = true
        firebaseAuth.signOut()
        userDao.deleteAllUsers()
    }

    override suspend fun getCurrentUser(): User? {
        val firebaseUser = firebaseAuth.currentUser ?: return null
        val cached = userDao.getUserById(firebaseUser.uid)
        if (cached != null) return cached.toDomain()
        return User(
            id = firebaseUser.uid,
            email = firebaseUser.email ?: "",
            displayName = firebaseUser.displayName ?: "",
            photoUrl = firebaseUser.photoUrl?.toString()
        )
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
            Result.failure(Exception("Gửi email đặt lại mật khẩu thất bại", e))
        }
    }

    /**
     * Deletes the current user's account from Firestore, Room, and Firebase Auth.
     * @return [Result.success] on success, or [Result.failure] with the error.
     */
    override suspend fun deleteAccount(): Result<Unit> {
        return try {
            val firebaseUser = firebaseAuth.currentUser
                ?: return Result.failure(Exception("Không có người dùng đang đăng nhập"))
            val uid = firebaseUser.uid
            // Remove from Firestore
            try {
                userRemoteDataSource.deleteUser(uid)
            } catch (_: Exception) {
                // Firestore cleanup failure is non-fatal
            }
            // Remove local cache
            userDao.deleteUserById(uid)
            // Delete Firebase Auth account
            firebaseUser.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Xóa tài khoản thất bại", e))
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
                ?: return Result.failure(Exception("Không có người dùng đang đăng nhập"))
            Result.success(Unit)
        } catch (e: FirebaseAuthInvalidUserException) {
            logout()
            ErrorEvent.post("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.")
            Result.failure(Exception("Phiên đăng nhập đã hết hạn", e))
        } catch (e: Exception) {
            Result.failure(Exception("Làm mới phiên đăng nhập thất bại", e))
        }
    }

    /**
     * Updates the current user's display name and/or photo URL.
     * Applies the change to Firebase Auth, then syncs to Firestore and the local Room cache.
     * @param displayName The new display name to set.
     * @param photoUrl The new photo URL to set, or null to leave unchanged.
     * @return [Result.success] on success, or [Result.failure] with the error.
     */
    override suspend fun updateProfile(displayName: String, photoUrl: String?): Result<Unit> {
        return try {
            val firebaseUser = firebaseAuth.currentUser
                ?: return Result.failure(Exception("Không có người dùng đang đăng nhập"))

            // Build and apply the Firebase Auth profile update
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .apply { if (photoUrl != null) setPhotoUri(Uri.parse(photoUrl)) }
                .build()
            firebaseUser.updateProfile(profileUpdates).await()

            val uid = firebaseUser.uid

            // Update local Room cache - preserve existing fields where possible
            val existing = userDao.getUserById(uid)
            if (existing != null) {
                userDao.updateUser(
                    existing.copy(
                        displayName = displayName,
                        photoUrl = photoUrl ?: existing.photoUrl
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
                        photoUrl = photoUrl ?: currentDto?.photoUrl
                    )
                )
            } catch (_: Exception) {
                // Firestore sync failure is non-fatal; local cache already updated
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Cập nhật hồ sơ thất bại", e))
        }
    }
}
