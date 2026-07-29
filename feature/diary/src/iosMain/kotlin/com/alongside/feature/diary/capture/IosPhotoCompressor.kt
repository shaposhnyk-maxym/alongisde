package com.alongside.feature.diary.capture

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import kotlin.math.max

/**
 * Downscales (longest edge to [maxDimensionPx]) then JPEG-recompresses a photo's raw bytes -
 * same rationale as [AndroidPhotoCompressor][com.alongside.feature.diary.capture.AndroidPhotoCompressor]
 * (avoids OOM-ing on the Gemini/Storage upload path with an uncompressed full-sensor-resolution
 * photo). Real `UIImage`/`UIGraphicsImageRenderer`-style calls, not unit-testable directly - same
 * class of problem as [IosExifPhotoReader]/[IosPhotoByteReader] (no fake `UIImage` off-device).
 */
public class IosPhotoCompressor(
    private val quality: Double = DEFAULT_JPEG_QUALITY,
    private val maxDimensionPx: Double = DEFAULT_MAX_DIMENSION_PX,
) : PhotoCompressor {
    @OptIn(ExperimentalForeignApi::class)
    override suspend fun compress(bytes: ByteArray): ByteArray =
        withContext(Dispatchers.Default) {
            val original = UIImage(data = bytes.toNSData())
            val scaled = original.downscaleToFit(maxDimensionPx)
            UIImageJPEGRepresentation(scaled, quality)?.toByteArray() ?: bytes
        }

    @OptIn(ExperimentalForeignApi::class)
    private fun UIImage.downscaleToFit(maxDimension: Double): UIImage {
        val (width, height) = size.useContents { width to height }
        val longestEdge = max(width, height)
        if (longestEdge <= maxDimension) return this
        val scale = maxDimension / longestEdge
        val scaledWidth = width * scale
        val scaledHeight = height * scale
        UIGraphicsBeginImageContextWithOptions(CGSizeMake(scaledWidth, scaledHeight), false, 1.0)
        return try {
            drawInRect(CGRectMake(0.0, 0.0, scaledWidth, scaledHeight))
            UIGraphicsGetImageFromCurrentImageContext() ?: this
        } finally {
            UIGraphicsEndImageContext()
        }
    }

    private companion object {
        const val DEFAULT_JPEG_QUALITY = 0.8
        const val DEFAULT_MAX_DIMENSION_PX = 1600.0
    }
}
