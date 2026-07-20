package com.alongside.feature.diary

import com.alongside.core.model.diary.Photo
import com.alongside.feature.diary.capture.ExifPhotoReader

/** Maps each URI to a fixed [Photo] by lookup, ignoring URIs it wasn't primed for. */
internal class FakeExifPhotoReader(
    private val photosByUri: Map<String, Photo>,
) : ExifPhotoReader {
    override suspend fun readExifPhotos(uris: List<String>): List<Photo> = uris.mapNotNull(photosByUri::get)
}
