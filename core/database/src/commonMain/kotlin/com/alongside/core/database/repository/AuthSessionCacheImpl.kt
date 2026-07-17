package com.alongside.core.database.repository

import com.alongside.core.database.AlongsideDatabase
import com.alongside.core.database.entity.toDomain
import com.alongside.core.database.entity.toEntity
import com.alongside.core.domain.auth.AuthSessionCache
import com.alongside.core.model.auth.AuthSession

internal class AuthSessionCacheImpl(
    private val database: AlongsideDatabase,
) : AuthSessionCache {
    override suspend fun get(): AuthSession? = database.authSessionDao().get()?.toDomain()

    override suspend fun save(session: AuthSession) {
        database.authSessionDao().upsert(session.toEntity())
    }

    override suspend fun clear() {
        database.authSessionDao().clear()
    }
}
