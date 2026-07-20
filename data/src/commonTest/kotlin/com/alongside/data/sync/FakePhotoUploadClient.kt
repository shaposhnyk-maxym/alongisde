package com.alongside.data.sync

import com.alongside.core.domain.diary.processing.PhotoUploadClient
import com.alongside.core.domain.diary.processing.PhotoUploadResult
import com.alongside.core.model.diary.Photo

/** Always "uploads" successfully, to a deterministic Storage-shaped URL - never `content://`. */
internal class FakePhotoUploadClient : PhotoUploadClient {
    override suspend fun upload(
        photo: Photo,
        bytes: ByteArray,
    ): PhotoUploadResult =
        PhotoUploadResult.Uploaded(
            "https://firebasestorage.googleapis.com/v0/b/test-bucket/o/photos%2F${photo.id}" +
                "?alt=media&token=fake-token-${photo.id}",
        )
}
