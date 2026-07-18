package com.alongside.core.network.places

public class GooglePlacesConfig(
    public val apiKey: String,
) {
    public fun reverseGeocodeUrl(
        latitude: Double,
        longitude: Double,
    ): String = "https://maps.googleapis.com/maps/api/geocode/json?latlng=$latitude,$longitude&key=$apiKey"
}
