package com.alongside.data.diary

import com.alongside.core.domain.diary.DiaryEntryRepository
import com.alongside.core.model.diary.DiaryEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Map-backed local diary-entries fake that records calls for assertions. */
internal class RecordingDiaryEntryRepository : DiaryEntryRepository {
    private val entries = MutableStateFlow<Map<String, DiaryEntry>>(emptyMap())
    val upserted = mutableListOf<DiaryEntry>()
    val deletedIds = mutableListOf<String>()

    override suspend fun upsert(entry: DiaryEntry) {
        upserted += entry
        entries.value = entries.value + (entry.id to entry)
    }

    override suspend fun getById(id: String): DiaryEntry? = entries.value[id]

    override fun observeByTrip(tripId: String): Flow<List<DiaryEntry>> = entries.map { it.forTrip(tripId) }

    override suspend fun delete(id: String) {
        deletedIds += id
        entries.value = entries.value - id
    }
}

private fun Map<String, DiaryEntry>.forTrip(tripId: String): List<DiaryEntry> = values.filter { it.tripId == tripId }
