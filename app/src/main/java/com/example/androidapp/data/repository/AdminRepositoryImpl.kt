package com.example.androidapp.data.repository

import com.example.androidapp.data.remote.firebase.AdminRemoteDataSource
import com.example.androidapp.data.remote.toDomain
import com.example.androidapp.domain.model.AdminPermission
import com.example.androidapp.domain.model.Attempt
import com.example.androidapp.domain.model.PaginatedResult
import com.example.androidapp.domain.model.Quiz
import com.example.androidapp.domain.model.SystemStats
import com.example.androidapp.domain.model.User
import com.example.androidapp.domain.model.UserRole
import com.example.androidapp.domain.repository.AdminRepository
import com.example.androidapp.domain.util.safeCall
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Implementation of [AdminRepository] using Firebase Firestore.
 *
 * Delegates all Firestore operations to [AdminRemoteDataSource].
 * Handles DTO to domain model conversions and error handling.
 */
class AdminRepositoryImpl(
    private val adminRemoteDataSource: AdminRemoteDataSource
) : AdminRepository {

    /** Cursor for user pagination. Held by the singleton repository instance. */
    private var lastUserDoc: DocumentSnapshot? = null

    /** Cursor for quiz pagination. */
    private var lastQuizDoc: DocumentSnapshot? = null

    // ========== USER MANAGEMENT ==========

    override fun getAllUsers(): Flow<List<User>> {
        return adminRemoteDataSource.getAllUsers()
            .map { userDtos -> userDtos.map { it.toDomain() } }
    }

    override suspend fun updateUserRole(userId: String, newRole: UserRole): Result<Unit> {
        return safeCall {
            adminRemoteDataSource.updateUserRole(userId, newRole.toStorageValue())
        }
    }

    override suspend fun banUser(userId: String): Result<Unit> {
        return safeCall {
            adminRemoteDataSource.banUser(userId)
        }
    }

    override suspend fun unbanUser(userId: String): Result<Unit> {
        return safeCall {
            adminRemoteDataSource.unbanUser(userId)
        }
    }

    override suspend fun deleteUserPermanently(userId: String): Result<Unit> {
        return safeCall {
            adminRemoteDataSource.deleteUserPermanently(userId)
        }
    }

    // ========== QUIZ MANAGEMENT ==========

    override fun getAllQuizzes(includeDeleted: Boolean): Flow<List<Quiz>> {
        return adminRemoteDataSource.getAllQuizzes(includeDeleted)
            .map { quizDtos -> quizDtos.map { it.toDomain() } }
    }

    override suspend fun deleteQuizPermanently(quizId: String): Result<Unit> {
        return safeCall {
            adminRemoteDataSource.deleteQuizPermanently(quizId)
        }
    }

    override suspend fun restoreQuiz(quizId: String): Result<Unit> {
        return safeCall {
            adminRemoteDataSource.restoreQuiz(quizId)
        }
    }

    override suspend fun forcePublishQuiz(quizId: String): Result<Unit> {
        return safeCall {
            adminRemoteDataSource.forcePublishQuiz(quizId)
        }
    }

    override suspend fun unpublishQuiz(quizId: String): Result<Unit> {
        return safeCall {
            adminRemoteDataSource.unpublishQuiz(quizId)
        }
    }

    // ========== ATTEMPT MANAGEMENT ==========

    override fun getAllAttempts(): Flow<List<Attempt>> {
        return adminRemoteDataSource.getAllAttempts()
            .map { attemptDtos -> attemptDtos.map { it.toDomain() } }
    }

    override suspend fun deleteAttempt(attemptId: String): Result<Unit> {
        return safeCall {
            adminRemoteDataSource.deleteAttempt(attemptId)
        }
    }

    // ========== STATISTICS ==========

    override fun getSystemStats(): Flow<SystemStats> = flow {
        // Fetch all statistics in parallel using coroutineScope + async
        val stats = coroutineScope {
            val totalUsers = async { adminRemoteDataSource.getTotalUsersCount() }
            val totalQuizzes = async { adminRemoteDataSource.getTotalQuizzesCount() }
            val totalAttempts = async { adminRemoteDataSource.getTotalAttemptsCount() }
            val totalQuestionsInPool = async { adminRemoteDataSource.getTotalQuestionsInPoolCount() }
            val activeUsers = async { adminRemoteDataSource.getActiveUsersCount() }
            val publicQuizzes = async { adminRemoteDataSource.getPublicQuizzesCount() }
            val privateQuizzes = async { adminRemoteDataSource.getPrivateQuizzesCount() }
            val deletedQuizzes = async { adminRemoteDataSource.getDeletedQuizzesCount() }
            val adminUsers = async { adminRemoteDataSource.getAdminUsersCount() }

            SystemStats(
                totalUsers = totalUsers.await(),
                totalQuizzes = totalQuizzes.await(),
                totalAttempts = totalAttempts.await(),
                totalQuestionsInPool = totalQuestionsInPool.await(),
                activeUsers = activeUsers.await(),
                publicQuizzes = publicQuizzes.await(),
                privateQuizzes = privateQuizzes.await(),
                deletedQuizzes = deletedQuizzes.await(),
                adminUsers = adminUsers.await()
            )
        }

        emit(stats)
    }

    // ========== SEARCH ==========

    override fun searchUsers(query: String): Flow<List<User>> {
        return if (query.isBlank()) {
            getAllUsers()
        } else {
            adminRemoteDataSource.searchUsers(query)
                .map { userDtos -> userDtos.map { it.toDomain() } }
        }
    }

    override fun searchQuizzes(query: String, includeDeleted: Boolean): Flow<List<Quiz>> {
        return if (query.isBlank()) {
            getAllQuizzes(includeDeleted)
        } else {
            adminRemoteDataSource.searchQuizzes(query, includeDeleted)
                .map { quizDtos -> quizDtos.map { it.toDomain() } }
        }
    }

    // ==================== Paginated queries ====================

    override suspend fun getUsersPage(pageSize: Int, loadMore: Boolean): PaginatedResult<User> {
        if (!loadMore) lastUserDoc = null
        val (dtos, lastDoc) = adminRemoteDataSource.getUsersPage(pageSize, lastUserDoc)
        lastUserDoc = lastDoc
        return PaginatedResult(
            items = dtos.map { it.toDomain() },
            hasMore = dtos.size >= pageSize
        )
    }

    override suspend fun getQuizzesPage(
        pageSize: Int,
        includeDeleted: Boolean,
        loadMore: Boolean
    ): PaginatedResult<Quiz> {
        if (!loadMore) lastQuizDoc = null
        val (dtos, lastDoc) = adminRemoteDataSource.getQuizzesPage(
            pageSize, includeDeleted, lastQuizDoc
        )
        lastQuizDoc = lastDoc
        return PaginatedResult(
            items = dtos.map { it.toDomain() },
            hasMore = dtos.size >= pageSize
        )
    }

    // ========== PERMISSION MANAGEMENT ==========

    override suspend fun updateAdminPermissions(
        userId: String,
        permissions: Set<AdminPermission>
    ): Result<Unit> {
        return safeCall {
            adminRemoteDataSource.updateUserPermissions(
                userId,
                permissions.map { it.toStorageValue() }
            )
        }
    }

    override suspend fun getCurrentAdminPermissions(): Set<AdminPermission> {
        return try {
            val user = adminRemoteDataSource.getCurrentUser()?.toDomain() ?: return emptySet()
            user.effectivePermissions()
        } catch (_: Exception) {
            emptySet()
        }
    }
}
