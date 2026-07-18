package com.alongside.core.network.places.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class AddressComponent(
    @SerialName("long_name") val longName: String,
    @SerialName("short_name") val shortName: String,
    val types: List<String> = emptyList(),
)
