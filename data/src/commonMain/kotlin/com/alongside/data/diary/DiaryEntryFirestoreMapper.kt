package com.alongside.data.diary

import com.alongside.core.model.SyncStatus
import com.alongside.core.model.diary.DiaryEntry
import com.alongside.core.network.firestore.model.FirestoreDocument
import com.alongside.core.network.firestore.model.FirestoreValue
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * DiaryEntry <-> Firestore document fields. `syncStatus` is deliberately not serialized - it is
 * a local-only bookkeeping flag; anything read back from the remote is [SyncStatus.SYNCED].
 */
public object DiaryEntryFirestoreMapper {
    public const val COLLECTION_PATH: String = "diaryEntries"

    public fun toFields(entry: DiaryEntry): Map<String, FirestoreValue> =
        mapOf(
            "id" to FirestoreValue.StringValue(entry.id),
            "tripId" to FirestoreValue.StringValue(entry.tripId),
            "userId" to FirestoreValue.StringValue(entry.userId),
            "date" to FirestoreValue.StringValue(entry.date.toString()),
            "createdAt" to FirestoreValue.TimestampValue(entry.createdAt.toString()),
            "updatedAt" to FirestoreValue.TimestampValue(entry.updatedAt.toString()),
        )

    public fun fromDocument(document: FirestoreDocument): DiaryEntry {
        val fields = document.fields
        return DiaryEntry(
            id = fields.requireString("id"),
            tripId = fields.requireString("tripId"),
            userId = fields.requireString("userId"),
            date = LocalDate.parse(fields.requireString("date")),
            syncStatus = SyncStatus.SYNCED,
            createdAt = Instant.parse(fields.requireTimestamp("createdAt")),
            updatedAt = Instant.parse(fields.requireTimestamp("updatedAt")),
        )
    }

    private fun Map<String, FirestoreValue>.requireString(key: String): String =
        (this[key] as? FirestoreValue.StringValue)?.value
            ?: throw IllegalArgumentException("DiaryEntry document is missing string field '$key'")

    private fun Map<String, FirestoreValue>.requireTimestamp(key: String): String =
        (this[key] as? FirestoreValue.TimestampValue)?.value
            ?: throw IllegalArgumentException("DiaryEntry document is missing timestamp field '$key'")
}
