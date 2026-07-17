package com.alongside.core.network.queue

public sealed class SyncResult {
    public data object Success : SyncResult()

    public data class Failure(
        public val retryable: Boolean,
        public val cause: Throwable,
    ) : SyncResult()
}
