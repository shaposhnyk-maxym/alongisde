package com.alongside.core.database.repository

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.alongside.core.database.AlongsideDatabase
import com.alongside.core.model.recap.Recap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RecapRepositoryImplTest {
    private lateinit var database: AlongsideDatabase
    private lateinit var repository: RecapRepositoryImpl

    @BeforeTest
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder<AlongsideDatabase>()
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        repository = RecapRepositoryImpl(database)
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    private fun recap(
        tripId: String = "trip-1",
        availableAt: LocalDate = LocalDate(2026, 7, 24),
    ) = Recap(tripId = tripId, availableAt = availableAt)

    @Test
    fun `ensureScheduled then getById returns the domain recap`() =
        runTest {
            val recap = recap()

            repository.ensureScheduled(recap.tripId, recap.availableAt)

            assertEquals(recap, repository.getById(recap.tripId))
        }

    @Test
    fun `getById returns null for unknown trip`() =
        runTest {
            assertNull(repository.getById("unknown"))
        }

    @Test
    fun `a second ensureScheduled call does not overwrite the stored availableAt`() =
        runTest {
            val recap = recap()
            repository.ensureScheduled(recap.tripId, recap.availableAt)

            repository.ensureScheduled(recap.tripId, LocalDate(2026, 8, 1))

            assertEquals(recap, repository.getById(recap.tripId))
        }

    @Test
    fun `observeById emits the mapped domain recap`() =
        runTest {
            val recap = recap()

            repository.ensureScheduled(recap.tripId, recap.availableAt)

            assertEquals(recap, repository.observeById(recap.tripId).first())
        }
}
