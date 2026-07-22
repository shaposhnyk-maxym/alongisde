package com.alongside.feature.places

import com.alongside.core.domain.place.PlaceContentPuller

/** No-op by default - tests that care about polling behavior can subclass or wrap this. */
internal class FakePlaceContentPuller : PlaceContentPuller {
    val pulls = mutableListOf<Pair<String, String>>()

    override suspend fun pullTripContent(
        tripId: String,
        ownUserId: String,
    ) {
        pulls += tripId to ownUserId
    }
}
