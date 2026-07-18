package com.alongside.data.sync

import kotlin.test.Test
import kotlin.test.assertEquals
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
}
