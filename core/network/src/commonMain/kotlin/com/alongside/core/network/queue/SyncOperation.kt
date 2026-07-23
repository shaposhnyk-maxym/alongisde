package com.alongside.core.network.queue

import com.alongside.core.network.firestore.model.FirestoreValue

public enum class SyncOperationType {
    UPSERT,

    DELETE,

    /**
     * Like [UPSERT], but always wins any remote conflict - for deliberate, user-confirmed
     * changes (e.g. Leave Trip) where local intent must never be silently overridden by a
     * concurrent write, unlike routine offline-first upserts.
     */
    FORCE_UPSERT,
}

/**
 * A single pending write against Firestore. Deliberately entity-agnostic (no core:model/
 * core:domain types) - the `data` module maps its own entities to/from this shape.
 */
public data class SyncOperation(
    public val id: String,
    public val collectionPath: String,
    public val documentId: String,
    public val type: SyncOperationType,
    public val fields: Map<String, FirestoreValue> = emptyMap(),
    public val attempts: Int = 0,
)
