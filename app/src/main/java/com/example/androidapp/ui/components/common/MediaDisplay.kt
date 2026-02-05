package com.example.androidapp.ui.components.common

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import com.example.androidapp.R
import com.example.androidapp.ui.theme.QuizCodeTheme

/**
 * Displays media content (image or video thumbnail) with loading and error states.
 * Supports image URLs and video stream URLs.
 *
 * @param mediaUrl The URL of the media to display (image or video).
 * @param mediaType Type of media: "image" or "video". Auto-detected if null.
 * @param contentDescription Accessibility description for the media.
 * @param onVideoClick Callback when video play button is clicked. Only used for video type.
 * @param modifier Modifier for styling and layout customization.
 */
@Composable
fun MediaDisplay(
    mediaUrl: String?,
    mediaType: MediaType? = null,
    contentDescription: String? = null,
    onVideoClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Return early if no URL
    if (mediaUrl.isNullOrBlank()) {
        return
    }

    // Auto-detect media type if not provided
    val detectedType = mediaType ?: detectMediaType(mediaUrl)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        when (detectedType) {
            MediaType.IMAGE -> {
                ImageContent(
                    imageUrl = mediaUrl,
                    contentDescription = contentDescription
                )
            }
            MediaType.VIDEO -> {
                VideoThumbnail(
                    contentDescription = contentDescription,
                    onPlayClick = onVideoClick
                )
            }
        }
    }
}

/**
 * Image content with loading and error states.
 */
@Composable
private fun ImageContent(
    imageUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    var imageState by remember { mutableStateOf<ImageLoadState>(ImageLoadState.Loading) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 150.dp, max = 300.dp),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth(),
            onState = { state ->
                imageState = when (state) {
                    is AsyncImagePainter.State.Loading -> ImageLoadState.Loading
                    is AsyncImagePainter.State.Success -> ImageLoadState.Success
                    is AsyncImagePainter.State.Error -> ImageLoadState.Error
                    else -> ImageLoadState.Loading
                }
            }
        )

        // Loading state overlay
        if (imageState == ImageLoadState.Loading) {
            LoadingOverlay()
        }

        // Error state overlay
        if (imageState == ImageLoadState.Error) {
            ErrorOverlay(message = stringResource(R.string.image_load_error))
        }
    }
}

/**
 * Video thumbnail with play button overlay.
 */
@Composable
private fun VideoThumbnail(
    contentDescription: String?,
    onPlayClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(Color.Black.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center
    ) {
        // Video icon background
        Icon(
            imageVector = Icons.Default.Image,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color.White.copy(alpha = 0.3f)
        )

        // Play button
        if (onPlayClick != null) {
            FilledIconButton(
                onClick = onPlayClick,
                modifier = Modifier.size(64.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = contentDescription ?: stringResource(R.string.play_video),
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        // Video URL label
        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp),
            shape = RoundedCornerShape(4.dp),
            color = Color.Black.copy(alpha = 0.7f)
        ) {
            Text(
                text = stringResource(R.string.video_label),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

/**
 * Loading overlay with circular progress indicator.
 */
@Composable
private fun LoadingOverlay(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 3.dp
        )
    }
}

/**
 * Error overlay with icon and message.
 */
@Composable
private fun ErrorOverlay(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.BrokenImage,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

/**
 * Detects media type based on URL extension or pattern.
 */
private fun detectMediaType(url: String): MediaType {
    val lowerUrl = url.lowercase()
    return when {
        lowerUrl.endsWith(".mp4") ||
        lowerUrl.endsWith(".webm") ||
        lowerUrl.endsWith(".avi") ||
        lowerUrl.contains("youtube.com") ||
        lowerUrl.contains("youtu.be") ||
        lowerUrl.contains("vimeo.com") -> MediaType.VIDEO
        else -> MediaType.IMAGE
    }
}

/**
 * Enum representing supported media types.
 */
enum class MediaType {
    IMAGE,
    VIDEO
}

/**
 * Internal state for image loading.
 */
private enum class ImageLoadState {
    Loading,
    Success,
    Error
}
