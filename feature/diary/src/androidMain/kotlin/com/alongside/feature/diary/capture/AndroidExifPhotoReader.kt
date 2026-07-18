package com.alongside.feature.diary.capture

import android.content.ContentResolver
import android.net.Uri
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.alongside.core.model.diary.Photo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.time.Instant

/**
 * Photo.id is the URI itself - naturally unique per MediaStore entry, no separate id generator
 * needed.
 *
 * On API 29+, MediaStore redacts GPS EXIF tags from `openInputStream()` by default (scoped
 * storage privacy) unless the caller holds `ACCESS_MEDIA_LOCATION` and explicitly requests the
 * unredacted copy via [MediaStore.setRequireOriginal] - without both, `ExifInterface.latLong`
 * silently returns null for every photo on any modern device. [readOne] always attempts the
 * unredacted URI first; `setRequireOriginal` throws when the permission isn't held, so this falls
 * back to the (GPS-redacted) original URI rather than failing outright. The actual runtime
 * permission *request* isn't wired up in M10 - no UI/permission flow exists yet (see roadmap.md)
 * - so today this permission is simply never granted and every photo silently has no GPS, exactly
 * as before this fix; the difference is this now degrades gracefully instead of relying on
 * undocumented redaction behavior, and will pick up real GPS the moment something grants the
 * permission.
 */
public class AndroidExifPhotoReader(
    private val contentResolver: ContentResolver,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ExifPhotoReader {
    override suspend fun readExifPhotos(uris: List<String>): List<Photo> =
        withContext(dispatcher) {
            uris.mapNotNull(::readOne)
        }

    private fun readOne(uri: String): Photo? =
        openUnredactedOrOriginal(Uri.parse(uri))?.use { stream ->
            val exif = ExifInterface(stream)
            val latLong = exif.latLong ?: return null
            val takenAt = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)?.let(::parseExifDate) ?: return null
            Photo(
                id = uri,
                uri = uri,
                takenAt = takenAt,
                latitude = latLong[0],
                longitude = latLong[1],
            )
        }

    private fun openUnredactedOrOriginal(uri: Uri) =
        runCatching { contentResolver.openInputStream(MediaStore.setRequireOriginal(uri)) }
            .getOrNull() ?: contentResolver.openInputStream(uri)

    private fun parseExifDate(value: String): Instant? =
        runCatching {
            val format = SimpleDateFormat(EXIF_DATE_FORMAT, Locale.US)
            Instant.fromEpochMilliseconds(format.parse(value)!!.time)
        }.getOrNull()

    private companion object {
        const val EXIF_DATE_FORMAT = "yyyy:MM:dd HH:mm:ss"
    }
}
