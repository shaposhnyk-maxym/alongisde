package com.alongside.core.domain.trip

public interface TripManagementRepository {
    /** Only [callerId] == the trip's owner may delete it - see [DeleteTripResult]. */
    public suspend fun deleteTrip(
        tripId: String,
        callerId: String,
    ): DeleteTripResult

    /** Either the owner or the member may leave - see [LeaveTripResult]. */
    public suspend fun leaveTrip(
        tripId: String,
        callerId: String,
    ): LeaveTripResult
}
