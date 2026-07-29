package com.alongside.feature.diary.capture

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Photos.PHImageManager
import platform.Photos.PHImageRequestOptions
import kotlin.coroutines.resume

/**
 * `PHImageManager.requestImageDataAndOrientationForAsset` is completion-handler based, wrapped in
 * `suspendCancellableCoroutine` right here since [PhotoByteReader.readBytes] is already
 * `suspend`-shaped at the interface - no separate `AuthContainer`-style wrapping layer needed the
 * way [com.alongside.feature.auth.GoogleAuthProvider] required (that seam is deliberately
 * callback-based instead, since it's implemented in Swift).
 */
public class IosPhotoByteReader : PhotoByteReader {
    @OptIn(ExperimentalForeignApi::class)
    override suspend fun readBytes(uri: String): ByteArray {
        val asset = fetchAsset(uri) ?: error("PHAsset not found for localIdentifier=$uri")
        val data =
            suspendCancellableCoroutine { continuation ->
                val options = PHImageRequestOptions().apply { networkAccessAllowed = true }
                PHImageManager.defaultManager().requestImageDataAndOrientationForAsset(
                    asset,
                    options,
                ) { data, _, _, _ ->
                    continuation.resume(data)
                }
            }
        return data?.toByteArray() ?: error("No image data for localIdentifier=$uri")
    }
}
