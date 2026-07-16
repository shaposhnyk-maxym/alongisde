package com.alongside.core.network.firestore

import com.alongside.core.network.firestore.model.FirestoreValue
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FirestoreApiReadTest {
    private val documentJson =
        """{"name":"trips/trip-1","fields":{"ownerId":{"stringValue":"owner-1"}}}"""

    @Test
    fun `getDocument success parses the document`() =
        runBlocking {
            val api = testFirestoreApi { respondJson(documentJson) }

            val document = api.getDocument("trips", "trip-1")

            assertEquals(FirestoreValue.StringValue("owner-1"), document.fields["ownerId"])
        }

    @Test
    fun `getDocument 401 unauthorized throws ClientError`() =
        runBlocking {
            val api =
                testFirestoreApi {
                    respondJson(
                        """{"error":{"code":401,"message":"invalid credentials","status":"UNAUTHENTICATED"}}""",
                        HttpStatusCode.Unauthorized,
                    )
                }

            val error = assertFailsWith<FirestoreException.ClientError> { api.getDocument("trips", "trip-1") }

            assertEquals(401, error.code)
            assertEquals("UNAUTHENTICATED", error.status)
        }

    @Test
    fun `getDocument 404 not found throws a distinct ClientError`() =
        runBlocking {
            val api =
                testFirestoreApi {
                    respondJson(
                        """{"error":{"code":404,"message":"no such document","status":"NOT_FOUND"}}""",
                        HttpStatusCode.NotFound,
                    )
                }

            val error = assertFailsWith<FirestoreException.ClientError> { api.getDocument("trips", "missing") }

            assertEquals(404, error.code)
            assertEquals("NOT_FOUND", error.status)
        }

    @Test
    fun `getDocument 500 throws ServerError`() =
        runBlocking {
            val api =
                testFirestoreApi {
                    respondJson(
                        """{"error":{"code":500,"message":"internal error","status":"INTERNAL"}}""",
                        HttpStatusCode.InternalServerError,
                    )
                }

            val error = assertFailsWith<FirestoreException.ServerError> { api.getDocument("trips", "trip-1") }

            assertEquals(500, error.code)
        }

    @Test
    fun `getDocument with malformed JSON body throws MalformedResponse`() =
        runBlocking<Unit> {
            val api = testFirestoreApi { respondJson("""{"fields": not valid json""") }

            assertFailsWith<FirestoreException.MalformedResponse> { api.getDocument("trips", "trip-1") }
        }

    @Test
    fun `getDocument that never responds in time throws NetworkTimeout`() =
        runBlocking<Unit> {
            val api =
                testFirestoreApi(requestTimeoutMillis = 200L) {
                    delay(2_000)
                    respondJson(documentJson)
                }

            assertFailsWith<FirestoreException.NetworkTimeout> { api.getDocument("trips", "trip-1") }
        }

    @Test
    fun `listDocuments sends pageSize and pageToken and parses nextPageToken`() =
        runBlocking {
            var capturedUrl: String? = null
            val api =
                testFirestoreApi { request ->
                    capturedUrl = request.url.toString()
                    respondJson("""{"documents":[$documentJson],"nextPageToken":"page-2"}""")
                }

            val response = api.listDocuments("trips", pageSize = 20, pageToken = "page-1")

            assertEquals(1, response.documents.size)
            assertEquals("page-2", response.nextPageToken)
            val url = assertNotNull(capturedUrl)
            assertTrue(url.contains("pageSize=20"))
            assertTrue(url.contains("pageToken=page-1"))
        }

    @Test
    fun `listDocuments without a next page returns null nextPageToken`() =
        runBlocking {
            val api = testFirestoreApi { respondJson("""{"documents":[$documentJson]}""") }

            val response = api.listDocuments("trips")

            assertNull(response.nextPageToken)
        }
}
