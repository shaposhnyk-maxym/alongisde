package com.alongside.core.network.storage

import com.alongside.core.domain.pretrip.PreTripPhotoUploadClient
import com.alongside.core.domain.pretrip.PreTripPhotoUploadResult
import com.alongside.core.model.pretrip.PreTripPhoto
import com.alongside.core.network.storage.model.firstDownloadToken
import kotlinx.coroutines.CancellationException

/**
 * Adapts [FirebaseStorageApi] to the [PreTripPhotoUploadClient] seam core:domain depends on -
 * the pre-trip-photo counterpart of [FirebaseStorageUploadClient]. Reuses
 * [FirebaseStorageApi]/[FirebaseStorageConfig] as-is: the object id they take is just a string,
 * not tied to any diary-specific type, so no changes to either were needed. The `pretrip_` prefix
 * keeps the object id namespaced within the shared flat `photos/{photoId}` path, the same way
 * [FirebasePlacePhotoUploadClient] prefixes its own object ids with `place_`.
 */
public class FirebasePreTripPhotoUploadClient(
    private val api: FirebaseStorageApi,
    private val config: FirebaseStorageConfig,
) : PreTripPhotoUploadClient {
    override suspend fun upload(
        photo: PreTripPhoto,
        bytes: ByteArray,
    ): PreTripPhotoUploadResult {
        val objectId = "pretrip_${photo.id}"
        return try {
            val response = api.upload(objectId, bytes)
            val token = response.firstDownloadToken()
            if (token != null) {
                PreTripPhotoUploadResult.Uploaded(config.downloadUrl(objectId, token))
            } else {
                PreTripPhotoUploadResult.Failure(
                    IllegalStateException("Upload response for $objectId had no downloadTokens"),
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseStorageException) {
            PreTripPhotoUploadResult.Failure(e)
        }
    }
}
