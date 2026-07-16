package com.alongside.core.network.queue

import com.alongside.core.network.client.configureFirestoreHttpClient
import com.alongside.core.network.firestore.FirestoreApi
import com.alongside.core.network.firestore.FirestoreConfig
import com.alongside.core.network.firestore.FirestoreTokenProvider
import com.alongside.core.network.firestore.respondJson
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FirestoreSyncNetworkClientTest {
    private fun clientWith(
        handler: suspend MockRequestHandleScope.(request: HttpRequestData) -> HttpResponseData,
    ): FirestoreSyncNetworkClient {
        val engine = MockEngine { request -> handler(request) }
        val httpClient = HttpClient(engine) { configureFirestoreHttpClient() }
        val api =
            FirestoreApi(httpClient, FirestoreConfig(projectId = "alongside-test"), FirestoreTokenProvider { null })
        return FirestoreSyncNetworkClient(api)
    }

    private fun op(type: SyncOperationType): SyncOperation =
        SyncOperation(id = "a", collectionPath = "trips", documentId = "trip-1", type = type)

    @Test
    fun `UPSERT calls upsertDocument and maps a 2xx response to Success`() =
        runBlocking {
            var capturedMethod: HttpMethod? = null
            val client =
                clientWith { request ->
                    capturedMethod = request.method
                    respondJson(
                        """{"name":"projects/alongside-test/databases/(default)/documents/trips/trip-1","fields":{}}""",
                    )
                }

            val result = client.push(op(SyncOperationType.UPSERT))

            assertEquals(SyncResult.Success, result)
            assertEquals(HttpMethod.Patch, capturedMethod)
        }

    @Test
    fun `DELETE calls deleteDocument and maps a 2xx response to Success`() =
        runBlocking {
            var capturedMethod: HttpMethod? = null
            val client =
                clientWith { request ->
                    capturedMethod = request.method
                    respondJson("{}")
                }

            val result = client.push(op(SyncOperationType.DELETE))

            assertEquals(SyncResult.Success, result)
            assertEquals(HttpMethod.Delete, capturedMethod)
        }

    @Test
    fun `a 5xx response maps to a retryable Failure`() =
        runBlocking {
            val client =
                clientWith {
                    respondJson(
                        """{"error":{"code":500,"message":"internal error","status":"INTERNAL"}}""",
                        HttpStatusCode.InternalServerError,
                    )
                }

            val result = client.push(op(SyncOperationType.UPSERT))

            val failure = assertIs<SyncResult.Failure>(result)
            assertTrue(failure.retryable)
        }

    @Test
    fun `a 404 response maps to a non-retryable Failure`() =
        runBlocking {
            val client =
                clientWith {
                    respondJson(
                        """{"error":{"code":404,"message":"no such document","status":"NOT_FOUND"}}""",
                        HttpStatusCode.NotFound,
                    )
                }

            val result = client.push(op(SyncOperationType.UPSERT))

            val failure = assertIs<SyncResult.Failure>(result)
            assertTrue(!failure.retryable)
        }
}
