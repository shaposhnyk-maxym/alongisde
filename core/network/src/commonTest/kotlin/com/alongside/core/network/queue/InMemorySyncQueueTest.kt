package com.alongside.core.network.queue

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InMemorySyncQueueTest {
    private fun op(id: String) = SyncOperation(id = id, collectionPath = "trips", documentId = id, type = SyncOperationType.UPSERT)

    @Test
    fun `enqueue preserves FIFO order`() {
        val queue = InMemorySyncQueue()

        queue.enqueue(op("a"))
        queue.enqueue(op("b"))
        queue.enqueue(op("c"))

        assertEquals(listOf("a", "b", "c"), queue.peekAll().map { it.id })
    }

    @Test
    fun `enqueueAll preserves order of the input list`() {
        val queue = InMemorySyncQueue()

        queue.enqueueAll(listOf(op("a"), op("b"), op("c")))

        assertEquals(listOf("a", "b", "c"), queue.peekAll().map { it.id })
    }

    @Test
    fun `isEmpty reflects state before and after removeFirstOrNull`() {
        val queue = InMemorySyncQueue()
        assertTrue(queue.isEmpty())

        queue.enqueue(op("a"))
        assertFalse(queue.isEmpty())

        assertEquals("a", queue.removeFirstOrNull()?.id)
        assertTrue(queue.isEmpty())
        assertNull(queue.removeFirstOrNull())
    }
}
