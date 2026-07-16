package com.alongside.core.database.converter

import com.alongside.core.model.SyncStatus
import com.alongside.core.model.place.SwipeDirection
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class AlongsideTypeConvertersTest {
    @Test
    fun `local date round trips through string`() {
        val date = LocalDate(2026, 7, 16)

        val serialized = AlongsideTypeConverters.fromLocalDate(date)
        val deserialized = AlongsideTypeConverters.toLocalDate(serialized)

        assertEquals(date, deserialized)
    }

    @Test
    fun `null local date round trips as null`() {
        assertNull(AlongsideTypeConverters.fromLocalDate(null))
        assertNull(AlongsideTypeConverters.toLocalDate(null))
    }

    @Test
    fun `instant round trips through epoch millis`() {
        val instant = Instant.fromEpochMilliseconds(1_752_600_000_000)

        val serialized = AlongsideTypeConverters.fromInstant(instant)
        val deserialized = AlongsideTypeConverters.toInstant(serialized)

        assertEquals(instant, deserialized)
    }

    @Test
    fun `null instant round trips as null`() {
        assertNull(AlongsideTypeConverters.fromInstant(null))
        assertNull(AlongsideTypeConverters.toInstant(null))
    }

    @Test
    fun `sync status round trips through name`() {
        for (status in SyncStatus.entries) {
            val serialized = AlongsideTypeConverters.fromSyncStatus(status)
            val deserialized = AlongsideTypeConverters.toSyncStatus(serialized)

            assertEquals(status, deserialized)
        }
    }

    @Test
    fun `null sync status round trips as null`() {
        assertNull(AlongsideTypeConverters.fromSyncStatus(null))
        assertNull(AlongsideTypeConverters.toSyncStatus(null))
    }

    @Test
    fun `swipe direction round trips through name`() {
        for (direction in SwipeDirection.entries) {
            val serialized = AlongsideTypeConverters.fromSwipeDirection(direction)
            val deserialized = AlongsideTypeConverters.toSwipeDirection(serialized)

            assertEquals(direction, deserialized)
        }
    }

    @Test
    fun `null swipe direction round trips as null`() {
        assertNull(AlongsideTypeConverters.fromSwipeDirection(null))
        assertNull(AlongsideTypeConverters.toSwipeDirection(null))
    }
}
