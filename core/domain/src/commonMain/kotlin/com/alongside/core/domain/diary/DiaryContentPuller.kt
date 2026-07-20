package com.alongside.core.domain.diary

/**
 * Pulls diary content (entries + episodes) for [tripId] from the remote store into local
 * storage. Without this seam, a paired device only ever sees its own diary content:
 * `observeByTrip`/`observeByDiaryEntry` are plain local Room queries with no remote listener
 * behind them.
 *
 * Partner-authored entries are always overwritten from remote on every call - local storage
 * never independently edits the partner's documents, so there's nothing to protect. Own-authored
 * entries are only ever pulled in to fill a genuine local gap (docs/roadmap.md M12.7 - local
 * storage lost after this device's own entry already synced, e.g. app data cleared): an own entry
 * that already exists locally, in any state, is left untouched, so this can never clobber a
 * pending local edit still working its way through the sync queue.
 */
public interface DiaryContentPuller {
    public suspend fun pullTripContent(
        tripId: String,
        ownUserId: String,
    )
}
