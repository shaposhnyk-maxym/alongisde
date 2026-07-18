package com.alongside.core.database.dao

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.alongside.core.database.AlongsideDatabase
import com.alongside.core.database.entity.SyncOperationEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class SyncOperationDaoTest {
    private lateinit var database: AlongsideDatabase
    private lateinit var dao: SyncOperationDao

    @BeforeTest
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder<AlongsideDatabase>()
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        dao = database.syncOperationDao()
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    private fun operation(opId: String) =
        SyncOperationEntity(
            opId = opId,
            collectionPath = "trips",
            documentId = "doc-$opId",
            type = "UPSERT",
            fieldsJson = "{}",
            attempts = 0,
            status = "PENDING",
            enqueuedAt = Instant.fromEpochMilliseconds(1_752_600_000_000),
        )

    @Test
    fun `getAll returns operations in insertion order`() =
        runTest {
            dao.insert(operation("op-1"))
            dao.insert(operation("op-2"))
            dao.insert(operation("op-3"))

            assertEquals(listOf("op-1", "op-2", "op-3"), dao.getAll().map { it.opId })
        }

    @Test
    fun `deleteByOpIds removes only the named operations`() =
        runTest {
            dao.insert(operation("op-1"))
            dao.insert(operation("op-2"))
            dao.insert(operation("op-3"))

            dao.deleteByOpIds(listOf("op-1", "op-3"))

            assertEquals(listOf("op-2"), dao.getAll().map { it.opId })
        }

    @Test
    fun `updateStatus rewrites status and attempts in place keeping queue order`() =
        runTest {
            dao.insert(operation("op-1"))
            dao.insert(operation("op-2"))

            dao.updateStatus("op-1", "RETRY", attempts = 5)

            val all = dao.getAll()
            assertEquals(listOf("op-1", "op-2"), all.map { it.opId })
            assertEquals("RETRY", all.first().status)
            assertEquals(5, all.first().attempts)
            assertEquals("PENDING", all.last().status)
        }
}
