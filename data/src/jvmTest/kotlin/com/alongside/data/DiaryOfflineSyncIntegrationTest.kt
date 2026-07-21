package com.alongside.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.alongside.core.database.AlongsideDatabase
import com.alongside.core.database.diaryEntryRepository
import com.alongside.core.database.episodeRepository
import com.alongside.core.database.syncOperationStore
import com.alongside.core.domain.diary.DayUnlockState
import com.alongside.core.domain.diary.diaryDayStatus
import com.alongside.core.domain.diary.processing.EpisodeProcessingPipeline
import com.alongside.core.domain.diary.processing.EpisodeVisionDescriptionClient
import com.alongside.core.domain.diary.processing.GeocodingResult
import com.alongside.core.domain.diary.processing.PlaceGeocodingClient
import com.alongside.core.domain.diary.processing.VisionDescriptionResult
import com.alongside.core.domain.diary.resolveDayUnlockState
import com.alongside.core.domain.diary.shouldTriggerPartnerReadyPush
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.diary.Episode
import com.alongside.core.model.diary.Photo
import com.alongside.core.network.firestore.model.FirestoreValue
import com.alongside.core.network.queue.MaxAttemptsRetryPolicy
import com.alongside.core.network.queue.SyncQueueProcessor
import com.alongside.data.FakeBackgroundWorkScheduler
import com.alongside.data.diary.DiaryEntryFirestoreMapper
import com.alongside.data.diary.DiaryEntrySyncEntityBinding
import com.alongside.data.diary.SyncingDiaryEntryRepository
import com.alongside.data.episode.EpisodeFirestoreMapper
import com.alongside.data.episode.EpisodeSyncEntityBinding
import com.alongside.data.episode.SyncingEpisodeRepository
import com.alongside.data.sync.FakePhotoUploadClient
import com.alongside.data.sync.FakeRemoteDocumentReader
import com.alongside.data.sync.RecordingSyncNetworkClient
import com.alongside.data.sync.SyncCoordinator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

private val FIXED_NOW = Instant.fromEpochMilliseconds(1_752_800_000_000)

// One day after testDiaryEntry's default date - so a plain SYNCED entry auto-closes via the
// date-has-passed rule (docs/roadmap.md M12.6) without every test in this file needing to
// thread an explicit closedAt through just to exercise the sync mechanism.
private val TODAY = LocalDate(2026, 7, 19)

private object DiaryFixedClock : Clock {
    override fun now(): Instant = FIXED_NOW
}

/** Always finds the same place, regardless of who's asking. */
private class FakeGeocodingClient : PlaceGeocodingClient {
    override suspend fun reverseGeocode(
        latitude: Double,
        longitude: Double,
    ): GeocodingResult = GeocodingResult.Found("Rynok Square")
}

private class FakeVisionClient : EpisodeVisionDescriptionClient {
    override suspend fun describeEpisode(
        images: List<ByteArray>,
        placeName: String?,
        languageTag: String,
    ): VisionDescriptionResult = VisionDescriptionResult.Generated("A wander through the old town.")
}

/**
 * M11 accept criterion 3, on the real stack: a DiaryEntry for each side of a trip goes through
 * capture (synthetic photos) -> processing (M10's [EpisodeProcessingPipeline]) -> persistence ->
 * sync (M9's [SyncCoordinator] pattern, extended to DiaryEntry + Episode) -> the unlock state
 * (M11's [resolveDayUnlockState]) flips from LOCKED to UNLOCKED only once both sides are SYNCED.
 */
class DiaryOfflineSyncIntegrationTest {
    private lateinit var database: AlongsideDatabase
    private lateinit var diaryEntryRepository: SyncingDiaryEntryRepository
    private lateinit var episodeRepository: SyncingEpisodeRepository
    private lateinit var coordinator: SyncCoordinator
    private val networkClient = RecordingSyncNetworkClient()
    private val remoteReader = FakeRemoteDocumentReader()
    private var nextOpId = 1

    @BeforeTest
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder<AlongsideDatabase>()
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        val localDiaryEntries = database.diaryEntryRepository()
        val localEpisodes = database.episodeRepository()
        diaryEntryRepository =
            SyncingDiaryEntryRepository(
                local = localDiaryEntries,
                store = database.syncOperationStore(),
                backgroundWorkScheduler = FakeBackgroundWorkScheduler(),
                clock = DiaryFixedClock,
                generateOpId = { "op-${nextOpId++}" },
            )
        episodeRepository =
            SyncingEpisodeRepository(
                local = localEpisodes,
                store = database.syncOperationStore(),
                backgroundWorkScheduler = FakeBackgroundWorkScheduler(),
                clock = DiaryFixedClock,
                generateOpId = { "op-${nextOpId++}" },
            )
        coordinator =
            SyncCoordinator(
                store = database.syncOperationStore(),
                processor = SyncQueueProcessor(networkClient, MaxAttemptsRetryPolicy(2)),
                remoteReader = remoteReader,
                bindings =
                    listOf(
                        DiaryEntrySyncEntityBinding(localDiaryEntries),
                        EpisodeSyncEntityBinding(localEpisodes),
                    ),
            )
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    private fun photo(
        id: String,
        offsetMinutes: Int,
    ) = Photo(
        id = id,
        uri = "content://photos/$id",
        takenAt = FIXED_NOW + offsetMinutes.minutes,
        latitude = 49.8397,
        longitude = 24.0297,
    )

    /** Capture -> processing (M10) -> persistence, independently for each side of the trip. */
    private suspend fun captureAndPersistBothSides(): Pair<List<Episode>, List<Episode>> {
        val pipeline =
            EpisodeProcessingPipeline(
                geocodingClient = FakeGeocodingClient(),
                visionDescriptionClient = FakeVisionClient(),
                imageBytesLoader = { byteArrayOf(1) },
                photoUploadClient = FakePhotoUploadClient(),
                clock = DiaryFixedClock,
            )
        val ownEpisodes =
            pipeline.process(
                diaryEntryId = "entry-own",
                photos = listOf(photo("own-1", 0), photo("own-2", 10)),
                languageTag = "en",
            )
        val partnerEpisodes =
            pipeline.process(
                diaryEntryId = "entry-partner",
                photos = listOf(photo("partner-1", 0)),
                languageTag = "en",
            )

        diaryEntryRepository.upsert(testDiaryEntry(id = "entry-own", userId = "owner-1"))
        ownEpisodes.forEach { episodeRepository.upsert(it) }
        diaryEntryRepository.upsert(testDiaryEntry(id = "entry-partner", userId = "member-1"))
        partnerEpisodes.forEach { episodeRepository.upsert(it) }

        return ownEpisodes to partnerEpisodes
    }

    private suspend fun assertUnlockState(expected: DayUnlockState) {
        val own = diaryEntryRepository.getById("entry-own")
        val partner = diaryEntryRepository.getById("entry-partner")
        assertEquals(expected, resolveDayUnlockState(diaryDayStatus(own, TODAY), diaryDayStatus(partner, TODAY)))
        assertEquals(expected == DayUnlockState.UNLOCKED, shouldTriggerPartnerReadyPush(own, partner, TODAY))
    }

    @Test
    fun `capture through processing through sync flips the day from locked to unlocked`() =
        runTest {
            val (ownEpisodes, partnerEpisodes) = captureAndPersistBothSides()

            // Pre-sync: neither DiaryEntry has been confirmed on the other device yet.
            assertUnlockState(DayUnlockState.LOCKED)

            // The network appears: the queue drains fully - 2 DiaryEntry ops, plus one Episode op
            // per side (both photo sets cluster into a single episode each, same time/place).
            val result = coordinator.sync()

            assertEquals(2 + ownEpisodes.size + partnerEpisodes.size, result.succeeded.size)
            assertTrue(database.syncOperationStore().loadAll().isEmpty())
            assertEquals(SyncStatus.SYNCED, diaryEntryRepository.getById("entry-own")?.syncStatus)
            assertEquals(SyncStatus.SYNCED, diaryEntryRepository.getById("entry-partner")?.syncStatus)
            val syncedOwn = database.episodeRepository().observeByDiaryEntry("entry-own").first()
            val syncedPartner = database.episodeRepository().observeByDiaryEntry("entry-partner").first()
            assertTrue((syncedOwn + syncedPartner).all { it.syncStatus == SyncStatus.SYNCED })

            // Post-sync: the day unlocks and the push condition fires.
            assertUnlockState(DayUnlockState.UNLOCKED)

            // Sanity: what got pushed really is diaryEntries/episodes, not misrouted.
            assertEquals(
                setOf(DiaryEntryFirestoreMapper.COLLECTION_PATH, EpisodeFirestoreMapper.COLLECTION_PATH),
                networkClient.pushed.map { it.collectionPath }.toSet(),
            )
        }

    @Test
    fun `synced episode fields carry the Storage remoteUrl, not the original content uri`() =
        runTest {
            captureAndPersistBothSides()

            coordinator.sync()

            val episodeOps = networkClient.pushed.filter { it.collectionPath == EpisodeFirestoreMapper.COLLECTION_PATH }
            val allPhotoValues =
                episodeOps.flatMap { op -> (op.fields["photos"] as FirestoreValue.ArrayValue).value.values }
            assertTrue(allPhotoValues.isNotEmpty())
            allPhotoValues.forEach { photoValue ->
                val photoFields = (photoValue as FirestoreValue.MapValue).value.fields
                val remoteUrl = (photoFields["remoteUrl"] as FirestoreValue.StringValue).value
                assertFalse(remoteUrl.startsWith("content://"))
                assertTrue(remoteUrl.startsWith("https://firebasestorage.googleapis.com/"))
            }
        }
}
