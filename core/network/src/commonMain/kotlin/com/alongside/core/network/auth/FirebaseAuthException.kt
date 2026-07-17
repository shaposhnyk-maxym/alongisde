package com.alongside.core.network.auth

public sealed class FirebaseAuthException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    public class InvalidIdToken(
        public val code: Int,
        public val status: String,
        message: String,
    ) : FirebaseAuthException(message)

    public class ClientError(
        public val code: Int,
        public val status: String,
        message: String,
    ) : FirebaseAuthException(message)

    public class ServerError(
        public val code: Int,
        public val status: String,
        message: String,
    ) : FirebaseAuthException(message)

    public class NetworkTimeout(
        cause: Throwable,
    ) : FirebaseAuthException("Firebase Auth request timed out", cause)

    public class MalformedResponse(
        message: String,
        cause: Throwable? = null,
    ) : FirebaseAuthException(message, cause)

    public class Unknown(
        cause: Throwable,
    ) : FirebaseAuthException(cause.message ?: "Unknown network error", cause)
}
