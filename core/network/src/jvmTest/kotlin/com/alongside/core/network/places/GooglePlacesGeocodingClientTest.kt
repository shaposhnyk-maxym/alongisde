package com.alongside.core.network.places

import com.alongside.core.domain.diary.processing.GeocodingResult
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

// runBlocking, not runTest: see the M3 note in docs/roadmap.md.
class GooglePlacesGeocodingClientTest {
    @Test
    fun `OK status with a point of interest maps to Found with the preferred name`() =
        runBlocking {
            val api =
                testGooglePlacesGeocodingApi {
                    respondJson(
                        """
                        {
                          "results": [{
                            "formatted_address": "Rynok Square, Lviv, Ukraine",
                            "address_components": [
                              {"long_name": "Rynok Square", "short_name": "Rynok Sq", "types": ["point_of_interest"]}
                            ]
                          }],
                          "status": "OK"
                        }
                        """.trimIndent(),
                    )
                }
            val client = GooglePlacesGeocodingClient(api)

            val result = client.reverseGeocode(49.8397, 24.0297)

            assertEquals(GeocodingResult.Found("Rynok Square"), result)
        }

    @Test
    fun `ZERO_RESULTS maps to NotFound`() =
        runBlocking {
            val api = testGooglePlacesGeocodingApi { respondJson("""{"results": [], "status": "ZERO_RESULTS"}""") }
            val client = GooglePlacesGeocodingClient(api)

            val result = client.reverseGeocode(0.0, 0.0)

            assertEquals(GeocodingResult.NotFound, result)
        }

    @Test
    fun `REQUEST_DENIED maps to Failure`() {
        runBlocking {
            val api =
                testGooglePlacesGeocodingApi {
                    respondJson("""{"status": "REQUEST_DENIED", "error_message": "bad key"}""")
                }
            val client = GooglePlacesGeocodingClient(api)

            val result = client.reverseGeocode(0.0, 0.0)

            assertIs<GeocodingResult.Failure>(result)
        }
    }

    @Test
    fun `HTTP-level failure maps to Failure, not an exception`() {
        runBlocking {
            val api =
                testGooglePlacesGeocodingApi {
                    respondJson("""{"status": "UNKNOWN_ERROR"}""", HttpStatusCode.InternalServerError)
                }
            val client = GooglePlacesGeocodingClient(api)

            val result = client.reverseGeocode(0.0, 0.0)

            assertIs<GeocodingResult.Failure>(result)
        }
    }
}
