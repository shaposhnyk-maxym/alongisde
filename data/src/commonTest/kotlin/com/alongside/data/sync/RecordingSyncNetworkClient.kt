package com.alongside.data.sync

import com.alongside.core.network.queue.SyncNetworkClient
import com.alongside.core.network.queue.SyncOperation
import com.alongside.core.network.queue.SyncResult

/**
 * Scripted [SyncNetworkClient] that records every push - the "fake network client that
 * captures calls" the M9 accept criteria ask to verify against. Fails every push for
 * [failingDocumentIds] (retryably) until the script changes; everything else succeeds.
 */
internal class RecordingSyncNetworkClient : SyncNetworkClient {
    val pushed = mutableListOf<SyncOperation>()
    var failingDocumentIds: Set<String> = emptySet()
    var failAll: Boolean = false

    override suspend fun push(operation: SyncOperation): SyncResult {
        pushed += operation
        return if (failAll || operation.documentId in failingDocumentIds) {
            SyncResult.Failure(retryable = true, cause = RuntimeException("scripted network failure"))
        } else {
            SyncResult.Success
        }
    }
}
