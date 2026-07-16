package com.alongside.core.database.dao

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.alongside.core.database.AlongsideDatabase
import com.alongside.core.database.entity.DiaryEntryEntity
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

class DiaryEntryDaoTest {
    private lateinit var database: AlongsideDatabase
    private lateinit var dao: DiaryEntryDao

    @BeforeTest
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder<AlongsideDatabase>()
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        dao = database.diaryEntryDao()
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    private fun entryEntity(
        id: String = "entry-1",
        tripId: String = "trip-1",
        userId: String = "owner-1",
    ) = DiaryEntryEntity(
        id = id,
        tripId = tripId,
        userId = userId,
        date = LocalDate(2026, 7, 16),
        syncStatus = SyncStatus.PENDING,
        createdAt = Instant.fromEpochMilliseconds(1_752_600_000_000),
    )

    @Test
    fun `insert then getById returns the inserted entry`() =
        runTest {
            val entry = entryEntity()

            dao.upsert(entry)

            assertEquals(entry, dao.getById(entry.id))
        }

    @Test
    fun `getById returns null for unknown id`() =
        runTest {
            assertNull(dao.getById("unknown"))
        }

    @Test
    fun `upsert with existing id replaces existing row`() =
        runTest {
            val original = entryEntity()
            val replacement = original.copy(syncStatus = SyncStatus.SYNCED)

            dao.upsert(original)
            dao.upsert(replacement)

            assertEquals(replacement, dao.getById(original.id))
        }

    @Test
    fun `delete removes the entry`() =
        runTest {
            val entry = entryEntity()
            dao.upsert(entry)

            dao.delete(entry.id)

            assertNull(dao.getById(entry.id))
        }

    @Test
    fun `observeByTrip emits list updates on insert update and delete`() =
        runTest {
            val tripId = "trip-1"
            val entryA = entryEntity(id = "entry-a", tripId = tripId)
            val entryB = entryEntity(id = "entry-b", tripId = tripId)
            val emissions = Channel<List<DiaryEntryEntity>>(capacity = Channel.UNLIMITED)
            val job = launch { dao.observeByTrip(tripId).collect { emissions.send(it) } }

            assertEquals(emptyList(), emissions.receive())

            dao.upsert(entryA)
            assertEquals(listOf(entryA), emissions.receive())

            dao.upsert(entryB)
            assertEquals(setOf(entryA, entryB), emissions.receive().toSet())

            dao.delete(entryA.id)
            assertEquals(listOf(entryB), emissions.receive())

            job.cancel()
        }

    @Test
    fun `observeByTrip does not include entries from other trips`() =
        runTest {
            val entryOtherTrip = entryEntity(id = "entry-other", tripId = "trip-2")
            dao.upsert(entryOtherTrip)

            val emissions = Channel<List<DiaryEntryEntity>>(capacity = Channel.UNLIMITED)
            val job = launch { dao.observeByTrip("trip-1").collect { emissions.send(it) } }

            assertEquals(emptyList(), emissions.receive())

            job.cancel()
        }
}
