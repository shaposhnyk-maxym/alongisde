package com.alongside.core.domain.diary.processing

public sealed class PhotoUploadResult {
    public data class Uploaded(
        public val remoteUrl: String,
    ) : PhotoUploadResult()

    public data class Failure(
        public val cause: Throwable,
    ) : PhotoUploadResult()
}
