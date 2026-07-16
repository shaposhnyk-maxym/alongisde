package com.alongside.core.database.repository

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.alongside.core.database.AlongsideDatabase
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.place.PlaceCandidate
import com.alongside.core.model.place.SwipeDirection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class PlaceCandidateRepositoryImplTest {
    private lateinit var database: AlongsideDatabase
    private lateinit var repository: PlaceCandidateRepositoryImpl

    @BeforeTest
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder<AlongsideDatabase>()
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        repository = PlaceCandidateRepositoryImpl(database)
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    private fun place(
        id: String = "place-1",
        tripId: String = "trip-1",
    ) = PlaceCandidate(
        id = id,
        tripId = tripId,
        name = "Lviv Coffee Manufacture",
        latitude = 49.8397,
        longitude = 24.0297,
        note = null,
        addedByUserId = "owner-1",
        ownerSwipe = SwipeDirection.LIKE,
        memberSwipe = null,
        syncStatus = SyncStatus.PENDING,
        createdAt = Instant.fromEpochMilliseconds(1_752_600_000_000),
    )

    @Test
    fun `upsert then getById returns the domain place`() =
        runTest {
            val place = place()

            repository.upsert(place)

            assertEquals(place, repository.getById(place.id))
        }

    @Test
    fun `getById returns null for unknown id`() =
        runTest {
            assertNull(repository.getById("unknown"))
        }

    @Test
    fun `delete removes the place`() =
        runTest {
            val place = place()
            repository.upsert(place)

            repository.delete(place.id)

            assertNull(repository.getById(place.id))
        }

    @Test
    fun `observeByTrip emits the mapped domain places`() =
        runTest {
            val place = place()

            repository.upsert(place)

            assertEquals(listOf(place), repository.observeByTrip(place.tripId).first())
        }
}
