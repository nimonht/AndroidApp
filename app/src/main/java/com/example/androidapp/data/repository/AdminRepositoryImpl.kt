package com.example.androidapp.data.repository

import com.example.androidapp.data.remote.firebase.AdminRemoteDataSource
import com.example.androidapp.data.remote.toDomain
import com.example.androidapp.domain.model.Attempt
import com.example.androidapp.domain.model.Quiz
import com.example.androidapp.domain.model.SystemStats
import com.example.androidapp.domain.model.User
import com.example.androidapp.domain.model.UserRole
import com.example.androidapp.domain.repository.AdminRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
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

    // ========== USER MANAGEMENT ==========

    override fun getAllUsers(): Flow<List<User>> {
        return adminRemoteDataSource.getAllUsers()
            .map { userDtos -> userDtos.map { it.toDomain() } }
            .catch { e ->
                // Log error and emit empty list as fallback
                emit(emptyList())
            }
    }

    override suspend fun updateUserRole(userId: String, newRole: UserRole): Result<Unit> {
        return try {
            adminRemoteDataSource.updateUserRole(userId, newRole.toFirestoreValue())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun banUser(userId: String): Result<Unit> {
        return try {
            adminRemoteDataSource.banUser(userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unbanUser(userId: String): Result<Unit> {
        return try {
            adminRemoteDataSource.unbanUser(userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteUserPermanently(userId: String): Result<Unit> {
        return try {
            adminRemoteDataSource.deleteUserPermanently(userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== QUIZ MANAGEMENT ==========

    override fun getAllQuizzes(includeDeleted: Boolean): Flow<List<Quiz>> {
        return adminRemoteDataSource.getAllQuizzes(includeDeleted)
            .map { quizDtos -> quizDtos.map { it.toDomain() } }
            .catch { e ->
                // Log error and emit empty list as fallback
                emit(emptyList())
            }
    }

    override suspend fun deleteQuizPermanently(quizId: String): Result<Unit> {
        return try {
            adminRemoteDataSource.deleteQuizPermanently(quizId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun restoreQuiz(quizId: String): Result<Unit> {
        return try {
            adminRemoteDataSource.restoreQuiz(quizId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun forcePublishQuiz(quizId: String): Result<Unit> {
        return try {
            adminRemoteDataSource.forcePublishQuiz(quizId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unpublishQuiz(quizId: String): Result<Unit> {
        return try {
            adminRemoteDataSource.unpublishQuiz(quizId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== ATTEMPT MANAGEMENT ==========

    override fun getAllAttempts(): Flow<List<Attempt>> {
        return adminRemoteDataSource.getAllAttempts()
            .map { attemptDtos -> attemptDtos.map { it.toDomain() } }
            .catch { e ->
                // Log error and emit empty list as fallback
                emit(emptyList())
            }
    }

    override suspend fun deleteAttempt(attemptId: String): Result<Unit> {
        return try {
            adminRemoteDataSource.deleteAttempt(attemptId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== STATISTICS ==========

    override fun getSystemStats(): Flow<SystemStats> = flow {
        try {
            // Fetch all statistics in parallel
            val totalUsers = adminRemoteDataSource.getTotalUsersCount()
            val totalQuizzes = adminRemoteDataSource.getTotalQuizzesCount()
            val totalAttempts = adminRemoteDataSource.getTotalAttemptsCount()
            val totalQuestionsInPool = adminRemoteDataSource.getTotalQuestionsInPoolCount()
            val activeUsers = adminRemoteDataSource.getActiveUsersCount()
            val publicQuizzes = adminRemoteDataSource.getPublicQuizzesCount()
            val privateQuizzes = adminRemoteDataSource.getPrivateQuizzesCount()
            val draftQuizzes = adminRemoteDataSource.getDraftQuizzesCount()
            val deletedQuizzes = adminRemoteDataSource.getDeletedQuizzesCount()
            val adminUsers = adminRemoteDataSource.getAdminUsersCount()

            val stats = SystemStats(
                totalUsers = totalUsers,
                totalQuizzes = totalQuizzes,
                totalAttempts = totalAttempts,
                totalQuestionsInPool = totalQuestionsInPool,
                activeUsers = activeUsers,
                publicQuizzes = publicQuizzes,
                privateQuizzes = privateQuizzes,
                draftQuizzes = draftQuizzes,
                deletedQuizzes = deletedQuizzes,
                adminUsers = adminUsers
            )

            emit(stats)
        } catch (e: Exception) {
            // Emit default stats on error
            emit(SystemStats())
        }
    }.catch { e ->
        // Fallback to empty stats
        emit(SystemStats())
    }

    // ========== SEARCH ==========

    override fun searchUsers(query: String): Flow<List<User>> {
        return if (query.isBlank()) {
            getAllUsers()
        } else {
            adminRemoteDataSource.searchUsers(query)
                .map { userDtos -> userDtos.map { it.toDomain() } }
                .catch { e ->
                    emit(emptyList())
                }
        }
    }

    override fun searchQuizzes(query: String, includeDeleted: Boolean): Flow<List<Quiz>> {
        return if (query.isBlank()) {
            getAllQuizzes(includeDeleted)
        } else {
            adminRemoteDataSource.searchQuizzes(query, includeDeleted)
                .map { quizDtos -> quizDtos.map { it.toDomain() } }
                .catch { e ->
                    emit(emptyList())
                }
        }
    }
}
