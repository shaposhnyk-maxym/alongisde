package com.alongside.core.domain.auth

/**
 * Failure modes an [AuthSessionRepository] can surface, deliberately narrower than the
 * network-layer exceptions they are mapped from - callers (e.g. `AuthContainer`) only need to
 * distinguish "the token was rejected" from "we couldn't reach the server" from "something else".
 */
public sealed class AuthException(
    message: String?,
    cause: Throwable?,
) : Exception(message, cause) {
    public class InvalidToken(
        cause: Throwable? = null,
    ) : AuthException("Invalid Google ID token", cause)

    public class Network(
        cause: Throwable? = null,
    ) : AuthException("Network failure during sign-in", cause)

    public class Unknown(
        cause: Throwable? = null,
    ) : AuthException("Unknown sign-in failure", cause)
}
