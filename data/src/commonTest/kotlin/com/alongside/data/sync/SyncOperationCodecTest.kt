package com.alongside.data.sync

import com.alongside.core.database.sync.PersistedSyncOperationStatus
import com.alongside.core.database.sync.PersistedSyncOperationType
import com.alongside.core.network.firestore.model.FirestoreArrayValue
import com.alongside.core.network.firestore.model.FirestoreMapValue
import com.alongside.core.network.firestore.model.FirestoreValue
import com.alongside.core.network.queue.SyncOperation
import com.alongside.core.network.queue.SyncOperationType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class SyncOperationCodecTest {
    private val enqueuedAt = Instant.fromEpochMilliseconds(1_752_600_000_000)

    private val everyValueVariant: Map<String, FirestoreValue> =
        mapOf(
            "string" to FirestoreValue.StringValue("text"),
            "integer" to FirestoreValue.IntegerValue(42),
            "double" to FirestoreValue.DoubleValue(3.5),
            "boolean" to FirestoreValue.BooleanValue(true),
            "timestamp" to FirestoreValue.TimestampValue("2026-07-18T00:00:00Z"),
            "null" to FirestoreValue.NullValue,
            "map" to FirestoreValue.MapValue(FirestoreMapValue(mapOf("nested" to FirestoreValue.StringValue("x")))),
            "array" to FirestoreValue.ArrayValue(FirestoreArrayValue(listOf(FirestoreValue.IntegerValue(1)))),
        )

    @Test
    fun `upsert operation round trips through the persisted record with every value variant`() {
        val operation =
            SyncOperation(
                id = "op-1",
                collectionPath = "trips",
                documentId = "trip-1",
                type = SyncOperationType.UPSERT,
                fields = everyValueVariant,
                attempts = 2,
            )

        val persisted = SyncOperationCodec.toPersisted(operation, enqueuedAt)

        assertEquals(PersistedSyncOperationType.UPSERT, persisted.type)
        assertEquals(PersistedSyncOperationStatus.PENDING, persisted.status)
        assertEquals(enqueuedAt, persisted.enqueuedAt)
        assertEquals(operation, SyncOperationCodec.toOperation(persisted))
    }

    @Test
    fun `delete operation round trips with empty fields`() {
        val operation =
            SyncOperation(
                id = "op-2",
                collectionPath = "trips",
                documentId = "trip-1",
                type = SyncOperationType.DELETE,
            )

        val persisted = SyncOperationCodec.toPersisted(operation, enqueuedAt)

        assertEquals(PersistedSyncOperationType.DELETE, persisted.type)
        assertEquals(operation, SyncOperationCodec.toOperation(persisted))
    }

    @Test
    fun `retry marked record decodes back with its persisted attempt count`() {
        val operation = SyncOperation(id = "op-3", collectionPath = "trips", documentId = "t", type = SyncOperationType.UPSERT)
        val persisted =
            SyncOperationCodec
                .toPersisted(operation, enqueuedAt)
                .copy(status = PersistedSyncOperationStatus.RETRY, attempts = 5)

        assertEquals(5, SyncOperationCodec.toOperation(persisted).attempts)
    }
}
