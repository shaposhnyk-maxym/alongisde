package com.alongside.data.episode

import com.alongside.core.model.SyncStatus
import com.alongside.core.model.diary.Episode
import com.alongside.core.model.diary.Photo
import com.alongside.core.network.firestore.model.FirestoreArrayValue
import com.alongside.core.network.firestore.model.FirestoreDocument
import com.alongside.core.network.firestore.model.FirestoreMapValue
import com.alongside.core.network.firestore.model.FirestoreValue
import kotlin.time.Instant

/**
 * Episode <-> Firestore document fields. `syncStatus` is deliberately not serialized - it is a
 * local-only bookkeeping flag; anything read back from the remote is [SyncStatus.SYNCED].
 * [Episode.photos] has no separate Firestore collection (there's no cross-device benefit to
 * splitting it out - unlike Room's FK+cascade table, it's a plain embedded array here).
 */
public object EpisodeFirestoreMapper {
    public const val COLLECTION_PATH: String = "episodes"

    public fun toFields(episode: Episode): Map<String, FirestoreValue> =
        mapOf(
            "id" to FirestoreValue.StringValue(episode.id),
            "diaryEntryId" to FirestoreValue.StringValue(episode.diaryEntryId),
            "startTime" to FirestoreValue.TimestampValue(episode.startTime.toString()),
            "endTime" to FirestoreValue.TimestampValue(episode.endTime.toString()),
            "latitude" to FirestoreValue.DoubleValue(episode.latitude),
            "longitude" to FirestoreValue.DoubleValue(episode.longitude),
            "placeName" to (episode.placeName?.let { FirestoreValue.StringValue(it) } ?: FirestoreValue.NullValue),
            "description" to
                (episode.description?.let { FirestoreValue.StringValue(it) } ?: FirestoreValue.NullValue),
            "descriptionAttempts" to FirestoreValue.IntegerValue(episode.descriptionAttempts.toLong()),
            "photos" to
                FirestoreValue.ArrayValue(FirestoreArrayValue(episode.photos.map { it.toFirestoreValue() })),
            "updatedAt" to FirestoreValue.TimestampValue(episode.updatedAt.toString()),
            "city" to (episode.city?.let { FirestoreValue.StringValue(it) } ?: FirestoreValue.NullValue),
            "cityPlaceId" to
                (episode.cityPlaceId?.let { FirestoreValue.StringValue(it) } ?: FirestoreValue.NullValue),
            "countryCode" to
                (episode.countryCode?.let { FirestoreValue.StringValue(it) } ?: FirestoreValue.NullValue),
            "geocodeAttempts" to FirestoreValue.IntegerValue(episode.geocodeAttempts.toLong()),
        )

    public fun fromDocument(document: FirestoreDocument): Episode {
        val fields = document.fields
        return Episode(
            id = fields.requireString("id"),
            diaryEntryId = fields.requireString("diaryEntryId"),
            startTime = Instant.parse(fields.requireTimestamp("startTime")),
            endTime = Instant.parse(fields.requireTimestamp("endTime")),
            latitude = fields.requireDouble("latitude"),
            longitude = fields.requireDouble("longitude"),
            placeName = (fields["placeName"] as? FirestoreValue.StringValue)?.value,
            description = (fields["description"] as? FirestoreValue.StringValue)?.value,
            descriptionAttempts = fields.requireInt("descriptionAttempts"),
            photos = fields.requirePhotos("photos"),
            syncStatus = SyncStatus.SYNCED,
            updatedAt = Instant.parse(fields.requireTimestamp("updatedAt")),
            city = (fields["city"] as? FirestoreValue.StringValue)?.value,
            cityPlaceId = (fields["cityPlaceId"] as? FirestoreValue.StringValue)?.value,
            countryCode = (fields["countryCode"] as? FirestoreValue.StringValue)?.value,
            // Lenient, not requireInt like descriptionAttempts: this field shipped after
            // descriptionAttempts, so documents already synced by earlier app versions lack it -
            // treat a missing value as "no attempts yet" rather than throwing on old data.
            geocodeAttempts = (fields["geocodeAttempts"] as? FirestoreValue.IntegerValue)?.value?.toInt() ?: 0,
        )
    }

    private fun Photo.toFirestoreValue(): FirestoreValue =
        FirestoreValue.MapValue(
            FirestoreMapValue(
                mapOf(
                    "id" to FirestoreValue.StringValue(id),
                    "uri" to FirestoreValue.StringValue(uri),
                    "takenAt" to FirestoreValue.TimestampValue(takenAt.toString()),
                    "latitude" to FirestoreValue.DoubleValue(latitude),
                    "longitude" to FirestoreValue.DoubleValue(longitude),
                    "remoteUrl" to (remoteUrl?.let { FirestoreValue.StringValue(it) } ?: FirestoreValue.NullValue),
                ),
            ),
        )

    private fun FirestoreValue.toPhoto(): Photo {
        val fields = (this as FirestoreValue.MapValue).value.fields
        return Photo(
            id = fields.requireString("id"),
            uri = fields.requireString("uri"),
            takenAt = Instant.parse(fields.requireTimestamp("takenAt")),
            latitude = fields.requireDouble("latitude"),
            longitude = fields.requireDouble("longitude"),
            remoteUrl = (fields["remoteUrl"] as? FirestoreValue.StringValue)?.value,
        )
    }

    private fun Map<String, FirestoreValue>.requirePhotos(key: String): List<Photo> =
        ((this[key] as? FirestoreValue.ArrayValue)?.value?.values ?: emptyList()).map { it.toPhoto() }

    private fun Map<String, FirestoreValue>.requireString(key: String): String =
        (this[key] as? FirestoreValue.StringValue)?.value
            ?: throw IllegalArgumentException("Episode document is missing string field '$key'")

    private fun Map<String, FirestoreValue>.requireTimestamp(key: String): String =
        (this[key] as? FirestoreValue.TimestampValue)?.value
            ?: throw IllegalArgumentException("Episode document is missing timestamp field '$key'")

    private fun Map<String, FirestoreValue>.requireDouble(key: String): Double =
        (this[key] as? FirestoreValue.DoubleValue)?.value
            ?: throw IllegalArgumentException("Episode document is missing double field '$key'")

    private fun Map<String, FirestoreValue>.requireInt(key: String): Int =
        (this[key] as? FirestoreValue.IntegerValue)?.value?.toInt()
            ?: throw IllegalArgumentException("Episode document is missing integer field '$key'")
}
