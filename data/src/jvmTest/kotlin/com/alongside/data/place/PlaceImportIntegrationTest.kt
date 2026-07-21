package com.alongside.data.place

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.alongside.core.database.AlongsideDatabase
import com.alongside.core.database.placeCandidateRepository
import com.alongside.core.domain.place.PlaceCandidateRepository
import com.alongside.core.domain.place.importing.PlaceImportPipeline
import com.alongside.core.domain.place.importing.PlaceImportResult
import com.alongside.core.network.client.configureFirestoreHttpClient
import com.alongside.core.network.firestore.FirestoreTokenProvider
import com.alongside.core.network.places.GooglePlacesConfig
import com.alongside.core.network.places.GooglePlacesDetailsApi
import com.alongside.core.network.places.GooglePlacesDetailsClient
import com.alongside.core.network.places.GooglePlacesGeocodingApi
import com.alongside.core.network.places.GooglePlacesGeocodingClient
import com.alongside.core.network.places.GooglePlacesPhotoApi
import com.alongside.core.network.places.GooglePlacesPhotoClient
import com.alongside.core.network.places.KtorShareLinkRedirectResolver
import com.alongside.core.network.storage.FirebasePlacePhotoUploadClient
import com.alongside.core.network.storage.FirebaseStorageApi
import com.alongside.core.network.storage.FirebaseStorageConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondRedirect
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

private fun MockRequestHandleScope.respondJson(
    json: String,
    status: HttpStatusCode = HttpStatusCode.OK,
): HttpResponseData =
    respond(
        content = json,
        status = status,
        headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
    )

private fun mockClient(
    followRedirects: Boolean = true,
    handler: suspend MockRequestHandleScope.(request: HttpRequestData) -> HttpResponseData,
): HttpClient {
    val engine = MockEngine { request -> handler(request) }
    return HttpClient(engine) {
        this.followRedirects = followRedirects
        configureFirestoreHttpClient()
    }
}

private val FIXED_NOW = Instant.fromEpochMilliseconds(1_752_800_000_000)

private object FixedClock : Clock {
    override fun now(): Instant = FIXED_NOW
}

/**
 * The real short link from the M13 test set (`https://maps.app.goo.gl/Mnzn3A1DSQYoXsDw5`),
 * resolved by hand (see docs/roadmap.md M13.1) - reused here as the redirect target so this test
 * exercises the actual URL shape a real share produces, not an invented one.
 */
private const val RESOLVED_URL =
    "https://www.google.com/maps/place/Global+Solar,+Tyvrivske+shose,+1,+Vinnytsia,+Vinnytsia+Oblast,+21000/" +
        "data=!4m2!3m1!1s0x472d5d007ffab745:0xc807e74d1edeaf6e!18m1!1e1"

/**
 * End to end, entirely against real Ktor `MockEngine` clients (not domain-level fakes) plus a
 * real in-memory Room database: a share-link URL resolves through
 * [PlaceImportPipeline] and the resulting [com.alongside.core.model.place.PlaceCandidate] round
 * trips through [PlaceCandidateRepositoryImpl] with its `photos` carrying Storage-hosted
 * `remoteUrl`s, not raw Google photo references.
 */
class PlaceImportIntegrationTest {
    private lateinit var database: AlongsideDatabase
    private lateinit var repository: PlaceCandidateRepository

    @BeforeTest
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder<AlongsideDatabase>()
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        repository = database.placeCandidateRepository()
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    private val placesConfig = GooglePlacesConfig(apiKey = "test-api-key")
    private val storageConfig = FirebaseStorageConfig(bucket = "test-bucket.firebasestorage.app")

    private fun buildRedirectResolver() =
        KtorShareLinkRedirectResolver(mockClient(followRedirects = false) { respondRedirect(RESOLVED_URL) })

    private fun buildDetailsClient(): GooglePlacesDetailsClient {
        val detailsJson =
            """
            {
              "places": [{
                "displayName": {"text": "Global Solar"},
                "rating": 4.2,
                "primaryTypeDisplayName": {"text": "Solar energy company"},
                "photos": [
                  {"name": "places/abc/photos/photo-1"},
                  {"name": "places/abc/photos/photo-2"}
                ],
                "location": {"latitude": 49.24, "longitude": 28.47}
              }]
            }
            """.trimIndent()
        val detailsApi = GooglePlacesDetailsApi(mockClient { respondJson(detailsJson) }, placesConfig)
        return GooglePlacesDetailsClient(detailsApi)
    }

    private fun buildPhotoClient() =
        GooglePlacesPhotoClient(GooglePlacesPhotoApi(mockClient { respond(byteArrayOf(1, 2, 3)) }, placesConfig))

    private fun buildGeocodingClient(): GooglePlacesGeocodingClient {
        val geocodeJson =
            """
            {
              "results": [{
                "formatted_address": "Tyvrivske shose, 1, Vinnytsia, Ukraine",
                "address_components": [
                  {"long_name": "Vinnytsia", "short_name": "Vinnytsia", "types": ["locality", "political"]}
                ]
              }],
              "status": "OK"
            }
            """.trimIndent()
        val geocodingApi = GooglePlacesGeocodingApi(mockClient { respondJson(geocodeJson) }, placesConfig)
        return GooglePlacesGeocodingClient(geocodingApi)
    }

    private fun buildPhotoUploadClient(): FirebasePlacePhotoUploadClient {
        val storageApi =
            FirebaseStorageApi(
                mockClient { request ->
                    val objectId =
                        request.url.parameters["name"]
                            .orEmpty()
                            .substringAfterLast('/')
                    respondJson(
                        """{"name":"photos/$objectId","bucket":"test-bucket","downloadTokens":"token-$objectId"}""",
                    )
                },
                storageConfig,
                FirestoreTokenProvider { "test-id-token" },
            )
        return FirebasePlacePhotoUploadClient(storageApi, storageConfig)
    }

    private fun buildPipeline(): PlaceImportPipeline =
        PlaceImportPipeline(
            redirectResolver = buildRedirectResolver(),
            detailsLookupClient = buildDetailsClient(),
            photoClient = buildPhotoClient(),
            photoUploadClient = buildPhotoUploadClient(),
            placeGeocodingClient = buildGeocodingClient(),
            generatePlaceId = { "place-1" },
            clock = FixedClock,
        )

    // runBlocking, not runTest: see the M3 note in docs/roadmap.md.
    @Test
    fun `share link resolves through the full pipeline and round trips via Room with Storage photo urls`() =
        runBlocking {
            val result =
                buildPipeline().import(
                    shareUrl = "https://maps.app.goo.gl/Mnzn3A1DSQYoXsDw5",
                    tripId = "trip-1",
                    addedByUserId = "owner-1",
                )

            val imported = assertIs<PlaceImportResult.Imported>(result)
            assertEquals("Global Solar", imported.place.name)
            assertEquals(4.2, imported.place.rating)
            assertEquals("Solar energy company", imported.place.category)
            assertEquals("Vinnytsia", imported.place.city)
            assertEquals(2, imported.place.photos.size)
            imported.place.photos.forEach { photo ->
                val url = requireNotNull(photo.remoteUrl)
                assertTrue(url.startsWith("https://firebasestorage.googleapis.com/"))
                assertFalse(url.contains("places.googleapis.com"))
            }

            repository.upsert(imported.place)

            val reloaded = repository.getById("place-1")
            assertEquals(imported.place, reloaded)
        }
}
