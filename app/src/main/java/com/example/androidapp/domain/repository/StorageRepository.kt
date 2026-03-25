package com.example.androidapp.domain.repository

import android.net.Uri
import kotlinx.coroutines.flow.Flow

/**
 * Represents the state of a media upload process.
 */
sealed class UploadState {
    /** Output when the upload is progressing. [percent] is between 0 and 100. */
    data class Progress(val percent: Int) : UploadState()
    /** Output when the upload is successfully finished. [url] is the download URL. */
    data class Success(val url: String) : UploadState()
    /** Output when the upload fails. */
    data class Error(val exception: Throwable) : UploadState()
}

/**
 * Interface for media storage operations (Cloud Storage for Firebase).
 */
interface StorageRepository {

    /**
     * Uploads an image, compressing it to JPEG before saving.
     *
     * @param userId The ID of the uploading user.
     * @param imageUri The local content URI of the image.
     * @return [Result] containing the download URL or a failure.
     */
    suspend fun uploadImage(userId: String, imageUri: Uri): Result<String>

    /**
     * Uploads media asynchronously. Generally used as fallback for non-video items without progress.
     */
    suspend fun uploadMedia(userId: String, mediaUri: Uri, mediaType: String): Result<String>

    /**
     * Uploads a video file, emitting progress updates.
     *
     * @param userId The ID of the uploading user.
     * @param videoUri The local content URI of the video.
     * @return A [Flow] of [UploadState] indicating progress, success, or failure.
     */
    fun uploadVideo(userId: String, videoUri: Uri): Flow<UploadState>

    /**
     * Generates a thumbnail frame from a video and uploads it as an image.
     *
     * @param userId The ID of the uploading user.
     * @param videoUri The local content URI of the video.
     * @return [Result] containing the thumbnail download URL or a failure.
     */
    suspend fun generateAndUploadVideoThumbnail(userId: String, videoUri: Uri): Result<String>

    /**
     * Deletes a media file from storage given its full download URL.
     */
    suspend fun deleteMedia(mediaUrl: String): Result<Unit>

    /**
     * Gets the download URL for a specific storage path.
     */
    suspend fun getDownloadUrl(storagePath: String): Result<String>
}

