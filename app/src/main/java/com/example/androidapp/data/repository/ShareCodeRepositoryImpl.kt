package com.example.androidapp.data.repository

import com.example.androidapp.data.remote.firebase.ShareCodeRemoteDataSource
import com.example.androidapp.domain.repository.ShareCodeRepository
import com.example.androidapp.domain.util.ShareCodeUtil
import com.example.androidapp.domain.util.safeCall

/**
 * Remote-only implementation of [ShareCodeRepository].
 *
 * Share codes are lightweight lookup entries stored only in Firestore (no Room caching).
 * Code generation uses [ShareCodeUtil] with a retry loop to guarantee uniqueness.
 *
 * @property remoteDataSource Firestore data source for share code CRUD operations.
 */
class ShareCodeRepositoryImpl(
    private val remoteDataSource: ShareCodeRemoteDataSource
) : ShareCodeRepository {

    /** {@inheritDoc} */
    override suspend fun lookupQuizId(shareCode: String): Result<String?> {
        return safeCall {
            val dto = remoteDataSource.lookupShareCode(shareCode)
            dto?.quizId
        }
    }

    /** {@inheritDoc} */
    override suspend fun generateShareCode(quizId: String): Result<String> {
        return safeCall {
            val maxAttempts = 10

            repeat(maxAttempts) {
                val code = ShareCodeUtil.generateCode()
                val created = remoteDataSource.createShareCodeIfNotExists(code, quizId)
                if (created) {
                    return@safeCall code
                }
            }

            // All attempts exhausted without finding unique code
            throw IllegalStateException("Failed to generate unique share code after $maxAttempts attempts")
        }
    }

    /** {@inheritDoc} */
    override suspend fun deleteShareCode(shareCode: String): Result<Unit> {
        return safeCall {
            remoteDataSource.deleteShareCode(shareCode)
        }
    }

    /** {@inheritDoc} */
    override suspend fun regenerateShareCode(
        quizId: String,
        oldShareCode: String
    ): Result<String> {
        return safeCall {
            // Create the new code first; only delete the old one after the new one is persisted.
            val newCodeResult = generateShareCode(quizId)
            if (newCodeResult.isSuccess) {
                remoteDataSource.deleteShareCode(oldShareCode)
            }
            newCodeResult.getOrThrow()
        }
    }

    /** {@inheritDoc} */
    override suspend fun validateShareCode(shareCode: String): Result<String> {
        return safeCall {
            // Validate format locally first to avoid unnecessary network calls
            val normalizedCode = shareCode.trim().uppercase()
            val pattern = Regex("^[A-Z0-9]{6}$")

            if (!pattern.matches(normalizedCode)) {
                throw IllegalArgumentException("Invalid share code format: must be 6 alphanumeric characters")
            }

            val dto = remoteDataSource.lookupShareCode(normalizedCode)
            if (dto != null && dto.quizId.isNotBlank()) {
                dto.quizId
            } else {
                throw IllegalArgumentException("Share code does not exist or is not associated with a quiz")
            }
        }
    }
}
