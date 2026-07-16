package com.alongside.core.network.queue

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RetryPolicyTest {
    private fun op(attempts: Int) =
        SyncOperation(id = "a", collectionPath = "trips", documentId = "a", type = SyncOperationType.UPSERT, attempts = attempts)

    @Test
    fun `never retries a non-retryable failure regardless of attempts`() {
        val policy = MaxAttemptsRetryPolicy(maxAttempts = 5)
        val failure = SyncResult.Failure(retryable = false, cause = Throwable())

        assertFalse(policy.shouldRetry(op(attempts = 0), failure))
    }

    @Test
    fun `retries a retryable failure while under the attempt cap`() {
        val policy = MaxAttemptsRetryPolicy(maxAttempts = 3)
        val failure = SyncResult.Failure(retryable = true, cause = Throwable())

        assertTrue(policy.shouldRetry(op(attempts = 2), failure))
    }

    @Test
    fun `stops retrying once the attempt cap is reached`() {
        val policy = MaxAttemptsRetryPolicy(maxAttempts = 3)
        val failure = SyncResult.Failure(retryable = true, cause = Throwable())

        assertFalse(policy.shouldRetry(op(attempts = 3), failure))
    }
}
