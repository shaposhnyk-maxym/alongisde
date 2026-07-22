package com.alongside.core.database.dao

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.alongside.core.database.AlongsideDatabase
import com.alongside.core.database.entity.PlaceSwipeEntity
import com.alongside.core.model.SyncStatus
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

class PlaceSwipeDaoTest {
    private lateinit var database: AlongsideDatabase
    private lateinit var dao: PlaceSwipeDao

    @BeforeTest
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder<AlongsideDatabase>()
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        dao = database.placeSwipeDao()
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    private fun swipeEntity(
        id: String = "place-1::owner-1",
        tripId: String = "trip-1",
        candidateId: String = "place-1",
        userId: String = "owner-1",
        direction: SwipeDirection = SwipeDirection.LIKE,
    ) = PlaceSwipeEntity(
        id = id,
        tripId = tripId,
        candidateId = candidateId,
        userId = userId,
        direction = direction,
        swipedAt = Instant.fromEpochMilliseconds(1_752_600_000_000),
        syncStatus = SyncStatus.PENDING,
        updatedAt = Instant.fromEpochMilliseconds(1_752_600_000_000),
    )

    @Test
    fun `upsert then getById returns the inserted swipe`() =
        runTest {
            val swipe = swipeEntity()

            dao.upsert(swipe)

            assertEquals(swipe, dao.getById(swipe.id))
        }

    @Test
    fun `getById returns null for unknown id`() =
        runTest {
            assertNull(dao.getById("unknown"))
        }

    @Test
    fun `re-swiping the same candidate overwrites the same user's own row, never anyone else's`() =
        runTest {
            val ownerSwipe = swipeEntity(id = "place-1::owner-1", userId = "owner-1", direction = SwipeDirection.LIKE)
            val memberSwipe = swipeEntity(id = "place-1::member-1", userId = "member-1")
            dao.upsert(ownerSwipe)
            dao.upsert(memberSwipe)

            val ownerReconsidered = ownerSwipe.copy(direction = SwipeDirection.DISLIKE)
            dao.upsert(ownerReconsidered)

            assertEquals(ownerReconsidered, dao.getById(ownerSwipe.id))
            assertEquals(memberSwipe, dao.getById(memberSwipe.id))
        }

    @Test
    fun `observeByTrip emits list updates on insert and update, scoped to the trip`() =
        runTest {
            val tripId = "trip-1"
            val swipe = swipeEntity(tripId = tripId)
            val otherTripSwipe = swipeEntity(id = "place-2::owner-1", tripId = "trip-2", candidateId = "place-2")
            val emissions = Channel<List<PlaceSwipeEntity>>(capacity = Channel.UNLIMITED)
            val job = launch { dao.observeByTrip(tripId).collect { emissions.send(it) } }

            assertEquals(emptyList(), emissions.receive())

            // Room's Flow invalidation is per-table, not per-WHERE-clause - a write to a
            // different trip's row still re-runs this query, just to the same (still empty) result.
            dao.upsert(otherTripSwipe)
            assertEquals(emptyList(), emissions.receive())

            dao.upsert(swipe)
            assertEquals(listOf(swipe), emissions.receive())

            val updated = swipe.copy(direction = SwipeDirection.DISLIKE)
            dao.upsert(updated)
            assertEquals(listOf(updated), emissions.receive())

            job.cancel()
        }
}
