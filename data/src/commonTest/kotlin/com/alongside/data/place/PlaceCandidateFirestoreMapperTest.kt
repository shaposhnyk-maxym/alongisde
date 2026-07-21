package com.alongside.data.place

import com.alongside.core.model.SyncStatus
import com.alongside.core.model.place.PlacePhoto
import com.alongside.core.model.place.SwipeDirection
import com.alongside.core.network.firestore.model.FirestoreDocument
import com.alongside.core.network.firestore.model.FirestoreValue
import com.alongside.data.testPlace
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.time.Instant

class PlaceCandidateFirestoreMapperTest {
    private val createdAt = Instant.parse("2026-07-18T10:00:00Z")
    private val updatedAt = Instant.parse("2026-07-18T12:30:00Z")

    @Test
    fun `toFields maps scalar fields with RFC3339 timestamps`() {
        val place = testPlace(createdAt = createdAt, updatedAt = updatedAt)

        val fields = PlaceCandidateFirestoreMapper.toFields(place)

        assertEquals(FirestoreValue.StringValue("place-1"), fields["id"])
        assertEquals(FirestoreValue.StringValue("trip-1"), fields["tripId"])
        assertEquals(FirestoreValue.StringValue("Lviv Coffee Manufacture"), fields["name"])
        assertEquals(FirestoreValue.DoubleValue(49.8397), fields["latitude"])
        assertEquals(FirestoreValue.DoubleValue(24.0297), fields["longitude"])
        assertEquals(FirestoreValue.StringValue("owner-1"), fields["addedByUserId"])
        assertEquals(FirestoreValue.TimestampValue("2026-07-18T10:00:00Z"), fields["createdAt"])
        assertEquals(FirestoreValue.TimestampValue("2026-07-18T12:30:00Z"), fields["updatedAt"])
    }

    @Test
    fun `toFields does not serialize the local-only syncStatus`() {
        assertFalse("syncStatus" in PlaceCandidateFirestoreMapper.toFields(testPlace()))
    }

    @Test
    fun `toFields writes null note swipes rating category and city as NullValue`() {
        val fields = PlaceCandidateFirestoreMapper.toFields(testPlace())

        assertEquals(FirestoreValue.NullValue, fields["note"])
        assertEquals(FirestoreValue.NullValue, fields["ownerSwipe"])
        assertEquals(FirestoreValue.NullValue, fields["memberSwipe"])
        assertEquals(FirestoreValue.NullValue, fields["rating"])
        assertEquals(FirestoreValue.NullValue, fields["category"])
        assertEquals(FirestoreValue.NullValue, fields["city"])
    }

    @Test
    fun `toFields writes swipes as their enum name`() {
        val place = testPlace(ownerSwipe = SwipeDirection.LIKE, memberSwipe = SwipeDirection.DISLIKE)

        val fields = PlaceCandidateFirestoreMapper.toFields(place)

        assertEquals(FirestoreValue.StringValue("LIKE"), fields["ownerSwipe"])
        assertEquals(FirestoreValue.StringValue("DISLIKE"), fields["memberSwipe"])
    }

    @Test
    fun `toFields embeds photos as an array of maps`() {
        val photos =
            listOf(
                PlacePhoto(photoRef = "places/abc/photos/photo-1", remoteUrl = "https://storage/photo-1"),
                PlacePhoto(photoRef = "places/abc/photos/photo-2", remoteUrl = null),
            )
        val place = testPlace(photos = photos)

        val photosField = PlaceCandidateFirestoreMapper.toFields(place)["photos"] as FirestoreValue.ArrayValue

        assertEquals(2, photosField.value.values.size)
        val firstPhoto = (photosField.value.values.first() as FirestoreValue.MapValue).value.fields
        assertEquals(FirestoreValue.StringValue("places/abc/photos/photo-1"), firstPhoto["photoRef"])
        assertEquals(FirestoreValue.StringValue("https://storage/photo-1"), firstPhoto["remoteUrl"])
        val secondPhoto = (photosField.value.values.last() as FirestoreValue.MapValue).value.fields
        assertEquals(FirestoreValue.NullValue, secondPhoto["remoteUrl"])
    }

    @Test
    fun `fromDocument round trips a place including photos rating category and city and marks it SYNCED`() {
        val place =
            testPlace(
                createdAt = createdAt,
                updatedAt = updatedAt,
                ownerSwipe = SwipeDirection.LIKE,
                photos =
                    listOf(
                        PlacePhoto(photoRef = "places/abc/photos/photo-1", remoteUrl = "https://storage/photo-1"),
                        PlacePhoto(photoRef = "places/abc/photos/photo-2", remoteUrl = null),
                    ),
                rating = 4.6,
                category = "Coffee shop",
                city = "Lviv",
            )
        val document = FirestoreDocument(fields = PlaceCandidateFirestoreMapper.toFields(place))

        val decoded = PlaceCandidateFirestoreMapper.fromDocument(document)

        assertEquals(place.copy(syncStatus = SyncStatus.SYNCED), decoded)
    }

    @Test
    fun `fromDocument reads NullValue note ownerSwipe memberSwipe rating category and city as null`() {
        val document = FirestoreDocument(fields = PlaceCandidateFirestoreMapper.toFields(testPlace()))

        val decoded = PlaceCandidateFirestoreMapper.fromDocument(document)

        assertNull(decoded.note)
        assertNull(decoded.ownerSwipe)
        assertNull(decoded.memberSwipe)
        assertNull(decoded.rating)
        assertNull(decoded.category)
        assertNull(decoded.city)
    }
}
