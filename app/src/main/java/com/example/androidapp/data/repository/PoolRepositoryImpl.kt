package com.example.androidapp.data.repository

import android.util.Log
import com.example.androidapp.data.remote.firebase.FirestoreCollections
import com.example.androidapp.data.remote.firebase.PoolRemoteDataSource
import com.example.androidapp.data.remote.toDomain
import com.example.androidapp.data.remote.toDto
import com.example.androidapp.domain.model.Question
import com.example.androidapp.domain.model.PaginatedResult
import com.example.androidapp.domain.model.QuestionPoolItem
import com.example.androidapp.domain.repository.PoolRepository
import com.example.androidapp.domain.util.safeCall
import com.google.firebase.firestore.DocumentSnapshot
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

    // NOTE: Pagination cursors are stored as mutable fields on this singleton.
    // This is acceptable because each paginated screen has its own independent
    // cursor (contributions vs browse) and is only accessed from a single
    // ViewModel at a time. If concurrent access becomes necessary, cursors
    // should be moved to the ViewModel layer (passed as parameters and
    // returned alongside results).

    /** Cursor for contributions pagination. */
    private var lastContributionDoc: DocumentSnapshot? = null

    /** Cursor for pool browse pagination. */
    private var lastBrowseDoc: DocumentSnapshot? = null

    override suspend fun contributeQuestion(poolItem: QuestionPoolItem): Result<Unit> {
        return safeCall {
            remoteDataSource.addPoolItem(poolItem.toDto())
        }
    }

    override suspend fun contributeQuestions(
        questions: List<Question>,
        contributorId: String,
        sourceQuizId: String,
        tags: List<String>,
        anonymize: Boolean
    ): Result<Unit> {
        return safeCall {
            val effectiveContributorId = if (anonymize) null else contributorId

            // Use WriteBatch for atomic multi-document writes
            val batch: WriteBatch = firestore.batch()
            val collectionRef = firestore.collection(FirestoreCollections.QUESTION_POOL)

            questions.forEach { question ->
                val poolItem = QuestionPoolItem(
                    id = UUID.randomUUID().toString(),
                    question = question,
                    contributorId = effectiveContributorId,
                    sourceQuizId = sourceQuizId,
                    tags = tags,
                    usageCount = 0,
                    isActive = true
                )
                val docRef = collectionRef.document(poolItem.id)
                batch.set(docRef, poolItem.toDto())
            }

            // Commit the batch atomically; await() will throw on failure
            batch.commit().await()
            Log.d(TAG, "contributeQuestions: committed ${questions.size} items for quiz $sourceQuizId")
            Unit
        }.also { result ->
            result.exceptionOrNull()?.let { e ->
                Log.e(TAG, "contributeQuestions: batch commit failed for quiz $sourceQuizId", e)
            }
        }
    }

    override suspend fun getPoolQuestionsByTags(
        tags: List<String>,
        activeOnly: Boolean
    ): Result<List<QuestionPoolItem>> {
        return safeCall {
            val dtos = if (activeOnly) {
                remoteDataSource.getActivePoolItemsByTags(tags)
            } else {
                remoteDataSource.getPoolItemsByTags(tags)
            }
            dtos.map { it.toDomain() }
        }
    }

    override suspend fun getMyContributions(userId: String): Result<List<QuestionPoolItem>> {
        return safeCall {
            val dtos = remoteDataSource.getContributionsByUser(userId)
            dtos.map { it.toDomain() }
        }
    }

    override suspend fun revokeContribution(poolItemId: String): Result<Unit> {
        Log.d(TAG, "revokeContribution: revoking pool item $poolItemId")
        return safeCall {
            remoteDataSource.setPoolItemActive(poolItemId, false)
            Log.d(TAG, "revokeContribution: successfully revoked pool item $poolItemId")
            Unit
        }.also { result ->
            result.exceptionOrNull()?.let { e ->
                Log.e(TAG, "revokeContribution: failed to revoke pool item $poolItemId", e)
            }
        }
    }

    override suspend fun incrementUsageCount(poolItemId: String): Result<Unit> {
        return safeCall {
            remoteDataSource.incrementUsageCount(poolItemId)
        }
    }

    override suspend fun autoGenerateQuiz(
        tags: List<String>,
        count: Int
    ): Result<List<QuestionPoolItem>> {
        // Validate count parameter up front
        if (count < 0) {
            return Result.failure(
                IllegalArgumentException("Count must be non-negative, got: $count")
            )
        }

        return safeCall {
            val dtos = remoteDataSource.getActivePoolItemsByTags(tags)
            dtos.shuffled()
                .take(count)
                .map { it.toDomain() }
        }
    }

    // ==================== Paginated query implementations ====================

    override suspend fun getMyContributionsPaged(
        userId: String,
        pageSize: Int,
        loadMore: Boolean
    ): Result<PaginatedResult<QuestionPoolItem>> {
        return safeCall {
            if (!loadMore) lastContributionDoc = null
            val (dtos, lastDoc) = remoteDataSource.getContributionsByUserPaged(
                userId, pageSize, lastContributionDoc
            )
            lastContributionDoc = lastDoc
            PaginatedResult(
                items = dtos.map { it.toDomain() },
                hasMore = dtos.size >= pageSize
            )
        }
    }

    override suspend fun getPoolQuestionsByTagsPaged(
        tags: List<String>,
        activeOnly: Boolean,
        pageSize: Int,
        loadMore: Boolean
    ): Result<PaginatedResult<QuestionPoolItem>> {
        return safeCall {
            if (!loadMore) lastBrowseDoc = null
            val (dtos, lastDoc) = remoteDataSource.getActivePoolItemsByTagsPaged(
                tags, pageSize, lastBrowseDoc
            )
            lastBrowseDoc = lastDoc
            PaginatedResult(
                items = dtos.map { it.toDomain() },
                hasMore = dtos.size >= pageSize
            )
        }
    }

    companion object {
        private const val TAG = "PoolRepositoryImpl"
    }
}
