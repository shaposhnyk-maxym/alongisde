package com.alongside.data.place

import com.alongside.core.database.sync.PersistedSyncOperationStatus
import com.alongside.core.database.sync.PersistedSyncOperationType
import com.alongside.core.domain.work.BackgroundJobKind
import com.alongside.core.model.SyncStatus
import com.alongside.data.FakeBackgroundWorkScheduler
import com.alongside.data.sync.InMemorySyncOperationStore
import com.alongside.data.sync.SyncOperationCodec
import com.alongside.data.testPlace
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

private val FIXED_NOW = Instant.fromEpochMilliseconds(1_752_800_000_000)

private object SyncingClock : Clock {
    override fun now(): Instant = FIXED_NOW
}

class SyncingPlaceCandidateRepositoryTest {
    private val local = RecordingPlaceCandidateRepository()
    private val store = InMemorySyncOperationStore()
    private val backgroundWorkScheduler = FakeBackgroundWorkScheduler()
    private var nextOpId = 0
    private val repository =
        SyncingPlaceCandidateRepository(
            local = local,
            store = store,
            backgroundWorkScheduler = backgroundWorkScheduler,
            clock = SyncingClock,
            generateOpId = { "op-${++nextOpId}" },
        )

    @Test
    fun `upsert stamps updatedAt and PENDING before writing locally`() =
        runTest {
            val place = testPlace(syncStatus = SyncStatus.SYNCED)

            repository.upsert(place)

            val stamped = place.copy(updatedAt = FIXED_NOW, syncStatus = SyncStatus.PENDING)
            assertEquals(listOf(stamped), local.upserted)
            assertEquals(stamped, repository.getById(place.id))
        }

    @Test
    fun `upsert appends a durable UPSERT operation carrying the stamped fields`() =
        runTest {
            val place = testPlace()

            repository.upsert(place)

            val record = store.loadAll().single()
            assertEquals("op-1", record.id)
            assertEquals("placeCandidates", record.collectionPath)
            assertEquals("place-1", record.documentId)
            assertEquals(PersistedSyncOperationType.UPSERT, record.type)
            assertEquals(PersistedSyncOperationStatus.PENDING, record.status)
            assertEquals(FIXED_NOW, record.enqueuedAt)

            val stamped = place.copy(updatedAt = FIXED_NOW, syncStatus = SyncStatus.PENDING)
            assertEquals(
                PlaceCandidateFirestoreMapper.toFields(stamped),
                SyncOperationCodec.toOperation(record).fields,
            )
        }

    @Test
    fun `delete removes locally and appends a DELETE operation`() =
        runTest {
            repository.upsert(testPlace())

            repository.delete("place-1")

            assertEquals(listOf("place-1"), local.deletedIds)
            val record = store.loadAll().last()
            assertEquals(PersistedSyncOperationType.DELETE, record.type)
            assertEquals("place-1", record.documentId)
        }

    @Test
    fun `upsert and delete each schedule a SYNC_QUEUE_FLUSH backstop`() =
        runTest {
            repository.upsert(testPlace())
            repository.delete("place-1")

            assertEquals(
                listOf(BackgroundJobKind.SYNC_QUEUE_FLUSH, BackgroundJobKind.SYNC_QUEUE_FLUSH),
                backgroundWorkScheduler.scheduledOneOffs,
            )
        }
}
