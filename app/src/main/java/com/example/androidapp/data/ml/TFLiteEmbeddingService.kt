package com.example.androidapp.data.ml

import android.content.Context
import android.util.Log
import com.example.androidapp.domain.service.EmbeddingService
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder.TextEmbedderOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

/**
 * On-device embedding service backed by MediaPipe Tasks Text.
 *
 * Uses MediaPipe [com.google.mediapipe.tasks.text.textembedder.TextEmbedder]
 * as the inference backend, which handles model loading and tokenization
 * internally.
 *
 * Produces dense embedding vectors whose dimensionality depends on the bundled
 * model. All inference runs on [Dispatchers.IO] to avoid blocking the UI thread.
 *
 * The model file is resolved by [ModelManager] (local cache -> bundled asset).
 * Thread-safety is guaranteed by a [Mutex] around the underlying embedder
 * instance.
 *
 * Implements [Closeable] to release native resources.
 */
class TFLiteEmbeddingService(
    private val context: Context,
    private val modelManager: ModelManager
) : EmbeddingService, Closeable {

    private val _isReady = MutableStateFlow(false)
    override val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    /**
     * Observable model download/loading state, delegated from [ModelManager].
     * The UI layer uses this to show download progress indicators.
     */
    val modelState: StateFlow<ModelState> get() = modelManager.state

    // --- MediaPipe path ---
    private var mediaPipeEmbedder: TextEmbedder? = null

    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch { initialize() }
    }

    // ------------------------------------------------------------------
    // Initialization
    // ------------------------------------------------------------------

    private suspend fun initialize() {
        if (tryInitializeMediaPipe()) return

        Log.w(
            TAG,
            "MediaPipe initialization failed. Semantic search will be unavailable."
        )
    }

    // ---- MediaPipe path ------------------------------------------------

    /**
     * Attempts to create a MediaPipe [TextEmbedder] from the bundled asset
     * or, failing that, from a [ModelManager]-resolved file.
     *
     * @return `true` if the embedder was created successfully.
     */
    private suspend fun tryInitializeMediaPipe(): Boolean {
        // Fast path: load directly from the APK asset.
        try {
            val embedder = createMediaPipeFromAsset()
            if (embedder != null) {
                mutex.withLock {
                    mediaPipeEmbedder = embedder
                    _isReady.value = true
                    modelManager.reportReady()
                }
                Log.d(TAG, "MediaPipe TextEmbedder initialized from bundled asset")
                return true
            }
        } catch (e: Throwable) {
            // Catch Throwable (not just Exception) because MediaPipe's static
            // initializer throws UnsatisfiedLinkError (an Error, not Exception)
            // when the native library is missing for the current ABI (e.g. x86_64).
            Log.d(TAG, "MediaPipe asset path failed: ${e.message}")
        }

        // File path: local cache or Firebase ML download.
        try {
            val modelFile = modelManager.getModelFile()
            if (modelFile != null) {
                val embedder = createMediaPipeFromFile(modelFile)
                if (embedder != null) {
                    mutex.withLock {
                        mediaPipeEmbedder = embedder
                        _isReady.value = true
                        modelManager.reportReady()
                    }
                    Log.d(
                        TAG,
                        "MediaPipe TextEmbedder initialized from file " +
                                "(${modelFile.length() / 1_048_576} MB)"
                    )
                    return true
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "MediaPipe file path failed: ${e.message}")
        }

        return false
    }

    private fun createMediaPipeFromAsset(): TextEmbedder? {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath(ModelManager.MODEL_FILE_NAME)
            .build()
        val options = TextEmbedderOptions.builder()
            .setBaseOptions(baseOptions)
            .build()
        return TextEmbedder.createFromOptions(context, options)
    }

    private fun createMediaPipeFromFile(
        file: File
    ): TextEmbedder? {
        // Memory-map the model file into a ByteBuffer for zero-copy loading.
        val channel = FileInputStream(file).channel
        val buffer: ByteBuffer =
            channel.map(FileChannel.MapMode.READ_ONLY, 0, file.length())
        channel.close()

        val baseOptions = BaseOptions.builder()
            .setModelAssetBuffer(buffer)
            .build()
        val options = TextEmbedderOptions.builder()
            .setBaseOptions(baseOptions)
            .build()
        return TextEmbedder.createFromOptions(context, options)
    }

    // ------------------------------------------------------------------
    // Inference
    // ------------------------------------------------------------------

    override suspend fun embed(text: String): FloatArray? = withContext(Dispatchers.IO) {
        if (!_isReady.value) return@withContext null
        mutex.withLock {
            val truncated = text.take(MAX_INPUT_LENGTH)

            mediaPipeEmbedder?.let { embedder ->
                return@withContext try {
                    val result = embedder.embed(truncated)
                    val embeddings = result.embeddingResult().embeddings()
                    if (embeddings.isNotEmpty()) {
                        embeddings[0].floatEmbedding()
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "MediaPipe inference failed: '${truncated.take(50)}...'", e)
                    null
                }
            }

            null
        }
    }

    override suspend fun embedBatch(texts: List<String>): List<FloatArray?> {
        return texts.map { embed(it) }
    }

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    /**
     * Releases all native resources and cancels the background scope.
     *
     * After calling this method the service will no longer produce embeddings.
     * This is safe to call multiple times.
     */
    override fun close() {
        _isReady.value = false
        scope.cancel()

        mediaPipeEmbedder?.close()
        mediaPipeEmbedder = null

        Log.d(TAG, "TFLite embedding service closed")
    }

    companion object {
        private const val TAG = "TFLiteEmbedding"

        /** Maximum input character length to prevent OOM on very large texts. */
        private const val MAX_INPUT_LENGTH = 2048
    }
}
