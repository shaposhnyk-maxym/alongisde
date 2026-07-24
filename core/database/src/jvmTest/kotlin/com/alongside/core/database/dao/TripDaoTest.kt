package com.alongside.core.database.dao

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.alongside.core.database.AlongsideDatabase
import com.alongside.core.database.entity.TripEntity
import com.alongside.core.model.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class TripDaoTest {
    private lateinit var database: AlongsideDatabase
    private lateinit var dao: TripDao

    @BeforeTest
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder<AlongsideDatabase>()
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        dao = database.tripDao()
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    private fun tripEntity(
        id: String = "trip-1",
        ownerId: String = "owner-1",
        memberId: String? = "member-1",
    ) = TripEntity(
        id = id,
        ownerId = ownerId,
        memberId = memberId,
        inviteCode = "ABC123",
        startDate = LocalDate(2026, 7, 16),
        endDate = LocalDate(2026, 7, 23),
        syncStatus = SyncStatus.PENDING,
        createdAt = Instant.fromEpochMilliseconds(1_752_600_000_000),
        updatedAt = Instant.fromEpochMilliseconds(1_752_600_000_000),
    )

    @Test
    fun `insert then getById returns the inserted trip`() =
        runTest {
            val trip = tripEntity()

            dao.upsert(trip)

            assertEquals(trip, dao.getById(trip.id))
        }

    @Test
    fun `getById returns null for unknown id`() =
        runTest {
            assertNull(dao.getById("unknown"))
        }

    @Test
    fun `upsert with existing id replaces existing row`() =
        runTest {
            val original = tripEntity(memberId = null)
            val replacement = original.copy(memberId = "member-2", inviteCode = "XYZ789")

            dao.upsert(original)
            dao.upsert(replacement)

            assertEquals(replacement, dao.getById(original.id))
        }

    @Test
    fun `delete removes the trip`() =
        runTest {
            val trip = tripEntity()
            dao.upsert(trip)

            dao.delete(trip.id)

            assertNull(dao.getById(trip.id))
        }

    @Test
    fun `getByUserId returns the most recently updated row when multiple rows match the same user`() =
        runTest {
            val older =
                tripEntity(id = "trip-old", ownerId = "user-1", memberId = null)
                    .copy(updatedAt = Instant.fromEpochMilliseconds(1_000))
            val newer =
                tripEntity(id = "trip-new", ownerId = "someone-else", memberId = "user-1")
                    .copy(updatedAt = Instant.fromEpochMilliseconds(2_000))
            dao.upsert(older)
            dao.upsert(newer)

            assertEquals(newer, dao.getByUserId("user-1"))
        }

    @Test
    fun `observeByUserId emits the most recently updated matching row after a newer trip is upserted`() =
        runTest {
            val older =
                tripEntity(id = "trip-old", ownerId = "user-1", memberId = null)
                    .copy(updatedAt = Instant.fromEpochMilliseconds(1_000))
            dao.upsert(older)

            val emissions = Channel<TripEntity?>(capacity = Channel.UNLIMITED)
            val job = launch { dao.observeByUserId("user-1").collect { emissions.send(it) } }
            assertEquals(older, emissions.receive())

            val newer =
                tripEntity(id = "trip-new", ownerId = "someone-else", memberId = "user-1")
                    .copy(updatedAt = Instant.fromEpochMilliseconds(2_000))
            dao.upsert(newer)
            assertEquals(newer, emissions.receive())

            job.cancel()
        }

    @Test
    fun `observeById emits on insert update and delete`() =
        runTest {
            val trip = tripEntity()
            val emissions = Channel<TripEntity?>(capacity = Channel.UNLIMITED)
            val job = launch { dao.observeById(trip.id).collect { emissions.send(it) } }

            assertEquals(null, emissions.receive())

            dao.upsert(trip)
            assertEquals(trip, emissions.receive())

            val updated = trip.copy(inviteCode = "NEW999")
            dao.upsert(updated)
            assertEquals(updated, emissions.receive())

            dao.delete(trip.id)
            assertEquals(null, emissions.receive())

            job.cancel()
        }
}
