package com.alongside.data.place

import com.alongside.core.model.SyncStatus
import com.alongside.core.model.place.PlaceCandidate
import com.alongside.core.model.place.PlacePhoto
import com.alongside.core.model.place.SwipeDirection
import com.alongside.core.network.firestore.model.FirestoreArrayValue
import com.alongside.core.network.firestore.model.FirestoreDocument
import com.alongside.core.network.firestore.model.FirestoreMapValue
import com.alongside.core.network.firestore.model.FirestoreValue
import kotlin.time.Instant

/**
 * PlaceCandidate <-> Firestore document fields. `syncStatus` is deliberately not serialized - it
 * is a local-only bookkeeping flag; anything read back from the remote is [SyncStatus.SYNCED].
 * [PlaceCandidate.photos] has no separate Firestore collection, same "plain embedded array"
 * reasoning as [com.alongside.data.episode.EpisodeFirestoreMapper]'s `photos` field.
 */
public object PlaceCandidateFirestoreMapper {
    public const val COLLECTION_PATH: String = "placeCandidates"

    public fun toFields(place: PlaceCandidate): Map<String, FirestoreValue> =
        mapOf(
            "id" to FirestoreValue.StringValue(place.id),
            "tripId" to FirestoreValue.StringValue(place.tripId),
            "name" to FirestoreValue.StringValue(place.name),
            "latitude" to FirestoreValue.DoubleValue(place.latitude),
            "longitude" to FirestoreValue.DoubleValue(place.longitude),
            "note" to (place.note?.let { FirestoreValue.StringValue(it) } ?: FirestoreValue.NullValue),
            "addedByUserId" to FirestoreValue.StringValue(place.addedByUserId),
            "ownerSwipe" to place.ownerSwipe.toFirestoreValue(),
            "memberSwipe" to place.memberSwipe.toFirestoreValue(),
            "createdAt" to FirestoreValue.TimestampValue(place.createdAt.toString()),
            "updatedAt" to FirestoreValue.TimestampValue(place.updatedAt.toString()),
            "photos" to FirestoreValue.ArrayValue(FirestoreArrayValue(place.photos.map { it.toFirestoreValue() })),
            "rating" to (place.rating?.let { FirestoreValue.DoubleValue(it) } ?: FirestoreValue.NullValue),
            "category" to (place.category?.let { FirestoreValue.StringValue(it) } ?: FirestoreValue.NullValue),
            "city" to (place.city?.let { FirestoreValue.StringValue(it) } ?: FirestoreValue.NullValue),
        )

    public fun fromDocument(document: FirestoreDocument): PlaceCandidate {
        val fields = document.fields
        return PlaceCandidate(
            id = fields.requireString("id"),
            tripId = fields.requireString("tripId"),
            name = fields.requireString("name"),
            latitude = fields.requireDouble("latitude"),
            longitude = fields.requireDouble("longitude"),
            note = (fields["note"] as? FirestoreValue.StringValue)?.value,
            addedByUserId = fields.requireString("addedByUserId"),
            ownerSwipe = fields.toSwipeDirection("ownerSwipe"),
            memberSwipe = fields.toSwipeDirection("memberSwipe"),
            syncStatus = SyncStatus.SYNCED,
            createdAt = Instant.parse(fields.requireTimestamp("createdAt")),
            updatedAt = Instant.parse(fields.requireTimestamp("updatedAt")),
            photos = fields.requirePhotos("photos"),
            rating = (fields["rating"] as? FirestoreValue.DoubleValue)?.value,
            category = (fields["category"] as? FirestoreValue.StringValue)?.value,
            city = (fields["city"] as? FirestoreValue.StringValue)?.value,
        )
    }

    private fun SwipeDirection?.toFirestoreValue(): FirestoreValue =
        this?.let { FirestoreValue.StringValue(it.name) } ?: FirestoreValue.NullValue

    private fun Map<String, FirestoreValue>.toSwipeDirection(key: String): SwipeDirection? =
        (this[key] as? FirestoreValue.StringValue)?.value?.let { SwipeDirection.valueOf(it) }

    private fun PlacePhoto.toFirestoreValue(): FirestoreValue =
        FirestoreValue.MapValue(
            FirestoreMapValue(
                mapOf(
                    "photoRef" to FirestoreValue.StringValue(photoRef),
                    "remoteUrl" to (remoteUrl?.let { FirestoreValue.StringValue(it) } ?: FirestoreValue.NullValue),
                ),
            ),
        )

    private fun FirestoreValue.toPlacePhoto(): PlacePhoto {
        val fields = (this as FirestoreValue.MapValue).value.fields
        return PlacePhoto(
            photoRef = fields.requireString("photoRef"),
            remoteUrl = (fields["remoteUrl"] as? FirestoreValue.StringValue)?.value,
        )
    }

    private fun Map<String, FirestoreValue>.requirePhotos(key: String): List<PlacePhoto> =
        ((this[key] as? FirestoreValue.ArrayValue)?.value?.values ?: emptyList()).map { it.toPlacePhoto() }

    private fun Map<String, FirestoreValue>.requireString(key: String): String =
        (this[key] as? FirestoreValue.StringValue)?.value
            ?: throw IllegalArgumentException("PlaceCandidate document is missing string field '$key'")

    private fun Map<String, FirestoreValue>.requireTimestamp(key: String): String =
        (this[key] as? FirestoreValue.TimestampValue)?.value
            ?: throw IllegalArgumentException("PlaceCandidate document is missing timestamp field '$key'")

    private fun Map<String, FirestoreValue>.requireDouble(key: String): Double =
        (this[key] as? FirestoreValue.DoubleValue)?.value
            ?: throw IllegalArgumentException("PlaceCandidate document is missing double field '$key'")
}
