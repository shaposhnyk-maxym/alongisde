package com.alongside.core.network.storage

import com.alongside.core.network.storage.model.firstDownloadToken
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// runBlocking, not runTest: see the M3 note in docs/roadmap.md - runTest's virtual-time scheduler
// falsely times out Ktor's HttpTimeout plugin against MockEngine.
class FirebaseStorageApiTest {
    private fun HttpRequestData.bodyBytes(): ByteArray = (body as OutgoingContent.ByteArrayContent).bytes()

    @Test
    fun `upload posts the raw bytes with content type and an authenticated Firebase-scheme header`() =
        runBlocking {
            var captured: HttpRequestData? = null
            val api =
                testFirebaseStorageApi { request ->
                    captured = request
                    respondJson("""{"name":"photos/p1","bucket":"test-bucket.firebasestorage.app"}""")
                }

            api.upload("p1", byteArrayOf(1, 2, 3))

            val request = assertNotNull(captured)
            assertTrue(request.url.toString().contains("name=photos%2Fp1"))
            assertEquals(listOf<Byte>(1, 2, 3), request.bodyBytes().toList())
            assertEquals("image/jpeg", request.body.contentType?.toString())
            assertEquals("Firebase test-id-token", request.headers[HttpHeaders.Authorization])
        }

    @Test
    fun `upload parses a successful response and its downloadTokens`() =
        runBlocking {
            val api =
                testFirebaseStorageApi {
                    respondJson(
                        """{"name":"photos/p1","bucket":"test-bucket.firebasestorage.app","downloadTokens":"abc123"}""",
                    )
                }

            val response = api.upload("p1", byteArrayOf(1))

            assertEquals("photos/p1", response.name)
            assertEquals("abc123", response.firstDownloadToken())
        }

    @Test
    fun `upload takes the first token when downloadTokens has multiple comma-separated values`() =
        runBlocking {
            val api =
                testFirebaseStorageApi {
                    respondJson(
                        """{"name":"photos/p1","bucket":"test-bucket","downloadTokens":"abc,def"}""",
                    )
                }

            val response = api.upload("p1", byteArrayOf(1))

            assertEquals("abc", response.firstDownloadToken())
        }

    @Test
    fun `HTTP 4xx throws ClientError`() =
        runBlocking {
            val api =
                testFirebaseStorageApi {
                    respondJson(
                        """{"error":{"code":403,"message":"Permission denied","status":"PERMISSION_DENIED"}}""",
                        HttpStatusCode.Forbidden,
                    )
                }

            val error = assertFailsWith<FirebaseStorageException.ClientError> { api.upload("p1", byteArrayOf(1)) }
            assertEquals(403, error.httpStatus)
        }

    @Test
    fun `HTTP 5xx throws ServerError`() =
        runBlocking {
            val api =
                testFirebaseStorageApi {
                    respondJson(
                        """{"error":{"code":500,"message":"internal error","status":"INTERNAL"}}""",
                        HttpStatusCode.InternalServerError,
                    )
                }

            val error = assertFailsWith<FirebaseStorageException.ServerError> { api.upload("p1", byteArrayOf(1)) }
            assertEquals(500, error.httpStatus)
        }

    @Test
    fun `request that never responds in time throws NetworkTimeout`() =
        runBlocking<Unit> {
            val api =
                testFirebaseStorageApi(requestTimeoutMillis = 200L) {
                    delay(2_000)
                    respondJson("""{"name":"photos/p1","bucket":"test-bucket.firebasestorage.app"}""")
                }

            assertFailsWith<FirebaseStorageException.NetworkTimeout> { api.upload("p1", byteArrayOf(1)) }
        }

    @Test
    fun `malformed body throws MalformedResponse`() =
        runBlocking<Unit> {
            val api = testFirebaseStorageApi { respondJson("not json at all") }

            assertFailsWith<FirebaseStorageException.MalformedResponse> { api.upload("p1", byteArrayOf(1)) }
        }
}
