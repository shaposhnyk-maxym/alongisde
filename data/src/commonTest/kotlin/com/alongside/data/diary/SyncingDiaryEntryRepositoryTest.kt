package com.alongside.data.diary

import com.alongside.core.database.sync.PersistedSyncOperationStatus
import com.alongside.core.database.sync.PersistedSyncOperationType
import com.alongside.core.domain.work.BackgroundJobKind
import com.alongside.core.model.SyncStatus
import com.alongside.data.FakeBackgroundWorkScheduler
import com.alongside.data.sync.InMemorySyncOperationStore
import com.alongside.data.sync.SyncOperationCodec
import com.alongside.data.testDiaryEntry
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

private val FIXED_NOW = Instant.fromEpochMilliseconds(1_752_800_000_000)

private object FixedClock : Clock {
    override fun now(): Instant = FIXED_NOW
}

class SyncingDiaryEntryRepositoryTest {
    private val local = RecordingDiaryEntryRepository()
    private val store = InMemorySyncOperationStore()
    private val backgroundWorkScheduler = FakeBackgroundWorkScheduler()
    private var nextOpId = 0
    private val repository =
        SyncingDiaryEntryRepository(
            local = local,
            store = store,
            backgroundWorkScheduler = backgroundWorkScheduler,
            clock = FixedClock,
            generateOpId = { "op-${++nextOpId}" },
        )

    @Test
    fun `upsert stamps updatedAt and PENDING before writing locally`() =
        runTest {
            val entry = testDiaryEntry(syncStatus = SyncStatus.SYNCED)

            repository.upsert(entry)

            val stamped = entry.copy(updatedAt = FIXED_NOW, syncStatus = SyncStatus.PENDING)
            assertEquals(listOf(stamped), local.upserted)
            assertEquals(stamped, repository.getById(entry.id))
        }

    @Test
    fun `upsert appends a durable UPSERT operation carrying the stamped fields`() =
        runTest {
            val entry = testDiaryEntry()

            repository.upsert(entry)

            val record = store.loadAll().single()
            assertEquals("op-1", record.id)
            assertEquals("diaryEntries", record.collectionPath)
            assertEquals("entry-1", record.documentId)
            assertEquals(PersistedSyncOperationType.UPSERT, record.type)
            assertEquals(PersistedSyncOperationStatus.PENDING, record.status)
            assertEquals(FIXED_NOW, record.enqueuedAt)

            val stamped = entry.copy(updatedAt = FIXED_NOW, syncStatus = SyncStatus.PENDING)
            assertEquals(DiaryEntryFirestoreMapper.toFields(stamped), SyncOperationCodec.toOperation(record).fields)
        }

    @Test
    fun `delete removes locally and appends a DELETE operation`() =
        runTest {
            repository.upsert(testDiaryEntry())

            repository.delete("entry-1")

            assertEquals(listOf("entry-1"), local.deletedIds)
            val record = store.loadAll().last()
            assertEquals(PersistedSyncOperationType.DELETE, record.type)
            assertEquals("entry-1", record.documentId)
        }

    @Test
    fun `upsert and delete each schedule a SYNC_QUEUE_FLUSH backstop`() =
        runTest {
            repository.upsert(testDiaryEntry())
            repository.delete("entry-1")

            assertEquals(
                listOf(BackgroundJobKind.SYNC_QUEUE_FLUSH, BackgroundJobKind.SYNC_QUEUE_FLUSH),
                backgroundWorkScheduler.scheduledOneOffs,
            )
        }
}
