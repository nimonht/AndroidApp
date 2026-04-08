package com.example.androidapp.ui.components.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidapp.R
import com.example.androidapp.domain.model.Quiz
import com.example.androidapp.ui.components.quiz.QuizThumbnail
import com.example.androidapp.ui.theme.QuizzezTheme

/**
 * Quiz card component for admin quiz management screen.
 *
 * @param quiz The quiz to display.
 * @param onClick Callback when card is clicked.
 * @param onPublishToggle Callback when publish/unpublish is requested.
 * @param onRestore Callback when restore is requested (for soft-deleted quizzes).
 * @param onDelete Callback when delete is requested.
 * @param modifier Modifier for styling.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminQuizCard(
    quiz: Quiz,
    onClick: () -> Unit,
    onPublishToggle: (() -> Unit)? = null,
    onRestore: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    // Determine if the menu should be shown at all
    val hasAnyAction = onPublishToggle != null || onRestore != null || onDelete != null

    ElevatedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (quiz.deletedAt != null) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column {
            // Header Image/Thumbnail with overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                QuizThumbnail(
                    thumbnailUrl = quiz.thumbnailUrl,
                    title = quiz.title,
                    textStyle = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.fillMaxSize()
                )

                // Status badges
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Visibility badge
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = if (quiz.isPublic) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = if (quiz.isPublic) Icons.Default.Public else Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = if (quiz.isPublic) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Text(
                                text = if (quiz.isPublic) stringResource(R.string.admin_quiz_status_public)
                                else stringResource(R.string.admin_quiz_status_private),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                ),
                                color = if (quiz.isPublic) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }

                    // Deleted badge
                    if (quiz.deletedAt != null) {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = stringResource(R.string.admin_quiz_status_deleted),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                ),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Action menu
                if (hasAnyAction) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                    ) {
                        IconButton(
                            onClick = { showMenu = true },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.admin_quiz_actions),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            if (quiz.deletedAt == null && onPublishToggle != null) {
                                // Publish/Unpublish
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (quiz.isPublic) stringResource(R.string.admin_quiz_unpublish)
                                            else stringResource(R.string.admin_quiz_publish)
                                        )
                                    },
                                    onClick = {
                                        onPublishToggle()
                                        showMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            if (quiz.isPublic) Icons.Default.Lock else Icons.Default.Public,
                                            contentDescription = null
                                        )
                                    }
                                )

                                if (onRestore != null || onDelete != null) {
                                    HorizontalDivider()
                                }
                            }

                            // Restore (if deleted)
                            if (quiz.deletedAt != null && onRestore != null) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.admin_quiz_restore)) },
                                    onClick = {
                                        onRestore()
                                        showMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Restore, contentDescription = null)
                                    }
                                )

                                if (onDelete != null) {
                                    HorizontalDivider()
                                }
                            }

                            // Delete
                            if (onDelete != null) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (quiz.deletedAt != null) stringResource(R.string.admin_quiz_delete_permanent)
                                            else stringResource(R.string.delete),
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        onDelete()
                                        showMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Content Section
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Title
                Text(
                    text = quiz.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Author + Stats Row
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = quiz.authorName.ifBlank { stringResource(R.string.admin_quiz_card_unknown_author) },
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 13.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Quiz,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${quiz.questionCount}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "${quiz.attemptCount}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                ),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                // Tags
                if (quiz.tags.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(quiz.tags.take(3)) { tag ->
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Light")
@Composable
private fun AdminQuizCardPreview() {
    QuizzezTheme {
        AdminQuizCard(
            quiz = Quiz(
                id = "quiz1",
                title = "Kiểm tra tiếng Việt lớp 10",
                ownerId = "user1",
                authorName = "Nguyễn Văn A",
                tags = listOf("Tiếng Việt", "Lớp 10", "Ngữ pháp"),
                questionCount = 20,
                attemptCount = 145,
                isPublic = true,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                deletedAt = null
            ),
            onClick = {},
            onPublishToggle = {},
            onRestore = null,
            onDelete = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(
    showBackground = true,
    name = "Dark",
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun AdminQuizCardDarkPreview() {
    QuizzezTheme {
        AdminQuizCard(
            quiz = Quiz(
                id = "quiz1",
                title = "Kiểm tra tiếng Việt lớp 10",
                ownerId = "user1",
                authorName = "Nguyễn Văn A",
                tags = listOf("Tiếng Việt", "Lớp 10", "Ngữ pháp"),
                questionCount = 20,
                attemptCount = 145,
                isPublic = true,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                deletedAt = null
            ),
            onClick = {},
            onPublishToggle = {},
            onRestore = null,
            onDelete = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
