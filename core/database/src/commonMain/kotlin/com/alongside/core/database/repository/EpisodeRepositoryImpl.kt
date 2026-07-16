package com.alongside.core.database.repository

import com.alongside.core.database.AlongsideDatabase
import com.alongside.core.database.entity.toDomain
import com.alongside.core.database.entity.toEntity
import com.alongside.core.domain.diary.EpisodeRepository
import com.alongside.core.model.diary.Episode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class EpisodeRepositoryImpl(
    private val database: AlongsideDatabase,
) : EpisodeRepository {
    override suspend fun upsert(episode: Episode) {
        database.episodeDao().upsert(
            episode = episode.toEntity(),
            photos = episode.photos.map { it.toEntity(episodeId = episode.id) },
        )
    }

    override suspend fun getById(id: String): Episode? = database.episodeDao().getById(id)?.toDomain()

    override fun observeByDiaryEntry(diaryEntryId: String): Flow<List<Episode>> =
        database.episodeDao().observeByDiaryEntry(diaryEntryId).map { episodes -> episodes.map { it.toDomain() } }

    override suspend fun delete(id: String) {
        database.episodeDao().delete(id)
    }
}
