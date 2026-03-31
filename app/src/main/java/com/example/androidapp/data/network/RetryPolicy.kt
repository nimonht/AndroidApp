package com.example.androidapp.data.network

import kotlinx.coroutines.delay

/**
 * Executes [block] with exponential backoff retry.
 *
 * @param maxRetries Maximum number of retry attempts (default: 3).
 * @param initialDelayMs Initial delay before first retry in milliseconds.
 * @param maxDelayMs Maximum delay cap in milliseconds.
 * @param shouldRetry Predicate to determine if an exception should trigger a retry.
 * @param block The suspending block to execute.
 * @return The result of the successful execution.
 * @throws Exception The last exception if all retries fail.
 */
suspend fun <T> retryWithBackoff(
    maxRetries: Int = 3,
    initialDelayMs: Long = 1000,
    maxDelayMs: Long = 10000,
    shouldRetry: (Exception) -> Boolean = { true },
    block: suspend () -> T
): T {
    var currentDelay = initialDelayMs
    var retryCount = 0

    while (true) {
        try {
            return block()
        } catch (e: Exception) {
            if (retryCount >= maxRetries || !shouldRetry(e)) {
                throw e
            }
            delay(currentDelay)
            retryCount++
            currentDelay = (currentDelay * 2).coerceAtMost(maxDelayMs)
        }
    }
}
