package com.alongside.core.network.firestore.model

import kotlinx.serialization.Serializable

/**
 * Mirrors Firestore's documented typed-value wrapper (each value is encoded as an object with
 * exactly one key identifying its type, e.g. `{"stringValue": "..."}`).
 * See: https://firebase.google.com/docs/firestore/reference/rest/v1/Value
 */
@Serializable(with = FirestoreValueSerializer::class)
public sealed interface FirestoreValue {
    public data class StringValue(
        public val value: String,
    ) : FirestoreValue

    // Firestore encodes int64 as a decimal string, not a JSON number.
    public data class IntegerValue(
        public val value: Long,
    ) : FirestoreValue

    public data class DoubleValue(
        public val value: Double,
    ) : FirestoreValue

    public data class BooleanValue(
        public val value: Boolean,
    ) : FirestoreValue

    // RFC3339 UTC, e.g. "2024-01-01T00:00:00Z".
    public data class TimestampValue(
        public val value: String,
    ) : FirestoreValue

    public data object NullValue : FirestoreValue

    public data class MapValue(
        public val value: FirestoreMapValue,
    ) : FirestoreValue

    public data class ArrayValue(
        public val value: FirestoreArrayValue,
    ) : FirestoreValue
}

@Serializable
public data class FirestoreMapValue(
    public val fields: Map<String, FirestoreValue> = emptyMap(),
)

@Serializable
public data class FirestoreArrayValue(
    public val values: List<FirestoreValue> = emptyList(),
)
