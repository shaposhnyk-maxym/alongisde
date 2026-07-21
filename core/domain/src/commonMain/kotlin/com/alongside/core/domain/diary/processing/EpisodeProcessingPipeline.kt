package com.alongside.core.domain.diary.processing

import com.alongside.core.model.SyncStatus
import com.alongside.core.model.diary.Episode
import com.alongside.core.model.diary.Photo
import kotlinx.coroutines.CancellationException
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Orchestrates capture -> processing: cluster photos into episodes, reverse-geocode each
 * episode's centroid, pick a representative photo subset, and generate a vision-based
 * description - entirely against the [PlaceGeocodingClient]/[EpisodeVisionDescriptionClient]
 * seams, so this is testable with fakes independent of core:network (M3 already covers the HTTP
 * layer; this covers what the pipeline does with it).
 */
public class EpisodeProcessingPipeline
    @OptIn(ExperimentalUuidApi::class)
    constructor(
        private val geocodingClient: PlaceGeocodingClient,
        private val visionDescriptionClient: EpisodeVisionDescriptionClient,
        private val imageBytesLoader: suspend (Photo) -> ByteArray,
        private val photoUploadClient: PhotoUploadClient,
        private val generateEpisodeId: () -> String = { Uuid.random().toString() },
        private val clock: Clock = Clock.System,
    ) {
        /**
         * [onEpisodeReady] fires the moment each cluster's episode is built, before the next
         * cluster starts processing - callers that persist incrementally (see
         * `DiaryCaptureCoordinator`) keep whatever earlier clusters already completed even if a
         * later cluster fails. A failing cluster is skipped (logged, not thrown) rather than
         * aborting the whole batch - the same "log failures, not successes" convention used
         * elsewhere in this codebase's network boundary code (e.g. `FirestoreApi.rawRequest`).
         */
        public suspend fun process(
            diaryEntryId: String,
            photos: List<Photo>,
            languageTag: String,
            onEpisodeReady: suspend (Episode) -> Unit = {},
        ): List<Episode> {
            val episodes = mutableListOf<Episode>()
            for (cluster in clusterPhotosIntoEpisodes(photos)) {
                val episode = processClusterOrNull(diaryEntryId, cluster, languageTag) ?: continue
                episodes += episode
                onEpisodeReady(episode)
            }
            return episodes
        }

        @Suppress("TooGenericExceptionCaught")
        private suspend fun processClusterOrNull(
            diaryEntryId: String,
            cluster: List<Photo>,
            languageTag: String,
        ): Episode? =
            try {
                processCluster(diaryEntryId, cluster, languageTag)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                println(
                    "EpisodeProcessingPipeline: cluster ${cluster.map { it.id }} failed, skipping - " +
                        "${e::class.simpleName}: ${e.message}",
                )
                null
            }

        private suspend fun processCluster(
            diaryEntryId: String,
            cluster: List<Photo>,
            languageTag: String,
        ): Episode {
            val centroidLatitude = cluster.map { it.latitude }.average()
            val centroidLongitude = cluster.map { it.longitude }.average()
            val geocoded =
                when (val result = geocodingClient.reverseGeocode(centroidLatitude, centroidLongitude)) {
                    is GeocodingResult.Found -> result
                    GeocodingResult.NotFound -> null
                    is GeocodingResult.Failure -> null
                }
            val placeName = geocoded?.placeName
            val description = describeCluster(cluster, placeName, languageTag)
            val uploadedPhotos = uploadCluster(cluster)

            return Episode(
                id = generateEpisodeId(),
                diaryEntryId = diaryEntryId,
                startTime = cluster.minOf { it.takenAt },
                endTime = cluster.maxOf { it.takenAt },
                latitude = centroidLatitude,
                longitude = centroidLongitude,
                placeName = placeName,
                description = description,
                descriptionAttempts = 1,
                photos = uploadedPhotos,
                syncStatus = SyncStatus.PENDING,
                updatedAt = clock.now(),
                city = geocoded?.city,
                cityPlaceId = geocoded?.cityPlaceId,
                countryCode = geocoded?.countryCode,
                geocodeAttempts = 1,
            )
        }

        /**
         * Retries whatever's still incomplete on [episode] - a photo with no `remoteUrl` (its
         * upload never succeeded, e.g. no network at capture time) gets a fresh upload attempt, a
         * missing `description` gets a fresh vision call, and a missing `placeName` (reverse-
         * geocoding never succeeded, e.g. no network at capture time either) gets a fresh
         * geocoding call - using [episode.latitude]/[episode.longitude] as-is, no re-clustering.
         * `descriptionAttempts`/`geocodeAttempts` only increment when that step was actually
         * attempted (i.e. its result was null going in); an unrelated retry doesn't count against
         * either. A freshly-resolved `placeName` feeds the description call below it, so a
         * capture that failed both geocoding and description offline can recover a place-aware
         * description in the same retry pass. Returns [episode] unchanged if nothing needed
         * retrying or nothing changed.
         */
        public suspend fun retryIncomplete(
            episode: Episode,
            languageTag: String,
        ): Episode {
            val retriedPhotos = retryPhotoUploads(episode.photos)
            val geocodeWasAttempted = episode.placeName == null
            val geocoded = if (geocodeWasAttempted) retryGeocode(episode) else null
            val retriedPlaceName = geocoded?.placeName ?: episode.placeName

            val descriptionWasAttempted = episode.description == null
            val retriedDescription =
                if (descriptionWasAttempted) {
                    describeCluster(retriedPhotos, retriedPlaceName, languageTag)
                } else {
                    episode.description
                }

            if (retriedPhotos == episode.photos &&
                retriedDescription == episode.description &&
                retriedPlaceName == episode.placeName
            ) {
                return episode
            }
            val newDescriptionAttempts =
                if (descriptionWasAttempted) episode.descriptionAttempts + 1 else episode.descriptionAttempts
            val newGeocodeAttempts =
                if (geocodeWasAttempted) episode.geocodeAttempts + 1 else episode.geocodeAttempts
            return episode.copy(
                photos = retriedPhotos,
                description = retriedDescription,
                descriptionAttempts = newDescriptionAttempts,
                placeName = retriedPlaceName,
                city = geocoded?.city ?: episode.city,
                cityPlaceId = geocoded?.cityPlaceId ?: episode.cityPlaceId,
                countryCode = geocoded?.countryCode ?: episode.countryCode,
                geocodeAttempts = newGeocodeAttempts,
                updatedAt = clock.now(),
            )
        }

        private suspend fun retryPhotoUploads(photos: List<Photo>): List<Photo> =
            photos.map { photo ->
                if (photo.remoteUrl != null) return@map photo
                val bytes = loadImageBytesOrNull(photo) ?: return@map photo
                when (val result = photoUploadClient.upload(photo, bytes)) {
                    is PhotoUploadResult.Uploaded -> photo.copy(remoteUrl = result.remoteUrl)
                    is PhotoUploadResult.Failure -> photo
                }
            }

        private suspend fun retryGeocode(episode: Episode): GeocodingResult.Found? =
            when (val result = geocodingClient.reverseGeocode(episode.latitude, episode.longitude)) {
                is GeocodingResult.Found -> result
                GeocodingResult.NotFound -> null
                is GeocodingResult.Failure -> null
            }

        private suspend fun describeCluster(
            cluster: List<Photo>,
            placeName: String?,
            languageTag: String,
        ): String? {
            val representativePhotos = selectRepresentativePhotos(cluster)
            val images = representativePhotos.mapNotNull { loadImageBytesOrNull(it) }
            // Couldn't read/compress every representative photo - the same degraded outcome as a
            // vision Failure, not worth sending a partial/wrong image set.
            if (images.size != representativePhotos.size) return null
            return when (val result = visionDescriptionClient.describeEpisode(images, placeName, languageTag)) {
                is VisionDescriptionResult.Generated -> result.text
                is VisionDescriptionResult.Failure -> null
            }
        }

        // Every photo in the cluster gets an upload attempt, not just the representative subset
        // selected for Gemini above - a photo the vision client never sees still needs to sync to
        // the partner. This re-reads bytes for photos already loaded for vision; acceptable
        // duplication, not an oversight (compression, if any, is the upload client's concern -
        // see FirebaseStorageUploadClient - not this orchestration layer's).
        private suspend fun uploadCluster(cluster: List<Photo>): List<Photo> =
            cluster.map { photo ->
                val bytes = loadImageBytesOrNull(photo)
                if (bytes == null) {
                    photo
                } else {
                    when (val result = photoUploadClient.upload(photo, bytes)) {
                        is PhotoUploadResult.Uploaded -> photo.copy(remoteUrl = result.remoteUrl)
                        is PhotoUploadResult.Failure -> photo
                    }
                }
            }

        // A photo's local read/compress step (EXIF decode, bitmap scaling, ...) can throw
        // uncaught (SecurityException on a revoked content:// permission, OutOfMemoryError on a
        // huge original, ...) unlike the network clients above, which already convert every
        // failure into their own Result types - degrade to "no bytes" instead of losing the
        // whole cluster over one bad photo.
        @Suppress("TooGenericExceptionCaught")
        private suspend fun loadImageBytesOrNull(photo: Photo): ByteArray? =
            try {
                imageBytesLoader(photo)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                println(
                    "EpisodeProcessingPipeline: failed to read/compress photo ${photo.id} - " +
                        "${e::class.simpleName}: ${e.message}",
                )
                null
            }
    }
