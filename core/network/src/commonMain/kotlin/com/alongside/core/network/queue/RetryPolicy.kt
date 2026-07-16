package com.alongside.core.network.queue

public fun interface RetryPolicy {
    public fun shouldRetry(
        operation: SyncOperation,
        result: SyncResult.Failure,
    ): Boolean
}

public class MaxAttemptsRetryPolicy(
    private val maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
) : RetryPolicy {
    override fun shouldRetry(
        operation: SyncOperation,
        result: SyncResult.Failure,
    ): Boolean = result.retryable && operation.attempts < maxAttempts

    private companion object {
        const val DEFAULT_MAX_ATTEMPTS = 5
    }
}
