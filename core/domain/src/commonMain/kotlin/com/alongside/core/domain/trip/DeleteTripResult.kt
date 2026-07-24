package com.alongside.core.domain.trip

/** Outcome of attempting to delete a trip. */
public sealed interface DeleteTripResult {
    /** The trip was removed. */
    public data object Deleted : DeleteTripResult

    /** The caller isn't the trip's owner - only the owner may delete a trip. */
    public data object NotOwner : DeleteTripResult

    /** No trip exists with this id. */
    public data object NotFound : DeleteTripResult

    /** Authorized and recorded, but couldn't be confirmed against the remote right now. */
    public data object SyncFailed : DeleteTripResult
}
