package com.alongside.core.domain.pretrip

import com.alongside.core.model.pretrip.PreTripPhoto

/**
 * Uploads a pre-trip photo's bytes to remote storage so a synced [PreTripPhoto] carries a
 * resolvable URL instead of a local `uri`. Same shape as
 * [com.alongside.core.domain.diary.processing.PhotoUploadClient]: a narrow interface in
 * core:domain, with the real Firebase Storage-backed implementation living in core:network so
 * this stays testable against fakes, independent of Ktor.
 */
public interface PreTripPhotoUploadClient {
    public suspend fun upload(
        photo: PreTripPhoto,
        bytes: ByteArray,
    ): PreTripPhotoUploadResult
}

public sealed class PreTripPhotoUploadResult {
    public data class Uploaded(
        public val remoteUrl: String,
    ) : PreTripPhotoUploadResult()

    public data class Failure(
        public val cause: Throwable,
    ) : PreTripPhotoUploadResult()
}
