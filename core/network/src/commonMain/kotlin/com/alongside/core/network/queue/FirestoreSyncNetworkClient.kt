package com.alongside.core.network.queue

import com.alongside.core.network.firestore.FirestoreApi
import com.alongside.core.network.firestore.FirestoreException

/** Wires [SyncQueueProcessor] to a real [FirestoreApi], classifying failures as retryable
 * (server-side/timeout/unknown) or not (rejected by the server - client error). */
public class FirestoreSyncNetworkClient(
    private val api: FirestoreApi,
) : SyncNetworkClient {
    override suspend fun push(operation: SyncOperation): SyncResult =
        try {
            when (operation.type) {
                SyncOperationType.UPSERT ->
                    api.upsertDocument(operation.collectionPath, operation.documentId, operation.fields)
                SyncOperationType.DELETE ->
                    api.deleteDocument(operation.collectionPath, operation.documentId)
            }
            SyncResult.Success
        } catch (e: FirestoreException.ClientError) {
            println(
                "FirestoreSyncNetworkClient: push ${operation.collectionPath}/${operation.documentId} " +
                    "failed (non-retryable): code=${e.code} status=${e.status} message=${e.message}",
            )
            SyncResult.Failure(retryable = false, cause = e)
        } catch (e: FirestoreException) {
            println(
                "FirestoreSyncNetworkClient: push ${operation.collectionPath}/${operation.documentId} " +
                    "failed (retryable): ${e::class.simpleName}: ${e.message}",
            )
            SyncResult.Failure(retryable = true, cause = e)
        }
}
