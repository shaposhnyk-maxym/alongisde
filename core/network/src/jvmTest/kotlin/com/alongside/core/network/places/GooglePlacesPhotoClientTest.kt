package com.alongside.core.network.places

import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertNull

// runBlocking, not runTest: see the M3 note in docs/roadmap.md.
class GooglePlacesPhotoClientTest {
    @Test
    fun `success returns the photo bytes`() =
        runBlocking {
            val bytes = byteArrayOf(1, 2, 3)
            val api = testGooglePlacesPhotoApi { respond(bytes) }
            val client = GooglePlacesPhotoClient(api)

            assertContentEquals(bytes, client.fetchPhotoBytes("places/abc/photos/xyz"))
        }

    @Test
    fun `an exception thrown by the underlying api maps to null, not a rethrow`() =
        runBlocking {
            val api =
                testGooglePlacesPhotoApi {
                    respondJson(
                        """{"error": {"code": 404, "message": "Photo not found", "status": "NOT_FOUND"}}""",
                        HttpStatusCode.NotFound,
                    )
                }
            val client = GooglePlacesPhotoClient(api)

            assertNull(client.fetchPhotoBytes("places/abc/photos/xyz"))
        }
}
