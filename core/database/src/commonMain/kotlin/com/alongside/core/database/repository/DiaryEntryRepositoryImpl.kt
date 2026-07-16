package com.alongside.core.database.repository

import com.alongside.core.database.AlongsideDatabase
import com.alongside.core.database.entity.toDomain
import com.alongside.core.database.entity.toEntity
import com.alongside.core.domain.diary.DiaryEntryRepository
import com.alongside.core.model.diary.DiaryEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class DiaryEntryRepositoryImpl(
    private val database: AlongsideDatabase,
) : DiaryEntryRepository {
    override suspend fun upsert(entry: DiaryEntry) {
        database.diaryEntryDao().upsert(entry.toEntity())
    }

    override suspend fun getById(id: String): DiaryEntry? = database.diaryEntryDao().getById(id)?.toDomain()

    override fun observeByTrip(tripId: String): Flow<List<DiaryEntry>> =
        database.diaryEntryDao().observeByTrip(tripId).map { entries -> entries.map { it.toDomain() } }

    override suspend fun delete(id: String) {
        database.diaryEntryDao().delete(id)
    }
}
