package com.alongside.core.network.places

public class GooglePlacesConfig(
    public val apiKey: String,
) {
    public fun reverseGeocodeUrl(
        latitude: Double,
        longitude: Double,
    ): String = "https://maps.googleapis.com/maps/api/geocode/json?latlng=$latitude,$longitude&key=$apiKey"

    /** Places API (New) Photo Media endpoint - [photoName] is a photo's `name` field, as returned by a text search. */
    public fun photoMediaUrl(
        photoName: String,
        maxWidthPx: Int = DEFAULT_PHOTO_MAX_WIDTH_PX,
    ): String = "https://places.googleapis.com/v1/$photoName/media?maxWidthPx=$maxWidthPx&key=$apiKey"

    public companion object {
        /** Places API (New) Text Search - the single call that returns name+rating+category+photos together. */
        public const val SEARCH_TEXT_URL: String = "https://places.googleapis.com/v1/places:searchText"
        private const val DEFAULT_PHOTO_MAX_WIDTH_PX = 800
    }
}
