package com.alongside.data.pretrip

import com.alongside.core.model.SyncStatus
import com.alongside.core.model.pretrip.PreTripPhoto
import com.alongside.core.network.firestore.model.FirestoreDocument
import com.alongside.core.network.firestore.model.FirestoreValue
import kotlin.time.Instant

/**
 * PreTripPhoto <-> Firestore document fields. `syncStatus` is deliberately not serialized - same
 * local-only bookkeeping reasoning as every other mapper in this package.
 */
public object PreTripPhotoFirestoreMapper {
    public const val COLLECTION_PATH: String = "preTripPhotos"

    public fun toFields(photo: PreTripPhoto): Map<String, FirestoreValue> =
        mapOf(
            "id" to FirestoreValue.StringValue(photo.id),
            "tripId" to FirestoreValue.StringValue(photo.tripId),
            "userId" to FirestoreValue.StringValue(photo.userId),
            "uri" to FirestoreValue.StringValue(photo.uri),
            "takenAt" to FirestoreValue.TimestampValue(photo.takenAt.toString()),
            "latitude" to FirestoreValue.DoubleValue(photo.latitude),
            "longitude" to FirestoreValue.DoubleValue(photo.longitude),
            "remoteUrl" to (photo.remoteUrl?.let { FirestoreValue.StringValue(it) } ?: FirestoreValue.NullValue),
        )

    public fun fromDocument(document: FirestoreDocument): PreTripPhoto {
        val fields = document.fields
        return PreTripPhoto(
            id = fields.requireString("id"),
            tripId = fields.requireString("tripId"),
            userId = fields.requireString("userId"),
            uri = fields.requireString("uri"),
            takenAt = Instant.parse(fields.requireTimestamp("takenAt")),
            latitude = fields.requireDouble("latitude"),
            longitude = fields.requireDouble("longitude"),
            remoteUrl = (fields["remoteUrl"] as? FirestoreValue.StringValue)?.value,
            syncStatus = SyncStatus.SYNCED,
        )
    }

    private fun Map<String, FirestoreValue>.requireString(key: String): String =
        (this[key] as? FirestoreValue.StringValue)?.value
            ?: throw IllegalArgumentException("PreTripPhoto document is missing string field '$key'")

    private fun Map<String, FirestoreValue>.requireTimestamp(key: String): String =
        (this[key] as? FirestoreValue.TimestampValue)?.value
            ?: throw IllegalArgumentException("PreTripPhoto document is missing timestamp field '$key'")

    private fun Map<String, FirestoreValue>.requireDouble(key: String): Double =
        (this[key] as? FirestoreValue.DoubleValue)?.value
            ?: throw IllegalArgumentException("PreTripPhoto document is missing double field '$key'")
}
