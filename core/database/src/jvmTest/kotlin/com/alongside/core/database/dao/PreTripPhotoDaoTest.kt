package com.alongside.core.database.dao

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.alongside.core.database.AlongsideDatabase
import com.alongside.core.database.entity.PreTripPhotoEntity
import com.alongside.core.model.SyncStatus
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

class PreTripPhotoDaoTest {
    private lateinit var database: AlongsideDatabase
    private lateinit var dao: PreTripPhotoDao

    @BeforeTest
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder<AlongsideDatabase>()
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        dao = database.preTripPhotoDao()
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    private fun photoEntity(
        id: String = "photo-1",
        tripId: String = "trip-1",
        userId: String = "owner-1",
        uri: String = "content://photos/photo-1",
        remoteUrl: String? = null,
        syncStatus: SyncStatus = SyncStatus.PENDING,
    ) = PreTripPhotoEntity(
        id = id,
        tripId = tripId,
        userId = userId,
        uri = uri,
        takenAt = Instant.fromEpochMilliseconds(1_752_600_000_000),
        latitude = 49.8397,
        longitude = 24.0297,
        remoteUrl = remoteUrl,
        syncStatus = syncStatus,
    )

    @Test
    fun `upsert then getById returns the inserted photo`() =
        runTest {
            val photo = photoEntity()

            dao.upsert(photo)

            assertEquals(photo, dao.getById(photo.id))
        }

    @Test
    fun `getById returns null for unknown id`() =
        runTest {
            assertNull(dao.getById("unknown"))
        }

    @Test
    fun `upsert replaces the row for the same id`() =
        runTest {
            val photo = photoEntity(syncStatus = SyncStatus.PENDING)
            dao.upsert(photo)

            val synced = photo.copy(remoteUrl = "https://example.com/photo-1", syncStatus = SyncStatus.SYNCED)
            dao.upsert(synced)

            assertEquals(synced, dao.getById(photo.id))
        }

    @Test
    fun `delete removes the photo`() =
        runTest {
            val photo = photoEntity()
            dao.upsert(photo)

            dao.delete(photo.id)

            assertNull(dao.getById(photo.id))
        }

    @Test
    fun `observeByTripAndUser only returns rows for that trip and user`() =
        runTest {
            val mine = photoEntity(id = "photo-1", tripId = "trip-1", userId = "owner-1")
            val otherUser = photoEntity(id = "photo-2", tripId = "trip-1", userId = "member-1")
            val otherTrip = photoEntity(id = "photo-3", tripId = "trip-2", userId = "owner-1")
            val emissions = Channel<List<PreTripPhotoEntity>>(capacity = Channel.UNLIMITED)
            val job = launch { dao.observeByTripAndUser("trip-1", "owner-1").collect { emissions.send(it) } }

            assertEquals(emptyList(), emissions.receive())

            dao.upsert(otherUser)
            assertEquals(emptyList(), emissions.receive())

            dao.upsert(otherTrip)
            assertEquals(emptyList(), emissions.receive())

            dao.upsert(mine)
            assertEquals(listOf(mine), emissions.receive())

            job.cancel()
        }
}
