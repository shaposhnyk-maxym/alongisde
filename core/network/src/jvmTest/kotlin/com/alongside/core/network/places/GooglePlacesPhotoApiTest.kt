package com.alongside.core.network.places

import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

// runBlocking, not runTest: see the M3 note in docs/roadmap.md.
class GooglePlacesPhotoApiTest {
    @Test
    fun `fetchPhoto issues a GET to the photo media endpoint with the api key`() =
        runBlocking {
            var capturedUrl: String? = null
            val api =
                testGooglePlacesPhotoApi { request ->
                    capturedUrl = request.url.toString()
                    respond(byteArrayOf(1, 2, 3))
                }

            api.fetchPhoto("places/abc/photos/xyz")

            assertEquals(
                "https://places.googleapis.com/v1/places/abc/photos/xyz/media?maxWidthPx=800&key=test-api-key",
                capturedUrl,
            )
        }

    @Test
    fun `fetchPhoto returns the raw response bytes on success`() =
        runBlocking {
            val bytes = byteArrayOf(1, 2, 3, 4)
            val api = testGooglePlacesPhotoApi { respond(bytes) }

            assertContentEquals(bytes, api.fetchPhoto("places/abc/photos/xyz"))
        }

    @Test
    fun `HTTP 4xx throws ClientError`() =
        runBlocking<Unit> {
            val api =
                testGooglePlacesPhotoApi {
                    respondJson(
                        """{"error": {"code": 404, "message": "Photo not found", "status": "NOT_FOUND"}}""",
                        HttpStatusCode.NotFound,
                    )
                }

            assertFailsWith<GooglePlacesException.ClientError> {
                api.fetchPhoto("places/abc/photos/xyz")
            }
        }

    @Test
    fun `HTTP 5xx throws ServerError`() =
        runBlocking<Unit> {
            val api =
                testGooglePlacesPhotoApi {
                    respondJson(
                        """{"error": {"code": 500, "message": "internal error", "status": "INTERNAL"}}""",
                        HttpStatusCode.InternalServerError,
                    )
                }

            assertFailsWith<GooglePlacesException.ServerError> {
                api.fetchPhoto("places/abc/photos/xyz")
            }
        }
}
