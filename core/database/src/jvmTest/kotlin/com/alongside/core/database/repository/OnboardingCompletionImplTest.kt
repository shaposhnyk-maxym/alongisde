package com.alongside.core.database.repository

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.alongside.core.database.AlongsideDatabase
import com.alongside.core.database.onboardingCompletionCache
import com.alongside.core.domain.onboarding.OnboardingCompletionCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OnboardingCompletionImplTest {
    private lateinit var database: AlongsideDatabase
    private lateinit var cache: OnboardingCompletionCache

    @BeforeTest
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder<AlongsideDatabase>()
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        cache = database.onboardingCompletionCache()
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun `isCompleted is false when nothing was ever marked`() =
        runTest {
            assertFalse(cache.isCompleted())
        }

    @Test
    fun `markCompleted then isCompleted returns true`() =
        runTest {
            cache.markCompleted()

            assertTrue(cache.isCompleted())
        }

    @Test
    fun `markCompleted twice still leaves isCompleted true`() =
        runTest {
            cache.markCompleted()
            cache.markCompleted()

            assertTrue(cache.isCompleted())
        }

    @Test
    fun `clear resets isCompleted to false`() =
        runTest {
            cache.markCompleted()

            cache.clear()

            assertFalse(cache.isCompleted())
        }
}
