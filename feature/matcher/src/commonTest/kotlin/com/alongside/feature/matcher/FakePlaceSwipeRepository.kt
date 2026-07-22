package com.alongside.feature.matcher

import com.alongside.core.domain.place.PlaceSwipeRepository
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.place.PlaceSwipe
import com.alongside.core.model.place.SwipeDirection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlin.time.Instant

internal fun fakeSwipe(
    candidateId: String,
    userId: String,
    direction: SwipeDirection,
    tripId: String = "trip-1",
): PlaceSwipe =
    PlaceSwipe(
        id = "$candidateId::$userId",
        tripId = tripId,
        candidateId = candidateId,
        userId = userId,
        direction = direction,
        swipedAt = Instant.fromEpochMilliseconds(0),
        syncStatus = SyncStatus.SYNCED,
        updatedAt = Instant.fromEpochMilliseconds(0),
    )

/** Map-backed local place swipes fake, keyed by id - upserting the same id overwrites it, mirroring Room's REPLACE. */
internal class FakePlaceSwipeRepository : PlaceSwipeRepository {
    private val swipes = MutableStateFlow<Map<String, PlaceSwipe>>(emptyMap())
    val upserted = mutableListOf<PlaceSwipe>()

    fun seed(vararg initial: PlaceSwipe) {
        swipes.value = swipes.value + initial.associateBy { it.id }
    }

    override suspend fun upsert(swipe: PlaceSwipe) {
        upserted += swipe
        swipes.value = swipes.value + (swipe.id to swipe)
    }

    override suspend fun getById(id: String): PlaceSwipe? = swipes.value[id]

    override fun observeByTrip(tripId: String): Flow<List<PlaceSwipe>> = swipes.map { it.forTrip(tripId) }
}

private fun Map<String, PlaceSwipe>.forTrip(tripId: String): List<PlaceSwipe> = values.filter { it.tripId == tripId }
