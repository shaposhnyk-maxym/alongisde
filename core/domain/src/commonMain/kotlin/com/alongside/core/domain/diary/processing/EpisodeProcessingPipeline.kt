package com.alongside.core.domain.diary.processing

import com.alongside.core.model.diary.Episode
import com.alongside.core.model.diary.Photo
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
        private val generateEpisodeId: () -> String = { Uuid.random().toString() },
    ) {
        public suspend fun process(
            diaryEntryId: String,
            photos: List<Photo>,
            languageTag: String,
        ): List<Episode> = clusterPhotosIntoEpisodes(photos).map { processCluster(diaryEntryId, it, languageTag) }

        private suspend fun processCluster(
            diaryEntryId: String,
            cluster: List<Photo>,
            languageTag: String,
        ): Episode {
            val centroidLatitude = cluster.map { it.latitude }.average()
            val centroidLongitude = cluster.map { it.longitude }.average()
            val placeName =
                when (val result = geocodingClient.reverseGeocode(centroidLatitude, centroidLongitude)) {
                    is GeocodingResult.Found -> result.placeName
                    GeocodingResult.NotFound -> null
                    is GeocodingResult.Failure -> null
                }

            val representativePhotos = selectRepresentativePhotos(cluster)
            val images = representativePhotos.map { imageBytesLoader(it) }
            val description =
                when (val result = visionDescriptionClient.describeEpisode(images, placeName, languageTag)) {
                    is VisionDescriptionResult.Generated -> result.text
                    is VisionDescriptionResult.Failure -> null
                }

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
                photos = cluster,
            )
        }
    }
