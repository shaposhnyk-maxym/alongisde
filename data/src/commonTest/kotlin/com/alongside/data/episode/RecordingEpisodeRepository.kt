package com.alongside.data.episode

import com.alongside.core.domain.diary.EpisodeRepository
import com.alongside.core.model.diary.Episode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Map-backed local episodes fake that records calls for assertions. */
internal class RecordingEpisodeRepository : EpisodeRepository {
    private val episodes = MutableStateFlow<Map<String, Episode>>(emptyMap())
    val upserted = mutableListOf<Episode>()
    val deletedIds = mutableListOf<String>()

    override suspend fun upsert(episode: Episode) {
        upserted += episode
        episodes.value = episodes.value + (episode.id to episode)
    }

    override suspend fun getById(id: String): Episode? = episodes.value[id]

    override fun observeByDiaryEntry(diaryEntryId: String): Flow<List<Episode>> =
        episodes.map { all -> all.values.filter { it.diaryEntryId == diaryEntryId } }

    override suspend fun delete(id: String) {
        deletedIds += id
        episodes.value = episodes.value - id
    }
}
