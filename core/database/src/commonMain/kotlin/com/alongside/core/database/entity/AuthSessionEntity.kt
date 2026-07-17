package com.alongside.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.alongside.core.model.auth.AuthSession
import com.alongside.core.model.auth.AuthUser
import kotlin.time.Instant

/** Single-row table (fixed [id]) - at most one signed-in session is cached locally at a time. */
@Entity(tableName = "auth_session")
internal data class AuthSessionEntity(
    @PrimaryKey val id: String = SINGLETON_ID,
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
    val idToken: String,
    val refreshToken: String?,
    val expiresInSeconds: Long,
    val issuedAt: Instant,
) {
    internal companion object {
        internal const val SINGLETON_ID = "current"
    }
}

internal fun AuthSessionEntity.toDomain(): AuthSession =
    AuthSession(
        user = AuthUser(uid = uid, email = email, displayName = displayName, photoUrl = photoUrl),
        idToken = idToken,
        refreshToken = refreshToken,
        expiresInSeconds = expiresInSeconds,
        issuedAt = issuedAt,
    )

internal fun AuthSession.toEntity(): AuthSessionEntity =
    AuthSessionEntity(
        uid = user.uid,
        email = user.email,
        displayName = user.displayName,
        photoUrl = user.photoUrl,
        idToken = idToken,
        refreshToken = refreshToken,
        expiresInSeconds = expiresInSeconds,
        issuedAt = issuedAt,
    )
