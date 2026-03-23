package com.example.androidapp.data.remote.firebase

/**
 * Firestore collection and field name constants.
 * Single source of truth for all Firestore path strings.
 */
object FirestoreCollections {
    const val USERS = "users"
    const val QUIZZES = "quizzes"
    const val QUESTIONS = "questions"
    const val CHOICES = "choices"
    const val ATTEMPTS = "attempts"
    const val SHARE_CODES = "shareCodes"
    const val QUESTION_POOL = "questionPool"

    // Field names
    object Fields {
        const val OWNER_ID = "ownerId"
        const val IS_PUBLIC = "isPublic"
        const val DELETED_AT = "deletedAt"
        const val SHARE_CODE = "shareCode"
        const val ATTEMPT_COUNT = "attemptCount"
        const val UPDATED_AT = "updatedAt"
        const val IS_ACTIVE = "isActive"
        const val CONTRIBUTOR_ID = "contributorId"
    }
}

