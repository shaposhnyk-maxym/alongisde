package com.alongside.data.episode

import com.alongside.core.database.sync.PersistedSyncOperationStatus
import com.alongside.core.database.sync.PersistedSyncOperationType
import com.alongside.core.domain.work.BackgroundJobKind
import com.alongside.core.model.SyncStatus
import com.alongside.data.FakeBackgroundWorkScheduler
import com.alongside.data.sync.InMemorySyncOperationStore
import com.alongside.data.sync.SyncOperationCodec
import com.alongside.data.testEpisode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

private val FIXED_NOW = Instant.fromEpochMilliseconds(1_752_800_000_000)

private object FixedClock : Clock {
    override fun now(): Instant = FIXED_NOW
}

class SyncingEpisodeRepositoryTest {
    private val local = RecordingEpisodeRepository()
    private val store = InMemorySyncOperationStore()
    private val backgroundWorkScheduler = FakeBackgroundWorkScheduler()
    private var nextOpId = 0
    private val repository =
        SyncingEpisodeRepository(
            local = local,
            store = store,
            backgroundWorkScheduler = backgroundWorkScheduler,
            clock = FixedClock,
            generateOpId = { "op-${++nextOpId}" },
        )

    @Test
    fun `upsert stamps updatedAt and PENDING before writing locally`() =
        runTest {
            val episode = testEpisode(syncStatus = SyncStatus.SYNCED)

            repository.upsert(episode)

            val stamped = episode.copy(updatedAt = FIXED_NOW, syncStatus = SyncStatus.PENDING)
            assertEquals(listOf(stamped), local.upserted)
            assertEquals(stamped, repository.getById(episode.id))
        }

    @Test
    fun `upsert appends a durable UPSERT operation carrying the stamped fields`() =
        runTest {
            val episode = testEpisode()

            repository.upsert(episode)

            val record = store.loadAll().single()
            assertEquals("op-1", record.id)
            assertEquals("episodes", record.collectionPath)
            assertEquals("episode-1", record.documentId)
            assertEquals(PersistedSyncOperationType.UPSERT, record.type)
            assertEquals(PersistedSyncOperationStatus.PENDING, record.status)
            assertEquals(FIXED_NOW, record.enqueuedAt)

            val stamped = episode.copy(updatedAt = FIXED_NOW, syncStatus = SyncStatus.PENDING)
            assertEquals(EpisodeFirestoreMapper.toFields(stamped), SyncOperationCodec.toOperation(record).fields)
        }

    @Test
    fun `delete removes locally and appends a DELETE operation`() =
        runTest {
            repository.upsert(testEpisode())

            repository.delete("episode-1")

            assertEquals(listOf("episode-1"), local.deletedIds)
            val record = store.loadAll().last()
            assertEquals(PersistedSyncOperationType.DELETE, record.type)
            assertEquals("episode-1", record.documentId)
        }

    @Test
    fun `upsert and delete each schedule a SYNC_QUEUE_FLUSH backstop`() =
        runTest {
            repository.upsert(testEpisode())
            repository.delete("episode-1")

            assertEquals(
                listOf(BackgroundJobKind.SYNC_QUEUE_FLUSH, BackgroundJobKind.SYNC_QUEUE_FLUSH),
                backgroundWorkScheduler.scheduledOneOffs,
            )
        }
}
