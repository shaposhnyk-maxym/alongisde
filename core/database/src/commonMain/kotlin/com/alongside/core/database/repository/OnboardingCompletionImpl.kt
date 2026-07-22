package com.alongside.core.database.repository

import com.alongside.core.database.AlongsideDatabase
import com.alongside.core.database.entity.OnboardingCompletionEntity
import com.alongside.core.domain.onboarding.OnboardingCompletionCache

internal class OnboardingCompletionImpl(
    private val database: AlongsideDatabase,
) : OnboardingCompletionCache {
    override suspend fun isCompleted(): Boolean = database.onboardingCompletionDao().get() != null

    override suspend fun markCompleted() {
        database.onboardingCompletionDao().upsert(OnboardingCompletionEntity())
    }

    override suspend fun clear() {
        database.onboardingCompletionDao().clear()
    }
}
