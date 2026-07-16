package com.alongside.core.domain.push

import com.alongside.core.model.push.PushToken
import kotlinx.coroutines.flow.Flow

public interface PushTokenRepository {
    public suspend fun upsert(pushToken: PushToken)

    public suspend fun getById(id: String): PushToken?

    public fun observeById(id: String): Flow<PushToken?>

    public suspend fun delete(id: String)
}
