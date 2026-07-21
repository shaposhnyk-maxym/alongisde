package com.alongside.androidapp.work

import android.app.Application
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.alongside.core.database.sync.PersistedSyncOperation
import com.alongside.core.database.sync.SyncOperationStore
import com.alongside.core.domain.auth.AuthSessionCache
import com.alongside.core.domain.diary.DiaryEntryRepository
import com.alongside.core.domain.diary.EpisodeRepository
import com.alongside.core.domain.diary.processing.EpisodeProcessingPipeline
import com.alongside.core.domain.diary.processing.EpisodeVisionDescriptionClient
import com.alongside.core.domain.diary.processing.GeocodingResult
import com.alongside.core.domain.diary.processing.PhotoUploadClient
import com.alongside.core.domain.diary.processing.PhotoUploadResult
import com.alongside.core.domain.diary.processing.PlaceGeocodingClient
import com.alongside.core.domain.diary.processing.VisionDescriptionResult
import com.alongside.core.domain.pairing.JoinTripResult
import com.alongside.core.domain.pairing.PairingRepository
import com.alongside.core.domain.place.PlaceCandidateRepository
import com.alongside.core.domain.place.importing.PlaceDetailsLookupClient
import com.alongside.core.domain.place.importing.PlaceDetailsResult
import com.alongside.core.domain.place.importing.PlaceImportPipeline
import com.alongside.core.domain.place.importing.PlaceLookupQuery
import com.alongside.core.domain.place.importing.PlacePhotoClient
import com.alongside.core.domain.place.importing.PlacePhotoUploadClient
import com.alongside.core.domain.place.importing.PlacePhotoUploadResult
import com.alongside.core.domain.place.importing.ShareLinkRedirectResolver
import com.alongside.core.domain.place.importing.ShareLinkRedirectResult
import com.alongside.core.domain.work.BackgroundJobKind
import com.alongside.core.domain.work.BackgroundWorkScheduler
import com.alongside.core.model.auth.AuthSession
import com.alongside.core.model.auth.AuthUser
import com.alongside.core.model.diary.DiaryEntry
import com.alongside.core.model.diary.Episode
import com.alongside.core.model.diary.Photo
import com.alongside.core.model.place.PlaceCandidate
import com.alongside.core.model.trip.Trip
import com.alongside.core.network.queue.SyncNetworkClient
import com.alongside.core.network.queue.SyncOperation
import com.alongside.core.network.queue.SyncQueueProcessor
import com.alongside.core.network.queue.SyncResult
import com.alongside.data.sync.RemoteDocumentReader
import com.alongside.data.sync.SyncCoordinator
import com.alongside.feature.diary.capture.ExifPhotoReader
import com.alongside.feature.diary.presentation.DiaryCaptureCoordinator
import com.alongside.feature.places.presentation.PlaceRetryCoordinator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.time.Instant

// Every stub below throws if actually invoked - each test only ever exercises the very first
// dependency each coordinator/SyncCoordinator touches (PairingRepository.observeActiveTrip /
// SyncOperationStore.loadAll), which short-circuits on a null trip / empty queue before anything
// downstream is ever called. That's what makes a bare "was dispatch correct" check possible
// without re-testing the coordinators' own retry logic (already covered by
// DiaryCaptureCoordinatorTest/PlaceRetryCoordinatorTest/Syncing*RepositoryTest).
private fun unusedExifPhotoReader() =
    object : ExifPhotoReader {
        override suspend fun readExifPhotos(uris: List<String>): List<Photo> = error("not reached in this test")
    }

private fun unusedEpisodeProcessingPipeline() =
    EpisodeProcessingPipeline(
        geocodingClient =
            object : PlaceGeocodingClient {
                override suspend fun reverseGeocode(
                    latitude: Double,
                    longitude: Double,
                ): GeocodingResult = error("not reached in this test")
            },
        visionDescriptionClient =
            object : EpisodeVisionDescriptionClient {
                override suspend fun describeEpisode(
                    images: List<ByteArray>,
                    placeName: String?,
                    languageTag: String,
                ): VisionDescriptionResult = error("not reached in this test")
            },
        imageBytesLoader = { error("not reached in this test") },
        photoUploadClient =
            object : PhotoUploadClient {
                override suspend fun upload(
                    photo: Photo,
                    bytes: ByteArray,
                ): PhotoUploadResult = error("not reached in this test")
            },
    )

private fun unusedDiaryEntryRepository() =
    object : DiaryEntryRepository {
        override suspend fun upsert(entry: DiaryEntry) = error("not reached in this test")

        override suspend fun getById(id: String): DiaryEntry? = error("not reached in this test")

        override fun observeByTrip(tripId: String): Flow<List<DiaryEntry>> = error("not reached in this test")

        override suspend fun delete(id: String) = error("not reached in this test")
    }

private fun unusedEpisodeRepository() =
    object : EpisodeRepository {
        override suspend fun upsert(episode: Episode) = error("not reached in this test")

        override suspend fun getById(id: String): Episode? = error("not reached in this test")

        override fun observeByDiaryEntry(diaryEntryId: String): Flow<List<Episode>> = error("not reached in this test")

        override suspend fun delete(id: String) = error("not reached in this test")
    }

private fun unusedPlaceCandidateRepository() =
    object : PlaceCandidateRepository {
        override suspend fun upsert(place: PlaceCandidate) = error("not reached in this test")

        override suspend fun getById(id: String): PlaceCandidate? = error("not reached in this test")

        override fun observeByTrip(tripId: String): Flow<List<PlaceCandidate>> = error("not reached in this test")

        override suspend fun delete(id: String) = error("not reached in this test")
    }

private fun unusedPlaceImportPipeline() =
    PlaceImportPipeline(
        redirectResolver =
            object : ShareLinkRedirectResolver {
                override suspend fun resolve(shortUrl: String): ShareLinkRedirectResult = error("unused")
            },
        detailsLookupClient =
            object : PlaceDetailsLookupClient {
                override suspend fun lookup(query: PlaceLookupQuery): PlaceDetailsResult = error("unused")
            },
        photoClient =
            object : PlacePhotoClient {
                override suspend fun fetchPhotoBytes(photoRef: String): ByteArray? = error("not reached in this test")
            },
        photoUploadClient =
            object : PlacePhotoUploadClient {
                override suspend fun upload(
                    placeCandidateId: String,
                    photoIndex: Int,
                    bytes: ByteArray,
                ): PlacePhotoUploadResult = error("not reached in this test")
            },
        placeGeocodingClient =
            object : PlaceGeocodingClient {
                override suspend fun reverseGeocode(
                    latitude: Double,
                    longitude: Double,
                ): GeocodingResult = error("not reached in this test")
            },
    )

private fun unusedSyncNetworkClient() =
    object : SyncNetworkClient {
        override suspend fun push(operation: SyncOperation): SyncResult = error("not reached in this test")
    }

private fun unusedBackgroundWorkScheduler() =
    object : BackgroundWorkScheduler {
        override fun scheduleOneOff(kind: BackgroundJobKind) = error("not used by this test")

        override fun ensurePeriodicSweepScheduled() = error("not used by this test")
    }

/** Records the queried userId (if any), always resolving to "no active trip" - see file kdoc. */
private class SpyPairingRepository : PairingRepository {
    var queriedUserId: String? = null

    override suspend fun createTrip(
        ownerId: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Trip = error("not used by this test")

    override suspend fun joinTrip(
        code: String,
        userId: String,
    ): JoinTripResult = error("not used by this test")

    override fun observeActiveTrip(userId: String): Flow<Trip?> {
        queriedUserId = userId
        return flowOf(null)
    }
}

/** Records whether the queue was ever read, always resolving to "nothing pending". */
private class SpySyncOperationStore : SyncOperationStore {
    var loadAllCalled = false

    override suspend fun append(record: PersistedSyncOperation) = error("not used by this test")

    override suspend fun loadAll(): List<PersistedSyncOperation> {
        loadAllCalled = true
        return emptyList()
    }

    override suspend fun remove(ids: List<String>) = error("not used by this test")

    override suspend fun markRetry(
        id: String,
        attempts: Int,
    ) = error("not used by this test")
}

private class FakeAuthSessionCache(
    private val session: AuthSession?,
) : AuthSessionCache {
    override suspend fun get(): AuthSession? = session

    override suspend fun save(session: AuthSession) = error("not used by this test")

    override suspend fun clear() = error("not used by this test")
}

private fun testAuthSession(uid: String) =
    AuthSession(
        user = AuthUser(uid = uid, email = null, displayName = null, photoUrl = null),
        idToken = "id-token",
        refreshToken = null,
        expiresInSeconds = 3600,
        issuedAt = Instant.fromEpochMilliseconds(0),
    )

// AlongsideApplication.onCreate() would otherwise start Koin for real before this test's own
// @Before runs (Robolectric instantiates the manifest-declared Application during environment
// setup) - a plain stub Application here avoids that collision entirely.
@Config(application = Application::class)
@RunWith(RobolectricTestRunner::class)
class BackgroundSyncWorkerTest {
    private val diaryPairingRepository = SpyPairingRepository()
    private val placePairingRepository = SpyPairingRepository()
    private val syncOperationStore = SpySyncOperationStore()

    private val diaryCaptureCoordinator =
        DiaryCaptureCoordinator(
            diaryEntryRepository = unusedDiaryEntryRepository(),
            episodeRepository = unusedEpisodeRepository(),
            processingPipeline = unusedEpisodeProcessingPipeline(),
            exifPhotoReader = unusedExifPhotoReader(),
            pairingRepository = diaryPairingRepository,
            backgroundWorkScheduler = unusedBackgroundWorkScheduler(),
        )

    private val placeRetryCoordinator =
        PlaceRetryCoordinator(
            pairingRepository = placePairingRepository,
            placeCandidateRepository = unusedPlaceCandidateRepository(),
            pipeline = unusedPlaceImportPipeline(),
        )

    private val syncCoordinator =
        SyncCoordinator(
            store = syncOperationStore,
            processor = SyncQueueProcessor(networkClient = unusedSyncNetworkClient()),
            remoteReader = RemoteDocumentReader { _, _ -> error("not used by this test") },
            bindings = emptyList(),
        )

    private lateinit var authSessionCache: FakeAuthSessionCache

    private fun restartKoinWith(session: AuthSession?) {
        stopKoin()
        authSessionCache = FakeAuthSessionCache(session)
        startKoin {
            modules(
                module {
                    single<AuthSessionCache> { authSessionCache }
                    single { diaryCaptureCoordinator }
                    single { placeRetryCoordinator }
                    single { syncCoordinator }
                },
            )
        }
    }

    @Before
    fun setUp() {
        stopKoin()
        restartKoinWith(testAuthSession("uid-1"))
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    private fun worker(inputData: Data = Data.EMPTY): BackgroundSyncWorker =
        TestListenableWorkerBuilder<BackgroundSyncWorker>(RuntimeEnvironment.getApplication())
            .setInputData(inputData)
            .build()

    @Test
    fun `EPISODE_RETRY dispatches to the diary coordinator only`() =
        runBlocking {
            val result = worker(workDataOf(KEY_JOB_KIND to BackgroundJobKind.EPISODE_RETRY.name)).doWork()

            assertEquals(ListenableWorker.Result.success(), result)
            assertEquals("uid-1", diaryPairingRepository.queriedUserId)
            assertEquals(null, placePairingRepository.queriedUserId)
            assertFalse(syncOperationStore.loadAllCalled)
        }

    @Test
    fun `PLACE_RETRY dispatches to the place coordinator only`() =
        runBlocking {
            val result = worker(workDataOf(KEY_JOB_KIND to BackgroundJobKind.PLACE_RETRY.name)).doWork()

            assertEquals(ListenableWorker.Result.success(), result)
            assertEquals("uid-1", placePairingRepository.queriedUserId)
            assertEquals(null, diaryPairingRepository.queriedUserId)
            assertFalse(syncOperationStore.loadAllCalled)
        }

    @Test
    fun `SYNC_QUEUE_FLUSH dispatches to the sync coordinator only`() =
        runBlocking {
            val result = worker(workDataOf(KEY_JOB_KIND to BackgroundJobKind.SYNC_QUEUE_FLUSH.name)).doWork()

            assertEquals(ListenableWorker.Result.success(), result)
            assertTrue(syncOperationStore.loadAllCalled)
            assertEquals(null, diaryPairingRepository.queriedUserId)
            assertEquals(null, placePairingRepository.queriedUserId)
        }

    @Test
    fun `no job kind runs a periodic sweep across all three kinds`() =
        runBlocking {
            val result = worker().doWork()

            assertEquals(ListenableWorker.Result.success(), result)
            assertEquals("uid-1", diaryPairingRepository.queriedUserId)
            assertEquals("uid-1", placePairingRepository.queriedUserId)
            assertTrue(syncOperationStore.loadAllCalled)
        }

    @Test
    fun `no signed-in user resolves to success without touching any coordinator`() =
        runBlocking {
            restartKoinWith(session = null)

            val result = worker(workDataOf(KEY_JOB_KIND to BackgroundJobKind.EPISODE_RETRY.name)).doWork()

            assertEquals(ListenableWorker.Result.success(), result)
            assertEquals(null, diaryPairingRepository.queriedUserId)
        }
}
