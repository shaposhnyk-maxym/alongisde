package com.alongside.core.network.places

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

// runBlocking, not runTest: runTest's virtual-time scheduler falsely times out
// Ktor HttpTimeout against MockEngine (see the M3 note in docs/roadmap.md).
class GooglePlacesGeocodingApiTest {
    private val okResponseJson =
        """
        {
          "results": [
            {
              "formatted_address": "Rynok Square, Lviv, Lviv Oblast, Ukraine",
              "address_components": [
                {"long_name": "Rynok Square", "short_name": "Rynok Sq", "types": ["point_of_interest"]},
                {"long_name": "Lviv", "short_name": "Lviv", "types": ["locality"]}
              ]
            }
          ],
          "status": "OK"
        }
        """.trimIndent()

    @Test
    fun `reverseGeocode issues a GET to the geocoding endpoint with latlng and key`() =
        runBlocking {
            var capturedUrl: String? = null
            val api =
                testGooglePlacesGeocodingApi { request ->
                    capturedUrl = request.url.toString()
                    respondJson(okResponseJson)
                }

            api.reverseGeocode(49.8397, 24.0297)

            assertEquals(
                "https://maps.googleapis.com/maps/api/geocode/json?latlng=49.8397,24.0297&key=test-api-key",
                capturedUrl,
            )
        }

    @Test
    fun `reverseGeocode parses a successful response`() =
        runBlocking {
            val api = testGooglePlacesGeocodingApi { respondJson(okResponseJson) }

            val response = api.reverseGeocode(49.8397, 24.0297)

            assertEquals("OK", response.status)
            assertEquals(1, response.results.size)
            assertEquals("Rynok Square, Lviv, Lviv Oblast, Ukraine", response.results.single().formattedAddress)
        }

    @Test
    fun `reverseGeocode parses a zero-results response`() =
        runBlocking {
            val api = testGooglePlacesGeocodingApi { respondJson("""{"results": [], "status": "ZERO_RESULTS"}""") }

            val response = api.reverseGeocode(0.0, 0.0)

            assertEquals("ZERO_RESULTS", response.status)
            assertEquals(emptyList(), response.results)
        }

    @Test
    fun `HTTP 4xx throws ClientError`() =
        runBlocking<Unit> {
            val api =
                testGooglePlacesGeocodingApi {
                    respondJson("""{"status": "INVALID_REQUEST"}""", HttpStatusCode.BadRequest)
                }

            assertFailsWith<GooglePlacesException.ClientError> {
                api.reverseGeocode(0.0, 0.0)
            }
        }

    @Test
    fun `HTTP 5xx throws ServerError`() =
        runBlocking<Unit> {
            val api =
                testGooglePlacesGeocodingApi {
                    respondJson("""{"status": "UNKNOWN_ERROR"}""", HttpStatusCode.InternalServerError)
                }

            assertFailsWith<GooglePlacesException.ServerError> {
                api.reverseGeocode(0.0, 0.0)
            }
        }

    @Test
    fun `malformed body throws MalformedResponse`() =
        runBlocking<Unit> {
            val api = testGooglePlacesGeocodingApi { respondJson("not json at all") }

            assertFailsWith<GooglePlacesException.MalformedResponse> {
                api.reverseGeocode(0.0, 0.0)
            }
        }
}
