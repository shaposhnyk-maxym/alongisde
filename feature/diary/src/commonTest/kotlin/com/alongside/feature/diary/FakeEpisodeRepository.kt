package com.alongside.feature.diary

import com.alongside.core.domain.diary.EpisodeRepository
import com.alongside.core.model.diary.Episode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

internal class FakeEpisodeRepository : EpisodeRepository {
    private val episodes = MutableStateFlow<Map<String, Episode>>(emptyMap())
    val upserted = mutableListOf<Episode>()

    override suspend fun upsert(episode: Episode) {
        upserted += episode
        episodes.value = episodes.value + (episode.id to episode)
    }

    override suspend fun getById(id: String): Episode? = episodes.value[id]

    override fun observeByDiaryEntry(diaryEntryId: String): Flow<List<Episode>> =
        episodes.map { it.values.filter { episode -> episode.diaryEntryId == diaryEntryId } }

    override suspend fun delete(id: String) {
        episodes.value = episodes.value - id
    }
}
