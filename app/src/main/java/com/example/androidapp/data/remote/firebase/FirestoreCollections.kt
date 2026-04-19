package com.example.androidapp.data.remote.firebase

/**
 * Firestore collection and field name constants.
 * Single source of truth for all Firestore path strings.
 */
object FirestoreCollections {
    /** Maximum operations per Firestore batch write. */
    const val BATCH_LIMIT = 500

    const val USERS = "users"
    const val QUIZZES = "quizzes"
    const val QUESTIONS = "questions"
    const val CHOICES = "choices"
    const val ATTEMPTS = "attempts"
    const val SHARE_CODES = "shareCodes"
    const val QUESTION_POOL = "questionPool"

    /**
     * Lightweight tombstone collection that records permanent quiz deletions.
     * Clients query this collection incrementally (by [Fields.DELETED_AT])
     * to detect quizzes removed from Firestore without re-fetching all quizzes.
     *
     * Document schema: `{ quizId: String, deletedAt: Timestamp }`.
     * Old entries are cleaned up by [BackendMaintenanceWorker] after 90 days.
     */
    const val QUIZ_DELETIONS = "quizDeletions"

    // Field names
    object Fields {
        const val OWNER_ID = "ownerId"
        const val USER_ID = "userId"
        const val QUIZ_ID = "quizId"
        const val IS_PUBLIC = "isPublic"
        const val IS_DRAFT = "isDraft"
        const val DELETED_AT = "deletedAt"
        const val SHARE_CODE = "shareCode"
        const val ATTEMPT_COUNT = "attemptCount"
        const val UPDATED_AT = "updatedAt"
        const val IS_ACTIVE = "isActive"
        const val CONTRIBUTOR_ID = "contributorId"
        const val ROLE = "role"
        const val PERMISSIONS = "permissions"
        const val ACTIVE = "active"
        const val CREATED_AT = "createdAt"
        const val EMAIL = "email"
        const val STARTED_AT = "startedAt"
        const val TAGS = "tags"
        const val USAGE_COUNT = "usageCount"
    }
}
