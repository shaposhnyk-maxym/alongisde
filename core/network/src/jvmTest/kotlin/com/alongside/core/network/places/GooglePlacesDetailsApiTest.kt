package com.alongside.core.network.places

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

// runBlocking, not runTest: see the M3 note in docs/roadmap.md.
class GooglePlacesDetailsApiTest {
    private val okResponseJson =
        """
        {
          "places": [
            {
              "displayName": {"text": "Lviv Coffee Manufacture"},
              "rating": 4.6,
              "primaryTypeDisplayName": {"text": "Coffee shop"},
              "photos": [{"name": "places/abc/photos/xyz"}],
              "location": {"latitude": 49.8397, "longitude": 24.0297}
            }
          ]
        }
        """.trimIndent()

    @Test
    fun `searchText posts to searchText endpoint with the api key and field mask headers`() =
        runBlocking {
            var capturedUrl: String? = null
            var capturedApiKeyHeader: String? = null
            var capturedFieldMaskHeader: String? = null
            val api =
                testGooglePlacesDetailsApi { request ->
                    capturedUrl = request.url.toString()
                    capturedApiKeyHeader = request.headers["X-Goog-Api-Key"]
                    capturedFieldMaskHeader = request.headers["X-Goog-FieldMask"]
                    respondJson(okResponseJson)
                }

            api.searchText("Lviv Coffee Manufacture, Lviv")

            assertEquals("https://places.googleapis.com/v1/places:searchText", capturedUrl)
            assertEquals("test-api-key", capturedApiKeyHeader)
            assertEquals(
                "places.displayName,places.rating,places.primaryTypeDisplayName,places.photos,places.location",
                capturedFieldMaskHeader,
            )
        }

    @Test
    fun `searchText parses a successful response`() =
        runBlocking {
            val api = testGooglePlacesDetailsApi { respondJson(okResponseJson) }

            val response = api.searchText("Lviv Coffee Manufacture, Lviv")

            val place = response.places.single()
            assertEquals("Lviv Coffee Manufacture", place.displayName?.text)
            assertEquals(4.6, place.rating)
            assertEquals("Coffee shop", place.primaryTypeDisplayName?.text)
            assertEquals(listOf("places/abc/photos/xyz"), place.photos.map { it.name })
            assertEquals(49.8397, place.location?.latitude)
            assertEquals(24.0297, place.location?.longitude)
        }

    @Test
    fun `searchText parses a zero-results response`() =
        runBlocking {
            val api = testGooglePlacesDetailsApi { respondJson("""{"places": []}""") }

            val response = api.searchText("nonexistent place")

            assertEquals(emptyList(), response.places)
        }

    @Test
    fun `HTTP 4xx throws ClientError`() =
        runBlocking<Unit> {
            val api =
                testGooglePlacesDetailsApi {
                    respondJson(
                        """{"error": {"code": 400, "message": "Invalid query", "status": "INVALID_ARGUMENT"}}""",
                        HttpStatusCode.BadRequest,
                    )
                }

            assertFailsWith<GooglePlacesException.ClientError> {
                api.searchText("query")
            }
        }

    @Test
    fun `HTTP 5xx throws ServerError`() =
        runBlocking<Unit> {
            val api =
                testGooglePlacesDetailsApi {
                    respondJson(
                        """{"error": {"code": 500, "message": "internal error", "status": "INTERNAL"}}""",
                        HttpStatusCode.InternalServerError,
                    )
                }

            assertFailsWith<GooglePlacesException.ServerError> {
                api.searchText("query")
            }
        }

    @Test
    fun `HTTP error with the standard Google error envelope surfaces its message`() =
        runBlocking<Unit> {
            val errorJson =
                """{"error": {"code": 403, "message": "API key not authorized", "status": "PERMISSION_DENIED"}}"""
            val api = testGooglePlacesDetailsApi { respondJson(errorJson, HttpStatusCode.Forbidden) }

            val exception =
                assertFailsWith<GooglePlacesException.ClientError> {
                    api.searchText("query")
                }
            assertEquals("API key not authorized", exception.message)
        }

    @Test
    fun `malformed body throws MalformedResponse`() =
        runBlocking<Unit> {
            val api = testGooglePlacesDetailsApi { respondJson("not json at all") }

            assertFailsWith<GooglePlacesException.MalformedResponse> {
                api.searchText("query")
            }
        }
}
