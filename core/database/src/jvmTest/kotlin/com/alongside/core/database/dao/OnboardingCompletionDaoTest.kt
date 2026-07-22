package com.alongside.core.database.dao

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.alongside.core.database.AlongsideDatabase
import com.alongside.core.database.entity.OnboardingCompletionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class OnboardingCompletionDaoTest {
    private lateinit var database: AlongsideDatabase
    private lateinit var dao: OnboardingCompletionDao

    @BeforeTest
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder<AlongsideDatabase>()
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        dao = database.onboardingCompletionDao()
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun `get returns null when nothing was ever saved`() =
        runTest {
            assertNull(dao.get())
        }

    @Test
    fun `upsert then get returns the singleton row`() =
        runTest {
            dao.upsert(OnboardingCompletionEntity())

            assertNotNull(dao.get())
        }

    @Test
    fun `upsert is idempotent - marking completion twice still leaves one row`() =
        runTest {
            dao.upsert(OnboardingCompletionEntity())
            dao.upsert(OnboardingCompletionEntity())

            assertNotNull(dao.get())
        }

    @Test
    fun `clear removes the saved row`() =
        runTest {
            dao.upsert(OnboardingCompletionEntity())

            dao.clear()

            assertNull(dao.get())
        }
}
