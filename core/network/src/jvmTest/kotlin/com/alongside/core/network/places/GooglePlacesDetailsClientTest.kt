package com.alongside.core.network.places

import com.alongside.core.domain.place.importing.PlaceDetailsResult
import com.alongside.core.domain.place.importing.PlaceLookupQuery
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

// runBlocking, not runTest: see the M3 note in docs/roadmap.md.
class GooglePlacesDetailsClientTest {
    private val query =
        PlaceLookupQuery(name = "Lviv Coffee Manufacture", address = "Lviv", latitude = null, longitude = null)

    @Test
    fun `found response maps to PlaceDetailsResult Found`() =
        runBlocking {
            val api =
                testGooglePlacesDetailsApi {
                    respondJson(
                        """
                        {
                          "places": [{
                            "displayName": {"text": "Lviv Coffee Manufacture"},
                            "rating": 4.6,
                            "primaryTypeDisplayName": {"text": "Coffee shop"},
                            "photos": [{"name": "places/abc/photos/xyz"}],
                            "location": {"latitude": 49.8397, "longitude": 24.0297}
                          }]
                        }
                        """.trimIndent(),
                    )
                }
            val client = GooglePlacesDetailsClient(api)

            val result = client.lookup(query)

            val found = assertIs<PlaceDetailsResult.Found>(result)
            assertEquals("Lviv Coffee Manufacture", found.name)
            assertEquals(4.6, found.rating)
            assertEquals("Coffee shop", found.category)
            assertEquals(listOf("places/abc/photos/xyz"), found.photoRefs)
            assertEquals(49.8397, found.latitude)
            assertEquals(24.0297, found.longitude)
        }

    @Test
    fun `empty places response maps to NotFound`() =
        runBlocking {
            val api = testGooglePlacesDetailsApi { respondJson("""{"places": []}""") }
            val client = GooglePlacesDetailsClient(api)

            assertEquals(PlaceDetailsResult.NotFound, client.lookup(query))
        }

    @Test
    fun `a place with no location and a query with no coordinates maps to NotFound`() =
        runBlocking {
            val api =
                testGooglePlacesDetailsApi {
                    respondJson("""{"places": [{"displayName": {"text": "Somewhere"}}]}""")
                }
            val client = GooglePlacesDetailsClient(api)

            assertEquals(PlaceDetailsResult.NotFound, client.lookup(query))
        }

    @Test
    fun `a place with no location falls back to the query's own coordinates`() =
        runBlocking {
            val api =
                testGooglePlacesDetailsApi {
                    respondJson("""{"places": [{"displayName": {"text": "Somewhere"}}]}""")
                }
            val client = GooglePlacesDetailsClient(api)
            val queryWithCoordinates = query.copy(latitude = 49.0, longitude = 24.0)

            val result = client.lookup(queryWithCoordinates)

            val found = assertIs<PlaceDetailsResult.Found>(result)
            assertEquals(49.0, found.latitude)
            assertEquals(24.0, found.longitude)
        }

    @Test
    fun `an exception thrown by the underlying api maps to Failure, not a rethrow`() {
        runBlocking {
            val api =
                testGooglePlacesDetailsApi {
                    respondJson(
                        """{"error": {"code": 500, "message": "internal error", "status": "INTERNAL"}}""",
                        HttpStatusCode.InternalServerError,
                    )
                }
            val client = GooglePlacesDetailsClient(api)

            val result = client.lookup(query)

            val failure = assertIs<PlaceDetailsResult.Failure>(result)
            assertIs<GooglePlacesException.ServerError>(failure.cause)
        }
    }
}
