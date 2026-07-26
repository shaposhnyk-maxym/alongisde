package com.alongside.core.ui.recap

import com.alongside.core.model.diary.Photo
import com.alongside.core.model.place.PlacePhoto
import com.alongside.core.model.pretrip.PreTripPhoto

/** Prefer the uploaded copy once synced, same convention as `feature:diary`'s photo galleries. */
internal fun Photo.loadableModel(): String = remoteUrl ?: uri

internal fun PreTripPhoto.loadableModel(): String = remoteUrl ?: uri

/** No local URI on this one - stays `null` (renders the muted fallback tile) until synced. */
internal fun PlacePhoto.loadableModel(): String? = remoteUrl

private const val METERS_PER_KILOMETER = 1000.0

/** "482 km" / "40 m" - whole-number, unit chosen by magnitude, for the recap distance slides. */
internal fun formatRecapDistanceMeters(distanceMeters: Double): String =
    if (distanceMeters >= METERS_PER_KILOMETER) {
        "${(distanceMeters / METERS_PER_KILOMETER).toInt()} km"
    } else {
        "${distanceMeters.toInt()} m"
    }
