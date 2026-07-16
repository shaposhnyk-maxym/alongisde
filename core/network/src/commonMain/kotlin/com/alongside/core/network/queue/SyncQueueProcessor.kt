package com.alongside.core.network.queue

public data class SyncBatchResult(
    public val succeeded: List<SyncOperation>,
    public val failed: List<SyncOperation>,
)

/**
 * Drains a [SyncQueue] one operation at a time, in FIFO order, pushing each through
 * [SyncNetworkClient] and retrying in place per [retryPolicy] before recording a final
 * success/failure. A single [processAll] call always terminates - it never re-enqueues.
 */
public class SyncQueueProcessor(
    private val networkClient: SyncNetworkClient,
    private val retryPolicy: RetryPolicy = MaxAttemptsRetryPolicy(),
) {
    public suspend fun processAll(queue: SyncQueue): SyncBatchResult {
        val succeeded = mutableListOf<SyncOperation>()
        val failed = mutableListOf<SyncOperation>()
        while (true) {
            val initial = queue.removeFirstOrNull() ?: break
            var operation = initial.copy(attempts = initial.attempts + 1)
            var result = networkClient.push(operation)
            while (result is SyncResult.Failure && retryPolicy.shouldRetry(operation, result)) {
                operation = operation.copy(attempts = operation.attempts + 1)
                result = networkClient.push(operation)
            }
            when (result) {
                is SyncResult.Success -> succeeded += operation
                is SyncResult.Failure -> failed += operation
            }
        }
        return SyncBatchResult(succeeded, failed)
    }
}
