package com.alongside.core.database.repository

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.alongside.core.database.AlongsideDatabase
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.push.PushPlatform
import com.alongside.core.model.push.PushToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class PushTokenRepositoryImplTest {
    private lateinit var database: AlongsideDatabase
    private lateinit var repository: PushTokenRepositoryImpl

    @BeforeTest
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder<AlongsideDatabase>()
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        repository = PushTokenRepositoryImpl(database)
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    private fun pushToken(userId: String = "user-1") =
        PushToken(
            userId = userId,
            token = "fcm-token-1",
            platform = PushPlatform.ANDROID,
            syncStatus = SyncStatus.PENDING,
            updatedAt = Instant.fromEpochMilliseconds(1_752_600_000_000),
        )

    @Test
    fun `upsert then getById returns the domain push token`() =
        runTest {
            val pushToken = pushToken()

            repository.upsert(pushToken)

            assertEquals(pushToken, repository.getById(pushToken.userId))
        }

    @Test
    fun `getById returns null for unknown id`() =
        runTest {
            assertNull(repository.getById("unknown"))
        }

    @Test
    fun `delete removes the push token`() =
        runTest {
            val pushToken = pushToken()
            repository.upsert(pushToken)

            repository.delete(pushToken.userId)

            assertNull(repository.getById(pushToken.userId))
        }

    @Test
    fun `observeById emits the mapped domain push token`() =
        runTest {
            val pushToken = pushToken()

            repository.upsert(pushToken)

            assertEquals(pushToken, repository.observeById(pushToken.userId).first())
        }
}
