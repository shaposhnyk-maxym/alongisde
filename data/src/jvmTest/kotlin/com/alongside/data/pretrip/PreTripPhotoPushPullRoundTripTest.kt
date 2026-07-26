package com.alongside.data.pretrip

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.alongside.core.database.AlongsideDatabase
import com.alongside.core.database.preTripPhotoRepository
import com.alongside.core.database.syncOperationStore
import com.alongside.core.domain.pretrip.PreTripPhotoUploadClient
import com.alongside.core.domain.pretrip.PreTripPhotoUploadResult
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.pretrip.PreTripPhoto
import com.alongside.core.network.client.configureFirestoreHttpClient
import com.alongside.core.network.firestore.FirestoreApi
import com.alongside.core.network.firestore.FirestoreConfig
import com.alongside.core.network.firestore.FirestoreTokenProvider
import com.alongside.core.network.firestore.model.FirestoreValue
import com.alongside.core.network.firestore.model.firestoreJson
import com.alongside.core.network.queue.MaxAttemptsRetryPolicy
import com.alongside.core.network.queue.SyncNetworkClient
import com.alongside.core.network.queue.SyncOperation
import com.alongside.core.network.queue.SyncQueueProcessor
import com.alongside.core.network.queue.SyncResult
import com.alongside.data.FakeBackgroundWorkScheduler
import com.alongside.data.sync.FakeRemoteDocumentReader
import com.alongside.data.sync.SyncCoordinator
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

private val FIXED_NOW = Instant.fromEpochMilliseconds(1_752_800_000_000)

private object RoundTripClock : Clock {
    override fun now(): Instant = FIXED_NOW
}

/**
 * M19.7 accept criterion 2: pushes a [PreTripPhoto] through [SyncingPreTripPhotoRepository]/
 * [SyncCoordinator] into a fake Firestore backend, then pulls it back "from another device"
 * through [FirestorePreTripPhotoContentPuller] - closing the full push+pull cycle in one test
 * rather than trusting each half in isolation.
 *
 * `runBlocking`, not `runTest`: the pull half drives a real [FirestoreApi] over `MockEngine`, and
 * `runTest`'s virtual-time scheduler falsely times out Ktor `HttpTimeout` against `MockEngine`
 * (see the M3 note in docs/roadmap.md, and [FirestorePreTripPhotoContentPullerTest]'s own comment).
 */
class PreTripPhotoPushPullRoundTripTest {
    /** Fake Firestore backend: push writes land here, pull reads come straight back out. */
    private class FakeFirestoreBackendNetworkClient(
        private val backend: MutableMap<String, Map<String, FirestoreValue>>,
    ) : SyncNetworkClient {
        override suspend fun push(operation: SyncOperation): SyncResult {
            backend["${operation.collectionPath}/${operation.documentId}"] = operation.fields
            return SyncResult.Success
        }
    }

    private class FakePreTripPhotoUploadClient : PreTripPhotoUploadClient {
        override suspend fun upload(
            photo: PreTripPhoto,
            bytes: ByteArray,
        ): PreTripPhotoUploadResult =
            PreTripPhotoUploadResult.Uploaded(
                "https://firebasestorage.googleapis.com/v0/b/test-bucket/o/preTrip%2F${photo.id}" +
                    "?alt=media&token=fake-token-${photo.id}",
            )
    }

    private fun MockRequestHandleScope.respondJson(json: String): HttpResponseData =
        respond(
            content = json,
            status = HttpStatusCode.OK,
            headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
        )

    private fun documentJson(
        path: String,
        fields: Map<String, FirestoreValue>,
    ): String {
        val fieldsJson = firestoreJson.encodeToString(fields)
        val name = "projects/p/databases/(default)/documents/$path"
        return """{"document": {"name": "$name", "fields": $fieldsJson}}"""
    }

    /** A real [FirestoreApi] over [MockEngine] whose "server" is the shared push-half [backend] map. */
    private fun pullApi(backend: Map<String, Map<String, FirestoreValue>>): FirestoreApi =
        FirestoreApi(
            HttpClient(
                MockEngine { request ->
                    val body = (request.body as TextContent).text
                    check(body.contains("preTripPhotos")) { "Unexpected query body: $body" }
                    val entries = backend.filterKeys { it.startsWith("preTripPhotos/") }
                    val documents = entries.entries.joinToString(",") { (path, fields) -> documentJson(path, fields) }
                    respondJson("[$documents]")
                },
            ) { configureFirestoreHttpClient() },
            FirestoreConfig(projectId = "alongside-test"),
            FirestoreTokenProvider { null },
        )

    private fun inMemoryDatabase(): AlongsideDatabase =
        Room
            .inMemoryDatabaseBuilder<AlongsideDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()

    private fun uploadedPhoto(
        userId: String,
        uri: String,
    ): PreTripPhoto {
        val bare = testPreTripPhoto(userId = userId, uri = uri)
        val uploaded = runBlocking { FakePreTripPhotoUploadClient().upload(bare, byteArrayOf(1)) }
        return bare.copy(remoteUrl = (uploaded as PreTripPhotoUploadResult.Uploaded).remoteUrl)
    }

    /** Pushes [photo] from [deviceA] into the shared fake [backend], asserting the no-preflight invariant. */
    private fun pushToBackend(
        deviceA: AlongsideDatabase,
        backend: MutableMap<String, Map<String, FirestoreValue>>,
        photo: PreTripPhoto,
    ): Map<String, FirestoreValue> {
        val local = deviceA.preTripPhotoRepository()
        val syncingRepo =
            SyncingPreTripPhotoRepository(
                local = local,
                store = deviceA.syncOperationStore(),
                backgroundWorkScheduler = FakeBackgroundWorkScheduler(),
                clock = RoundTripClock,
                generateOpId = { "op-1" },
            )
        val remoteReader = FakeRemoteDocumentReader()
        val networkClient = FakeFirestoreBackendNetworkClient(backend)
        val coordinator =
            SyncCoordinator(
                store = deviceA.syncOperationStore(),
                processor = SyncQueueProcessor(networkClient, MaxAttemptsRetryPolicy(2)),
                remoteReader = remoteReader,
                bindings = listOf(PreTripPhotoSyncEntityBinding(local)),
            )

        runBlocking {
            syncingRepo.upsert(photo)
            coordinator.sync()
        }

        // PreTripPhoto has no updatedAt field, so SyncCoordinator's preflight never reads
        // remote for this entity - regression guard for that invariant.
        assertTrue(remoteReader.readDocumentIds.isEmpty())

        return backend.getValue("preTripPhotos/${photo.id}")
    }

    private fun pushThenPull(
        photo: PreTripPhoto,
        pullOwnUserId: String,
    ): Pair<Map<String, FirestoreValue>, PreTripPhoto?> {
        val deviceA = inMemoryDatabase()
        val deviceB = inMemoryDatabase()
        try {
            val backend = mutableMapOf<String, Map<String, FirestoreValue>>()
            val pushedFields = pushToBackend(deviceA, backend, photo)

            val puller =
                FirestorePreTripPhotoContentPuller(
                    api = pullApi(backend),
                    localPreTripPhotoRepository = deviceB.preTripPhotoRepository(),
                )
            runBlocking { puller.pullTripContent(tripId = photo.tripId, ownUserId = pullOwnUserId) }

            val pulled = runBlocking { deviceB.preTripPhotoRepository().getById(photo.id) }
            return pushedFields to pulled
        } finally {
            deviceA.close()
            deviceB.close()
        }
    }

    @Test
    fun `a partner's photo pushed on device A is pulled onto device B`() {
        val photo = uploadedPhoto(userId = "partner-1", uri = "content://media/external/images/42")

        val (pushedFields, pulled) = pushThenPull(photo, pullOwnUserId = "own-user-on-device-b")

        // Push half (Accept 2): the synced record carries a real Storage remoteUrl, not the
        // original content:// uri - uri is left untouched, only remoteUrl is populated.
        val remoteUrl = (pushedFields.getValue("remoteUrl") as FirestoreValue.StringValue).value
        assertFalse(remoteUrl.startsWith("content://"))
        assertTrue(remoteUrl.startsWith("https://firebasestorage.googleapis.com/"))
        assertEquals(photo.uri, (pushedFields.getValue("uri") as FirestoreValue.StringValue).value)

        // Pull half (Accept 3): "another device" reading the same fake backend pulls the
        // partner's photo field-for-field identical (fromDocument always stamps SYNCED).
        assertEquals(photo.copy(syncStatus = SyncStatus.SYNCED), pulled)
    }

    @Test
    fun `own photo pushed on device A is pulled onto device B for the same user`() {
        val photo = uploadedPhoto(userId = "own-1", uri = "content://media/external/images/43")

        // Same userId as the photo's own author - the gap-fill branch, not the overwrite branch.
        val (_, pulled) = pushThenPull(photo, pullOwnUserId = "own-1")

        assertEquals(photo.copy(syncStatus = SyncStatus.SYNCED), pulled)
    }
}
