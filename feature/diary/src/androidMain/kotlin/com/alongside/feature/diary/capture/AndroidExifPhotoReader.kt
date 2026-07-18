package com.alongside.feature.diary.capture

import android.content.ContentResolver
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.alongside.core.model.diary.Photo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.time.Instant

/** Photo.id is the URI itself - naturally unique per MediaStore entry, no separate id generator needed. */
public class AndroidExifPhotoReader(
    private val contentResolver: ContentResolver,
) : ExifPhotoReader {
    override suspend fun readExifPhotos(uris: List<String>): List<Photo> =
        withContext(Dispatchers.IO) {
            uris.mapNotNull(::readOne)
        }

    private fun readOne(uri: String): Photo? =
        contentResolver.openInputStream(Uri.parse(uri))?.use { stream ->
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

    private fun parseExifDate(value: String): Instant? =
        runCatching {
            val format = SimpleDateFormat(EXIF_DATE_FORMAT, Locale.US)
            Instant.fromEpochMilliseconds(format.parse(value)!!.time)
        }.getOrNull()

    private companion object {
        const val EXIF_DATE_FORMAT = "yyyy:MM:dd HH:mm:ss"
    }
}
