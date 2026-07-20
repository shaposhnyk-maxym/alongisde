package com.alongside.data.diary

import com.alongside.core.model.SyncStatus
import com.alongside.core.network.firestore.model.FirestoreDocument
import com.alongside.core.network.firestore.model.FirestoreValue
import com.alongside.data.testDiaryEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.time.Instant

class DiaryEntryFirestoreMapperTest {
    private val createdAt = Instant.parse("2026-07-01T10:00:00Z")
    private val updatedAt = Instant.parse("2026-07-18T12:30:00Z")

    @Test
    fun `toFields maps every persisted field with RFC3339 timestamps`() {
        val entry = testDiaryEntry(createdAt = createdAt, updatedAt = updatedAt)

        val fields = DiaryEntryFirestoreMapper.toFields(entry)

        assertEquals(FirestoreValue.StringValue("entry-1"), fields["id"])
        assertEquals(FirestoreValue.StringValue("trip-1"), fields["tripId"])
        assertEquals(FirestoreValue.StringValue("owner-1"), fields["userId"])
        assertEquals(FirestoreValue.StringValue("2026-07-18"), fields["date"])
        assertEquals(FirestoreValue.TimestampValue("2026-07-01T10:00:00Z"), fields["createdAt"])
        assertEquals(FirestoreValue.TimestampValue("2026-07-18T12:30:00Z"), fields["updatedAt"])
        assertEquals(FirestoreValue.NullValue, fields["closedAt"])
    }

    @Test
    fun `toFields serializes a non-null closedAt as an RFC3339 timestamp`() {
        val closedAt = Instant.parse("2026-07-18T20:00:00Z")
        val entry = testDiaryEntry(closedAt = closedAt)

        val fields = DiaryEntryFirestoreMapper.toFields(entry)

        assertEquals(FirestoreValue.TimestampValue("2026-07-18T20:00:00Z"), fields["closedAt"])
    }

    @Test
    fun `toFields does not serialize the local-only syncStatus`() {
        assertFalse("syncStatus" in DiaryEntryFirestoreMapper.toFields(testDiaryEntry()))
    }

    @Test
    fun `fromDocument round trips an entry and marks it SYNCED`() {
        val entry = testDiaryEntry(createdAt = createdAt, updatedAt = updatedAt)
        val document = FirestoreDocument(fields = DiaryEntryFirestoreMapper.toFields(entry))

        val decoded = DiaryEntryFirestoreMapper.fromDocument(document)

        assertEquals(entry.copy(syncStatus = SyncStatus.SYNCED), decoded)
    }

    @Test
    fun `fromDocument round trips a non-null closedAt`() {
        val closedAt = Instant.parse("2026-07-18T20:00:00Z")
        val entry = testDiaryEntry(createdAt = createdAt, updatedAt = updatedAt, closedAt = closedAt)
        val document = FirestoreDocument(fields = DiaryEntryFirestoreMapper.toFields(entry))

        val decoded = DiaryEntryFirestoreMapper.fromDocument(document)

        assertEquals(closedAt, decoded.closedAt)
    }

    @Test
    fun `fromDocument tolerates a document missing the closedAt field entirely`() {
        val entry = testDiaryEntry(createdAt = createdAt, updatedAt = updatedAt)
        val fieldsWithoutClosedAt = DiaryEntryFirestoreMapper.toFields(entry) - "closedAt"
        val document = FirestoreDocument(fields = fieldsWithoutClosedAt)

        val decoded = DiaryEntryFirestoreMapper.fromDocument(document)

        assertEquals(null, decoded.closedAt)
    }
}
