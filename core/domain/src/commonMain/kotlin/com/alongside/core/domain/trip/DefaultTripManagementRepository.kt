package com.alongside.core.domain.trip

/** Owner-only delete, mutual leave - both enforced here, not just in the UI. */
public class DefaultTripManagementRepository(
    private val tripRepository: TripRepository,
) : TripManagementRepository {
    override suspend fun deleteTrip(
        tripId: String,
        callerId: String,
    ): DeleteTripResult {
        val trip = tripRepository.getById(tripId)
        return when {
            trip == null -> DeleteTripResult.NotFound
            trip.ownerId != callerId -> DeleteTripResult.NotOwner
            else -> {
                tripRepository.delete(tripId)
                DeleteTripResult.Deleted
            }
        }
    }

    override suspend fun leaveTrip(
        tripId: String,
        callerId: String,
    ): LeaveTripResult {
        val trip = tripRepository.getById(tripId) ?: return LeaveTripResult.NotFound
        return when (callerId) {
            trip.memberId -> {
                val updated = trip.copy(memberId = null)
                tripRepository.upsert(updated)
                LeaveTripResult.Left(updated)
            }
            trip.ownerId -> {
                val member = trip.memberId
                if (member != null) {
                    val updated = trip.copy(ownerId = member, memberId = null)
                    tripRepository.upsert(updated)
                    LeaveTripResult.OwnershipTransferred(updated)
                } else {
                    tripRepository.delete(tripId)
                    LeaveTripResult.Deleted
                }
            }
            else -> LeaveTripResult.NotFound
        }
    }
}
