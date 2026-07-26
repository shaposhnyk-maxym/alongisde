package com.alongside.feature.diary.presentation

import com.alongside.core.domain.pretrip.PreTripPhotoRepository
import com.alongside.core.domain.pretrip.PreTripPhotoUploadClient
import com.alongside.core.domain.pretrip.PreTripPhotoUploadResult
import com.alongside.core.domain.work.BackgroundJobKind
import com.alongside.core.domain.work.BackgroundWorkScheduler
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.diary.Photo
import com.alongside.core.model.pretrip.PreTripPhoto
import com.alongside.feature.diary.capture.ExifPhotoReader
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * The Countdown card's capture write-path: EXIF read -> eager upload -> persistence. Split out
 * from [DiaryCaptureCoordinator] (rather than added to it) purely to keep that constructor under
 * detekt's `LongParameterList` threshold, the same reasoning that class's own KDoc already
 * documents for why it was split from [DiaryTimelineContainer] in the first place.
 *
 * Pre-trip photos have no processing pipeline (no clustering, no vision description, no
 * geocoding) - unlike [DiaryCaptureCoordinator.capture], each photo is uploaded and persisted as
 * its own independent row, with a randomly generated id (no "existing entry to merge into"
 * concept that would require a deterministic one).
 */
public class PreTripPhotoCaptureCoordinator
    @OptIn(ExperimentalUuidApi::class)
    constructor(
        private val exifPhotoReader: ExifPhotoReader,
        private val imageBytesLoader: suspend (Photo) -> ByteArray,
        private val preTripPhotoUploadClient: PreTripPhotoUploadClient,
        private val preTripPhotoRepository: PreTripPhotoRepository,
        private val backgroundWorkScheduler: BackgroundWorkScheduler,
        private val generateId: () -> String = { Uuid.random().toString() },
    ) {
        public suspend fun capture(
            tripId: String,
            userId: String,
            uris: List<String>,
        ) {
            for (photo in exifPhotoReader.readExifPhotos(uris)) {
                val preTripPhoto =
                    PreTripPhoto(
                        id = generateId(),
                        tripId = tripId,
                        userId = userId,
                        uri = photo.uri,
                        takenAt = photo.takenAt,
                        latitude = photo.latitude,
                        longitude = photo.longitude,
                        remoteUrl = null,
                        syncStatus = SyncStatus.PENDING,
                    )
                preTripPhotoRepository.upsert(preTripPhoto.copy(remoteUrl = uploadOrNull(photo, preTripPhoto)))
            }
        }

        // Degrades to "no remoteUrl yet" instead of losing the photo over an upload hiccup (no
        // network at capture time, etc.) - the same "log failures, not successes" convention
        // EpisodeProcessingPipeline.uploadCluster already uses. Recoverable later via
        // flushPendingSync() or a future retry pass, not silently dropped.
        @Suppress("TooGenericExceptionCaught")
        private suspend fun uploadOrNull(
            photo: Photo,
            preTripPhoto: PreTripPhoto,
        ): String? {
            val bytes =
                try {
                    imageBytesLoader(photo)
                } catch (e: Exception) {
                    println("PreTripPhotoCaptureCoordinator: failed to read/compress photo ${photo.id} - ${e.message}")
                    return null
                }
            return when (val result = preTripPhotoUploadClient.upload(preTripPhoto, bytes)) {
                is PreTripPhotoUploadResult.Uploaded -> result.remoteUrl
                is PreTripPhotoUploadResult.Failure -> null
            }
        }

        /** Manual override while the background sync pipeline is still being debugged (M19.8). */
        public fun flushPendingSync() {
            backgroundWorkScheduler.scheduleOneOff(BackgroundJobKind.SYNC_QUEUE_FLUSH)
        }
    }
