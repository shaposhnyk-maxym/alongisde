package com.alongside.data.pretrip

import com.alongside.core.domain.pretrip.PreTripPhotoRepository
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.pretrip.PreTripPhoto
import com.alongside.core.network.client.configureFirestoreHttpClient
import com.alongside.core.network.firestore.FirestoreApi
import com.alongside.core.network.firestore.FirestoreConfig
import com.alongside.core.network.firestore.FirestoreTokenProvider
import com.alongside.core.network.firestore.model.firestoreJson
import com.alongside.data.testPreTripPhoto
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
class FirestorePreTripPhotoContentPullerTest {
    private class RecordingPreTripPhotoRepository : PreTripPhotoRepository {
        val upserted = mutableListOf<PreTripPhoto>()

        override suspend fun upsert(photo: PreTripPhoto) {
            upserted += photo
        }

        override suspend fun getById(id: String): PreTripPhoto? = upserted.find { it.id == id }

        override fun observeByTripAndUser(
            tripId: String,
            userId: String,
        ): Flow<List<PreTripPhoto>> = MutableStateFlow(upserted)

        override suspend fun delete(id: String) {
            upserted.removeAll { it.id == id }
        }
    }

    private fun MockRequestHandleScope.respondJson(json: String): HttpResponseData =
        respond(
            content = json,
            status = HttpStatusCode.OK,
            headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
        )

    private fun photoDocumentJson(photo: PreTripPhoto): String {
        val fields = firestoreJson.encodeToString(PreTripPhotoFirestoreMapper.toFields(photo))
        val name = "projects/p/databases/(default)/documents/preTripPhotos/${photo.id}"
        return """{"document": {"name": "$name", "fields": $fields}}"""
    }

    private fun puller(
        local: PreTripPhotoRepository,
        handler: suspend MockRequestHandleScope.(body: String) -> HttpResponseData,
    ): FirestorePreTripPhotoContentPuller {
        val api =
            FirestoreApi(
                HttpClient(
                    MockEngine { request -> handler((request.body as TextContent).text) },
                ) { configureFirestoreHttpClient() },
                FirestoreConfig(projectId = "alongside-test"),
                FirestoreTokenProvider { null },
            )
        return FirestorePreTripPhotoContentPuller(api, local)
    }

    @Test
    fun `pulls a partner-authored photo into local storage`() {
        val partnerPhoto = testPreTripPhoto(id = "photo-partner", tripId = "trip-1", userId = "partner-1")
        val local = RecordingPreTripPhotoRepository()
        val puller =
            puller(local) { body ->
                when {
                    body.contains("preTripPhotos") -> respondJson("[${photoDocumentJson(partnerPhoto)}]")
                    else -> error("Unexpected query body: $body")
                }
            }

        runBlocking { puller.pullTripContent(tripId = "trip-1", ownUserId = "own-1") }

        assertEquals(listOf("photo-partner"), local.upserted.map { it.id })
    }

    @Test
    fun `an own-authored photo missing locally is pulled in`() {
        val ownPhoto = testPreTripPhoto(id = "photo-own", tripId = "trip-1", userId = "own-1")
        val local = RecordingPreTripPhotoRepository()
        val puller = puller(local) { respondJson("[${photoDocumentJson(ownPhoto)}]") }

        runBlocking { puller.pullTripContent(tripId = "trip-1", ownUserId = "own-1") }

        assertEquals(listOf("photo-own"), local.upserted.map { it.id })
    }

    @Test
    fun `an own-authored photo already present locally is never overwritten from remote`() {
        val remoteOwnPhoto =
            testPreTripPhoto(
                id = "photo-own",
                tripId = "trip-1",
                userId = "own-1",
                remoteUrl = "https://firebasestorage.googleapis.com/new",
            )
        val existingLocalPhoto = testPreTripPhoto(id = "photo-own", tripId = "trip-1", userId = "own-1")
        val local = RecordingPreTripPhotoRepository()
        runBlocking { local.upsert(existingLocalPhoto) }

        val puller = puller(local) { respondJson("[${photoDocumentJson(remoteOwnPhoto)}]") }

        runBlocking { puller.pullTripContent(tripId = "trip-1", ownUserId = "own-1") }

        assertEquals(listOf(existingLocalPhoto), local.upserted)
    }

    @Test
    fun `no remote documents pulls nothing`() {
        val local = RecordingPreTripPhotoRepository()
        val puller = puller(local) { respondJson("[]") }

        runBlocking { puller.pullTripContent(tripId = "trip-1", ownUserId = "own-1") }

        assertTrue(local.upserted.isEmpty())
    }

    @Test
    fun `an unchanged partner photo is not re-upserted on a repeat poll`() {
        // fromDocument always stamps SYNCED (see PreTripPhotoFirestoreMapper) - the local copy
        // must already reflect that for the "unchanged" comparison to hold.
        val partnerPhoto =
            testPreTripPhoto(
                id = "photo-partner",
                tripId = "trip-1",
                userId = "partner-1",
                syncStatus = SyncStatus.SYNCED,
            )
        val local = RecordingPreTripPhotoRepository()
        runBlocking { local.upsert(partnerPhoto) }

        val puller = puller(local) { respondJson("[${photoDocumentJson(partnerPhoto)}]") }

        runBlocking { puller.pullTripContent(tripId = "trip-1", ownUserId = "own-1") }

        // Still just the one write from the setup above - the poll itself wrote nothing.
        assertEquals(listOf(partnerPhoto), local.upserted)
    }

    @Test
    fun `a changed partner photo is still re-upserted`() {
        val stalePhoto =
            testPreTripPhoto(
                id = "photo-partner",
                tripId = "trip-1",
                userId = "partner-1",
                syncStatus = SyncStatus.SYNCED,
            )
        val updatedPhoto = stalePhoto.copy(remoteUrl = "https://firebasestorage.googleapis.com/new")
        val local = RecordingPreTripPhotoRepository()
        runBlocking { local.upsert(stalePhoto) }

        val puller = puller(local) { respondJson("[${photoDocumentJson(updatedPhoto)}]") }

        runBlocking { puller.pullTripContent(tripId = "trip-1", ownUserId = "own-1") }

        assertEquals(listOf(stalePhoto, updatedPhoto), local.upserted)
    }
}
