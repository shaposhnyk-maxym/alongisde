package com.alongside.core.network.queue

import com.alongside.core.network.firestore.model.FirestoreValue

public enum class SyncOperationType { UPSERT, DELETE }

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
