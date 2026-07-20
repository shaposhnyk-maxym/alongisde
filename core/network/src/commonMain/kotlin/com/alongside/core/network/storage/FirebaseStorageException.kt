package com.alongside.core.network.storage

public sealed class FirebaseStorageException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    public class ClientError(
        public val httpStatus: Int,
        message: String,
    ) : FirebaseStorageException(message)

    public class ServerError(
        public val httpStatus: Int,
        message: String,
    ) : FirebaseStorageException(message)

    public class NetworkTimeout(
        cause: Throwable,
    ) : FirebaseStorageException("Firebase Storage request timed out", cause)

    public class MalformedResponse(
        message: String,
        cause: Throwable? = null,
    ) : FirebaseStorageException(message, cause)

    public class Unknown(
        cause: Throwable,
    ) : FirebaseStorageException(cause.message ?: "Unknown network error", cause)
}
