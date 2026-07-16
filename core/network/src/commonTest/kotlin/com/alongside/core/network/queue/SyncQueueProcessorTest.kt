package com.alongside.core.network.queue

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Fake [SyncNetworkClient] scriptable per-[SyncOperation.documentId] so processor tests never
 * touch Ktor - satisfies M3's "processed against a fake network client" requirement. */
private class FakeSyncNetworkClient(
    private val resultsByDocumentId: Map<String, List<SyncResult>>,
    private val default: SyncResult = SyncResult.Success,
) : SyncNetworkClient {
    val calls = mutableListOf<String>()
    private val callCounts = mutableMapOf<String, Int>()

    override suspend fun push(operation: SyncOperation): SyncResult {
        calls += operation.documentId
        val scripted = resultsByDocumentId[operation.documentId] ?: return default
        val callIndex = callCounts[operation.documentId] ?: 0
        callCounts[operation.documentId] = callIndex + 1
        return scripted.getOrElse(callIndex) { scripted.last() }
    }
}

private fun op(id: String) = SyncOperation(id = id, collectionPath = "trips", documentId = id, type = SyncOperationType.UPSERT)

class SyncQueueProcessorTest {
    @Test
    fun `processes operations in FIFO order`() =
        runTest {
            val fake = FakeSyncNetworkClient(resultsByDocumentId = emptyMap())
            val queue = InMemorySyncQueue().apply { enqueueAll(listOf(op("a"), op("b"), op("c"))) }
            val processor = SyncQueueProcessor(fake)

            processor.processAll(queue)

            assertEquals(listOf("a", "b", "c"), fake.calls)
        }

    @Test
    fun `partial failure - two of three succeed one doesn't`() =
        runTest {
            val failure = SyncResult.Failure(retryable = false, cause = Throwable("rejected"))
            val fake = FakeSyncNetworkClient(resultsByDocumentId = mapOf("b" to listOf(failure)))
            val queue = InMemorySyncQueue().apply { enqueueAll(listOf(op("a"), op("b"), op("c"))) }
            val processor = SyncQueueProcessor(fake)

            val result = processor.processAll(queue)

            assertEquals(listOf("a", "c"), result.succeeded.map { it.documentId })
            assertEquals(listOf("b"), result.failed.map { it.documentId })
        }

    @Test
    fun `retries a retryable failure until it succeeds`() =
        runTest {
            val retryableFailure = SyncResult.Failure(retryable = true, cause = Throwable("transient"))
            val fake =
                FakeSyncNetworkClient(
                    resultsByDocumentId = mapOf("a" to listOf(retryableFailure, retryableFailure, SyncResult.Success)),
                )
            val queue = InMemorySyncQueue().apply { enqueue(op("a")) }
            val processor = SyncQueueProcessor(fake, MaxAttemptsRetryPolicy(maxAttempts = 5))

            val result = processor.processAll(queue)

            assertEquals(listOf("a"), result.succeeded.map { it.documentId })
            assertEquals(3, fake.calls.size)
        }

    @Test
    fun `stops retrying once the attempt cap is reached without looping forever`() =
        runTest {
            val retryableFailure = SyncResult.Failure(retryable = true, cause = Throwable("always fails"))
            val fake = FakeSyncNetworkClient(resultsByDocumentId = emptyMap(), default = retryableFailure)
            val queue = InMemorySyncQueue().apply { enqueue(op("a")) }
            val processor = SyncQueueProcessor(fake, MaxAttemptsRetryPolicy(maxAttempts = 3))

            val result = processor.processAll(queue)

            assertEquals(1, result.failed.size)
            assertEquals(3, result.failed.single().attempts)
            assertEquals(3, fake.calls.size)
        }

    @Test
    fun `a non-retryable failure stops after a single attempt`() =
        runTest {
            val failure = SyncResult.Failure(retryable = false, cause = Throwable("rejected"))
            val fake = FakeSyncNetworkClient(resultsByDocumentId = emptyMap(), default = failure)
            val queue = InMemorySyncQueue().apply { enqueue(op("a")) }
            val processor = SyncQueueProcessor(fake)

            val result = processor.processAll(queue)

            assertEquals(1, fake.calls.size)
            assertEquals(listOf("a"), result.failed.map { it.documentId })
        }

    @Test
    fun `processing an empty queue makes no calls and returns empty results`() =
        runTest {
            val fake = FakeSyncNetworkClient(resultsByDocumentId = emptyMap())
            val processor = SyncQueueProcessor(fake)

            val result = processor.processAll(InMemorySyncQueue())

            assertTrue(fake.calls.isEmpty())
            assertTrue(result.succeeded.isEmpty())
            assertTrue(result.failed.isEmpty())
        }
}
