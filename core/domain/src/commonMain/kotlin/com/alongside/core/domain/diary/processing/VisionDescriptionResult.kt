package com.alongside.core.domain.diary.processing

public sealed class VisionDescriptionResult {
    public data class Generated(
        public val text: String,
    ) : VisionDescriptionResult()

    public data class Failure(
        public val cause: Throwable,
    ) : VisionDescriptionResult()
}
