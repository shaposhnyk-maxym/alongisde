package com.alongside.core.domain.auth

import com.alongside.core.model.auth.AuthSession
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

// A session is treated as expired slightly before its idToken actually is, so a request already
// in flight (or about to start) doesn't race the real expiry.
private val EXPIRY_SAFETY_MARGIN = 5.minutes

public fun AuthSession.isExpired(now: Instant = Clock.System.now()): Boolean =
    now >= issuedAt + expiresInSeconds.seconds - EXPIRY_SAFETY_MARGIN
