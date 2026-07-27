package com.alongside.core.domain.trip

import com.alongside.core.model.trip.Trip
import kotlinx.datetime.LocalDate

public enum class TripPhase { PRE_TRIP, ACTIVE, POST_TRIP }

/** Where [today] falls relative to [trip]'s window - both [Trip.startDate] and [Trip.endDate] are active days. */
public fun tripPhase(
    trip: Trip,
    today: LocalDate,
): TripPhase =
    when {
        today < trip.startDate -> TripPhase.PRE_TRIP
        today <= trip.endDate -> TripPhase.ACTIVE
        else -> TripPhase.POST_TRIP
    }
