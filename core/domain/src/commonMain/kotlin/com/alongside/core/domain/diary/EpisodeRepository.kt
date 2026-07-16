package com.alongside.core.domain.diary

import com.alongside.core.model.diary.Episode
import kotlinx.coroutines.flow.Flow

public interface EpisodeRepository {
    public suspend fun upsert(episode: Episode)

    public suspend fun getById(id: String): Episode?

    public fun observeByDiaryEntry(diaryEntryId: String): Flow<List<Episode>>

    public suspend fun delete(id: String)
}
