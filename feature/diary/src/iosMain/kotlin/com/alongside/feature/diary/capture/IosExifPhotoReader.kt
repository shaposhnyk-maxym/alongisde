package com.alongside.feature.diary.capture

import com.alongside.core.model.diary.Photo
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.timeIntervalSince1970
import platform.Photos.PHAsset
import kotlin.time.Instant

/**
 * Unlike Android (which parses the raw EXIF GPS/DateTimeOriginal dictionary out of the file
 * bytes), this reads [PHAsset.location]/[PHAsset.creationDate] directly - Photos already parses
 * EXIF into these asset-level properties, so there's no need to fetch full image data just to
 * read two metadata fields, and no `CFDictionaryRef`-from-a-C-API bridging risk. `PHAsset` is the
 * same system framework [IosPermissionController][com.alongside.feature.onboarding.IosPermissionController]
 * already uses with zero custom cinterop - no Swift intermediary, no `ImageIO` dependency.
 *
 * A photo is dropped (same as Android's `mapNotNull` behavior) when either property is null - e.g.
 * screenshots or photos taken with Location Services off never had GPS embedded in the first
 * place.
 */
public class IosExifPhotoReader : ExifPhotoReader {
    override suspend fun readExifPhotos(uris: List<String>): List<Photo> =
        withContext(Dispatchers.Default) {
            uris.mapNotNull(::readOne)
        }

    @OptIn(ExperimentalForeignApi::class)
    private fun readOne(uri: String): Photo? =
        fetchAsset(uri)?.let { asset ->
            val location = asset.location ?: return null
            val creationDate = asset.creationDate ?: return null
            Photo(
                id = uri,
                uri = uri,
                takenAt = Instant.fromEpochSeconds(creationDate.timeIntervalSince1970.toLong()),
                latitude = location.coordinate.useContents { latitude },
                longitude = location.coordinate.useContents { longitude },
            )
        }
}
