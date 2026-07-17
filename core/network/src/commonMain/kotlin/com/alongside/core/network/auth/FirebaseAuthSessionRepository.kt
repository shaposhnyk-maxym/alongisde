package com.alongside.core.network.auth

import com.alongside.core.domain.auth.AuthException
import com.alongside.core.domain.auth.AuthSessionRepository
import com.alongside.core.model.auth.AuthSession
import com.alongside.core.model.auth.AuthUser
import com.alongside.core.network.auth.model.FirebaseSignInResponse
import kotlinx.coroutines.CancellationException
import kotlin.time.Clock

public class FirebaseAuthSessionRepository(
    private val api: FirebaseAuthApi,
) : AuthSessionRepository {
    override suspend fun signInWithGoogle(googleIdToken: String): AuthSession =
        try {
            api.signInWithGoogleIdToken(googleIdToken).toAuthSession()
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseAuthException.InvalidIdToken) {
            throw AuthException.InvalidToken(e)
        } catch (e: FirebaseAuthException.NetworkTimeout) {
            throw AuthException.Network(e)
        } catch (e: FirebaseAuthException) {
            throw AuthException.Unknown(e)
        }

    private fun FirebaseSignInResponse.toAuthSession(): AuthSession =
        AuthSession(
            user =
                AuthUser(
                    uid = localId,
                    email = email,
                    displayName = displayName,
                    photoUrl = photoUrl,
                ),
            idToken = idToken,
            refreshToken = refreshToken,
            expiresInSeconds = expiresIn?.toLong() ?: DEFAULT_EXPIRES_IN_SECONDS,
            issuedAt = Clock.System.now(),
        )

    private companion object {
        // Firebase ID tokens are fixed at ~1h; used when signInWithIdp omits expiresIn (observed
        // on-device alongside a missing refreshToken) rather than guessing at a value.
        const val DEFAULT_EXPIRES_IN_SECONDS = 3600L
    }
}
