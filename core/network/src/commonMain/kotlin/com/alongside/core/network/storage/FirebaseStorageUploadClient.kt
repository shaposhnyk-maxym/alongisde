package com.alongside.core.network.storage

import com.alongside.core.domain.diary.processing.PhotoUploadClient
import com.alongside.core.domain.diary.processing.PhotoUploadResult
import com.alongside.core.model.diary.Photo
import com.alongside.core.network.storage.model.firstDownloadToken
import kotlinx.coroutines.CancellationException

/**
 * Adapts [FirebaseStorageApi] to the [PhotoUploadClient] seam core:domain depends on.
 * [compress] is a bandwidth/cost concern specific to sending bytes over the wire to Storage - the
 * default is a no-op passthrough (used by every test), with the real Android-native compressor
 * wired in only at the DI edge (see `AndroidAppModule`).
 */
public class FirebaseStorageUploadClient(
    private val api: FirebaseStorageApi,
    private val config: FirebaseStorageConfig,
    private val compress: suspend (ByteArray) -> ByteArray = { it },
) : PhotoUploadClient {
    override suspend fun upload(
        photo: Photo,
        bytes: ByteArray,
    ): PhotoUploadResult =
        try {
            val response = api.upload(photo.id, compress(bytes))
            val token = response.firstDownloadToken()
            if (token != null) {
                PhotoUploadResult.Uploaded(config.downloadUrl(photo.id, token))
            } else {
                PhotoUploadResult.Failure(
                    IllegalStateException("Upload response for ${photo.id} had no downloadTokens"),
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseStorageException) {
            PhotoUploadResult.Failure(e)
        }
}
