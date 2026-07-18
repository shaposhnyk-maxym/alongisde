package com.alongside.core.database.repository

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.alongside.core.database.AlongsideDatabase
import com.alongside.core.database.pairingTripLocalDataSource
import com.alongside.core.domain.pairing.PairingTripDataSource
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.trip.Trip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class RoomPairingTripDataSourceTest {
    private lateinit var database: AlongsideDatabase
    private lateinit var dataSource: PairingTripDataSource

    @BeforeTest
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder<AlongsideDatabase>()
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        dataSource = database.pairingTripLocalDataSource()
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    private fun trip(
        id: String = "trip-1",
        ownerId: String = "owner-1",
        memberId: String? = null,
        inviteCode: String = "ABCD23",
    ) = Trip(
        id = id,
        ownerId = ownerId,
        memberId = memberId,
        inviteCode = inviteCode,
        startDate = LocalDate(2026, 7, 16),
        endDate = LocalDate(2026, 7, 23),
        syncStatus = SyncStatus.PENDING,
        createdAt = Instant.fromEpochMilliseconds(1_752_600_000_000),
        updatedAt = Instant.fromEpochMilliseconds(1_752_600_000_000),
    )

    @Test
    fun `findByInviteCode returns the saved trip and null for unknown code`() =
        runTest {
            val saved = trip()
            dataSource.save(saved)

            assertEquals(saved, dataSource.findByInviteCode("ABCD23"))
            assertNull(dataSource.findByInviteCode("XXXX99"))
        }

    @Test
    fun `observeByUserId matches the trip owner`() =
        runTest {
            val saved = trip(ownerId = "owner-1")
            dataSource.save(saved)

            assertEquals(saved, dataSource.observeByUserId("owner-1").first())
        }

    @Test
    fun `observeByUserId matches the joined member`() =
        runTest {
            val saved = trip(ownerId = "owner-1", memberId = "member-1")
            dataSource.save(saved)

            assertEquals(saved, dataSource.observeByUserId("member-1").first())
        }

    @Test
    fun `observeByUserId emits null for a stranger`() =
        runTest {
            dataSource.save(trip())

            assertNull(dataSource.observeByUserId("stranger").first())
        }

    @Test
    fun `observeByUserId emits again when the partner joins`() =
        runTest {
            val created = trip(ownerId = "owner-1", memberId = null)
            val emissions = Channel<Trip?>(capacity = Channel.UNLIMITED)
            val job = launch { dataSource.observeByUserId("owner-1").collect { emissions.send(it) } }

            assertNull(emissions.receive())

            dataSource.save(created)
            assertEquals(created, emissions.receive())

            val joined = created.copy(memberId = "member-1")
            dataSource.save(joined)
            assertEquals(joined, emissions.receive())

            job.cancel()
        }
}
