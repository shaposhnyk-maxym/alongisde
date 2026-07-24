package com.alongside.feature.settings

import com.alongside.core.domain.trip.DeleteTripResult
import com.alongside.core.domain.trip.LeaveTripResult
import com.alongside.core.domain.trip.TripManagementRepository

internal class FakeTripManagementRepository : TripManagementRepository {
    val deleteCalls = mutableListOf<Pair<String, String>>()
    val leaveCalls = mutableListOf<Pair<String, String>>()
    var nextDeleteResult: DeleteTripResult = DeleteTripResult.Deleted
    var nextLeaveResult: LeaveTripResult = LeaveTripResult.Deleted

    override suspend fun deleteTrip(
        tripId: String,
        callerId: String,
    ): DeleteTripResult {
        deleteCalls += tripId to callerId
        return nextDeleteResult
    }

    override suspend fun leaveTrip(
        tripId: String,
        callerId: String,
    ): LeaveTripResult {
        leaveCalls += tripId to callerId
        return nextLeaveResult
    }
}
