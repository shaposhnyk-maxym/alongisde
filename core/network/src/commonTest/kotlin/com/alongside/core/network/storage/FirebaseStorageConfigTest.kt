package com.alongside.core.network.storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class FirebaseStorageConfigTest {
    private val config = FirebaseStorageConfig(bucket = "test-bucket")

    @Test
    fun `objectPath sanitizes a content URI photoId into a safe object name`() {
        val photoId = "content://com.android.providers.media.documents/document/image%3A21892"

        val path = config.objectPath(photoId)

        assertEquals("photos/content___com.android.providers.media.documents_document_image_3A21892", path)
        assertFalse(path.contains("//"))
        assertFalse(path.endsWith("/"))
    }

    @Test
    fun `uploadUrl and downloadUrl never contain a raw double slash from the photoId`() {
        val photoId = "content://media/external/images/media/42"

        val uploadUrl = config.uploadUrl(photoId)
        val downloadUrl = config.downloadUrl(photoId, downloadToken = "tok")

        assertFalse(uploadUrl.contains("//media/external"))
        assertFalse(downloadUrl.contains("//media/external"))
    }

    @Test
    fun `objectPath leaves an already-safe id unchanged`() {
        assertEquals("photos/photo-1", config.objectPath("photo-1"))
    }
}
