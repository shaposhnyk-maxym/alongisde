package com.alongside.data.place

import com.alongside.core.database.sync.PersistedSyncOperationStatus
import com.alongside.core.database.sync.PersistedSyncOperationType
import com.alongside.core.domain.work.BackgroundJobKind
import com.alongside.core.model.SyncStatus
import com.alongside.data.FakeBackgroundWorkScheduler
import com.alongside.data.sync.InMemorySyncOperationStore
import com.alongside.data.sync.SyncOperationCodec
import com.alongside.data.testPlaceSwipe
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

private val FIXED_NOW = Instant.fromEpochMilliseconds(1_752_800_000_000)

private object SwipeSyncingClock : Clock {
    override fun now(): Instant = FIXED_NOW
}

class SyncingPlaceSwipeRepositoryTest {
    private val local = RecordingPlaceSwipeRepository()
    private val store = InMemorySyncOperationStore()
    private val backgroundWorkScheduler = FakeBackgroundWorkScheduler()
    private var nextOpId = 0
    private val repository =
        SyncingPlaceSwipeRepository(
            local = local,
            store = store,
            backgroundWorkScheduler = backgroundWorkScheduler,
            clock = SwipeSyncingClock,
            generateOpId = { "op-${++nextOpId}" },
        )

    @Test
    fun `upsert stamps updatedAt and PENDING before writing locally`() =
        runTest {
            val swipe = testPlaceSwipe(syncStatus = SyncStatus.SYNCED)

            repository.upsert(swipe)

            val stamped = swipe.copy(updatedAt = FIXED_NOW, syncStatus = SyncStatus.PENDING)
            assertEquals(listOf(stamped), local.upserted)
            assertEquals(stamped, repository.getById(swipe.id))
        }

    @Test
    fun `upsert appends a durable UPSERT operation carrying the stamped fields`() =
        runTest {
            val swipe = testPlaceSwipe()

            repository.upsert(swipe)

            val record = store.loadAll().single()
            assertEquals("op-1", record.id)
            assertEquals("placeSwipes", record.collectionPath)
            assertEquals(swipe.id, record.documentId)
            assertEquals(PersistedSyncOperationType.UPSERT, record.type)
            assertEquals(PersistedSyncOperationStatus.PENDING, record.status)
            assertEquals(FIXED_NOW, record.enqueuedAt)

            val stamped = swipe.copy(updatedAt = FIXED_NOW, syncStatus = SyncStatus.PENDING)
            assertEquals(
                PlaceSwipeFirestoreMapper.toFields(stamped),
                SyncOperationCodec.toOperation(record).fields,
            )
        }

    @Test
    fun `upsert schedules a SYNC_QUEUE_FLUSH backstop`() =
        runTest {
            repository.upsert(testPlaceSwipe())

            assertEquals(listOf(BackgroundJobKind.SYNC_QUEUE_FLUSH), backgroundWorkScheduler.scheduledOneOffs)
        }
}
