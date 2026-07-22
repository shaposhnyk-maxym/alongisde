package com.alongside.feature.onboarding

import com.alongside.core.domain.onboarding.OnboardingCompletionCache

internal class FakeOnboardingCompletionCache(
    initiallyCompleted: Boolean = false,
) : OnboardingCompletionCache {
    private var completed = initiallyCompleted
    var markCompletedCallCount: Int = 0
        private set

    override suspend fun isCompleted(): Boolean = completed

    override suspend fun markCompleted() {
        markCompletedCallCount++
        completed = true
    }

    override suspend fun clear() {
        completed = false
    }
}
