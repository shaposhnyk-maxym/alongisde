package com.alongside.feature.diary.capture

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create
import platform.Photos.PHAsset
import platform.posix.memcpy

/**
 * `uris` in [ExifPhotoReader]/[PhotoByteReader] are `PHAsset.localIdentifier` strings on iOS - the
 * platform equivalent of Android's MediaStore content-URI string, shared by both readers below.
 */
@OptIn(ExperimentalForeignApi::class)
internal fun fetchAsset(localIdentifier: String): PHAsset? =
    PHAsset.fetchAssetsWithLocalIdentifiers(listOf(localIdentifier), options = null).firstObject as? PHAsset

@OptIn(ExperimentalForeignApi::class)
internal fun NSData.toByteArray(): ByteArray {
    val result = ByteArray(length.toInt())
    if (result.isNotEmpty()) {
        result.usePinned { pinned -> memcpy(pinned.addressOf(0), bytes, length) }
    }
    return result
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal fun ByteArray.toNSData(): NSData =
    if (isEmpty()) {
        NSData()
    } else {
        usePinned { pinned -> NSData.create(bytes = pinned.addressOf(0), length = size.toULong()) }
    }
