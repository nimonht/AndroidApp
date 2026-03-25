package com.example.androidapp.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.example.androidapp.domain.repository.StorageRepository
import com.example.androidapp.domain.repository.UploadState
import com.example.androidapp.domain.util.MediaUrlValidator
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.util.UUID

/**
 * Implementation of [StorageRepository] backed by Firebase Storage.
 *
 * @property context Application context for accessing ContentResolver.
 * @property storage Firebase Storage instance.
 */
class StorageRepositoryImpl(
    private val context: Context,
    private val storage: FirebaseStorage
) : StorageRepository {

    /** {@inheritDoc} */
    override suspend fun uploadImage(userId: String, imageUri: Uri): Result<String> {
        return try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(imageUri)
                ?: return Result.failure(Exception("Cannot read image stream"))
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            // Scale down if extremely large to save bandwidth, then compress to JPEG
            val maxWidth = 1920
            val maxHeight = 1080
            val scale = minOf(
                maxWidth.toFloat() / originalBitmap.width,
                maxHeight.toFloat() / originalBitmap.height
            ).coerceAtMost(1f)

            val scaledBitmap = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    originalBitmap,
                    (originalBitmap.width * scale).toInt(),
                    (originalBitmap.height * scale).toInt(),
                    true
                )
            } else {
                originalBitmap
            }

            val baos = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
            val data = baos.toByteArray()

            val filename = "${UUID.randomUUID()}.jpg"
            val storageRef = storage.reference.child("users/$userId/media/$filename")

            storageRef.putBytes(data).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()

            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** {@inheritDoc} */
    override suspend fun uploadMedia(
        userId: String,
        mediaUri: Uri,
        mediaType: String
    ): Result<String> {
        return try {
            val extension = when {
                mediaType.startsWith("image/png") -> "png"
                mediaType.startsWith("image/") -> "jpg"
                mediaType.startsWith("video/") -> "mp4"
                else -> "bin"
            }
            val filename = "${UUID.randomUUID()}.$extension"
            val storageRef = storage.reference.child("users/$userId/media/$filename")

            storageRef.putFile(mediaUri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** {@inheritDoc} */
    override fun uploadVideo(userId: String, videoUri: Uri): Flow<UploadState> = callbackFlow {
        val filename = "${UUID.randomUUID()}.mp4"
        val storageRef = storage.reference.child("users/$userId/media/$filename")

        val uploadTask = storageRef.putFile(videoUri)

        uploadTask.addOnProgressListener { snapshot ->
            val progress = (100.0 * snapshot.bytesTransferred / snapshot.totalByteCount).toInt()
            trySend(UploadState.Progress(progress))
        }.addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                trySend(UploadState.Success(uri.toString()))
                close()
            }.addOnFailureListener {
                trySend(UploadState.Error(it))
                close(it)
            }
        }.addOnFailureListener {
            trySend(UploadState.Error(it))
            close(it)
        }

        awaitClose { uploadTask.cancel() }
    }

    /** {@inheritDoc} */
    override suspend fun generateAndUploadVideoThumbnail(
        userId: String,
        videoUri: Uri
    ): Result<String> {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, videoUri)
            // Extract frame at 1 second
            val bitmap = retriever.getFrameAtTime(1000000)
                ?: return Result.failure(Exception("Could not extract frame from video"))
            retriever.release()

            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
            val data = baos.toByteArray()

            val filename = "${UUID.randomUUID()}_thumb.jpg"
            val storageRef = storage.reference.child("users/$userId/media/$filename")

            storageRef.putBytes(data).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()

            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** {@inheritDoc} */
    override suspend fun deleteMedia(mediaUrl: String): Result<Unit> {
        // Only delete if it's a valid Firebase Storage URL
        if (!MediaUrlValidator.isValidMediaUrl(mediaUrl)) {
            return Result.failure(IllegalArgumentException("Invalid media URL"))
        }
        return try {
            val ref = storage.getReferenceFromUrl(mediaUrl)
            ref.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** {@inheritDoc} */
    override suspend fun getDownloadUrl(storagePath: String): Result<String> {
        return try {
            val url = storage.reference.child(storagePath).downloadUrl.await().toString()
            Result.success(url)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
