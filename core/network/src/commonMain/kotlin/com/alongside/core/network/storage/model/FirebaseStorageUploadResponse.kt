package com.alongside.core.network.storage.model

import kotlinx.serialization.Serializable

/**
 * [downloadTokens] is comma-separated when multiple tokens exist for the object; only the first
 * is needed to build a working download URL.
 */
@Serializable
public data class FirebaseStorageUploadResponse(
    public val name: String,
    public val bucket: String,
    public val downloadTokens: String? = null,
)

public fun FirebaseStorageUploadResponse.firstDownloadToken(): String? = downloadTokens?.substringBefore(",")
