package com.alongside.core.model.place

/**
 * One imported place photo: [photoRef] is the Google Places photo reference the byte fetch and
 * any retry key off of; [remoteUrl] is the Firebase Storage download URL once uploaded, null
 * while still pending or after a failed attempt - the same "identity survives upload failure"
 * shape as [com.alongside.core.model.diary.Photo].
 */
public data class PlacePhoto(
    val photoRef: String,
    val remoteUrl: String?,
)
