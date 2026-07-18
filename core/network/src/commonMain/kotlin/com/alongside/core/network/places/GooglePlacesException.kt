package com.alongside.core.network.places

public sealed class GooglePlacesException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    public class ClientError(
        public val httpStatus: Int,
        message: String,
    ) : GooglePlacesException(message)

    public class ServerError(
        public val httpStatus: Int,
        message: String,
    ) : GooglePlacesException(message)

    /** Google's Geocoding API returns HTTP 200 even for most errors, encoded in the body's `status` field. */
    public class ApiStatus(
        public val status: String,
        message: String?,
    ) : GooglePlacesException(message ?: status)

    public class NetworkTimeout(
        cause: Throwable,
    ) : GooglePlacesException("Google Places request timed out", cause)

    public class MalformedResponse(
        message: String,
        cause: Throwable? = null,
    ) : GooglePlacesException(message, cause)

    public class Unknown(
        cause: Throwable,
    ) : GooglePlacesException(cause.message ?: "Unknown network error", cause)
}
