package com.alongside.core.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.alongside.core.database.entity.AuthSessionEntity
import com.alongside.core.database.entity.DiaryEntryEntity
import com.alongside.core.database.entity.EpisodeEntity
import com.alongside.core.database.entity.PhotoEntity
import com.alongside.core.database.entity.PlaceCandidateEntity
import com.alongside.core.database.entity.PlaceSwipeEntity
import com.alongside.core.database.entity.PushTokenEntity
import com.alongside.core.database.entity.SyncOperationEntity
import com.alongside.core.database.entity.TripEntity
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.place.SwipeDirection
import com.alongside.core.model.push.PushPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class DatabaseCreationTest {
    private lateinit var database: AlongsideDatabase

    private val createdAt = Instant.fromEpochMilliseconds(1_752_600_000_000)

    @BeforeTest
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder<AlongsideDatabase>()
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun `trips table round trips a row from a freshly created database`() =
        runTest {
            val trip = trip()

            database.tripDao().upsert(trip)

            assertEquals(trip, database.tripDao().getById(trip.id))
        }

    @Test
    fun `diary entries table round trips a row from a freshly created database`() =
        runTest {
            val entry = diaryEntry()

            database.diaryEntryDao().upsert(entry)

            assertEquals(entry, database.diaryEntryDao().getById(entry.id))
        }

    @Test
    fun `episodes and photos tables round trip a row from a freshly created database`() =
        runTest {
            val episode = episode()
            val photo = photo(episode.id)

            database.episodeDao().upsert(episode, listOf(photo))

            val loaded = database.episodeDao().getById(episode.id)
            assertEquals(episode, loaded?.episode)
            assertEquals(listOf(photo), loaded?.photos)
        }

    @Test
    fun `place candidates table round trips a row from a freshly created database`() =
        runTest {
            val place = placeCandidate()

            database.placeCandidateDao().upsert(place)

            assertEquals(place, database.placeCandidateDao().getById(place.id))
        }

    @Test
    fun `place swipes table round trips a row from a freshly created database`() =
        runTest {
            val swipe = placeSwipe()

            database.placeSwipeDao().upsert(swipe)

            assertEquals(swipe, database.placeSwipeDao().getById(swipe.id))
        }

    @Test
    fun `push tokens table round trips a row from a freshly created database`() =
        runTest {
            val pushToken = pushToken()

            database.pushTokenDao().upsert(pushToken)

            assertEquals(pushToken, database.pushTokenDao().getById(pushToken.userId))
        }

    @Test
    fun `auth session table round trips a row from a freshly created database`() =
        runTest {
            val session = authSession()

            database.authSessionDao().upsert(session)

            assertEquals(session, database.authSessionDao().get())
        }

    @Test
    fun `sync operations table round trips a row from a freshly created database`() =
        runTest {
            val operation = syncOperation()

            database.syncOperationDao().insert(operation)

            assertEquals(listOf(operation.copy(seq = 1)), database.syncOperationDao().getAll())
        }

    private fun trip() =
        TripEntity(
            id = "trip-1",
            ownerId = "owner-1",
            memberId = "member-1",
            inviteCode = "ABC123",
            startDate = LocalDate(2026, 7, 16),
            endDate = LocalDate(2026, 7, 23),
            syncStatus = SyncStatus.PENDING,
            createdAt = createdAt,
            updatedAt = createdAt,
        )

    private fun diaryEntry() =
        DiaryEntryEntity(
            id = "entry-1",
            tripId = "trip-1",
            userId = "owner-1",
            date = LocalDate(2026, 7, 16),
            syncStatus = SyncStatus.PENDING,
            createdAt = createdAt,
            updatedAt = createdAt,
        )

    private fun episode() =
        EpisodeEntity(
            id = "episode-1",
            diaryEntryId = "entry-1",
            startTime = createdAt,
            endTime = createdAt,
            latitude = 49.8397,
            longitude = 24.0297,
            placeName = "Rynok Square",
            description = "Wandering the old town",
            descriptionAttempts = 0,
            syncStatus = SyncStatus.PENDING,
            updatedAt = createdAt,
        )

    private fun photo(episodeId: String) =
        PhotoEntity(
            id = "photo-1",
            episodeId = episodeId,
            uri = "content://photos/photo-1",
            takenAt = createdAt,
            latitude = 49.8397,
            longitude = 24.0297,
        )

    private fun placeCandidate() =
        PlaceCandidateEntity(
            id = "place-1",
            tripId = "trip-1",
            name = "Lviv Coffee Manufacture",
            latitude = 49.8397,
            longitude = 24.0297,
            note = null,
            addedByUserId = "owner-1",
            syncStatus = SyncStatus.PENDING,
            createdAt = createdAt,
            updatedAt = createdAt,
        )

    private fun placeSwipe() =
        PlaceSwipeEntity(
            id = "place-1::owner-1",
            tripId = "trip-1",
            candidateId = "place-1",
            userId = "owner-1",
            direction = SwipeDirection.LIKE,
            swipedAt = createdAt,
            syncStatus = SyncStatus.PENDING,
            updatedAt = createdAt,
        )

    private fun pushToken() =
        PushTokenEntity(
            userId = "owner-1",
            token = "fcm-token-1",
            platform = PushPlatform.ANDROID,
            syncStatus = SyncStatus.PENDING,
            updatedAt = createdAt,
        )

    private fun syncOperation() =
        SyncOperationEntity(
            opId = "op-1",
            collectionPath = "trips",
            documentId = "trip-1",
            type = "UPSERT",
            fieldsJson = """{"ownerId":{"stringValue":"owner-1"}}""",
            attempts = 0,
            status = "PENDING",
            enqueuedAt = createdAt,
        )

    private fun authSession() =
        AuthSessionEntity(
            uid = "owner-1",
            email = "owner@example.com",
            displayName = "Owner One",
            photoUrl = null,
            idToken = "id-token-1",
            refreshToken = null,
            expiresInSeconds = 3600L,
            issuedAt = createdAt,
        )
}
