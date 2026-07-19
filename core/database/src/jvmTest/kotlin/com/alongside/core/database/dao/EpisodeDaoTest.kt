package com.alongside.core.database.dao

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.alongside.core.database.AlongsideDatabase
import com.alongside.core.database.entity.EpisodeEntity
import com.alongside.core.database.entity.EpisodeWithPhotos
import com.alongside.core.database.entity.PhotoEntity
import com.alongside.core.model.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class EpisodeDaoTest {
    private lateinit var database: AlongsideDatabase
    private lateinit var dao: EpisodeDao

    @BeforeTest
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder<AlongsideDatabase>()
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        dao = database.episodeDao()
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    private fun episodeEntity(
        id: String = "episode-1",
        diaryEntryId: String = "entry-1",
    ) = EpisodeEntity(
        id = id,
        diaryEntryId = diaryEntryId,
        startTime = Instant.fromEpochMilliseconds(1_752_600_000_000),
        endTime = Instant.fromEpochMilliseconds(1_752_607_200_000),
        latitude = 49.8397,
        longitude = 24.0297,
        placeName = "Rynok Square",
        description = "Wandering the old town",
        descriptionAttempts = 0,
        syncStatus = SyncStatus.PENDING,
        updatedAt = Instant.fromEpochMilliseconds(1_752_607_200_000),
    )

    private fun photoEntity(
        id: String,
        episodeId: String,
    ) = PhotoEntity(
        id = id,
        episodeId = episodeId,
        uri = "content://photos/$id",
        takenAt = Instant.fromEpochMilliseconds(1_752_600_500_000),
        latitude = 49.8397,
        longitude = 24.0297,
    )

    @Test
    fun `insert episode with photos then getById returns both`() =
        runTest {
            val episode = episodeEntity()
            val photos = listOf(photoEntity("photo-1", episode.id), photoEntity("photo-2", episode.id))

            dao.upsert(episode, photos)

            val loaded = dao.getById(episode.id)
            assertEquals(episode, loaded?.episode)
            assertEquals(photos.toSet(), loaded?.photos?.toSet())
        }

    @Test
    fun `getById returns null for unknown id`() =
        runTest {
            assertNull(dao.getById("unknown"))
        }

    @Test
    fun `upsert with existing id replaces the photo set`() =
        runTest {
            val episode = episodeEntity()
            val original = listOf(photoEntity("photo-1", episode.id))
            val replacement = listOf(photoEntity("photo-2", episode.id), photoEntity("photo-3", episode.id))

            dao.upsert(episode, original)
            dao.upsert(episode, replacement)

            val loaded = dao.getById(episode.id)
            assertEquals(replacement.toSet(), loaded?.photos?.toSet())
        }

    @Test
    fun `delete removes the episode and cascades to its photos`() =
        runTest {
            val episode = episodeEntity()
            val photos = listOf(photoEntity("photo-1", episode.id))
            dao.upsert(episode, photos)

            dao.delete(episode.id)

            assertNull(dao.getById(episode.id))
        }

    @Test
    fun `observeByDiaryEntry emits list updates on insert update and delete`() =
        runTest {
            val diaryEntryId = "entry-1"
            val episode = episodeEntity(diaryEntryId = diaryEntryId)
            val emissions = Channel<List<EpisodeWithPhotos>>(capacity = Channel.UNLIMITED)
            val job = launch { dao.observeByDiaryEntry(diaryEntryId).collect { emissions.send(it) } }

            assertEquals(emptyList(), emissions.receive())

            dao.upsert(episode, emptyList())
            assertEquals(listOf(episode), emissions.receive().map { it.episode })

            dao.upsert(episode, listOf(photoEntity("photo-1", episode.id)))
            val withPhoto = emissions.receive()
            assertEquals(1, withPhoto.single().photos.size)

            dao.delete(episode.id)
            assertEquals(emptyList(), emissions.receive())

            job.cancel()
        }
}
