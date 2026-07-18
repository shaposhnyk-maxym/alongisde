package com.alongside.core.network.firestore.model

import kotlinx.serialization.Serializable

/**
 * Minimal structured-query support for `documents:runQuery` - just what pairing lookups need
 * (equality filters, OR composition, limit).
 * See: https://firebase.google.com/docs/firestore/reference/rest/v1/StructuredQuery
 */
@Serializable
public data class RunQueryRequest(
    public val structuredQuery: StructuredQuery,
)

@Serializable
public data class StructuredQuery(
    public val from: List<CollectionSelector>,
    public val where: QueryFilter? = null,
    public val limit: Int? = null,
)

@Serializable
public data class CollectionSelector(
    public val collectionId: String,
)

/** Union-by-nullable-field, like the Firestore REST `Filter` message: exactly one side is set. */
@Serializable
public data class QueryFilter(
    public val fieldFilter: FieldFilter? = null,
    public val compositeFilter: CompositeFilter? = null,
)

@Serializable
public data class FieldFilter(
    public val field: FieldReference,
    public val op: String,
    public val value: FirestoreValue,
)

@Serializable
public data class CompositeFilter(
    public val op: String,
    public val filters: List<QueryFilter>,
)

@Serializable
public data class FieldReference(
    public val fieldPath: String,
)

/** One streamed element of a runQuery response; elements without [document] carry only metadata. */
@Serializable
public data class RunQueryResponseElement(
    public val document: FirestoreDocument? = null,
    public val readTime: String? = null,
    public val done: Boolean? = null,
)
