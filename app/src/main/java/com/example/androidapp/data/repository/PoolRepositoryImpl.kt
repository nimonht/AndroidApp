package com.example.androidapp.data.repository

import com.example.androidapp.data.remote.firebase.PoolRemoteDataSource
import com.example.androidapp.data.remote.toDomain
import com.example.androidapp.data.remote.toDto
import com.example.androidapp.domain.model.Question
import com.example.androidapp.domain.model.QuestionPoolItem
import com.example.androidapp.domain.repository.PoolRepository
import java.util.UUID

/**
 * Remote-only implementation of [PoolRepository].
 *
 * All operations delegate directly to [PoolRemoteDataSource] since pool items
 * are not cached locally in Room.
 *
 * @property remoteDataSource Firestore data source for question pool CRUD operations.
 */
class PoolRepositoryImpl(
    private val remoteDataSource: PoolRemoteDataSource
) : PoolRepository {

    /** {@inheritDoc} */
    override suspend fun contributeQuestion(poolItem: QuestionPoolItem): Result<Unit> {
        return try {
            remoteDataSource.addPoolItem(poolItem.toDto())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** {@inheritDoc} */
    override suspend fun contributeQuestions(
        questions: List<Question>,
        authorId: String,
        tags: List<String>,
        anonymize: Boolean
    ): Result<Unit> {
        return try {
            val effectiveAuthorId = if (anonymize) "" else authorId
            questions.forEach { question ->
                val poolItem = QuestionPoolItem(
                    id = UUID.randomUUID().toString(),
                    question = question,
                    authorId = effectiveAuthorId,
                    tags = tags,
                    usageCount = 0,
                    isActive = true
                )
                remoteDataSource.addPoolItem(poolItem.toDto())
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** {@inheritDoc} */
    override suspend fun getPoolQuestionsByTags(
        tags: List<String>,
        activeOnly: Boolean
    ): Result<List<QuestionPoolItem>> {
        return try {
            val dtos = if (activeOnly) {
                remoteDataSource.getActivePoolItemsByTags(tags)
            } else {
                remoteDataSource.getPoolItemsByTags(tags)
            }
            Result.success(dtos.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** {@inheritDoc} */
    override suspend fun getMyContributions(userId: String): Result<List<QuestionPoolItem>> {
        return try {
            val dtos = remoteDataSource.getContributionsByUser(userId)
            Result.success(dtos.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** {@inheritDoc} */
    override suspend fun revokeContribution(poolItemId: String): Result<Unit> {
        return try {
            remoteDataSource.setPoolItemActive(poolItemId, false)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** {@inheritDoc} */
    override suspend fun incrementUsageCount(poolItemId: String): Result<Unit> {
        return try {
            remoteDataSource.incrementUsageCount(poolItemId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** {@inheritDoc} */
    override suspend fun autoGenerateQuiz(
        tags: List<String>,
        count: Int
    ): Result<List<QuestionPoolItem>> {
        return try {
            val dtos = remoteDataSource.getActivePoolItemsByTags(tags)
            val selected = dtos
                .shuffled()
                .take(count)
                .map { it.toDomain() }
            Result.success(selected)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

