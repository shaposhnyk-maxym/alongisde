package com.alongside.core.database.repository

import com.alongside.core.database.AlongsideDatabase
import com.alongside.core.database.entity.toDomain
import com.alongside.core.database.entity.toEntity
import com.alongside.core.domain.push.PushTokenRepository
import com.alongside.core.model.push.PushToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class PushTokenRepositoryImpl(
    private val database: AlongsideDatabase,
) : PushTokenRepository {
    override suspend fun upsert(pushToken: PushToken) {
        database.pushTokenDao().upsert(pushToken.toEntity())
    }

    override suspend fun getById(id: String): PushToken? = database.pushTokenDao().getById(id)?.toDomain()

    override fun observeById(id: String): Flow<PushToken?> {
        val entityFlow = database.pushTokenDao().observeById(id)
        return entityFlow.map { it?.toDomain() }
    }

    override suspend fun delete(id: String) {
        database.pushTokenDao().delete(id)
    }
}
