package com.alongside.core.domain.place.importing

import com.alongside.core.domain.diary.processing.GeocodingResult
import com.alongside.core.domain.diary.processing.PlaceGeocodingClient
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.place.PlaceCandidate
import com.alongside.core.model.place.PlacePhoto
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

public sealed class PlaceImportResult {
    public data class Imported(
        public val place: PlaceCandidate,
    ) : PlaceImportResult()

    public data object NotFound : PlaceImportResult()

    public data class Failure(
        public val cause: Throwable,
    ) : PlaceImportResult()
}

/**
 * Orchestrates a Google Maps share-link import end to end: resolve the short link's redirect,
 * parse the resolved URL, look up the place's details, re-host its photos in Firebase Storage,
 * and assemble a [PlaceCandidate] - entirely against the [ShareLinkRedirectResolver]/
 * [PlaceDetailsLookupClient]/[PlacePhotoClient]/[PlacePhotoUploadClient] seams, so this is
 * testable with fakes independent of core:network (same split as
 * [com.alongside.core.domain.diary.processing.EpisodeProcessingPipeline]).
 */
public class PlaceImportPipeline
    @OptIn(ExperimentalUuidApi::class)
    constructor(
        private val redirectResolver: ShareLinkRedirectResolver,
        private val detailsLookupClient: PlaceDetailsLookupClient,
        private val photoClient: PlacePhotoClient,
        private val photoUploadClient: PlacePhotoUploadClient,
        private val placeGeocodingClient: PlaceGeocodingClient,
        private val generatePlaceId: () -> String = { Uuid.random().toString() },
        private val clock: Clock = Clock.System,
    ) {
        public suspend fun import(
            shareUrl: String,
            tripId: String,
            addedByUserId: String,
        ): PlaceImportResult =
            when (val outcome = resolveAndLookup(shareUrl)) {
                is LookupOutcome.NotFound -> PlaceImportResult.NotFound
                is LookupOutcome.Failure -> PlaceImportResult.Failure(outcome.cause)
                is LookupOutcome.Found -> buildImportedResult(outcome.details, tripId, addedByUserId)
            }

        private sealed class LookupOutcome {
            data class Found(
                val details: PlaceDetailsResult.Found,
            ) : LookupOutcome()

            data object NotFound : LookupOutcome()

            data class Failure(
                val cause: Throwable,
            ) : LookupOutcome()
        }

        private suspend fun resolveAndLookup(shareUrl: String): LookupOutcome {
            val resolvedUrl =
                when (val redirectResult = redirectResolver.resolve(shareUrl)) {
                    is ShareLinkRedirectResult.Resolved -> redirectResult.url
                    is ShareLinkRedirectResult.Failure -> return LookupOutcome.Failure(redirectResult.cause)
                }

            val parsedLink = GoogleMapsShareLinkParser.parse(resolvedUrl)
            return if (parsedLink == null) {
                LookupOutcome.Failure(IllegalArgumentException("Not a Google Maps place URL: $resolvedUrl"))
            } else {
                lookupDetails(parsedLink)
            }
        }

        private suspend fun lookupDetails(parsedLink: ParsedGoogleMapsLink): LookupOutcome {
            val query =
                PlaceLookupQuery(
                    name = parsedLink.displayName,
                    address = parsedLink.address,
                    latitude = parsedLink.latitude,
                    longitude = parsedLink.longitude,
                )
            return when (val result = detailsLookupClient.lookup(query)) {
                is PlaceDetailsResult.Found -> LookupOutcome.Found(result)
                PlaceDetailsResult.NotFound -> LookupOutcome.NotFound
                is PlaceDetailsResult.Failure -> LookupOutcome.Failure(result.cause)
            }
        }

        private suspend fun buildImportedResult(
            details: PlaceDetailsResult.Found,
            tripId: String,
            addedByUserId: String,
        ): PlaceImportResult.Imported {
            val placeId = generatePlaceId()
            val now = clock.now()
            val geocoded = lookupGeocoding(details.latitude, details.longitude)
            return PlaceImportResult.Imported(
                PlaceCandidate(
                    id = placeId,
                    tripId = tripId,
                    name = details.name,
                    latitude = details.latitude,
                    longitude = details.longitude,
                    note = null,
                    addedByUserId = addedByUserId,
                    ownerSwipe = null,
                    memberSwipe = null,
                    syncStatus = SyncStatus.PENDING,
                    createdAt = now,
                    updatedAt = now,
                    photos = uploadPhotos(placeId, details.photoRefs),
                    rating = details.rating,
                    category = details.category,
                    city = geocoded?.city,
                    cityPlaceId = geocoded?.cityPlaceId,
                    countryCode = geocoded?.countryCode,
                ),
            )
        }

        // City/country are grouping/display metadata, not required for the place to import - same
        // "enrichment can fail without failing the import" convention as photo upload above.
        private suspend fun lookupGeocoding(
            latitude: Double,
            longitude: Double,
        ): GeocodingResult.Found? =
            when (val result = placeGeocodingClient.reverseGeocode(latitude, longitude)) {
                is GeocodingResult.Found -> result
                GeocodingResult.NotFound -> null
                is GeocodingResult.Failure -> null
            }

        // One failed photo (fetch or upload) leaves that PlacePhoto's remoteUrl null rather than
        // dropping it - the same "degrade, don't abort" convention as
        // EpisodeProcessingPipeline.uploadCluster, but keeping the photoRef around (instead of
        // discarding the entry entirely) is what makes retryIncomplete possible below.
        private suspend fun uploadPhotos(
            placeId: String,
            photoRefs: List<String>,
        ): List<PlacePhoto> =
            photoRefs.mapIndexed { index, photoRef ->
                PlacePhoto(photoRef = photoRef, remoteUrl = uploadOne(placeId, index, photoRef))
            }

        private suspend fun uploadOne(
            placeId: String,
            index: Int,
            photoRef: String,
        ): String? {
            val bytes = photoClient.fetchPhotoBytes(photoRef) ?: return null
            return when (val result = photoUploadClient.upload(placeId, index, bytes)) {
                is PlacePhotoUploadResult.Uploaded -> result.remoteUrl
                is PlacePhotoUploadResult.Failure -> null
            }
        }

        /**
         * Background maintenance pass (see `feature:places`' retry poll loop): a capture-time
         * hiccup can leave a [PlaceCandidate] with a photo missing its `remoteUrl` - only ever
         * visible locally, never synced. Re-attempts each such photo by its original index (so a
         * retried upload lands at the same Storage path a first-try success would have), leaving
         * already-uploaded photos untouched and a still-failing photo null without throwing - the
         * same degrade-not-abort convention as [import].
         */
        public suspend fun retryIncomplete(place: PlaceCandidate): PlaceCandidate {
            val retried =
                place.photos.mapIndexed { index, photo ->
                    if (photo.remoteUrl != null) {
                        photo
                    } else {
                        photo.copy(remoteUrl = uploadOne(place.id, index, photo.photoRef))
                    }
                }
            return place.copy(photos = retried)
        }
    }
