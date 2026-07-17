package com.alongside.core.network.firestore

import com.alongside.core.network.firestore.model.FirestoreValue
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpMethod
import io.ktor.http.content.OutgoingContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FirestoreApiWriteTest {
    private val updatedDocumentJson =
        """{"name":"trips/trip-1","fields":{"ownerId":{"stringValue":"owner-1"}}}"""

    private fun bodyOf(request: HttpRequestData): String {
        val content = request.body as OutgoingContent.ByteArrayContent
        return content.bytes().decodeToString()
    }

    @Test
    fun `upsertDocument sends a PATCH with the documented write-request shape`() =
        runBlocking {
            var capturedMethod: HttpMethod? = null
            var capturedUrl: String? = null
            var capturedBody: String? = null
            val api =
                testFirestoreApi { request ->
                    capturedMethod = request.method
                    capturedUrl = request.url.toString()
                    capturedBody = bodyOf(request)
                    respondJson(updatedDocumentJson)
                }

            api.upsertDocument("trips", "trip-1", mapOf("ownerId" to FirestoreValue.StringValue("owner-1")))

            assertEquals(HttpMethod.Patch, capturedMethod)
            assertEquals(true, capturedUrl?.endsWith("/documents/trips/trip-1"))
            assertEquals(
                Json.parseToJsonElement("""{"fields":{"ownerId":{"stringValue":"owner-1"}}}"""),
                Json.parseToJsonElement(capturedBody.orEmpty()),
            )
        }

    @Test
    fun `upsertDocument sends a bearer token header when the token provider returns one`() =
        runBlocking {
            var authHeader: String? = null
            val api =
                testFirestoreApi(tokenProvider = FirestoreTokenProvider { "token-123" }) { request ->
                    authHeader = request.headers["Authorization"]
                    respondJson(updatedDocumentJson)
                }

            api.upsertDocument("trips", "trip-1", emptyMap())

            assertEquals("Bearer token-123", authHeader)
        }

    @Test
    fun `upsertDocument omits the Authorization header when no token is available`() =
        runBlocking {
            var authHeader: String? = null
            val api =
                testFirestoreApi(tokenProvider = FirestoreTokenProvider { null }) { request ->
                    authHeader = request.headers["Authorization"]
                    respondJson(updatedDocumentJson)
                }

            api.upsertDocument("trips", "trip-1", emptyMap())

            assertNull(authHeader)
        }

    @Test
    fun `deleteDocument sends DELETE and tolerates an empty response body`() =
        runBlocking {
            var capturedMethod: HttpMethod? = null
            val api =
                testFirestoreApi { request ->
                    capturedMethod = request.method
                    respondJson("{}")
                }

            api.deleteDocument("trips", "trip-1")

            assertEquals(HttpMethod.Delete, capturedMethod)
        }
}
