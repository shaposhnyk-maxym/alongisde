package com.alongside.data.trip

import com.alongside.core.model.SyncStatus
import com.alongside.core.model.trip.Trip
import com.alongside.core.network.firestore.model.FirestoreDocument
import com.alongside.core.network.firestore.model.FirestoreValue
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * Trip <-> Firestore document fields. `syncStatus` is deliberately not serialized - it is
 * a local-only bookkeeping flag; anything read back from the remote is [SyncStatus.SYNCED].
 */
public object TripFirestoreMapper {
    public const val COLLECTION_PATH: String = "trips"

    public fun toFields(trip: Trip): Map<String, FirestoreValue> =
        mapOf(
            "id" to FirestoreValue.StringValue(trip.id),
            "ownerId" to FirestoreValue.StringValue(trip.ownerId),
            "memberId" to (trip.memberId?.let { FirestoreValue.StringValue(it) } ?: FirestoreValue.NullValue),
            "inviteCode" to FirestoreValue.StringValue(trip.inviteCode),
            "startDate" to FirestoreValue.StringValue(trip.startDate.toString()),
            "endDate" to FirestoreValue.StringValue(trip.endDate.toString()),
            "createdAt" to FirestoreValue.TimestampValue(trip.createdAt.toString()),
            "updatedAt" to FirestoreValue.TimestampValue(trip.updatedAt.toString()),
        )

    public fun fromDocument(document: FirestoreDocument): Trip {
        val fields = document.fields
        return Trip(
            id = fields.requireString("id"),
            ownerId = fields.requireString("ownerId"),
            memberId = (fields["memberId"] as? FirestoreValue.StringValue)?.value,
            inviteCode = fields.requireString("inviteCode"),
            startDate = LocalDate.parse(fields.requireString("startDate")),
            endDate = LocalDate.parse(fields.requireString("endDate")),
            syncStatus = SyncStatus.SYNCED,
            createdAt = Instant.parse(fields.requireTimestamp("createdAt")),
            updatedAt = Instant.parse(fields.requireTimestamp("updatedAt")),
        )
    }

    private fun Map<String, FirestoreValue>.requireString(key: String): String =
        (this[key] as? FirestoreValue.StringValue)?.value
            ?: throw IllegalArgumentException("Trip document is missing string field '$key'")

    private fun Map<String, FirestoreValue>.requireTimestamp(key: String): String =
        (this[key] as? FirestoreValue.TimestampValue)?.value
            ?: throw IllegalArgumentException("Trip document is missing timestamp field '$key'")
}
