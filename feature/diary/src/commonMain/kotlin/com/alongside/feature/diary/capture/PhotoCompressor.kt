package com.alongside.feature.diary.capture

/**
 * Downscales/compresses a photo's raw bytes - used both before upload to Firebase Storage and
 * before feeding a photo to Gemini vision (neither needs full sensor resolution). Same
 * Android-only, untested-native-seam treatment as [PhotoByteReader]/[ExifPhotoReader] - the real
 * implementation (`AndroidPhotoCompressor`) uses `BitmapFactory`/`Bitmap.compress`, with no fake
 * available.
 */
public interface PhotoCompressor {
    public suspend fun compress(bytes: ByteArray): ByteArray
}
