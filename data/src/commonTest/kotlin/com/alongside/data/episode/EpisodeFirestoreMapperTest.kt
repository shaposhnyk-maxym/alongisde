package com.alongside.data.episode

import com.alongside.core.model.SyncStatus
import com.alongside.core.network.firestore.model.FirestoreDocument
import com.alongside.core.network.firestore.model.FirestoreValue
import com.alongside.data.testEpisode
import com.alongside.data.testPhoto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.time.Instant

class EpisodeFirestoreMapperTest {
    private val startTime = Instant.parse("2026-07-18T10:00:00Z")
    private val endTime = Instant.parse("2026-07-18T12:00:00Z")
    private val updatedAt = Instant.parse("2026-07-18T12:30:00Z")

    @Test
    fun `toFields maps scalar fields with RFC3339 timestamps`() {
        val episode = testEpisode(startTime = startTime, endTime = endTime, updatedAt = updatedAt)

        val fields = EpisodeFirestoreMapper.toFields(episode)

        assertEquals(FirestoreValue.StringValue("episode-1"), fields["id"])
        assertEquals(FirestoreValue.StringValue("entry-1"), fields["diaryEntryId"])
        assertEquals(FirestoreValue.TimestampValue("2026-07-18T10:00:00Z"), fields["startTime"])
        assertEquals(FirestoreValue.TimestampValue("2026-07-18T12:00:00Z"), fields["endTime"])
        assertEquals(FirestoreValue.DoubleValue(49.8397), fields["latitude"])
        assertEquals(FirestoreValue.DoubleValue(24.0297), fields["longitude"])
        assertEquals(FirestoreValue.StringValue("Rynok Square"), fields["placeName"])
        assertEquals(FirestoreValue.StringValue("Wandering the old town"), fields["description"])
        assertEquals(FirestoreValue.IntegerValue(1), fields["descriptionAttempts"])
        assertEquals(FirestoreValue.TimestampValue("2026-07-18T12:30:00Z"), fields["updatedAt"])
    }

    @Test
    fun `toFields does not serialize the local-only syncStatus`() {
        assertFalse("syncStatus" in EpisodeFirestoreMapper.toFields(testEpisode()))
    }

    @Test
    fun `toFields writes null placeName and description as NullValue`() {
        val fields = EpisodeFirestoreMapper.toFields(testEpisode(placeName = null, description = null))

        assertEquals(FirestoreValue.NullValue, fields["placeName"])
        assertEquals(FirestoreValue.NullValue, fields["description"])
    }

    @Test
    fun `toFields embeds photos as an array of maps`() {
        val photos = listOf(testPhoto(id = "photo-1"), testPhoto(id = "photo-2"))
        val episode = testEpisode(photos = photos)

        val photosField = EpisodeFirestoreMapper.toFields(episode)["photos"] as FirestoreValue.ArrayValue

        assertEquals(2, photosField.value.values.size)
        val firstPhoto = (photosField.value.values.first() as FirestoreValue.MapValue).value.fields
        assertEquals(FirestoreValue.StringValue("photo-1"), firstPhoto["id"])
        assertEquals(FirestoreValue.StringValue("content://photos/photo-1"), firstPhoto["uri"])
    }

    @Test
    fun `fromDocument round trips an episode including photos and marks it SYNCED`() {
        val episode =
            testEpisode(
                startTime = startTime,
                endTime = endTime,
                updatedAt = updatedAt,
                photos = listOf(testPhoto(id = "photo-1"), testPhoto(id = "photo-2")),
            )
        val document = FirestoreDocument(fields = EpisodeFirestoreMapper.toFields(episode))

        val decoded = EpisodeFirestoreMapper.fromDocument(document)

        assertEquals(episode.copy(syncStatus = SyncStatus.SYNCED), decoded)
    }

    @Test
    fun `toFields and fromDocument round trip a photo's remoteUrl when present`() {
        val photos = listOf(testPhoto(id = "photo-1", remoteUrl = "https://firebasestorage.googleapis.com/photo-1"))
        val episode = testEpisode(photos = photos)
        val document = FirestoreDocument(fields = EpisodeFirestoreMapper.toFields(episode))

        val decoded = EpisodeFirestoreMapper.fromDocument(document)

        assertEquals("https://firebasestorage.googleapis.com/photo-1", decoded.photos.single().remoteUrl)
    }

    @Test
    fun `toFields and fromDocument round trip a photo's remoteUrl when null`() {
        val photos = listOf(testPhoto(id = "photo-1", remoteUrl = null))
        val episode = testEpisode(photos = photos)
        val document = FirestoreDocument(fields = EpisodeFirestoreMapper.toFields(episode))

        val decoded = EpisodeFirestoreMapper.fromDocument(document)

        assertNull(decoded.photos.single().remoteUrl)
    }

    @Test
    fun `fromDocument reads NullValue placeName and description as null`() {
        val document =
            FirestoreDocument(fields = EpisodeFirestoreMapper.toFields(testEpisode(placeName = null, description = null)))

        val decoded = EpisodeFirestoreMapper.fromDocument(document)

        assertNull(decoded.placeName)
        assertNull(decoded.description)
    }
}
