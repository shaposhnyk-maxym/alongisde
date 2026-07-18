package com.alongside.core.database.repository

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.alongside.core.database.AlongsideDatabase
import com.alongside.core.database.sync.PersistedSyncOperation
import com.alongside.core.database.sync.PersistedSyncOperationStatus
import com.alongside.core.database.sync.PersistedSyncOperationType
import com.alongside.core.database.sync.SyncOperationStore
import com.alongside.core.database.syncOperationStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class SyncOperationStoreImplTest {
    private lateinit var database: AlongsideDatabase
    private lateinit var store: SyncOperationStore

    @BeforeTest
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder<AlongsideDatabase>()
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        store = database.syncOperationStore()
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    private fun record(
        id: String,
        status: PersistedSyncOperationStatus = PersistedSyncOperationStatus.PENDING,
    ) = PersistedSyncOperation(
        id = id,
        collectionPath = "trips",
        documentId = "doc-$id",
        type = PersistedSyncOperationType.UPSERT,
        fieldsJson = """{"ownerId":{"stringValue":"owner-1"}}""",
        attempts = 0,
        status = status,
        enqueuedAt = Instant.fromEpochMilliseconds(1_752_600_000_000),
    )

    @Test
    fun `append then loadAll round trips records in FIFO order`() =
        runTest {
            store.append(record("op-1"))
            store.append(record("op-2"))

            assertEquals(listOf(record("op-1"), record("op-2")), store.loadAll())
        }

    @Test
    fun `loadAll returns retry records alongside pending ones`() =
        runTest {
            store.append(record("op-1"))
            store.append(record("op-2"))
            store.markRetry("op-1", attempts = 3)

            val loaded = store.loadAll()
            assertEquals(listOf("op-1", "op-2"), loaded.map { it.id })
            assertEquals(PersistedSyncOperationStatus.RETRY, loaded.first().status)
            assertEquals(3, loaded.first().attempts)
        }

    @Test
    fun `remove drops only the named records`() =
        runTest {
            store.append(record("op-1"))
            store.append(record("op-2"))
            store.append(record("op-3"))

            store.remove(listOf("op-2"))

            assertEquals(listOf("op-1", "op-3"), store.loadAll().map { it.id })
        }
}
