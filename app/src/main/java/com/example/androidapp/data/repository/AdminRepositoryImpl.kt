package com.example.androidapp.data.repository

import com.example.androidapp.data.remote.firebase.AdminRemoteDataSource
import com.example.androidapp.data.remote.toDomain
import com.example.androidapp.domain.model.Attempt
import com.example.androidapp.domain.model.Quiz
import com.example.androidapp.domain.model.SystemStats
import com.example.androidapp.domain.model.User
import com.example.androidapp.domain.model.UserRole
import com.example.androidapp.domain.repository.AdminRepository
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

    // ========== USER MANAGEMENT ==========

    override fun getAllUsers(): Flow<List<User>> {
        return adminRemoteDataSource.getAllUsers()
            .map { userDtos -> userDtos.map { it.toDomain() } }
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
}
