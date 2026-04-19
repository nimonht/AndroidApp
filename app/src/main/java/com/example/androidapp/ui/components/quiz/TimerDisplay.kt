package com.example.androidapp.ui.components.quiz

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.androidapp.R
import com.example.androidapp.ui.theme.FullShape
import com.example.androidapp.ui.theme.QuizzezTheme
import java.util.Locale

/**
 * Displays elapsed time in MM:SS format inside a pill-shaped container.
 *
 * @param secondsElapsed Total seconds elapsed to format and display.
 * @param modifier Modifier for styling and layout customization.
 */
@Composable
fun TimerDisplay(
    secondsElapsed: Long,
    modifier: Modifier = Modifier
) {
    val minutes = secondsElapsed / 60
    val seconds = secondsElapsed % 60
    val formattedTime = String.format(Locale.US, "%02d:%02d", minutes, seconds)

    Row(
        modifier = modifier
            .clip(FullShape)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Timer,
            contentDescription = stringResource(R.string.timer_content_description),
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(16.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = formattedTime,
            style = MaterialTheme.typography.labelLarge,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Preview(showBackground = true, name = "Light")
@Composable
private fun TimerDisplayPreview() {
    QuizzezTheme {
        TimerDisplay(
            secondsElapsed = 754,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TimerDisplayDarkPreview() {
    QuizzezTheme {
        TimerDisplay(
            secondsElapsed = 754,
            modifier = Modifier.padding(16.dp)
        )
    }
}
