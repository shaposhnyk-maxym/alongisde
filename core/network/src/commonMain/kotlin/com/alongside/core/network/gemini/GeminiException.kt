package com.alongside.core.network.gemini

public sealed class GeminiException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    public class ClientError(
        public val httpStatus: Int,
        message: String,
    ) : GeminiException(message)

    public class ServerError(
        public val httpStatus: Int,
        message: String,
    ) : GeminiException(message)

    public class NetworkTimeout(
        cause: Throwable,
    ) : GeminiException("Gemini request timed out", cause)

    public class MalformedResponse(
        message: String,
        cause: Throwable? = null,
    ) : GeminiException(message, cause)

    public class Unknown(
        cause: Throwable,
    ) : GeminiException(cause.message ?: "Unknown network error", cause)
}
