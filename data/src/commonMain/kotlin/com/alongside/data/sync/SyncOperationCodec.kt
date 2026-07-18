package com.alongside.data.sync

import com.alongside.core.database.sync.PersistedSyncOperation
import com.alongside.core.database.sync.PersistedSyncOperationStatus
import com.alongside.core.database.sync.PersistedSyncOperationType
import com.alongside.core.network.firestore.model.FirestoreValue
import com.alongside.core.network.firestore.model.firestoreJson
import com.alongside.core.network.queue.SyncOperation
import com.alongside.core.network.queue.SyncOperationType
import kotlin.time.Instant

/**
 * Bridges core:network's in-memory [SyncOperation] and core:database's [PersistedSyncOperation]:
 * the two modules cannot see each other, so the field map crosses as firestoreJson text.
 */
public object SyncOperationCodec {
    public fun toPersisted(
        operation: SyncOperation,
        enqueuedAt: Instant,
    ): PersistedSyncOperation =
        PersistedSyncOperation(
            id = operation.id,
            collectionPath = operation.collectionPath,
            documentId = operation.documentId,
            type =
                when (operation.type) {
                    SyncOperationType.UPSERT -> PersistedSyncOperationType.UPSERT
                    SyncOperationType.DELETE -> PersistedSyncOperationType.DELETE
                },
            fieldsJson = firestoreJson.encodeToString<Map<String, FirestoreValue>>(operation.fields),
            attempts = operation.attempts,
            status = PersistedSyncOperationStatus.PENDING,
            enqueuedAt = enqueuedAt,
        )

    public fun toOperation(record: PersistedSyncOperation): SyncOperation =
        SyncOperation(
            id = record.id,
            collectionPath = record.collectionPath,
            documentId = record.documentId,
            type =
                when (record.type) {
                    PersistedSyncOperationType.UPSERT -> SyncOperationType.UPSERT
                    PersistedSyncOperationType.DELETE -> SyncOperationType.DELETE
                },
            fields = firestoreJson.decodeFromString<Map<String, FirestoreValue>>(record.fieldsJson),
            attempts = record.attempts,
        )
}
