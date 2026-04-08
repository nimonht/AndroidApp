package com.example.androidapp.ui.components.quiz

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.androidapp.ui.theme.PlayfairDisplayFamily
import com.example.androidapp.ui.theme.QuizzezTheme

/**
 * Shared quiz thumbnail composable that displays either a remote image
 * or a gradient placeholder with the first letter of the quiz title.
 *
 * When [thumbnailUrl] is non-null and non-empty, an [AsyncImage] is rendered
 * with [ContentScale.Crop]. Otherwise a linear gradient from
 * [primary] to [secondary] (both at 60 % opacity) is shown with the
 * uppercased first character of [title] centered on top.
 *
 * Callers control the overall size (especially height) through [modifier].
 * The placeholder letter style defaults to [MaterialTheme.typography.displayMedium]
 * but can be overridden via [textStyle] for smaller card variants.
 *
 * @param thumbnailUrl Optional URL of the quiz thumbnail image.
 * @param title Quiz title used to derive the placeholder letter.
 * @param modifier Modifier for sizing and positioning.
 * @param textStyle Text style for the placeholder letter.
 */
@Composable
fun QuizThumbnail(
    thumbnailUrl: String?,
    title: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.displayMedium
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.primaryContainer)
    ) {
        if (!thumbnailUrl.isNullOrEmpty()) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title.take(1).uppercase(),
                    style = textStyle,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontFamily = PlayfairDisplayFamily
                )
            }
        }
    }
}

// -- Previews -----------------------------------------------------------------

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun QuizThumbnailWithImagePreview() {
    QuizzezTheme {
        QuizThumbnail(
            thumbnailUrl = "https://example.com/image.jpg",
            title = "Sample Quiz",
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun QuizThumbnailPlaceholderPreview() {
    QuizzezTheme {
        QuizThumbnail(
            thumbnailUrl = null,
            title = "Sample Quiz",
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun QuizThumbnailSmallPreview() {
    QuizzezTheme {
        QuizThumbnail(
            thumbnailUrl = null,
            title = "Admin Quiz",
            textStyle = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .padding(horizontal = 16.dp)
        )
    }
}
