package com.alongside.core.database.dao

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.alongside.core.database.AlongsideDatabase
import com.alongside.core.database.entity.PlaceCandidateEntity
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.place.PlacePhoto
import com.alongside.core.model.place.SwipeDirection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class PlaceCandidateDaoTest {
    private lateinit var database: AlongsideDatabase
    private lateinit var dao: PlaceCandidateDao

    @BeforeTest
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder<AlongsideDatabase>()
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        dao = database.placeCandidateDao()
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    private fun placeEntity(
        id: String = "place-1",
        tripId: String = "trip-1",
        ownerSwipe: SwipeDirection? = null,
        memberSwipe: SwipeDirection? = null,
    ) = PlaceCandidateEntity(
        id = id,
        tripId = tripId,
        name = "Lviv Coffee Manufacture",
        latitude = 49.8397,
        longitude = 24.0297,
        note = null,
        addedByUserId = "owner-1",
        ownerSwipe = ownerSwipe,
        memberSwipe = memberSwipe,
        syncStatus = SyncStatus.PENDING,
        createdAt = Instant.fromEpochMilliseconds(1_752_600_000_000),
        updatedAt = Instant.fromEpochMilliseconds(1_752_600_000_000),
    )

    @Test
    fun `insert then getById returns the inserted place`() =
        runTest {
            val place = placeEntity()

            dao.upsert(place)

            assertEquals(place, dao.getById(place.id))
        }

    @Test
    fun `getById returns null for unknown id`() =
        runTest {
            assertNull(dao.getById("unknown"))
        }

    @Test
    fun `upsert with existing id replaces existing row`() =
        runTest {
            val original = placeEntity(ownerSwipe = null, memberSwipe = SwipeDirection.LIKE)
            val replacement = original.copy(ownerSwipe = SwipeDirection.LIKE)

            dao.upsert(original)
            dao.upsert(replacement)

            assertEquals(replacement, dao.getById(original.id))
        }

    @Test
    fun `nullable swipe columns round trip correctly`() =
        runTest {
            val place = placeEntity(ownerSwipe = null, memberSwipe = SwipeDirection.LIKE)

            dao.upsert(place)

            val loaded = dao.getById(place.id)
            assertEquals(null, loaded?.ownerSwipe)
            assertEquals(SwipeDirection.LIKE, loaded?.memberSwipe)
        }

    @Test
    fun `photos rating category and city round trip correctly`() =
        runTest {
            val photos =
                listOf(
                    PlacePhoto(photoRef = "places/abc/photos/photo-1", remoteUrl = "https://storage/photo-1.jpg"),
                    PlacePhoto(photoRef = "places/abc/photos/photo-2", remoteUrl = null),
                )
            val place =
                placeEntity().copy(
                    photos = photos,
                    rating = 4.3,
                    category = "Restaurant",
                    city = "Lviv",
                )

            dao.upsert(place)

            val loaded = dao.getById(place.id)
            assertEquals(photos, loaded?.photos)
            assertEquals(4.3, loaded?.rating)
            assertEquals("Restaurant", loaded?.category)
            assertEquals("Lviv", loaded?.city)
        }

    @Test
    fun `delete removes the place`() =
        runTest {
            val place = placeEntity()
            dao.upsert(place)

            dao.delete(place.id)

            assertNull(dao.getById(place.id))
        }

    @Test
    fun `observeByTrip emits list updates on insert update and delete`() =
        runTest {
            val tripId = "trip-1"
            val place = placeEntity(tripId = tripId)
            val emissions = Channel<List<PlaceCandidateEntity>>(capacity = Channel.UNLIMITED)
            val job = launch { dao.observeByTrip(tripId).collect { emissions.send(it) } }

            assertEquals(emptyList(), emissions.receive())

            dao.upsert(place)
            assertEquals(listOf(place), emissions.receive())

            val updated = place.copy(ownerSwipe = SwipeDirection.LIKE)
            dao.upsert(updated)
            assertEquals(listOf(updated), emissions.receive())

            dao.delete(place.id)
            assertEquals(emptyList(), emissions.receive())

            job.cancel()
        }
}
