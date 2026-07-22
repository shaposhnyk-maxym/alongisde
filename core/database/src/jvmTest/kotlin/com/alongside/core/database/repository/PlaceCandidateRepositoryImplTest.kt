package com.alongside.core.database.repository

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.alongside.core.database.AlongsideDatabase
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.place.PlaceCandidate
import com.alongside.core.model.place.PlacePhoto
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
        photos: List<PlacePhoto> = emptyList(),
        rating: Double? = null,
        category: String? = null,
        city: String? = null,
    ) = PlaceCandidate(
        id = id,
        tripId = tripId,
        name = "Lviv Coffee Manufacture",
        latitude = 49.8397,
        longitude = 24.0297,
        note = null,
        addedByUserId = "owner-1",
        syncStatus = SyncStatus.PENDING,
        createdAt = Instant.fromEpochMilliseconds(1_752_600_000_000),
        updatedAt = Instant.fromEpochMilliseconds(1_752_600_000_000),
        photos = photos,
        rating = rating,
        category = category,
        city = city,
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
    fun `upsert then getById round trips photos rating category and city`() =
        runTest {
            val place =
                place(
                    photos = listOf(PlacePhoto(photoRef = "places/abc/photos/photo-1", remoteUrl = null)),
                    rating = 4.7,
                    category = "Gym",
                    city = "Lviv",
                )

            repository.upsert(place)

            assertEquals(place, repository.getById(place.id))
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
