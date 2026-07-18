package com.alongside.core.domain.pairing

import com.alongside.core.model.SyncStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

private val FIXED_NOW = Instant.fromEpochMilliseconds(1_752_800_000_000)

private object FixedClock : Clock {
    override fun now(): Instant = FIXED_NOW
}

class DefaultPairingRepositoryTest {
    private val dataSource = RecordingPairingTripDataSource()

    private fun repository(seed: Int = 42) =
        DefaultPairingRepository(
            dataSource = dataSource,
            inviteCodeGenerator = InviteCodeGenerator(Random(seed)),
            clock = FixedClock,
            generateTripId = { "trip-new" },
        )

    @Test
    fun `createTrip persists a trip with valid code and pending sync`() =
        runTest {
            val startDate = LocalDate(2026, 7, 18)
            val endDate = LocalDate(2026, 8, 1)

            val created = repository().createTrip("owner-1", startDate, endDate)

            assertEquals(listOf(created), dataSource.savedTrips)
            assertEquals("trip-new", created.id)
            assertEquals("owner-1", created.ownerId)
            assertNull(created.memberId)
            assertTrue(isValidInviteCodeFormat(created.inviteCode), "bad code ${created.inviteCode}")
            assertEquals(startDate, created.startDate)
            assertEquals(endDate, created.endDate)
            assertEquals(SyncStatus.PENDING, created.syncStatus)
            assertEquals(FIXED_NOW, created.createdAt)
        }

    @Test
    fun `createTrip retries when the generated code is already taken`() =
        runTest {
            val takenCode = InviteCodeGenerator(Random(42)).generate()
            dataSource.save(pairingTestTrip(id = "existing", ownerId = "someone", inviteCode = takenCode))

            val created =
                repository(seed = 42).createTrip("owner-1", LocalDate(2026, 7, 18), LocalDate(2026, 8, 1))

            assertNotEquals(takenCode, created.inviteCode)
            assertTrue(isValidInviteCodeFormat(created.inviteCode))
            assertEquals(listOf(takenCode, created.inviteCode), dataSource.lookedUpCodes)
        }

    @Test
    fun `joinTrip success persists the member and emits through observe`() =
        runTest {
            dataSource.save(pairingTestTrip())

            val result = repository().joinTrip("ABCD23", "user-2")

            val joined = assertIs<JoinTripResult.Joined>(result)
            assertEquals("user-2", joined.trip.memberId)
            assertEquals(joined.trip, dataSource.savedTrips.last())
            assertEquals(joined.trip, repository().observeActiveTrip("user-2").first())
        }

    @Test
    fun `joinTrip normalizes whitespace and case before lookup`() =
        runTest {
            dataSource.save(pairingTestTrip())

            val result = repository().joinTrip(" abcd23 ", "user-2")

            assertIs<JoinTripResult.Joined>(result)
            assertEquals("ABCD23", dataSource.lookedUpCodes.last())
        }

    @Test
    fun `failed joins do not save anything`() =
        runTest {
            dataSource.save(pairingTestTrip(memberId = null))
            dataSource.save(pairingTestTrip(id = "trip-2", inviteCode = "USED22", memberId = "someone-else"))
            val savedBefore = dataSource.savedTrips.toList()

            assertEquals(JoinTripResult.InvalidCode, repository().joinTrip("ZZZZ99", "user-2"))
            assertEquals(JoinTripResult.OwnCode, repository().joinTrip("ABCD23", "owner-1"))
            assertEquals(JoinTripResult.AlreadyUsed, repository().joinTrip("USED22", "user-2"))

            assertEquals(savedBefore, dataSource.savedTrips)
        }

    @Test
    fun `observeActiveTrip emits for owner and member and null for strangers`() =
        runTest {
            val trip = pairingTestTrip(memberId = "user-2")
            dataSource.save(trip)

            assertEquals(trip, repository().observeActiveTrip("owner-1").first())
            assertEquals(trip, repository().observeActiveTrip("user-2").first())
            assertNull(repository().observeActiveTrip("stranger").first())
        }
}
