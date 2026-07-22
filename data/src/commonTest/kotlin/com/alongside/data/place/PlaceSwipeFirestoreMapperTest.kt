package com.alongside.data.place

import com.alongside.core.model.SyncStatus
import com.alongside.core.model.place.SwipeDirection
import com.alongside.core.network.firestore.model.FirestoreDocument
import com.alongside.core.network.firestore.model.FirestoreValue
import com.alongside.data.testPlaceSwipe
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.time.Instant

class PlaceSwipeFirestoreMapperTest {
    private val swipedAt = Instant.parse("2026-07-18T10:00:00Z")
    private val updatedAt = Instant.parse("2026-07-18T12:30:00Z")

    @Test
    fun `toFields maps scalar fields with RFC3339 timestamps`() {
        val swipe = testPlaceSwipe(swipedAt = swipedAt, updatedAt = updatedAt)

        val fields = PlaceSwipeFirestoreMapper.toFields(swipe)

        assertEquals(FirestoreValue.StringValue("place-1::owner-1"), fields["id"])
        assertEquals(FirestoreValue.StringValue("trip-1"), fields["tripId"])
        assertEquals(FirestoreValue.StringValue("place-1"), fields["candidateId"])
        assertEquals(FirestoreValue.StringValue("owner-1"), fields["userId"])
        assertEquals(FirestoreValue.StringValue("LIKE"), fields["direction"])
        assertEquals(FirestoreValue.TimestampValue("2026-07-18T10:00:00Z"), fields["swipedAt"])
        assertEquals(FirestoreValue.TimestampValue("2026-07-18T12:30:00Z"), fields["updatedAt"])
    }

    @Test
    fun `toFields does not serialize the local-only syncStatus`() {
        assertFalse("syncStatus" in PlaceSwipeFirestoreMapper.toFields(testPlaceSwipe()))
    }

    @Test
    fun `toFields writes the direction as its enum name`() {
        val swipe = testPlaceSwipe(direction = SwipeDirection.DISLIKE)

        val fields = PlaceSwipeFirestoreMapper.toFields(swipe)

        assertEquals(FirestoreValue.StringValue("DISLIKE"), fields["direction"])
    }

    @Test
    fun `fromDocument round trips a swipe and marks it SYNCED`() {
        val swipe = testPlaceSwipe(swipedAt = swipedAt, updatedAt = updatedAt)
        val document = FirestoreDocument(fields = PlaceSwipeFirestoreMapper.toFields(swipe))

        val decoded = PlaceSwipeFirestoreMapper.fromDocument(document)

        assertEquals(swipe.copy(syncStatus = SyncStatus.SYNCED), decoded)
    }
}
