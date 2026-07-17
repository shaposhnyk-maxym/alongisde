package com.alongside.core.database.repository

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.alongside.core.database.AlongsideDatabase
import com.alongside.core.database.authSessionCache
import com.alongside.core.domain.auth.AuthSessionCache
import com.alongside.core.model.auth.AuthSession
import com.alongside.core.model.auth.AuthUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class AuthSessionCacheImplTest {
    private lateinit var database: AlongsideDatabase
    private lateinit var cache: AuthSessionCache

    @BeforeTest
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder<AlongsideDatabase>()
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        cache = database.authSessionCache()
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    private fun authSession() =
        AuthSession(
            user = AuthUser(uid = "uid-1", email = "person@example.com", displayName = "Person One", photoUrl = null),
            idToken = "id-token-1",
            refreshToken = "refresh-token-1",
            expiresInSeconds = 3600L,
            issuedAt = Instant.fromEpochMilliseconds(1_752_600_000_000),
        )

    @Test
    fun `save then get returns the domain session`() =
        runTest {
            val session = authSession()

            cache.save(session)

            assertEquals(session, cache.get())
        }

    @Test
    fun `get returns null when nothing was ever saved`() =
        runTest {
            assertNull(cache.get())
        }

    @Test
    fun `save replaces a previously saved session`() =
        runTest {
            cache.save(authSession())
            val replacement = authSession().copy(idToken = "id-token-2")

            cache.save(replacement)

            assertEquals(replacement, cache.get())
        }

    @Test
    fun `clear removes the saved session`() =
        runTest {
            cache.save(authSession())

            cache.clear()

            assertNull(cache.get())
        }
}
