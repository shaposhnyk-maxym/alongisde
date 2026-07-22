package com.alongside.data.place

import com.alongside.core.model.SyncStatus
import com.alongside.core.model.place.PlaceSwipe
import com.alongside.core.model.place.SwipeDirection
import com.alongside.core.network.firestore.model.FirestoreDocument
import com.alongside.core.network.firestore.model.FirestoreValue
import kotlin.time.Instant

/**
 * PlaceSwipe <-> Firestore document fields. `syncStatus` is deliberately not serialized - same
 * local-only bookkeeping reasoning as every other mapper in this package.
 */
public object PlaceSwipeFirestoreMapper {
    public const val COLLECTION_PATH: String = "placeSwipes"

    public fun toFields(swipe: PlaceSwipe): Map<String, FirestoreValue> =
        mapOf(
            "id" to FirestoreValue.StringValue(swipe.id),
            "tripId" to FirestoreValue.StringValue(swipe.tripId),
            "candidateId" to FirestoreValue.StringValue(swipe.candidateId),
            "userId" to FirestoreValue.StringValue(swipe.userId),
            "direction" to FirestoreValue.StringValue(swipe.direction.name),
            "swipedAt" to FirestoreValue.TimestampValue(swipe.swipedAt.toString()),
            "updatedAt" to FirestoreValue.TimestampValue(swipe.updatedAt.toString()),
        )

    public fun fromDocument(document: FirestoreDocument): PlaceSwipe {
        val fields = document.fields
        return PlaceSwipe(
            id = fields.requireString("id"),
            tripId = fields.requireString("tripId"),
            candidateId = fields.requireString("candidateId"),
            userId = fields.requireString("userId"),
            direction = SwipeDirection.valueOf(fields.requireString("direction")),
            swipedAt = Instant.parse(fields.requireTimestamp("swipedAt")),
            syncStatus = SyncStatus.SYNCED,
            updatedAt = Instant.parse(fields.requireTimestamp("updatedAt")),
        )
    }

    private fun Map<String, FirestoreValue>.requireString(key: String): String =
        (this[key] as? FirestoreValue.StringValue)?.value
            ?: throw IllegalArgumentException("PlaceSwipe document is missing string field '$key'")

    private fun Map<String, FirestoreValue>.requireTimestamp(key: String): String =
        (this[key] as? FirestoreValue.TimestampValue)?.value
            ?: throw IllegalArgumentException("PlaceSwipe document is missing timestamp field '$key'")
}
