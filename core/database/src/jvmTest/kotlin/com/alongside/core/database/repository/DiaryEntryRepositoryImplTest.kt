package com.alongside.core.database.repository

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.alongside.core.database.AlongsideDatabase
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.diary.DiaryEntry
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

class DiaryEntryRepositoryImplTest {
    private lateinit var database: AlongsideDatabase
    private lateinit var repository: DiaryEntryRepositoryImpl

    @BeforeTest
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder<AlongsideDatabase>()
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        repository = DiaryEntryRepositoryImpl(database)
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    private fun entry(
        id: String = "entry-1",
        tripId: String = "trip-1",
    ) = DiaryEntry(
        id = id,
        tripId = tripId,
        userId = "owner-1",
        date = LocalDate(2026, 7, 16),
        syncStatus = SyncStatus.PENDING,
        createdAt = Instant.fromEpochMilliseconds(1_752_600_000_000),
    )

    @Test
    fun `upsert then getById returns the domain entry`() =
        runTest {
            val entry = entry()

            repository.upsert(entry)

            assertEquals(entry, repository.getById(entry.id))
        }

    @Test
    fun `getById returns null for unknown id`() =
        runTest {
            assertNull(repository.getById("unknown"))
        }

    @Test
    fun `delete removes the entry`() =
        runTest {
            val entry = entry()
            repository.upsert(entry)

            repository.delete(entry.id)

            assertNull(repository.getById(entry.id))
        }

    @Test
    fun `observeByTrip emits the mapped domain entries`() =
        runTest {
            val entry = entry()

            repository.upsert(entry)

            assertEquals(listOf(entry), repository.observeByTrip(entry.tripId).first())
        }
}
