package com.example.androidapp.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.androidapp.R

/**
 * Centralised success codes emitted by ViewModels.
 *
 * Mirrors [UiError] for positive feedback: ViewModels set a [UiSuccess] value
 * instead of a raw Vietnamese string so that the composable layer maps it to a
 * localised message via [toMessage]. This keeps ViewModels free of Android
 * resource dependencies while guaranteeing that every user-facing string lives
 * in `res/values/strings.xml`.
 */
enum class UiSuccess {

    // -- Trash / Recycle Bin -----------------------------------------------------

    /** RecycleBinViewModel — quiz restored from trash */
    QUIZ_RESTORED,

    /** RecycleBinViewModel — quiz permanently deleted */
    QUIZ_DELETED_PERMANENTLY,

    /** RecycleBinViewModel — all trash emptied */
    TRASH_EMPTIED,

    // -- Question Pool -----------------------------------------------------------

    /** QuestionPoolViewModel — contribution revoked from pool */
    CONTRIBUTION_REVOKED,
}

/**
 * Maps a [UiSuccess] to a user-facing Vietnamese string pulled from resources.
 */
@Composable
fun UiSuccess.toMessage(): String = when (this) {
    UiSuccess.QUIZ_RESTORED -> stringResource(R.string.trash_restored)
    UiSuccess.QUIZ_DELETED_PERMANENTLY -> stringResource(R.string.trash_deleted_permanently)
    UiSuccess.TRASH_EMPTIED -> stringResource(R.string.trash_emptied)
    UiSuccess.CONTRIBUTION_REVOKED -> stringResource(R.string.pool_revoked)
}
