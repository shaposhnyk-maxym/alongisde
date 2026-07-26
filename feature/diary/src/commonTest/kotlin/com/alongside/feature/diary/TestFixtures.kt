package com.alongside.feature.diary

import com.alongside.core.domain.diary.DiaryContentPuller
import com.alongside.core.domain.diary.processing.EpisodeVisionDescriptionClient
import com.alongside.core.domain.diary.processing.GeocodingResult
import com.alongside.core.domain.diary.processing.PhotoUploadClient
import com.alongside.core.domain.diary.processing.PhotoUploadResult
import com.alongside.core.domain.diary.processing.PlaceGeocodingClient
import com.alongside.core.domain.diary.processing.VisionDescriptionResult
import com.alongside.core.domain.pretrip.PreTripPhotoContentPuller
import com.alongside.core.domain.pretrip.PreTripPhotoRepository
import com.alongside.core.domain.pretrip.PreTripPhotoUploadClient
import com.alongside.core.domain.pretrip.PreTripPhotoUploadResult
import com.alongside.core.domain.work.BackgroundJobKind
import com.alongside.core.domain.work.BackgroundWorkScheduler
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.diary.DiaryEntry
import com.alongside.core.model.diary.Photo
import com.alongside.core.model.pretrip.PreTripPhoto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

internal fun testDiaryEntry(
    id: String,
    tripId: String = "trip-1",
    userId: String = "uid-1",
    date: LocalDate = LocalDate(2026, 7, 19),
    syncStatus: SyncStatus = SyncStatus.SYNCED,
    closedAt: Instant? = null,
): DiaryEntry =
    DiaryEntry(
        id = id,
        tripId = tripId,
        userId = userId,
        date = date,
        syncStatus = syncStatus,
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = Instant.fromEpochMilliseconds(0),
        closedAt = closedAt,
    )

/** Finds the same scripted result for every call, regardless of who's asking - mirrors the data-layer fixture. */
internal class FakeGeocodingClient(
    private val result: GeocodingResult = GeocodingResult.Found("Rynok Square"),
) : PlaceGeocodingClient {
    override suspend fun reverseGeocode(
        latitude: Double,
        longitude: Double,
    ): GeocodingResult = result
}

internal class FakeVisionClient : EpisodeVisionDescriptionClient {
    override suspend fun describeEpisode(
        images: List<ByteArray>,
        placeName: String?,
        languageTag: String,
    ): VisionDescriptionResult = VisionDescriptionResult.Generated("A wander through the old town.")
}

internal class FakePhotoUploadClient(
    private val resultFor: (Photo) -> PhotoUploadResult = { PhotoUploadResult.Uploaded("https://storage/${it.id}") },
) : PhotoUploadClient {
    override suspend fun upload(
        photo: Photo,
        bytes: ByteArray,
    ): PhotoUploadResult = resultFor(photo)
}

internal class FakeBackgroundWorkScheduler : BackgroundWorkScheduler {
    val scheduledOneOffs = mutableListOf<BackgroundJobKind>()
    var periodicSweepEnsured: Boolean = false
        private set

    override fun scheduleOneOff(kind: BackgroundJobKind) {
        scheduledOneOffs += kind
    }

    override fun ensurePeriodicSweepScheduled() {
        periodicSweepEnsured = true
    }
}

/** No-op by default - tests that care about polling behavior can subclass or wrap this. */
internal class FakeDiaryContentPuller : DiaryContentPuller {
    val pulls = mutableListOf<Pair<String, String>>()

    override suspend fun pullTripContent(
        tripId: String,
        ownUserId: String,
    ) {
        pulls += tripId to ownUserId
    }
}

internal fun testPreTripPhoto(
    id: String,
    tripId: String = "trip-1",
    userId: String = "owner-1",
    remoteUrl: String? = null,
    syncStatus: SyncStatus = SyncStatus.SYNCED,
): PreTripPhoto =
    PreTripPhoto(
        id = id,
        tripId = tripId,
        userId = userId,
        uri = "content://pretrip/$id",
        takenAt = Instant.fromEpochMilliseconds(0),
        latitude = 49.8397,
        longitude = 24.0297,
        remoteUrl = remoteUrl,
        syncStatus = syncStatus,
    )

internal class FakePreTripPhotoRepository : PreTripPhotoRepository {
    private val photos = MutableStateFlow<Map<String, PreTripPhoto>>(emptyMap())
    val upserted = mutableListOf<PreTripPhoto>()

    override suspend fun upsert(photo: PreTripPhoto) {
        upserted += photo
        photos.value = photos.value + (photo.id to photo)
    }

    override suspend fun getById(id: String): PreTripPhoto? = photos.value[id]

    override fun observeByTripAndUser(
        tripId: String,
        userId: String,
    ): Flow<List<PreTripPhoto>> = photos.map { all -> all.values.filter { it.tripId == tripId && it.userId == userId } }

    override suspend fun delete(id: String) {
        photos.value = photos.value - id
    }
}

/** No-op by default - tests that care about polling behavior can subclass or wrap this. */
internal class FakePreTripPhotoContentPuller : PreTripPhotoContentPuller {
    val pulls = mutableListOf<Pair<String, String>>()

    override suspend fun pullTripContent(
        tripId: String,
        ownUserId: String,
    ) {
        pulls += tripId to ownUserId
    }
}

internal class FakePreTripPhotoUploadClient(
    private val resultFor: (PreTripPhoto) -> PreTripPhotoUploadResult = {
        PreTripPhotoUploadResult.Uploaded("https://storage/${it.id}")
    },
) : PreTripPhotoUploadClient {
    val uploaded = mutableListOf<PreTripPhoto>()

    override suspend fun upload(
        photo: PreTripPhoto,
        bytes: ByteArray,
    ): PreTripPhotoUploadResult {
        uploaded += photo
        return resultFor(photo)
    }
}
