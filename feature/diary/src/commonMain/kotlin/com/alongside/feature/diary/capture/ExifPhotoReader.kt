package com.alongside.feature.diary.capture

import com.alongside.core.model.diary.Photo

/**
 * Reads EXIF metadata (GPS + capture timestamp) off device photo URIs into [Photo] domain
 * objects. Device/photo-library access is a platform concern that can't be unit-tested cheaply
 * (no fake ContentResolver) - mirrors M5's honest treatment of untestable native SDK code.
 * `clusterPhotosIntoEpisodes` and the rest of the processing pipeline (core:domain) are tested
 * independently against synthetic [Photo] lists, so this seam stays as thin as possible.
 *
 * Android-only for now (see docs/roadmap.md M10) - iOS photo-library/EXIF reading is deferred,
 * same status as M7 (iOS Onboarding).
 */
public interface ExifPhotoReader {
    public suspend fun readExifPhotos(uris: List<String>): List<Photo>
}
