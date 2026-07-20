package com.alongside.feature.diary

import com.alongside.core.domain.diary.DiaryEntryRepository
import com.alongside.core.model.diary.DiaryEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Map-backed fake so `observeByTrip` reacts live to `upsert`, the way Room's Flow does. */
internal class FakeDiaryEntryRepository : DiaryEntryRepository {
    private val entries = MutableStateFlow<Map<String, DiaryEntry>>(emptyMap())
    val upserted = mutableListOf<DiaryEntry>()

    override suspend fun upsert(entry: DiaryEntry) {
        upserted += entry
        entries.value = entries.value + (entry.id to entry)
    }

    override suspend fun getById(id: String): DiaryEntry? = entries.value[id]

    override fun observeByTrip(tripId: String): Flow<List<DiaryEntry>> =
        entries.map { it.values.filter { entry -> entry.tripId == tripId } }

    override suspend fun delete(id: String) {
        entries.value = entries.value - id
    }
}
