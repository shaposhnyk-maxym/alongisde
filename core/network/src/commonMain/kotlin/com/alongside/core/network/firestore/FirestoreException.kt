package com.alongside.core.network.firestore

public sealed class FirestoreException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    public class ClientError(
        public val code: Int,
        public val status: String,
        message: String,
    ) : FirestoreException(message)

    public class ServerError(
        public val code: Int,
        public val status: String,
        message: String,
    ) : FirestoreException(message)

    public class NetworkTimeout(
        cause: Throwable,
    ) : FirestoreException("Firestore request timed out", cause)

    public class MalformedResponse(
        message: String,
        cause: Throwable? = null,
    ) : FirestoreException(message, cause)

    public class Unknown(
        cause: Throwable,
    ) : FirestoreException(cause.message ?: "Unknown network error", cause)
}
