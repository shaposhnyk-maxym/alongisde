package com.alongside.core.network.places.model

import kotlinx.serialization.Serializable

/** Google's standard API error envelope - what actual HTTP 4xx/5xx failures return (distinct from
 * [GeocodeResponse.status], which reports API-level errors on an otherwise-200 response). */
@Serializable
public data class GooglePlacesErrorResponse(
    public val error: GooglePlacesErrorDetail,
)

@Serializable
public data class GooglePlacesErrorDetail(
    public val code: Int,
    public val message: String,
    public val status: String,
)
