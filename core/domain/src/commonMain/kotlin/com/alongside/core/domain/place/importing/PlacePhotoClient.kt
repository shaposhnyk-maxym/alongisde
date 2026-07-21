package com.alongside.core.domain.place.importing

/**
 * Fetches a Google Place photo's raw bytes by its `photoRef` (as returned by
 * [PlaceDetailsLookupClient]), so [PlaceImportPipeline] can re-host them in Firebase Storage
 * instead of storing a Google-hosted, API-key-bearing URL. Same seam shape as
 * [com.alongside.core.domain.diary.processing.PhotoUploadClient]. Returns `null` on any failure
 * (network, decoding, missing reference) - a single failed photo degrades to "skip this one
 * photo", never throws, mirroring
 * [com.alongside.core.domain.diary.processing.EpisodeProcessingPipeline]'s
 * `loadImageBytesOrNull` convention.
 */
public interface PlacePhotoClient {
    public suspend fun fetchPhotoBytes(photoRef: String): ByteArray?
}

/**
 * Uploads a place photo's bytes to remote storage and returns a resolvable URL - the
 * place-scoped counterpart of
 * [com.alongside.core.domain.diary.processing.PhotoUploadClient], which is keyed to a diary
 * [com.alongside.core.model.diary.Photo] rather than a place-photo index.
 */
public interface PlacePhotoUploadClient {
    public suspend fun upload(
        placeCandidateId: String,
        photoIndex: Int,
        bytes: ByteArray,
    ): PlacePhotoUploadResult
}

public sealed class PlacePhotoUploadResult {
    public data class Uploaded(
        public val remoteUrl: String,
    ) : PlacePhotoUploadResult()

    public data class Failure(
        public val cause: Throwable,
    ) : PlacePhotoUploadResult()
}
