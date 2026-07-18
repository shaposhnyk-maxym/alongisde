package com.alongside.data.trip

import com.alongside.core.model.SyncStatus
import com.alongside.core.network.firestore.model.FirestoreDocument
import com.alongside.core.network.firestore.model.FirestoreValue
import com.alongside.data.testTrip
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.time.Instant

class TripFirestoreMapperTest {
    private val createdAt = Instant.parse("2026-07-01T10:00:00Z")
    private val updatedAt = Instant.parse("2026-07-18T12:30:00Z")

    @Test
    fun `toFields maps every persisted field with RFC3339 timestamps`() {
        val trip = testTrip(memberId = "member-1", createdAt = createdAt, updatedAt = updatedAt)

        val fields = TripFirestoreMapper.toFields(trip)

        assertEquals(FirestoreValue.StringValue("trip-1"), fields["id"])
        assertEquals(FirestoreValue.StringValue("owner-1"), fields["ownerId"])
        assertEquals(FirestoreValue.StringValue("member-1"), fields["memberId"])
        assertEquals(FirestoreValue.StringValue("ABCD23"), fields["inviteCode"])
        assertEquals(FirestoreValue.StringValue("2026-07-18"), fields["startDate"])
        assertEquals(FirestoreValue.StringValue("2026-08-01"), fields["endDate"])
        assertEquals(FirestoreValue.TimestampValue("2026-07-01T10:00:00Z"), fields["createdAt"])
        assertEquals(FirestoreValue.TimestampValue("2026-07-18T12:30:00Z"), fields["updatedAt"])
    }

    @Test
    fun `toFields does not serialize the local-only syncStatus`() {
        assertFalse("syncStatus" in TripFirestoreMapper.toFields(testTrip()))
    }

    @Test
    fun `toFields writes a null memberId as NullValue`() {
        assertEquals(FirestoreValue.NullValue, TripFirestoreMapper.toFields(testTrip(memberId = null))["memberId"])
    }

    @Test
    fun `fromDocument round trips a trip and marks it SYNCED`() {
        val trip = testTrip(memberId = "member-1", createdAt = createdAt, updatedAt = updatedAt)
        val document = FirestoreDocument(fields = TripFirestoreMapper.toFields(trip))

        val decoded = TripFirestoreMapper.fromDocument(document)

        assertEquals(trip.copy(syncStatus = SyncStatus.SYNCED), decoded)
    }

    @Test
    fun `fromDocument reads NullValue memberId as null`() {
        val document = FirestoreDocument(fields = TripFirestoreMapper.toFields(testTrip(memberId = null)))

        assertNull(TripFirestoreMapper.fromDocument(document).memberId)
    }
}
