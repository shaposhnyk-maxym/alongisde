package com.alongside.data.diary

import com.alongside.core.domain.diary.DiaryEntryRepository
import com.alongside.core.domain.diary.EpisodeRepository
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.diary.DiaryEntry
import com.alongside.core.model.diary.Episode
import com.alongside.core.network.client.configureFirestoreHttpClient
import com.alongside.core.network.firestore.FirestoreApi
import com.alongside.core.network.firestore.FirestoreConfig
import com.alongside.core.network.firestore.FirestoreTokenProvider
import com.alongside.core.network.firestore.model.firestoreJson
import com.alongside.data.episode.EpisodeFirestoreMapper
import com.alongside.data.testDiaryEntry
import com.alongside.data.testEpisode
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
class FirestoreDiaryContentPullerTest {
    private class RecordingDiaryEntryRepository : DiaryEntryRepository {
        val upserted = mutableListOf<DiaryEntry>()

        override suspend fun upsert(entry: DiaryEntry) {
            upserted += entry
        }

        override suspend fun getById(id: String): DiaryEntry? = upserted.find { it.id == id }

        override fun observeByTrip(tripId: String): Flow<List<DiaryEntry>> = MutableStateFlow(upserted)

        override suspend fun delete(id: String) {
            upserted.removeAll { it.id == id }
        }
    }

    private class RecordingEpisodeRepository : EpisodeRepository {
        val upserted = mutableListOf<Episode>()

        override suspend fun upsert(episode: Episode) {
            upserted += episode
        }

        override suspend fun getById(id: String): Episode? = upserted.find { it.id == id }

        override fun observeByDiaryEntry(diaryEntryId: String): Flow<List<Episode>> = MutableStateFlow(upserted)

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

    private fun diaryEntryDocumentJson(entry: DiaryEntry): String {
        val fields = firestoreJson.encodeToString(DiaryEntryFirestoreMapper.toFields(entry))
        val name = "projects/p/databases/(default)/documents/diaryEntries/${entry.id}"
        return """{"document": {"name": "$name", "fields": $fields}}"""
    }

    private fun episodeDocumentJson(episode: Episode): String {
        val fields = firestoreJson.encodeToString(EpisodeFirestoreMapper.toFields(episode))
        val name = "projects/p/databases/(default)/documents/episodes/${episode.id}"
        return """{"document": {"name": "$name", "fields": $fields}}"""
    }

    private fun puller(
        localDiaryEntryRepository: DiaryEntryRepository,
        localEpisodeRepository: EpisodeRepository,
        handler: suspend MockRequestHandleScope.(body: String) -> HttpResponseData,
    ): FirestoreDiaryContentPuller {
        val api =
            FirestoreApi(
                HttpClient(
                    MockEngine { request -> handler((request.body as TextContent).text) },
                ) { configureFirestoreHttpClient() },
                FirestoreConfig(projectId = "alongside-test"),
                FirestoreTokenProvider { null },
            )
        return FirestoreDiaryContentPuller(api, localDiaryEntryRepository, localEpisodeRepository)
    }

    @Test
    fun `pulls a partner-authored entry and its episodes into local storage`() {
        val partnerEntry = testDiaryEntry(id = "entry-partner", tripId = "trip-1", userId = "partner-1")
        val partnerEpisode = testEpisode(id = "episode-1", diaryEntryId = "entry-partner")
        val localDiaryEntryRepository = RecordingDiaryEntryRepository()
        val localEpisodeRepository = RecordingEpisodeRepository()
        val diaryPuller =
            puller(localDiaryEntryRepository, localEpisodeRepository) { body ->
                when {
                    body.contains("diaryEntries") -> respondJson("[${diaryEntryDocumentJson(partnerEntry)}]")
                    body.contains("episodes") -> respondJson("[${episodeDocumentJson(partnerEpisode)}]")
                    else -> error("Unexpected query body: $body")
                }
            }

        runBlocking { diaryPuller.pullTripContent(tripId = "trip-1", ownUserId = "own-1") }

        assertEquals(listOf("entry-partner"), localDiaryEntryRepository.upserted.map { it.id })
        assertEquals(listOf("episode-1"), localEpisodeRepository.upserted.map { it.id })
    }

    @Test
    fun `an own-authored entry missing locally is pulled in along with its episodes`() {
        val ownEntry = testDiaryEntry(id = "entry-own", tripId = "trip-1", userId = "own-1")
        val ownEpisode = testEpisode(id = "episode-own-1", diaryEntryId = "entry-own")
        val localDiaryEntryRepository = RecordingDiaryEntryRepository()
        val localEpisodeRepository = RecordingEpisodeRepository()
        val diaryPuller =
            puller(localDiaryEntryRepository, localEpisodeRepository) { body ->
                when {
                    body.contains("diaryEntries") -> respondJson("[${diaryEntryDocumentJson(ownEntry)}]")
                    body.contains("episodes") -> respondJson("[${episodeDocumentJson(ownEpisode)}]")
                    else -> error("Unexpected query body: $body")
                }
            }

        runBlocking { diaryPuller.pullTripContent(tripId = "trip-1", ownUserId = "own-1") }

        assertEquals(listOf("entry-own"), localDiaryEntryRepository.upserted.map { it.id })
        assertEquals(listOf("episode-own-1"), localEpisodeRepository.upserted.map { it.id })
    }

    @Test
    fun `an own-authored entry already present locally is never overwritten from remote`() {
        val remoteOwnEntry = testDiaryEntry(id = "entry-own", tripId = "trip-1", userId = "own-1")
        val localDiaryEntryRepository = RecordingDiaryEntryRepository()
        val localEpisodeRepository = RecordingEpisodeRepository()
        val existingLocalEntry = testDiaryEntry(id = "entry-own", tripId = "trip-1", userId = "own-1")
        runBlocking { localDiaryEntryRepository.upsert(existingLocalEntry) }

        val diaryPuller =
            puller(localDiaryEntryRepository, localEpisodeRepository) { body ->
                when {
                    // No "episodes" branch - asserts pullEpisodes is never even called for a
                    // skipped own entry, since the mock would error on an unexpected request body.
                    body.contains("diaryEntries") -> respondJson("[${diaryEntryDocumentJson(remoteOwnEntry)}]")
                    else -> error("Unexpected query body: $body")
                }
            }

        runBlocking { diaryPuller.pullTripContent(tripId = "trip-1", ownUserId = "own-1") }

        assertEquals(listOf(existingLocalEntry), localDiaryEntryRepository.upserted)
        assertTrue(localEpisodeRepository.upserted.isEmpty())
    }

    @Test
    fun `no remote entries for the trip pulls nothing`() {
        val localDiaryEntryRepository = RecordingDiaryEntryRepository()
        val localEpisodeRepository = RecordingEpisodeRepository()
        val diaryPuller = puller(localDiaryEntryRepository, localEpisodeRepository) { respondJson("[]") }

        runBlocking { diaryPuller.pullTripContent(tripId = "trip-1", ownUserId = "own-1") }

        assertTrue(localDiaryEntryRepository.upserted.isEmpty())
        assertTrue(localEpisodeRepository.upserted.isEmpty())
    }

    @Test
    fun `an unchanged partner entry and episode are not re-upserted on a repeat poll`() {
        // fromDocument always stamps SYNCED (see DiaryEntryFirestoreMapper/EpisodeFirestoreMapper) -
        // the local copy must already reflect that for the "unchanged" comparison to hold.
        val partnerEntry =
            testDiaryEntry(
                id = "entry-partner",
                tripId = "trip-1",
                userId = "partner-1",
                syncStatus = SyncStatus.SYNCED,
            )
        val partnerEpisode =
            testEpisode(id = "episode-1", diaryEntryId = "entry-partner", syncStatus = SyncStatus.SYNCED)
        val localDiaryEntryRepository = RecordingDiaryEntryRepository()
        val localEpisodeRepository = RecordingEpisodeRepository()
        runBlocking {
            localDiaryEntryRepository.upsert(partnerEntry)
            localEpisodeRepository.upsert(partnerEpisode)
        }
        val diaryPuller =
            puller(localDiaryEntryRepository, localEpisodeRepository) { body ->
                when {
                    body.contains("diaryEntries") -> respondJson("[${diaryEntryDocumentJson(partnerEntry)}]")
                    body.contains("episodes") -> respondJson("[${episodeDocumentJson(partnerEpisode)}]")
                    else -> error("Unexpected query body: $body")
                }
            }

        runBlocking { diaryPuller.pullTripContent(tripId = "trip-1", ownUserId = "own-1") }

        // Still just the one write from the setup above - the poll itself wrote nothing,
        // since Room's Flow invalidation fires on any write regardless of content (see
        // FirestoreDiaryContentPuller.upsertEntryIfChanged's KDoc).
        assertEquals(listOf(partnerEntry), localDiaryEntryRepository.upserted)
        assertEquals(listOf(partnerEpisode), localEpisodeRepository.upserted)
    }

    @Test
    fun `a changed partner entry and episode are still re-upserted`() {
        val staleEntry =
            testDiaryEntry(
                id = "entry-partner",
                tripId = "trip-1",
                userId = "partner-1",
                syncStatus = SyncStatus.SYNCED,
            )
        val updatedEntry = staleEntry.copy(closedAt = kotlin.time.Instant.parse("2026-02-22T00:00:00Z"))
        val staleEpisode = testEpisode(id = "episode-1", diaryEntryId = "entry-partner", syncStatus = SyncStatus.SYNCED)
        val updatedEpisode = staleEpisode.copy(placeName = "Updated place")
        val localDiaryEntryRepository = RecordingDiaryEntryRepository()
        val localEpisodeRepository = RecordingEpisodeRepository()
        runBlocking {
            localDiaryEntryRepository.upsert(staleEntry)
            localEpisodeRepository.upsert(staleEpisode)
        }
        val diaryPuller =
            puller(localDiaryEntryRepository, localEpisodeRepository) { body ->
                when {
                    body.contains("diaryEntries") -> respondJson("[${diaryEntryDocumentJson(updatedEntry)}]")
                    body.contains("episodes") -> respondJson("[${episodeDocumentJson(updatedEpisode)}]")
                    else -> error("Unexpected query body: $body")
                }
            }

        runBlocking { diaryPuller.pullTripContent(tripId = "trip-1", ownUserId = "own-1") }

        assertEquals(listOf(staleEntry, updatedEntry), localDiaryEntryRepository.upserted)
        assertEquals(listOf(staleEpisode, updatedEpisode), localEpisodeRepository.upserted)
    }
}
