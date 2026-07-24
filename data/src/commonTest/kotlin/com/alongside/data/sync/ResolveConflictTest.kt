package com.alongside.data.sync

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Instant

class ResolveConflictTest {
    private val earlier = Instant.fromEpochMilliseconds(1_000)
    private val later = Instant.fromEpochMilliseconds(2_000)

    @Test
    fun `local wins when local updatedAt is newer`() {
        assertEquals(ConflictWinner.LOCAL, resolveConflict(localUpdatedAt = later, remoteUpdatedAt = earlier))
    }

    @Test
    fun `remote wins when remote updatedAt is newer`() {
        assertEquals(ConflictWinner.REMOTE, resolveConflict(localUpdatedAt = earlier, remoteUpdatedAt = later))
    }

    @Test
    fun `local wins the tie when both timestamps are equal`() {
        assertEquals(ConflictWinner.LOCAL, resolveConflict(localUpdatedAt = later, remoteUpdatedAt = later))
    }

    @Test
    fun `local wins when there is no remote timestamp`() {
        assertEquals(ConflictWinner.LOCAL, resolveConflict(localUpdatedAt = earlier, remoteUpdatedAt = null))
    }

    @Test
    fun `local wins the tie when remote only differs below millisecond precision`() {
        // Room truncates Instant to epoch millis on write (AlongsideTypeConverters.fromInstant);
        // Firestore round-trips the full sub-millisecond value from Instant#toString(). Without
        // millisecond-truncated comparison here, an unchanged trip looks "remote-newer" by a few
        // hundred microseconds on literally every poll tick forever, forcing a needless re-save
        // each time - which re-emits Room's Flow, which re-fires whatever's downstream (e.g.
        // PairingContainer's Paired side effect) on every single tick, indefinitely.
        val localMillisTruncated = Instant.fromEpochMilliseconds(later.toEpochMilliseconds())
        val remoteWithSubMillisRemainder = later + 739.microseconds

        assertEquals(
            ConflictWinner.LOCAL,
            resolveConflict(localUpdatedAt = localMillisTruncated, remoteUpdatedAt = remoteWithSubMillisRemainder),
        )
    }
}
