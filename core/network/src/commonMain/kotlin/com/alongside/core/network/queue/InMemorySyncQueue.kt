package com.alongside.core.network.queue

public class InMemorySyncQueue : SyncQueue {
    private val pending: ArrayDeque<SyncOperation> = ArrayDeque()

    override fun enqueue(operation: SyncOperation) {
        pending.addLast(operation)
    }

    override fun enqueueAll(operations: List<SyncOperation>) {
        pending.addAll(operations)
    }

    override fun peekAll(): List<SyncOperation> = pending.toList()

    override fun isEmpty(): Boolean = pending.isEmpty()

    override fun removeFirstOrNull(): SyncOperation? = pending.removeFirstOrNull()
}
