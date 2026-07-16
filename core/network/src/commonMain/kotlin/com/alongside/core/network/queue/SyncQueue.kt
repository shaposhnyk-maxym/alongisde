package com.alongside.core.network.queue

/**
 * Pending sync operations, FIFO. An interface (not just [InMemorySyncQueue]) because
 * core:network can't depend on core:database - a persistent implementation belongs to the `data`
 * module, which can swap it in without changing [SyncQueueProcessor].
 */
public interface SyncQueue {
    public fun enqueue(operation: SyncOperation)

    public fun enqueueAll(operations: List<SyncOperation>)

    public fun peekAll(): List<SyncOperation>

    public fun isEmpty(): Boolean

    public fun removeFirstOrNull(): SyncOperation?
}
