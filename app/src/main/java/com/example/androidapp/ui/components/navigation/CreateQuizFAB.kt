package com.example.androidapp.ui.components.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.androidapp.R

/**
 * Extended floating action button for creating a new quiz.
 *
 * @param onClick Callback when the FAB is clicked.
 * @param modifier Modifier for styling and layout customization.
 * @param expanded Whether the FAB shows the extended label or collapses to icon-only.
 */
@Composable
fun CreateQuizFAB(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = true
) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        icon = { Icon(Icons.Default.Add, contentDescription = null) },
        text = { Text(text = stringResource(R.string.quiz_create)) },
        expanded = expanded,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier
    )
}
