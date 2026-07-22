package com.alongside.core.domain.onboarding

/**
 * Local persistence for "has this device's user ever finished onboarding" - so the one-time
 * auth-gate walk (Login → Onboarding → Pairing) doesn't repeat on every cold start. Unlike
 * permission status (re-queried live from the OS every time) or pairing (a real trip record),
 * onboarding's own completion had no durable signal at all before this - see
 * `docs/known-issues.md`.
 */
public interface OnboardingCompletionCache {
    public suspend fun isCompleted(): Boolean

    public suspend fun markCompleted()

    public suspend fun clear()
}
