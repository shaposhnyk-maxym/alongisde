package com.alongside.core.network.storage

import com.alongside.core.domain.place.importing.PlacePhotoUploadClient
import com.alongside.core.domain.place.importing.PlacePhotoUploadResult
import com.alongside.core.network.storage.model.firstDownloadToken
import kotlinx.coroutines.CancellationException

/**
 * Adapts [FirebaseStorageApi] to the [PlacePhotoUploadClient] seam core:domain depends on - the
 * place-photo counterpart of [FirebaseStorageUploadClient], which is keyed to a diary
 * [com.alongside.core.model.diary.Photo] rather than a (place, index) pair. Reuses
 * [FirebaseStorageApi]/[FirebaseStorageConfig] as-is: the object id they take is just a string,
 * not tied to any diary-specific type, so no changes to either were needed.
 */
public class FirebasePlacePhotoUploadClient(
    private val api: FirebaseStorageApi,
    private val config: FirebaseStorageConfig,
) : PlacePhotoUploadClient {
    override suspend fun upload(
        placeCandidateId: String,
        photoIndex: Int,
        bytes: ByteArray,
    ): PlacePhotoUploadResult {
        val objectId = "place_${placeCandidateId}_$photoIndex"
        return try {
            val response = api.upload(objectId, bytes)
            val token = response.firstDownloadToken()
            if (token != null) {
                PlacePhotoUploadResult.Uploaded(config.downloadUrl(objectId, token))
            } else {
                PlacePhotoUploadResult.Failure(
                    IllegalStateException("Upload response for $objectId had no downloadTokens"),
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseStorageException) {
            PlacePhotoUploadResult.Failure(e)
        }
    }
}
