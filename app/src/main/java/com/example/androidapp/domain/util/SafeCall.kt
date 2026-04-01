package com.example.androidapp.domain.util

/**
 * Wraps a suspend block in a try-catch, returning [Result.success] on completion
 * or [Result.failure] if an exception is thrown.
 *
 * Eliminates the repetitive `return try { Result.success(...) } catch (e: Exception) { Result.failure(e) }`
 * pattern found across repository implementations.
 *
 * Usage:
 * ```
 * override suspend fun doSomething(): Result<Unit> = safeCall {
 *     // actual work
 * }
 * ```
 */
inline fun <T> safeCall(block: () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (e: Exception) {
        Result.failure(e)
    }
}
