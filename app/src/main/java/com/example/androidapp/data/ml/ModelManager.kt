package com.example.androidapp.data.ml

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * Manages the TFLite model lifecycle with a two-tier resolution strategy.
 *
 * Resolution order (first success wins):
 * 1. **Local cache** -- previously copied model in [Context.getFilesDir].
 * 2. **Bundled asset** -- copied from the APK asset to the local cache, or
 *    memory-mapped directly via [openBundledAssetBuffer] (zero-copy, fast).
 *
 * The model (~6 MB) is bundled as an APK asset; no network download is needed.
 *
 * Callers observe [state] to track loading progress and display appropriate
 * UI (e.g., a loading indicator on the Search screen).
 */
class ModelManager(private val context: Context) {

    private val _state = MutableStateFlow<ModelState>(ModelState.Idle)

    /** Observable loading state for UI consumption. */
    val state: StateFlow<ModelState> = _state.asStateFlow()

    /**
     * Returns a [File] pointing to the ready-to-use TFLite model.
     *
     * Tries each tier in order. Updates [state] as it progresses.
     * Returns null only if all tiers fail.
     */
    suspend fun getModelFile(): File? = withContext(Dispatchers.IO) {
        // Tier 1: Local cache (fastest path — previously copied from bundled asset)
        val cached = getLocalCache()
        if (cached != null) {
            // State will be set to Ready by TFLiteEmbeddingService.reportReady() after interpreter creation
            Log.d(TAG, "Model loaded from local cache (${cached.length() / 1_048_576} MB)")
            return@withContext cached
        }

        // Tier 2: Bundled asset (model shipped inside the APK)
        val asset = copyFromBundledAsset()
        if (asset != null) {
            // State will be set to Ready by TFLiteEmbeddingService.reportReady() after interpreter creation
            Log.d(TAG, "Model copied from bundled asset (${asset.length() / 1_048_576} MB)")
            return@withContext asset
        }

        _state.value = ModelState.Failed("Mo hinh khong kha dung. Dam bao file mo hinh ton tai trong thu muc assets.")
        Log.w(
            TAG, "All model resolution tiers failed. " +
                    "Download the model with: curl -L -o app/src/main/assets/use_multilingual_lite.tflite " +
                    "https://storage.googleapis.com/mediapipe-models/text_embedder/universal_sentence_encoder/float32/latest/universal_sentence_encoder.tflite"
        )
        return@withContext null
    }

    // ------------------------------------------------------------------
    // Tier 1: Local cache
    // ------------------------------------------------------------------

    private fun getLocalCache(): File? {
        val file = File(context.filesDir, MODEL_FILE_NAME)
        return if (file.exists() && file.length() > MIN_MODEL_SIZE) file else null
    }

    // ------------------------------------------------------------------
    // Tier 2: Bundled asset
    // ------------------------------------------------------------------

    /**
     * Opens the bundled TFLite model as a read-only [MappedByteBuffer].
     *
     * Uses [android.content.res.AssetManager.openFd] for zero-copy memory-mapped access.
     * This is the preferred fast path where the model is bundled in the APK as an
     * asset (requires `noCompress += "tflite"` in build.gradle.kts).
     *
     * No file copy is performed: the model is mapped directly from the APK.
     *
     * @return A [MappedByteBuffer] backed by the uncompressed asset, or null if the
     *         asset is not present.
     */
    fun openBundledAssetBuffer(): MappedByteBuffer? {
        return try {
            context.assets.openFd(MODEL_FILE_NAME).use { afd ->
                FileInputStream(afd.fileDescriptor).channel.use { channel ->
                    channel.map(
                        FileChannel.MapMode.READ_ONLY,
                        afd.startOffset,
                        afd.declaredLength
                    )
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Bundled asset not available: ${e.message}")
            null
        }
    }

    private fun copyFromBundledAsset(): File? {
        val outFile = File(context.filesDir, MODEL_FILE_NAME)
        return try {
            val afd = context.assets.openFd(MODEL_FILE_NAME)
            afd.use { descriptor ->
                FileInputStream(descriptor.fileDescriptor).use { inputStream ->
                    // Seek to the correct offset within the APK
                    inputStream.channel.position(descriptor.startOffset)
                    outFile.outputStream().buffered(8 * 1024 * 1024).use { output ->
                        val buffer = ByteArray(8 * 1024 * 1024)
                        var remaining = descriptor.declaredLength
                        while (remaining > 0) {
                            val toRead = minOf(buffer.size.toLong(), remaining).toInt()
                            val read = inputStream.read(buffer, 0, toRead)
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                            remaining -= read
                        }
                    }
                }
            }
            if (outFile.exists() && outFile.length() > MIN_MODEL_SIZE) {
                outFile
            } else {
                Log.w(TAG, "Copied asset file is too small (${outFile.length()} bytes), may be corrupt")
                outFile.delete()
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy bundled TFLite asset to local cache: ${e.message}")
            outFile.delete() // Clean up any partial file
            null
        }
    }

    /**
     * Reports that the MediaPipe [TextEmbedder] failed to initialize even
     * though the model file was successfully resolved.
     *
     * This allows [TFLiteEmbeddingService] (or any other consumer) to push
     * the state back to [ModelState.Failed] so the UI can display an
     * appropriate error instead of falsely showing a "ready" indicator.
     *
     * @param message Human-readable description of the initialization failure.
     */
    fun reportInitializationFailure(message: String) {
        _state.value = ModelState.Failed(message)
        Log.e(TAG, "Model initialization failed: $message")
    }

    /**
     * Reports that the model was successfully loaded and is ready for inference.
     *
     * Call this from [TFLiteEmbeddingService] after successfully creating a
     * MediaPipe [TextEmbedder] from the bundled asset or cached file.
     */
    fun reportReady() {
        _state.value = ModelState.Ready
        Log.d(TAG, "Model marked ready (loaded externally)")
    }

    companion object {
        private const val TAG = "ModelManager"

        /** TFLite model file name — used for both the APK asset and the local cache copy. */
        const val MODEL_FILE_NAME = "use_multilingual_lite.tflite"

        /** Minimum valid model file size (1 MB) to reject corrupt/empty files. */
        private const val MIN_MODEL_SIZE = 1L * 1024 * 1024
    }
}

/**
 * Observable state of the TFLite model loading lifecycle.
 *
 * Used by the UI layer to show loading progress and error states.
 */
sealed class ModelState {
    /** Initial state before any resolution attempt. */
    data object Idle : ModelState()

    /** Model is loaded and ready for inference. */
    data object Ready : ModelState()

    /**
     * All resolution tiers failed.
     * @property message Human-readable error description (Vietnamese).
     */
    data class Failed(val message: String?) : ModelState()
}
