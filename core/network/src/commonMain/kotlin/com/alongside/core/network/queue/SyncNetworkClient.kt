package com.alongside.core.network.queue

/** Abstraction over "send this operation over the network" so [SyncQueueProcessor] can be tested
 * against a fake, without touching Ktor directly. */
public interface SyncNetworkClient {
    public suspend fun push(operation: SyncOperation): SyncResult
}
