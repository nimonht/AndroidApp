package com.example.androidapp.data.repository

import com.example.androidapp.data.remote.firebase.PoolRemoteDataSource
import com.example.androidapp.data.remote.toDomain
import com.example.androidapp.data.remote.toDto
import com.example.androidapp.domain.model.Question
import com.example.androidapp.domain.model.QuestionPoolItem
import com.example.androidapp.domain.repository.PoolRepository
import com.google.firebase.firestore.WriteBatch
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Remote-only implementation of [PoolRepository].
 *
 * All operations delegate directly to [PoolRemoteDataSource] since pool items
 * are not cached locally in Room.
 *
 * @property remoteDataSource Firestore data source for question pool CRUD operations.
 * @property firestore Firestore instance for batch operations.
 */
class PoolRepositoryImpl(
    private val remoteDataSource: PoolRemoteDataSource,
    private val firestore: FirebaseFirestore
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
            val effectiveContributorId = if (anonymize) null else authorId

            // Use WriteBatch for atomic multi-document writes
            val batch: WriteBatch = firestore.batch()
            val collectionRef = firestore.collection("questionPool")

            questions.forEach { question ->
                val poolItem = QuestionPoolItem(
                    id = UUID.randomUUID().toString(),
                    question = question,
                    contributorId = effectiveContributorId,
                    sourceQuizId = "",  // Will be set by caller if needed
                    tags = tags,
                    usageCount = 0,
                    isActive = true
                )
                val docRef = collectionRef.document(poolItem.id)
                batch.set(docRef, poolItem.toDto())
            }

            // Commit the batch atomically
            batch.commit().addOnFailureListener { exception ->
                throw exception
            }.await()

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
            // Validate count parameter up front
            if (count < 0) {
                return Result.failure(
                    IllegalArgumentException("Count must be non-negative, got: $count")
                )
            }

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

