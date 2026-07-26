package com.alongside.data.pretrip

import com.alongside.core.database.sync.PersistedSyncOperationStatus
import com.alongside.core.database.sync.PersistedSyncOperationType
import com.alongside.core.domain.work.BackgroundJobKind
import com.alongside.core.model.SyncStatus
import com.alongside.data.FakeBackgroundWorkScheduler
import com.alongside.data.sync.InMemorySyncOperationStore
import com.alongside.data.sync.SyncOperationCodec
import com.alongside.data.testPreTripPhoto
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

private val FIXED_NOW = Instant.fromEpochMilliseconds(1_752_800_000_000)

private object SyncingClock : Clock {
    override fun now(): Instant = FIXED_NOW
}

class SyncingPreTripPhotoRepositoryTest {
    private val local = RecordingPreTripPhotoRepository()
    private val store = InMemorySyncOperationStore()
    private val backgroundWorkScheduler = FakeBackgroundWorkScheduler()
    private var nextOpId = 0
    private val repository =
        SyncingPreTripPhotoRepository(
            local = local,
            store = store,
            backgroundWorkScheduler = backgroundWorkScheduler,
            clock = SyncingClock,
            generateOpId = { "op-${++nextOpId}" },
        )

    // Unlike every other SyncingXRepository, this does NOT stamp updatedAt - PreTripPhoto has no
    // such field (see the model's KDoc; deliberate since M19.5/M19.6).
    @Test
    fun `upsert stamps PENDING before writing locally`() =
        runTest {
            val photo = testPreTripPhoto(syncStatus = SyncStatus.SYNCED)

            repository.upsert(photo)

            val stamped = photo.copy(syncStatus = SyncStatus.PENDING)
            assertEquals(listOf(stamped), local.upserted)
            assertEquals(stamped, repository.getById(photo.id))
        }

    @Test
    fun `upsert appends a durable UPSERT operation carrying the stamped fields`() =
        runTest {
            val photo = testPreTripPhoto()

            repository.upsert(photo)

            val record = store.loadAll().single()
            assertEquals("op-1", record.id)
            assertEquals("preTripPhotos", record.collectionPath)
            assertEquals("photo-1", record.documentId)
            assertEquals(PersistedSyncOperationType.UPSERT, record.type)
            assertEquals(PersistedSyncOperationStatus.PENDING, record.status)
            assertEquals(FIXED_NOW, record.enqueuedAt)

            val stamped = photo.copy(syncStatus = SyncStatus.PENDING)
            assertEquals(
                PreTripPhotoFirestoreMapper.toFields(stamped),
                SyncOperationCodec.toOperation(record).fields,
            )
        }

    @Test
    fun `delete removes locally and appends a DELETE operation`() =
        runTest {
            repository.upsert(testPreTripPhoto())

            repository.delete("photo-1")

            assertEquals(listOf("photo-1"), local.deletedIds)
            val record = store.loadAll().last()
            assertEquals(PersistedSyncOperationType.DELETE, record.type)
            assertEquals("photo-1", record.documentId)
        }

    @Test
    fun `upsert and delete each schedule a SYNC_QUEUE_FLUSH backstop`() =
        runTest {
            repository.upsert(testPreTripPhoto())
            repository.delete("photo-1")

            assertEquals(
                listOf(BackgroundJobKind.SYNC_QUEUE_FLUSH, BackgroundJobKind.SYNC_QUEUE_FLUSH),
                backgroundWorkScheduler.scheduledOneOffs,
            )
        }
}
