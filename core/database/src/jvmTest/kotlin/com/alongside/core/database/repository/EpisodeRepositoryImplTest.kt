package com.alongside.core.database.repository

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.alongside.core.database.AlongsideDatabase
import com.alongside.core.model.diary.Episode
import com.alongside.core.model.diary.Photo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class EpisodeRepositoryImplTest {
    private lateinit var database: AlongsideDatabase
    private lateinit var repository: EpisodeRepositoryImpl

    @BeforeTest
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder<AlongsideDatabase>()
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        repository = EpisodeRepositoryImpl(database)
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    private fun photo(id: String) =
        Photo(
            id = id,
            uri = "content://photos/$id",
            takenAt = Instant.fromEpochMilliseconds(1_752_600_500_000),
            latitude = 49.8397,
            longitude = 24.0297,
        )

    private fun episode(
        id: String = "episode-1",
        diaryEntryId: String = "entry-1",
        photos: List<Photo> = listOf(photo("photo-1"), photo("photo-2")),
    ) = Episode(
        id = id,
        diaryEntryId = diaryEntryId,
        startTime = Instant.fromEpochMilliseconds(1_752_600_000_000),
        endTime = Instant.fromEpochMilliseconds(1_752_607_200_000),
        latitude = 49.8397,
        longitude = 24.0297,
        placeName = "Rynok Square",
        description = "Wandering the old town",
        descriptionAttempts = 0,
        photos = photos,
    )

    @Test
    fun `upsert then getById round trips the full episode including photos`() =
        runTest {
            val episode = episode()

            repository.upsert(episode)

            val loaded = repository.getById(episode.id)
            assertEquals(episode.copy(photos = emptyList()), loaded?.copy(photos = emptyList()))
            assertEquals(episode.photos.toSet(), loaded?.photos?.toSet())
        }

    @Test
    fun `getById returns null for unknown id`() =
        runTest {
            assertNull(repository.getById("unknown"))
        }

    @Test
    fun `delete removes the episode`() =
        runTest {
            val episode = episode()
            repository.upsert(episode)

            repository.delete(episode.id)

            assertNull(repository.getById(episode.id))
        }

    @Test
    fun `observeByDiaryEntry emits the mapped domain episodes`() =
        runTest {
            val episode = episode()

            repository.upsert(episode)

            val loaded = repository.observeByDiaryEntry(episode.diaryEntryId).first().single()
            assertEquals(episode.copy(photos = emptyList()), loaded.copy(photos = emptyList()))
            assertEquals(episode.photos.toSet(), loaded.photos.toSet())
        }
}
