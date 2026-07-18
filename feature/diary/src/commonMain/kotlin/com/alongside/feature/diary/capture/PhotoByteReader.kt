package com.alongside.feature.diary.capture

/**
 * Reads the raw bytes of a photo off its content URI, for feeding into
 * `EpisodeVisionDescriptionClient.describeEpisode`. Same Android-only, untested-native-seam
 * treatment as [ExifPhotoReader].
 */
public interface PhotoByteReader {
    public suspend fun readBytes(uri: String): ByteArray
}
