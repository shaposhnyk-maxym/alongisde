package com.alongside.core.domain.diary.processing

public sealed class GeocodingResult {
    public data class Found(
        public val placeName: String,
        public val city: String? = null,
    ) : GeocodingResult()

    public data object NotFound : GeocodingResult()

    public data class Failure(
        public val cause: Throwable,
    ) : GeocodingResult()
}
