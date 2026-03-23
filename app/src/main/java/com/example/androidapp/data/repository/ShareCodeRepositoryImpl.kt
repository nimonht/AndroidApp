package com.example.androidapp.data.repository

import com.example.androidapp.data.remote.firebase.ShareCodeRemoteDataSource
import com.example.androidapp.domain.repository.ShareCodeRepository
import com.example.androidapp.domain.util.ShareCodeUtil

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
        return try {
            val dto = remoteDataSource.lookupShareCode(shareCode)
            Result.success(dto?.quizId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** {@inheritDoc} */
    override suspend fun generateShareCode(quizId: String): Result<String> {
        return try {
            var code: String
            var attempts = 0
            val maxAttempts = 10
            do {
                code = ShareCodeUtil.generateCode()
                val exists = remoteDataSource.shareCodeExists(code)
                attempts++
                if (!exists) break
            } while (attempts < maxAttempts)

            if (attempts >= maxAttempts) {
                return Result.failure(
                    IllegalStateException("Failed to generate unique share code after $maxAttempts attempts")
                )
            }

            remoteDataSource.createShareCode(code, quizId)
            Result.success(code)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** {@inheritDoc} */
    override suspend fun deleteShareCode(shareCode: String): Result<Unit> {
        return try {
            remoteDataSource.deleteShareCode(shareCode)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** {@inheritDoc} */
    override suspend fun regenerateShareCode(
        quizId: String,
        oldShareCode: String
    ): Result<String> {
        return try {
            // Create the new code first; only delete the old one after the new one is persisted.
            val newCodeResult = generateShareCode(quizId)
            if (newCodeResult.isSuccess) {
                remoteDataSource.deleteShareCode(oldShareCode)
            }
            newCodeResult
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** {@inheritDoc} */
    override suspend fun validateShareCode(shareCode: String): Result<String> {
        return try {
            val dto = remoteDataSource.lookupShareCode(shareCode)
            if (dto != null && dto.quizId.isNotBlank()) {
                Result.success(dto.quizId)
            } else {
                Result.failure(
                    IllegalArgumentException("Share code does not exist or is not associated with a quiz")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

