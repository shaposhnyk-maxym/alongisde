package com.alongside.core.database.dao

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.alongside.core.database.AlongsideDatabase
import com.alongside.core.database.entity.PushTokenEntity
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.push.PushPlatform
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

class PushTokenDaoTest {
    private lateinit var database: AlongsideDatabase
    private lateinit var dao: PushTokenDao

    @BeforeTest
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder<AlongsideDatabase>()
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        dao = database.pushTokenDao()
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    private fun pushTokenEntity(
        userId: String = "user-1",
        token: String = "fcm-token-1",
    ) = PushTokenEntity(
        userId = userId,
        token = token,
        platform = PushPlatform.ANDROID,
        syncStatus = SyncStatus.PENDING,
        updatedAt = Instant.fromEpochMilliseconds(1_752_600_000_000),
    )

    @Test
    fun `insert then getById returns the inserted push token`() =
        runTest {
            val pushToken = pushTokenEntity()

            dao.upsert(pushToken)

            assertEquals(pushToken, dao.getById(pushToken.userId))
        }

    @Test
    fun `getById returns null for unknown id`() =
        runTest {
            assertNull(dao.getById("unknown"))
        }

    @Test
    fun `upsert with existing id replaces existing row`() =
        runTest {
            val original = pushTokenEntity()
            val replacement = original.copy(token = "fcm-token-2", platform = PushPlatform.IOS)

            dao.upsert(original)
            dao.upsert(replacement)

            assertEquals(replacement, dao.getById(original.userId))
        }

    @Test
    fun `delete removes the push token`() =
        runTest {
            val pushToken = pushTokenEntity()
            dao.upsert(pushToken)

            dao.delete(pushToken.userId)

            assertNull(dao.getById(pushToken.userId))
        }

    @Test
    fun `observeById emits on insert update and delete`() =
        runTest {
            val pushToken = pushTokenEntity()
            val emissions = Channel<PushTokenEntity?>(capacity = Channel.UNLIMITED)
            val job = launch { dao.observeById(pushToken.userId).collect { emissions.send(it) } }

            assertEquals(null, emissions.receive())

            dao.upsert(pushToken)
            assertEquals(pushToken, emissions.receive())

            val updated = pushToken.copy(token = "fcm-token-new")
            dao.upsert(updated)
            assertEquals(updated, emissions.receive())

            dao.delete(pushToken.userId)
            assertEquals(null, emissions.receive())

            job.cancel()
        }
}
