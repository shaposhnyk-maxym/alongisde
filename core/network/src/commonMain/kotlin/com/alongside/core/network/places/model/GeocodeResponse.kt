package com.alongside.core.network.places.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class GeocodeResponse(
    val results: List<GeocodeResult> = emptyList(),
    val status: String,
    @SerialName("error_message") val errorMessage: String? = null,
)
