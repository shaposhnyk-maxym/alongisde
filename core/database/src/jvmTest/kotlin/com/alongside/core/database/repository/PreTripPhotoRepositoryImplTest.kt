package com.alongside.core.database.repository

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.alongside.core.database.AlongsideDatabase
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.pretrip.PreTripPhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class PreTripPhotoRepositoryImplTest {
    private lateinit var database: AlongsideDatabase
    private lateinit var repository: PreTripPhotoRepositoryImpl

    @BeforeTest
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder<AlongsideDatabase>()
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        repository = PreTripPhotoRepositoryImpl(database)
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    private fun photo(
        id: String = "photo-1",
        tripId: String = "trip-1",
        userId: String = "owner-1",
        remoteUrl: String? = null,
        syncStatus: SyncStatus = SyncStatus.PENDING,
    ) = PreTripPhoto(
        id = id,
        tripId = tripId,
        userId = userId,
        uri = "content://photos/$id",
        takenAt = Instant.fromEpochMilliseconds(1_752_600_000_000),
        latitude = 49.8397,
        longitude = 24.0297,
        remoteUrl = remoteUrl,
        syncStatus = syncStatus,
    )

    @Test
    fun `upsert then getById returns the domain photo`() =
        runTest {
            val photo = photo()

            repository.upsert(photo)

            assertEquals(photo, repository.getById(photo.id))
        }

    @Test
    fun `getById returns null for unknown id`() =
        runTest {
            assertNull(repository.getById("unknown"))
        }

    @Test
    fun `re-upserting with a new syncStatus transitions the stored status`() =
        runTest {
            val photo = photo(syncStatus = SyncStatus.PENDING)
            repository.upsert(photo)

            val syncing = photo.copy(syncStatus = SyncStatus.SYNCING)
            repository.upsert(syncing)
            assertEquals(SyncStatus.SYNCING, repository.getById(photo.id)?.syncStatus)

            val synced = photo.copy(remoteUrl = "https://example.com/photo-1", syncStatus = SyncStatus.SYNCED)
            repository.upsert(synced)
            assertEquals(synced, repository.getById(photo.id))
        }

    @Test
    fun `delete removes the photo`() =
        runTest {
            val photo = photo()
            repository.upsert(photo)

            repository.delete(photo.id)

            assertNull(repository.getById(photo.id))
        }

    @Test
    fun `observeByTripAndUser emits the mapped domain photos scoped to trip and user`() =
        runTest {
            val mine = photo(id = "photo-1", tripId = "trip-1", userId = "owner-1")
            val otherUser = photo(id = "photo-2", tripId = "trip-1", userId = "member-1")

            repository.upsert(mine)
            repository.upsert(otherUser)

            assertEquals(listOf(mine), repository.observeByTripAndUser("trip-1", "owner-1").first())
        }
}
