package com.alongside.core.domain.auth

import com.alongside.core.model.auth.AuthSession

/** Local persistence for the current [AuthSession], so sign-in survives an app restart. */
public interface AuthSessionCache {
    public suspend fun get(): AuthSession?

    public suspend fun save(session: AuthSession)

    public suspend fun clear()
}
