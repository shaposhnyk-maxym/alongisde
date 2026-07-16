package com.alongside.core.database.repository

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.alongside.core.database.AlongsideDatabase
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.trip.Trip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class TripRepositoryImplTest {
    private lateinit var database: AlongsideDatabase
    private lateinit var repository: TripRepositoryImpl

    @BeforeTest
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder<AlongsideDatabase>()
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        repository = TripRepositoryImpl(database)
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    private fun trip(id: String = "trip-1") =
        Trip(
            id = id,
            ownerId = "owner-1",
            memberId = "member-1",
            inviteCode = "ABC123",
            startDate = LocalDate(2026, 7, 16),
            endDate = LocalDate(2026, 7, 23),
            syncStatus = SyncStatus.PENDING,
            createdAt = Instant.fromEpochMilliseconds(1_752_600_000_000),
        )

    @Test
    fun `upsert then getById returns the domain trip`() =
        runTest {
            val trip = trip()

            repository.upsert(trip)

            assertEquals(trip, repository.getById(trip.id))
        }

    @Test
    fun `getById returns null for unknown id`() =
        runTest {
            assertNull(repository.getById("unknown"))
        }

    @Test
    fun `delete removes the trip`() =
        runTest {
            val trip = trip()
            repository.upsert(trip)

            repository.delete(trip.id)

            assertNull(repository.getById(trip.id))
        }

    @Test
    fun `observeById emits the mapped domain trip`() =
        runTest {
            val trip = trip()

            repository.upsert(trip)

            assertEquals(trip, repository.observeById(trip.id).first())
        }
}
