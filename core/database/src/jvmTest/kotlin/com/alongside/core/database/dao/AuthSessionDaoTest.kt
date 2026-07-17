package com.alongside.core.database.dao

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.alongside.core.database.AlongsideDatabase
import com.alongside.core.database.entity.AuthSessionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class AuthSessionDaoTest {
    private lateinit var database: AlongsideDatabase
    private lateinit var dao: AuthSessionDao

    @BeforeTest
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder<AlongsideDatabase>()
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        dao = database.authSessionDao()
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    private fun authSessionEntity(idToken: String = "id-token-1") =
        AuthSessionEntity(
            uid = "uid-1",
            email = "person@example.com",
            displayName = "Person One",
            photoUrl = null,
            idToken = idToken,
            refreshToken = null,
            expiresInSeconds = 3600L,
            issuedAt = Instant.fromEpochMilliseconds(1_752_600_000_000),
        )

    @Test
    fun `upsert then get returns the inserted session`() =
        runTest {
            val session = authSessionEntity()

            dao.upsert(session)

            assertEquals(session, dao.get())
        }

    @Test
    fun `get returns null when nothing was ever saved`() =
        runTest {
            assertNull(dao.get())
        }

    @Test
    fun `upsert replaces the existing singleton row`() =
        runTest {
            val original = authSessionEntity()
            val replacement = original.copy(idToken = "id-token-2")

            dao.upsert(original)
            dao.upsert(replacement)

            assertEquals(replacement, dao.get())
        }

    @Test
    fun `clear removes the saved session`() =
        runTest {
            dao.upsert(authSessionEntity())

            dao.clear()

            assertNull(dao.get())
        }
}
