package com.alongside.data.place

import com.alongside.core.domain.place.PlaceCandidateRepository
import com.alongside.core.domain.place.PlaceSwipeRepository
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.place.PlaceCandidate
import com.alongside.core.model.place.PlaceSwipe
import com.alongside.core.model.place.SwipeDirection
import com.alongside.core.network.client.configureFirestoreHttpClient
import com.alongside.core.network.firestore.FirestoreApi
import com.alongside.core.network.firestore.FirestoreConfig
import com.alongside.core.network.firestore.FirestoreTokenProvider
import com.alongside.core.network.firestore.model.firestoreJson
import com.alongside.data.testPlace
import com.alongside.data.testPlaceSwipe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpResponseData
import io.ktor.content.TextContent
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// runBlocking, not runTest: runTest's virtual-time scheduler falsely times out
// Ktor HttpTimeout against MockEngine (see the M3 note in docs/roadmap.md).
class FirestorePlaceContentPullerTest {
    private class RecordingPlaceCandidateRepository : PlaceCandidateRepository {
        val upserted = mutableListOf<PlaceCandidate>()

        override suspend fun upsert(place: PlaceCandidate) {
            upserted += place
        }

        override suspend fun getById(id: String): PlaceCandidate? = upserted.find { it.id == id }

        override fun observeByTrip(tripId: String): Flow<List<PlaceCandidate>> = MutableStateFlow(upserted)

        override suspend fun delete(id: String) {
            upserted.removeAll { it.id == id }
        }
    }

    private class RecordingPlaceSwipeRepository : PlaceSwipeRepository {
        val upserted = mutableListOf<PlaceSwipe>()

        override suspend fun upsert(swipe: PlaceSwipe) {
            upserted += swipe
        }

        override suspend fun getById(id: String): PlaceSwipe? = upserted.find { it.id == id }

        override fun observeByTrip(tripId: String): Flow<List<PlaceSwipe>> = MutableStateFlow(upserted)
    }

    private fun MockRequestHandleScope.respondJson(json: String): HttpResponseData =
        respond(
            content = json,
            status = HttpStatusCode.OK,
            headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
        )

    private fun candidateDocumentJson(place: PlaceCandidate): String {
        val fields = firestoreJson.encodeToString(PlaceCandidateFirestoreMapper.toFields(place))
        val name = "projects/p/databases/(default)/documents/placeCandidates/${place.id}"
        return """{"document": {"name": "$name", "fields": $fields}}"""
    }

    private fun swipeDocumentJson(swipe: PlaceSwipe): String {
        val fields = firestoreJson.encodeToString(PlaceSwipeFirestoreMapper.toFields(swipe))
        val name = "projects/p/databases/(default)/documents/placeSwipes/${swipe.id}"
        return """{"document": {"name": "$name", "fields": $fields}}"""
    }

    private fun puller(
        localPlaceCandidateRepository: PlaceCandidateRepository,
        localPlaceSwipeRepository: PlaceSwipeRepository,
        handler: suspend MockRequestHandleScope.(body: String) -> HttpResponseData,
    ): FirestorePlaceContentPuller {
        val api =
            FirestoreApi(
                HttpClient(
                    MockEngine { request -> handler((request.body as TextContent).text) },
                ) { configureFirestoreHttpClient() },
                FirestoreConfig(projectId = "alongside-test"),
                FirestoreTokenProvider { null },
            )
        return FirestorePlaceContentPuller(api, localPlaceCandidateRepository, localPlaceSwipeRepository)
    }

    @Test
    fun `pulls a partner-authored candidate and swipe into local storage`() {
        val partnerCandidate = testPlace(id = "place-partner", tripId = "trip-1", addedByUserId = "partner-1")
        val partnerSwipe =
            testPlaceSwipe(
                id = "place-partner::partner-1",
                tripId = "trip-1",
                candidateId = "place-partner",
                userId = "partner-1",
            )
        val localPlaceCandidateRepository = RecordingPlaceCandidateRepository()
        val localPlaceSwipeRepository = RecordingPlaceSwipeRepository()
        val placePuller =
            puller(localPlaceCandidateRepository, localPlaceSwipeRepository) { body ->
                when {
                    body.contains("placeCandidates") -> respondJson("[${candidateDocumentJson(partnerCandidate)}]")
                    body.contains("placeSwipes") -> respondJson("[${swipeDocumentJson(partnerSwipe)}]")
                    else -> error("Unexpected query body: $body")
                }
            }

        runBlocking { placePuller.pullTripContent(tripId = "trip-1", ownUserId = "own-1") }

        assertEquals(listOf("place-partner"), localPlaceCandidateRepository.upserted.map { it.id })
        assertEquals(listOf("place-partner::partner-1"), localPlaceSwipeRepository.upserted.map { it.id })
    }

    @Test
    fun `an own-authored candidate and swipe missing locally are pulled in`() {
        val ownCandidate = testPlace(id = "place-own", tripId = "trip-1", addedByUserId = "own-1")
        val ownSwipe =
            testPlaceSwipe(id = "place-own::own-1", tripId = "trip-1", candidateId = "place-own", userId = "own-1")
        val localPlaceCandidateRepository = RecordingPlaceCandidateRepository()
        val localPlaceSwipeRepository = RecordingPlaceSwipeRepository()
        val placePuller =
            puller(localPlaceCandidateRepository, localPlaceSwipeRepository) { body ->
                when {
                    body.contains("placeCandidates") -> respondJson("[${candidateDocumentJson(ownCandidate)}]")
                    body.contains("placeSwipes") -> respondJson("[${swipeDocumentJson(ownSwipe)}]")
                    else -> error("Unexpected query body: $body")
                }
            }

        runBlocking { placePuller.pullTripContent(tripId = "trip-1", ownUserId = "own-1") }

        assertEquals(listOf("place-own"), localPlaceCandidateRepository.upserted.map { it.id })
        assertEquals(listOf("place-own::own-1"), localPlaceSwipeRepository.upserted.map { it.id })
    }

    @Test
    fun `an own-authored candidate and swipe already present locally are never overwritten from remote`() {
        val remoteOwnCandidate = testPlace(id = "place-own", tripId = "trip-1", addedByUserId = "own-1", rating = 4.5)
        val remoteOwnSwipe =
            testPlaceSwipe(id = "place-own::own-1", tripId = "trip-1", candidateId = "place-own", userId = "own-1")
        val existingLocalCandidate =
            testPlace(id = "place-own", tripId = "trip-1", addedByUserId = "own-1", rating = null)
        val existingLocalSwipe =
            testPlaceSwipe(id = "place-own::own-1", tripId = "trip-1", candidateId = "place-own", userId = "own-1")
        val localPlaceCandidateRepository = RecordingPlaceCandidateRepository()
        val localPlaceSwipeRepository = RecordingPlaceSwipeRepository()
        runBlocking {
            localPlaceCandidateRepository.upsert(existingLocalCandidate)
            localPlaceSwipeRepository.upsert(existingLocalSwipe)
        }

        val placePuller =
            puller(localPlaceCandidateRepository, localPlaceSwipeRepository) { body ->
                when {
                    body.contains("placeCandidates") -> respondJson("[${candidateDocumentJson(remoteOwnCandidate)}]")
                    body.contains("placeSwipes") -> respondJson("[${swipeDocumentJson(remoteOwnSwipe)}]")
                    else -> error("Unexpected query body: $body")
                }
            }

        runBlocking { placePuller.pullTripContent(tripId = "trip-1", ownUserId = "own-1") }

        assertEquals(listOf(existingLocalCandidate), localPlaceCandidateRepository.upserted)
        assertEquals(listOf(existingLocalSwipe), localPlaceSwipeRepository.upserted)
    }

    @Test
    fun `no remote candidates or swipes for the trip pulls nothing`() {
        val localPlaceCandidateRepository = RecordingPlaceCandidateRepository()
        val localPlaceSwipeRepository = RecordingPlaceSwipeRepository()
        val placePuller = puller(localPlaceCandidateRepository, localPlaceSwipeRepository) { respondJson("[]") }

        runBlocking { placePuller.pullTripContent(tripId = "trip-1", ownUserId = "own-1") }

        assertTrue(localPlaceCandidateRepository.upserted.isEmpty())
        assertTrue(localPlaceSwipeRepository.upserted.isEmpty())
    }

    @Test
    fun `an unchanged partner candidate and swipe are not re-upserted on a repeat poll`() {
        // fromDocument always stamps SYNCED (see PlaceCandidateFirestoreMapper/PlaceSwipeFirestoreMapper) -
        // the local copy must already reflect that for the "unchanged" comparison to hold.
        val partnerCandidate =
            testPlace(
                id = "place-partner",
                tripId = "trip-1",
                addedByUserId = "partner-1",
                syncStatus = SyncStatus.SYNCED,
            )
        val partnerSwipe =
            testPlaceSwipe(
                id = "place-partner::partner-1",
                tripId = "trip-1",
                candidateId = "place-partner",
                userId = "partner-1",
                syncStatus = SyncStatus.SYNCED,
            )
        val localPlaceCandidateRepository = RecordingPlaceCandidateRepository()
        val localPlaceSwipeRepository = RecordingPlaceSwipeRepository()
        runBlocking {
            localPlaceCandidateRepository.upsert(partnerCandidate)
            localPlaceSwipeRepository.upsert(partnerSwipe)
        }
        val placePuller =
            puller(localPlaceCandidateRepository, localPlaceSwipeRepository) { body ->
                when {
                    body.contains("placeCandidates") -> respondJson("[${candidateDocumentJson(partnerCandidate)}]")
                    body.contains("placeSwipes") -> respondJson("[${swipeDocumentJson(partnerSwipe)}]")
                    else -> error("Unexpected query body: $body")
                }
            }

        runBlocking { placePuller.pullTripContent(tripId = "trip-1", ownUserId = "own-1") }

        // Still just the one write from the setup above - the poll itself wrote nothing, since
        // Room's Flow invalidation fires on any write regardless of content (see
        // FirestorePlaceContentPuller.upsertCandidateIfChanged's KDoc).
        assertEquals(listOf(partnerCandidate), localPlaceCandidateRepository.upserted)
        assertEquals(listOf(partnerSwipe), localPlaceSwipeRepository.upserted)
    }

    @Test
    fun `a changed partner candidate and swipe are still re-upserted`() {
        val staleCandidate =
            testPlace(
                id = "place-partner",
                tripId = "trip-1",
                addedByUserId = "partner-1",
                syncStatus = SyncStatus.SYNCED,
            )
        val updatedCandidate = staleCandidate.copy(rating = 4.8)
        val staleSwipe =
            testPlaceSwipe(
                id = "place-partner::partner-1",
                tripId = "trip-1",
                candidateId = "place-partner",
                userId = "partner-1",
                syncStatus = SyncStatus.SYNCED,
            )
        val updatedSwipe = staleSwipe.copy(direction = SwipeDirection.DISLIKE)
        val localPlaceCandidateRepository = RecordingPlaceCandidateRepository()
        val localPlaceSwipeRepository = RecordingPlaceSwipeRepository()
        runBlocking {
            localPlaceCandidateRepository.upsert(staleCandidate)
            localPlaceSwipeRepository.upsert(staleSwipe)
        }
        val placePuller =
            puller(localPlaceCandidateRepository, localPlaceSwipeRepository) { body ->
                when {
                    body.contains("placeCandidates") -> respondJson("[${candidateDocumentJson(updatedCandidate)}]")
                    body.contains("placeSwipes") -> respondJson("[${swipeDocumentJson(updatedSwipe)}]")
                    else -> error("Unexpected query body: $body")
                }
            }

        runBlocking { placePuller.pullTripContent(tripId = "trip-1", ownUserId = "own-1") }

        assertEquals(listOf(staleCandidate, updatedCandidate), localPlaceCandidateRepository.upserted)
        assertEquals(listOf(staleSwipe, updatedSwipe), localPlaceSwipeRepository.upserted)
    }
}
