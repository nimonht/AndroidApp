package com.example.androidapp.data.sync

import android.content.Context
import android.util.Log
import com.example.androidapp.data.local.LocalQuizPurger
import com.example.androidapp.data.local.dao.ChoiceDao
import com.example.androidapp.data.local.dao.QuestionDao
import com.example.androidapp.data.local.dao.QuizDao
import com.example.androidapp.data.network.NetworkMonitor
import com.example.androidapp.data.remote.firebase.QuizRemoteDataSource

/**
 * Manages invalidation of locally-cached quizzes that have been permanently
 * deleted from Firestore by other users, admin actions, or maintenance workers.
 *
 * Instead of re-fetching all quizzes to detect deletions (expensive for a large
 * Firestore), this manager queries a lightweight `quizDeletions` tombstone
 * collection that records recent permanent deletions. Clients track their last
 * check timestamp locally and only query for new tombstones each cycle.
 *
 * Three invalidation tiers (cheapest to most thorough):
 *
 * 1. **Lazy validation** -- [validateQuizExists] checks a single quiz on-demand
 *    when a user opens a cached quiz they do not own. Cost: 1 Firestore read.
 * 2. **Periodic tombstone sweep** -- [checkForDeletedQuizzes] runs during each
 *    background sync cycle, querying tombstones created since the last check.
 *    Cost: typically 1 small query returning 0-5 documents.
 * 3. **Full stale cleanup** -- existing logic in [SyncManager] and the repository
 *    layer re-fetches and compares full ID sets (unchanged safety net).
 *
 * @param context       Application context for accessing SharedPreferences.
 * @param quizDao       Room DAO for quiz entity operations.
 * @param questionDao   Room DAO for question entity operations.
 * @param choiceDao     Room DAO for choice entity operations.
 * @param quizRemoteDataSource Firestore data source for quiz and tombstone queries.
 * @param networkMonitor Network connectivity observer.
 */
class QuizInvalidationManager(
    private val context: Context,
    private val quizDao: QuizDao,
    private val questionDao: QuestionDao,
    private val choiceDao: ChoiceDao,
    private val quizRemoteDataSource: QuizRemoteDataSource,
    private val networkMonitor: NetworkMonitor
) {

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Queries the `quizDeletions` tombstone collection for entries created
     * since the last check, and removes matching quizzes from the local Room
     * database. Updates the last-check timestamp on success.
     *
     * This is the primary invalidation mechanism -- called during each
     * background sync cycle. Typical cost is a single Firestore query
     * returning very few documents.
     *
     * @return the number of local quizzes purged, or -1 if the check was
     *         skipped (offline or error).
     */
    suspend fun checkForDeletedQuizzes(): Int {
        if (!networkMonitor.isOnline.value) return -1

        return try {
            val lastCheck = prefs.getLong(KEY_LAST_INVALIDATION_CHECK, 0L)
            val deletedQuizIds = quizRemoteDataSource.getDeletionsSince(lastCheck)

            if (deletedQuizIds.isEmpty()) {
                updateLastCheckTimestamp()
                return 0
            }

            var purgedCount = 0
            for (quizId in deletedQuizIds) {
                val localQuiz = quizDao.getQuizById(quizId)
                if (localQuiz != null) {
                    LocalQuizPurger.purgeLocalQuiz(quizId, quizDao, questionDao, choiceDao)
                    purgedCount++
                }
            }

            updateLastCheckTimestamp()
            Log.d(
                TAG,
                "Invalidation sweep complete: purged $purgedCount local quizzes " +
                        "from ${deletedQuizIds.size} tombstones."
            )
            purgedCount
        } catch (e: Exception) {
            Log.e(TAG, "Invalidation check failed", e)
            -1
        }
    }

    /**
     * Validates whether a specific quiz still exists on Firestore and is not
     * soft-deleted. If the quiz document is missing or has a non-null
     * `deletedAt` timestamp, purges it from the local Room database.
     *
     * Use this for on-demand validation when a user opens a cached quiz
     * that they do not own -- avoids serving stale or deleted content.
     *
     * When the device is offline the method returns `true` (optimistic)
     * so the user can still access previously cached data.
     *
     * @param quizId the Firestore document ID of the quiz to validate.
     * @return `true` if the quiz still exists and is active on Firestore,
     *         `false` if it was deleted and purged from local storage.
     */
    suspend fun validateQuizExists(quizId: String): Boolean {
        if (!networkMonitor.isOnline.value) return true

        return try {
            val remoteQuiz = quizRemoteDataSource.getQuizById(quizId)
            if (remoteQuiz == null || remoteQuiz.deletedAt != null) {
                LocalQuizPurger.purgeLocalQuiz(quizId, quizDao, questionDao, choiceDao)
                Log.d(TAG, "Quiz $quizId no longer exists on remote; purged from local cache.")
                false
            } else {
                true
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to validate quiz $quizId; assuming still valid.", e)
            true
        }
    }

    /**
     * Persists the current wall-clock time as the last successful
     * invalidation check timestamp. Subsequent calls to
     * [checkForDeletedQuizzes] will only query tombstones newer than
     * this value.
     */
    private fun updateLastCheckTimestamp() {
        prefs.edit()
            .putLong(KEY_LAST_INVALIDATION_CHECK, System.currentTimeMillis())
            .apply()
    }

    companion object {
        private const val TAG = "QuizInvalidation"
        private const val PREFS_NAME = "quiz_invalidation_prefs"
        private const val KEY_LAST_INVALIDATION_CHECK = "last_invalidation_check_at"
    }
}
