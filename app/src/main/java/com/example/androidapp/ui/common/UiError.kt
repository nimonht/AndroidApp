package com.example.androidapp.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.androidapp.R

/**
 * Centralised error codes emitted by ViewModels.
 *
 * ViewModels set a [UiError] value (instead of a raw Vietnamese string) so that
 * the UI layer can map it to a localised message via [toMessage]. This keeps
 * ViewModels free of Android resource dependencies while guaranteeing that every
 * user-facing string lives in `res/values/strings.xml`.
 *
 * The enum is grouped by feature area. Each entry documents which ViewModel(s)
 * originally contained the corresponding hardcoded string.
 */
enum class UiError {

    // -- Authentication ----------------------------------------------------------

    /** AuthViewModel — "Dang nhap that bai" */
    LOGIN_FAILED,

    /** AuthViewModel — "Dang ky that bai" */
    REGISTER_FAILED,

    /** QuestionPoolViewModel — "Vui long dang nhap" */
    LOGIN_REQUIRED,

    /** RecycleBinViewModel — "Nguoi dung chua dang nhap" */
    USER_NOT_LOGGED_IN,

    // -- Network -----------------------------------------------------------------

    /** AdminQuizManagementViewModel, AdminUserManagementViewModel — offline guard */
    NETWORK_UNAVAILABLE,

    // -- Quiz (general) ----------------------------------------------------------

    /** EditQuizViewModel, QuizPreviewViewModel, QuizDetailViewModel, TakeQuizViewModel */
    QUIZ_NOT_FOUND,

    /** TakeQuizViewModel — quiz loaded but has zero questions */
    QUIZ_HAS_NO_QUESTIONS,

    /** CreateQuizViewModel, EditQuizViewModel — save/update failed */
    SAVE_QUIZ_FAILED,

    /** QuizDetailViewModel — soft-delete failed */
    DELETE_QUIZ_FAILED,

    /** QuizDetailViewModel — refreshQuizFromRemote failed */
    LOAD_REMOTE_FAILED,

    // -- Quiz results / attempts -------------------------------------------------

    /** QuizResultViewModel, AnswerReviewViewModel — "Khong tim thay ket qua" */
    RESULT_NOT_FOUND,

    /** TakeQuizViewModel — attempt persistence failed */
    SAVE_RESULT_FAILED,

    /** AttemptDetailViewModel — "Khong tim thay luot lam" */
    ATTEMPT_NOT_FOUND,

    // -- Join quiz ---------------------------------------------------------------

    /** HomeViewModel — code format validation failed */
    INVALID_JOIN_CODE,

    /** HomeViewModel — no quiz matched the share code */
    JOIN_QUIZ_NOT_FOUND,

    /** HomeViewModel — share-code lookup threw an exception */
    JOIN_QUIZ_FAILED,

    // -- CSV import --------------------------------------------------------------

    /** CsvImportViewModel — parsed file contained no valid rows */
    CSV_NO_VALID_DATA,

    /** CsvImportViewModel — file could not be read (detail = e.message) */
    CSV_READ_FAILED,

    /** CsvImportViewModel — import transaction failed (detail = e.message) */
    CSV_IMPORT_FAILED,

    // -- Profile -----------------------------------------------------------------

    /** EditProfileViewModel — display name is blank */
    DISPLAY_NAME_BLANK,

    /** EditProfileViewModel — updateProfile call failed */
    SAVE_PROFILE_FAILED,

    /** EditProfileViewModel — Wallhaven fetch threw */
    RANDOM_AVATAR_FAILED,

    /** EditProfileViewModel — Wallhaven returned empty data array */
    WALLHAVEN_NO_IMAGES,

    /** EditProfileViewModel — Wallhaven returned non-200 (detail = HTTP code) */
    WALLHAVEN_API_ERROR,

    // -- Admin: statistics -------------------------------------------------------

    /** BaseAdminStatsViewModel (dashboard + reports) */
    LOAD_STATS_FAILED,

    // -- Admin: quiz management --------------------------------------------------

    /** AdminQuizManagementViewModel — initial page load */
    LOAD_QUIZ_LIST_FAILED,

    /** AdminQuizManagementViewModel — pagination */
    LOAD_MORE_QUIZZES_FAILED,

    /** AdminQuizManagementViewModel — publish / unpublish toggle */
    UPDATE_QUIZ_STATUS_FAILED,

    /** AdminQuizManagementViewModel — restore soft-deleted quiz */
    RESTORE_QUIZ_FAILED,

    /** AdminQuizManagementViewModel — permanent delete */
    ADMIN_DELETE_QUIZ_FAILED,

    // -- Admin: permissions ------------------------------------------------------

    /** Admin ViewModels — action blocked by missing permission */
    INSUFFICIENT_PERMISSIONS,

    /** AdminUserManagementViewModel — permission update failed */
    UPDATE_PERMISSIONS_FAILED,

    // -- Admin: user management --------------------------------------------------

    /** AdminUserManagementViewModel — initial page load */
    LOAD_USER_LIST_FAILED,

    /** AdminUserManagementViewModel — pagination */
    LOAD_MORE_USERS_FAILED,

    /** AdminUserManagementViewModel — role change */
    UPDATE_USER_ROLE_FAILED,

    /** AdminUserManagementViewModel — ban action */
    BAN_USER_FAILED,

    /** AdminUserManagementViewModel — unban action */
    UNBAN_USER_FAILED,

    /** AdminUserManagementViewModel — delete action */
    DELETE_USER_FAILED,

    /** AdminUserManagementViewModel — admin attempted action on themselves */
    SELF_ACTION_NOT_ALLOWED,

    /** AdminUserManagementViewModel — non-superuser tried to act on a superuser */
    TARGET_IS_SUPERUSER,

    // -- Question pool -----------------------------------------------------------

    /** CreateQuizViewModel — pool search network error */
    POOL_SEARCH_FAILED,

    /** CreateQuizViewModel — search submitted with empty tags */
    POOL_SEARCH_EMPTY,

    /** QuestionPoolViewModel — browse submitted with empty tags */
    POOL_SEARCH_TAGS_EMPTY,

    /** QuestionPoolViewModel — revoke contribution failed */
    POOL_REVOKE_FAILED,

    /** QuestionPoolViewModel — generic data load failure */
    LOAD_DATA_FAILED,

    /** QuestionPoolViewModel — generic search failure */
    SEARCH_FAILED,
}

/**
 * Maps a [UiError] to a user-facing Vietnamese string pulled from resources.
 *
 * @param detail Optional dynamic detail appended or interpolated into the
 *   message. Used by [CSV_READ_FAILED], [CSV_IMPORT_FAILED] (exception
 *   message) and [WALLHAVEN_API_ERROR] (HTTP status code string).
 *   Ignored for error codes that have no parameterised string resource.
 */
@Composable
fun UiError.toMessage(detail: String? = null): String = when (this) {

    // Authentication
    UiError.LOGIN_FAILED -> stringResource(R.string.error_login_failed)
    UiError.REGISTER_FAILED -> stringResource(R.string.error_register_failed)
    UiError.LOGIN_REQUIRED -> stringResource(R.string.error_login_required)
    UiError.USER_NOT_LOGGED_IN -> stringResource(R.string.error_user_not_logged_in)

    // Network
    UiError.NETWORK_UNAVAILABLE -> stringResource(R.string.error_no_network)

    // Quiz (general)
    UiError.QUIZ_NOT_FOUND -> stringResource(R.string.error_quiz_not_found)
    UiError.QUIZ_HAS_NO_QUESTIONS -> stringResource(R.string.error_quiz_no_questions)
    UiError.SAVE_QUIZ_FAILED -> stringResource(R.string.error_quiz_save_failed)
    UiError.DELETE_QUIZ_FAILED -> stringResource(R.string.error_quiz_delete_failed)
    UiError.LOAD_REMOTE_FAILED -> stringResource(R.string.error_load_remote_failed)

    // Quiz results / attempts
    UiError.RESULT_NOT_FOUND -> stringResource(R.string.error_result_not_found)
    UiError.SAVE_RESULT_FAILED -> stringResource(R.string.error_save_result_failed)
    UiError.ATTEMPT_NOT_FOUND -> stringResource(R.string.error_attempt_not_found)

    // Join quiz
    UiError.INVALID_JOIN_CODE -> stringResource(R.string.error_join_code_invalid)
    UiError.JOIN_QUIZ_NOT_FOUND -> stringResource(R.string.error_join_quiz_not_found)
    UiError.JOIN_QUIZ_FAILED -> stringResource(R.string.error_join_quiz_failed)

    // CSV import (parameterised — detail is the exception message)
    UiError.CSV_NO_VALID_DATA -> stringResource(R.string.error_csv_no_data)
    UiError.CSV_READ_FAILED -> stringResource(R.string.error_csv_read_failed, detail ?: "")
    UiError.CSV_IMPORT_FAILED -> stringResource(R.string.error_csv_import_failed, detail ?: "")

    // Profile
    UiError.DISPLAY_NAME_BLANK -> stringResource(R.string.error_display_name_blank)
    UiError.SAVE_PROFILE_FAILED -> stringResource(R.string.error_save_profile_failed)
    UiError.RANDOM_AVATAR_FAILED -> stringResource(R.string.error_random_avatar_failed)
    UiError.WALLHAVEN_NO_IMAGES -> stringResource(R.string.error_wallhaven_no_image)
    UiError.WALLHAVEN_API_ERROR -> stringResource(R.string.error_wallhaven_http, detail?.toIntOrNull() ?: 0)

    // Admin: statistics
    UiError.LOAD_STATS_FAILED -> stringResource(R.string.error_load_stats_failed)

    // Admin: quiz management
    UiError.LOAD_QUIZ_LIST_FAILED -> stringResource(R.string.error_load_quizzes_failed)
    UiError.LOAD_MORE_QUIZZES_FAILED -> stringResource(R.string.error_load_more_quizzes_failed)
    UiError.UPDATE_QUIZ_STATUS_FAILED -> stringResource(R.string.error_update_quiz_status_failed)
    UiError.RESTORE_QUIZ_FAILED -> stringResource(R.string.error_restore_quiz_failed)
    UiError.ADMIN_DELETE_QUIZ_FAILED -> stringResource(R.string.error_delete_quiz_failed)
    // Admin: permissions
    UiError.INSUFFICIENT_PERMISSIONS -> stringResource(R.string.error_insufficient_permissions)
    UiError.UPDATE_PERMISSIONS_FAILED -> stringResource(R.string.error_update_permissions_failed)

    // Admin: user management
    UiError.LOAD_USER_LIST_FAILED -> stringResource(R.string.error_load_users_failed)
    UiError.LOAD_MORE_USERS_FAILED -> stringResource(R.string.error_load_more_users_failed)
    UiError.UPDATE_USER_ROLE_FAILED -> stringResource(R.string.error_update_role_failed)
    UiError.BAN_USER_FAILED -> stringResource(R.string.error_ban_user_failed)
    UiError.UNBAN_USER_FAILED -> stringResource(R.string.error_unban_user_failed)
    UiError.DELETE_USER_FAILED -> stringResource(R.string.error_delete_user_failed)
    UiError.SELF_ACTION_NOT_ALLOWED -> stringResource(R.string.error_self_action_not_allowed)
    UiError.TARGET_IS_SUPERUSER -> stringResource(R.string.error_target_is_superuser)

    // Question pool
    UiError.POOL_SEARCH_FAILED -> stringResource(R.string.error_pool_search_failed)
    UiError.POOL_SEARCH_EMPTY -> stringResource(R.string.error_pool_search_empty)
    UiError.POOL_SEARCH_TAGS_EMPTY -> stringResource(R.string.error_pool_search_tags_empty)
    UiError.POOL_REVOKE_FAILED -> stringResource(R.string.error_pool_revoke_failed)
    UiError.LOAD_DATA_FAILED -> stringResource(R.string.error_load_data_failed)
    UiError.SEARCH_FAILED -> stringResource(R.string.error_search_failed)
}
