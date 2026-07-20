package com.alongside.core.domain.diary.processing

import com.alongside.core.model.diary.Photo

/**
 * Uploads a photo's bytes to remote storage so a synced episode carries a resolvable URL instead
 * of a local `content://` URI. Same shape as [PlaceGeocodingClient]: a narrow interface in
 * core:domain, with the real Firebase Storage-backed implementation living in core:network so
 * this stays testable against fakes, independent of Ktor.
 */
public interface PhotoUploadClient {
    public suspend fun upload(
        photo: Photo,
        bytes: ByteArray,
    ): PhotoUploadResult
}
