package com.alongside.core.database.converter

import androidx.room.TypeConverter
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.place.PlacePhoto
import com.alongside.core.model.place.SwipeDirection
import com.alongside.core.model.push.PushPlatform
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

internal object AlongsideTypeConverters {
    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }

    @TypeConverter
    fun fromInstant(instant: Instant?): Long? = instant?.toEpochMilliseconds()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let { Instant.fromEpochMilliseconds(it) }

    @TypeConverter
    fun fromSyncStatus(status: SyncStatus?): String? = status?.name

    @TypeConverter
    fun toSyncStatus(value: String?): SyncStatus? = value?.let { SyncStatus.valueOf(it) }

    @TypeConverter
    fun fromSwipeDirection(direction: SwipeDirection?): String? = direction?.name

    @TypeConverter
    fun toSwipeDirection(value: String?): SwipeDirection? = value?.let { SwipeDirection.valueOf(it) }

    @TypeConverter
    fun fromPushPlatform(platform: PushPlatform?): String? = platform?.name

    @TypeConverter
    fun toPushPlatform(value: String?): PushPlatform? = value?.let { PushPlatform.valueOf(it) }
}

/** Split into its own object, not [AlongsideTypeConverters], purely to stay under detekt's TooManyFunctions. */
internal object StringListTypeConverters {
    // Newline-delimited, not JSON: this module has no kotlinx.serialization dependency, and every
    // element here is a URL (never contains a newline), so a plain join/split is sufficient.
    @TypeConverter
    fun fromStringList(values: List<String>): String = values.joinToString(separator = "\n")

    @TypeConverter
    fun toStringList(value: String): List<String> = if (value.isEmpty()) emptyList() else value.split("\n")
}

/**
 * One `photoRef\tremoteUrlOrEmpty` pair per line, newline-joined - same no-kotlinx.serialization
 * constraint as [StringListTypeConverters]. A single combined column (not two parallel
 * `List<String>` columns) deliberately avoids a same-length-invariant that two separately
 * converted lists would silently risk breaking. `photoRef` is never empty, so every encoded line
 * is non-empty even when `remoteUrl` is null - `toStringList`'s "empty string -> emptyList()"
 * collapse (see [StringListTypeConverters]) can only ever fire on a genuinely empty photo list.
 */
internal object PlacePhotoListTypeConverters {
    @TypeConverter
    fun fromPlacePhotoList(photos: List<PlacePhoto>): String =
        photos.joinToString(separator = "\n") { "${it.photoRef}\t${it.remoteUrl.orEmpty()}" }

    @TypeConverter
    fun toPlacePhotoList(value: String): List<PlacePhoto> =
        if (value.isEmpty()) {
            emptyList()
        } else {
            value.split("\n").map { line ->
                val (ref, url) = line.split("\t", limit = 2)
                PlacePhoto(photoRef = ref, remoteUrl = url.ifEmpty { null })
            }
        }
}
