package com.alongside.core.network.auth

import com.alongside.core.domain.auth.AuthSessionCache
import com.alongside.core.domain.auth.isExpired
import com.alongside.core.model.auth.AuthSession
import com.alongside.core.network.auth.model.FirebaseRefreshTokenResponse
import com.alongside.core.network.firestore.FirestoreTokenProvider
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock

/** Narrow refresh seam so the provider is testable without the concrete [FirebaseAuthApi]. */
public fun interface IdTokenRefresher {
    public suspend fun refresh(refreshToken: String): FirebaseRefreshTokenResponse
}

public fun FirebaseAuthApi.asIdTokenRefresher(): IdTokenRefresher = IdTokenRefresher(::refreshIdToken)

/**
 * The real [FirestoreTokenProvider]: serves the cached session's idToken while it is valid
 * (see [isExpired]'s safety margin) and exchanges the stored refresh token for a new one when
 * it is not, persisting the rotated session. Returns null - an unauthenticated request - when
 * there is no session, no refresh token, or the refresh fails; the cached session is kept
 * either way, since recovery is AuthContainer's silent re-sign-in on next launch.
 */
public class SessionFirestoreTokenProvider(
    private val cache: AuthSessionCache,
    private val refresher: IdTokenRefresher,
    private val clock: Clock = Clock.System,
) : FirestoreTokenProvider {
    private val mutex = Mutex()

    override suspend fun currentToken(): String? =
        mutex.withLock {
            val session = cache.get()
            println(
                "SessionFirestoreTokenProvider: session=${session != null} " +
                    "expired=${session?.isExpired(clock.now())}",
            )
            if (session == null) return@withLock null
            if (!session.isExpired(clock.now())) return@withLock session.idToken
            val refreshToken = session.refreshToken ?: return@withLock null
            try {
                val refreshed = session.refreshedWith(refresher.refresh(refreshToken))
                cache.save(refreshed)
                refreshed.idToken
            } catch (e: FirebaseAuthException) {
                println("SessionFirestoreTokenProvider: refresh failed - ${e::class.simpleName}: ${e.message}")
                null
            }
        }

    private fun AuthSession.refreshedWith(response: FirebaseRefreshTokenResponse): AuthSession =
        copy(
            idToken = response.idToken,
            refreshToken = response.refreshToken ?: refreshToken,
            expiresInSeconds = response.expiresIn?.toLongOrNull() ?: DEFAULT_EXPIRES_IN_SECONDS,
            issuedAt = clock.now(),
        )

    private companion object {
        const val DEFAULT_EXPIRES_IN_SECONDS = 3600L
    }
}
