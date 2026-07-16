package com.alongside.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.alongside.core.database.entity.EpisodeEntity
import com.alongside.core.database.entity.EpisodeWithPhotos
import com.alongside.core.database.entity.PhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface EpisodeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEpisode(episode: EpisodeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPhotos(photos: List<PhotoEntity>)

    @Query("DELETE FROM photos WHERE episodeId = :episodeId")
    suspend fun deletePhotosForEpisode(episodeId: String)

    @Transaction
    suspend fun upsert(
        episode: EpisodeEntity,
        photos: List<PhotoEntity>,
    ) {
        deletePhotosForEpisode(episode.id)
        upsertEpisode(episode)
        upsertPhotos(photos)
    }

    @Transaction
    @Query("SELECT * FROM episodes WHERE id = :id")
    suspend fun getById(id: String): EpisodeWithPhotos?

    @Transaction
    @Query("SELECT * FROM episodes WHERE diaryEntryId = :diaryEntryId")
    fun observeByDiaryEntry(diaryEntryId: String): Flow<List<EpisodeWithPhotos>>

    @Query("DELETE FROM episodes WHERE id = :id")
    suspend fun delete(id: String)
}
