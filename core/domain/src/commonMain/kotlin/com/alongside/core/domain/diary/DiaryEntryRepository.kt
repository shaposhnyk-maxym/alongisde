package com.alongside.core.domain.diary

import com.alongside.core.model.diary.DiaryEntry
import kotlinx.coroutines.flow.Flow

public interface DiaryEntryRepository {
    public suspend fun upsert(entry: DiaryEntry)

    public suspend fun getById(id: String): DiaryEntry?

    public fun observeByTrip(tripId: String): Flow<List<DiaryEntry>>

    public suspend fun delete(id: String)
}
