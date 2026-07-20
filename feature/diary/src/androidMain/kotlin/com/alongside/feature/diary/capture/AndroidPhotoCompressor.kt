package com.alongside.feature.diary.capture

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

/**
 * Downscales (longest edge to [maxDimensionPx]) then JPEG-recompresses a photo's raw bytes -
 * originally built only for the Firebase Storage upload path, now also applied to the bytes fed
 * to Gemini vision: a real device crashed with `OutOfMemoryError` JSON/base64-encoding an
 * uncompressed ~10-20MB photo straight from a 50MP camera sensor into the Gemini request body -
 * "that path already works uncompressed" was only ever true for small hand-picked test images.
 * Gemini's captioning doesn't need full sensor resolution anyway.
 *
 * Real `BitmapFactory.decodeByteArray`/`Bitmap.compress` call - NOT unit-testable directly, same
 * class of problem as [AndroidExifPhotoReader]/[AndroidPhotoByteReader] (no fake BitmapFactory
 * available off-device).
 */
public class AndroidPhotoCompressor(
    private val quality: Int = DEFAULT_JPEG_QUALITY,
    private val maxDimensionPx: Int = DEFAULT_MAX_DIMENSION_PX,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : PhotoCompressor {
    override suspend fun compress(bytes: ByteArray): ByteArray =
        withContext(dispatcher) {
            val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@withContext bytes
            val scaled = original.downscaleToFit(maxDimensionPx)
            ByteArrayOutputStream().use { stream ->
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, stream)
                stream.toByteArray()
            }
        }

    private fun Bitmap.downscaleToFit(maxDimension: Int): Bitmap {
        val longestEdge = max(width, height)
        if (longestEdge <= maxDimension) return this
        val scale = maxDimension.toFloat() / longestEdge
        return Bitmap.createScaledBitmap(this, (width * scale).toInt(), (height * scale).toInt(), true)
    }

    private companion object {
        const val DEFAULT_JPEG_QUALITY = 80
        const val DEFAULT_MAX_DIMENSION_PX = 1600
    }
}
