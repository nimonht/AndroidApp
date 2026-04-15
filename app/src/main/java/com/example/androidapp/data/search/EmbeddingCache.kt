package com.example.androidapp.data.search

import android.util.Log
import com.example.androidapp.data.local.dao.QuizDao
import com.example.androidapp.domain.service.EmbeddingIndex
import com.example.androidapp.domain.service.EmbeddingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory cache of quiz embeddings loaded from Room.
 *
 * Implements [EmbeddingIndex] so the UI and domain layers can depend on the
 * interface without importing this data-layer class. Backed by a
 * [ConcurrentHashMap] for lock-free concurrent reads. Periodically reloads
 * from Room to pick up new embeddings.
 *
 * Memory footprint estimate:
 * - 1,000 quizzes x 100 dims x 4 bytes = ~0.4 MB
 * - 10,000 quizzes x 100 dims x 4 bytes = ~4 MB
 *
 * @param quizDao DAO for loading embeddings from Room.
 * @param reindexTrigger Optional callback invoked by [requestFullReindex].
 *   Typically wired to [EmbeddingIndexWorker.enqueueFullIndex] in the DI layer.
 * @param modelVersion Current embedding model version for filtering stale entries.
 */
class EmbeddingCache(
    private val quizDao: QuizDao,
    private val reindexTrigger: (() -> Unit)? = null,
    private val modelVersion: Int = EmbeddingService.CURRENT_MODEL_VERSION
) : EmbeddingIndex, Closeable {

    private val store = ConcurrentHashMap<String, FloatArray>()
    private val _isReady = MutableStateFlow(false)

    @Volatile
    private var hasTriggeredReindexForEmptyStore = false

    /** True when at least one embedding has been loaded into the cache. */
    override val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            loadAll()
            observeChanges()
        }
    }

    /** Returns an unmodifiable snapshot of all cached embeddings. */
    override fun snapshot(): Map<String, FloatArray> =
        Collections.unmodifiableMap(HashMap(store))

    /** Returns the embedding for a single quiz, or null if not cached. */
    override operator fun get(quizId: String): FloatArray? = store[quizId]

    /** Number of entries currently in the cache. */
    override val size: Int get() = store.size

    /**
     * Triggers a full re-index of all quiz embeddings via the configured
     * [reindexTrigger] callback. Returns false if no trigger is configured.
     */
    override fun requestFullReindex(): Boolean {
        val trigger = reindexTrigger ?: return false
        trigger()
        Log.d(TAG, "Full reindex requested via trigger")
        return true
    }

    private suspend fun loadAll() {
        try {
            val rows = quizDao.getAllEmbeddings(modelVersion)
            val freshIds = HashSet<String>(rows.size * 2)
            var loaded = 0
            rows.forEach { row ->
                row.embedding?.let { bytes ->
                    bytesToFloatArray(bytes)?.let { floats ->
                        store[row.id] = floats
                        freshIds.add(row.id)
                        loaded++
                    }
                }
            }
            // Evict embeddings for quizzes no longer in the active index.
            // getAllEmbeddings filters WHERE deleted_at IS NULL, so soft-deleted
            // quizzes are absent from freshIds and will be removed here.
            store.keys.retainAll(freshIds)
            _isReady.value = store.isNotEmpty()  // reflects actual store content, not just this poll
            // Fallback auto-trigger: if the cache is empty after loading, there may be
            // quizzes in Room that haven't been indexed yet. Trigger once per empty period
            // so embeddings are computed even if the repository trigger was missed.
            if (store.isNotEmpty()) {
                // Store has data; allow a future re-trigger if it empties again.
                hasTriggeredReindexForEmptyStore = false
            } else if (!hasTriggeredReindexForEmptyStore) {
                hasTriggeredReindexForEmptyStore = true
                requestFullReindex()
                Log.d(TAG, "Cache empty after load — auto-triggered full reindex as fallback")
            }
            Log.d(TAG, "Loaded $loaded embeddings into cache (${store.size} total)")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load embeddings: ${e.message}")
        }
    }

    /**
     * Polls Room periodically for newly-indexed embeddings.
     * Uses a 15-second interval to balance freshness and resource usage.
     */
    private suspend fun observeChanges() {
        while (true) {
            delay(POLL_INTERVAL_MS)
            loadAll()
        }
    }

    private fun bytesToFloatArray(bytes: ByteArray): FloatArray? {
        if (bytes.isEmpty()) return null
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
        return FloatArray(buf.remaining()) { buf.get() }
    }

    /**
     * Cancels the background polling scope.
     */
    override fun close() {
        scope.cancel()
        Log.d(TAG, "Embedding cache closed")
    }

    companion object {
        private const val TAG = "EmbeddingCache"
        private const val POLL_INTERVAL_MS = 15_000L
    }
}
