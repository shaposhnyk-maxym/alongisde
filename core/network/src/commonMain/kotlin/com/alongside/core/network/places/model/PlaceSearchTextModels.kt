package com.alongside.core.network.places.model

import kotlinx.serialization.Serializable

@Serializable
internal data class PlaceSearchTextRequest(
    val textQuery: String,
)

@Serializable
public data class PlaceSearchTextResponse(
    public val places: List<GooglePlaceResult> = emptyList(),
)

@Serializable
public data class GooglePlaceResult(
    public val displayName: GoogleLocalizedText? = null,
    public val rating: Double? = null,
    public val primaryTypeDisplayName: GoogleLocalizedText? = null,
    public val photos: List<GooglePlacePhoto> = emptyList(),
    public val location: GoogleLatLng? = null,
)

@Serializable
public data class GoogleLocalizedText(
    public val text: String,
)

@Serializable
public data class GooglePlacePhoto(
    public val name: String,
)

@Serializable
public data class GoogleLatLng(
    public val latitude: Double,
    public val longitude: Double,
)
