package com.alongside.data.place

import com.alongside.core.database.sync.SyncOperationStore
import com.alongside.core.domain.place.PlaceSwipeRepository
import com.alongside.core.domain.work.BackgroundJobKind
import com.alongside.core.domain.work.BackgroundWorkScheduler
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.place.PlaceSwipe
import com.alongside.core.network.queue.SyncOperation
import com.alongside.core.network.queue.SyncOperationType
import com.alongside.data.sync.SyncOperationCodec
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Offline-first [PlaceSwipeRepository]: mirrors [SyncingPlaceCandidateRepository]'s pattern
 * exactly - every write lands in Room stamped `updatedAt = now, syncStatus = PENDING` and
 * enqueues a durable sync operation. Unlike [PlaceCandidate][com.alongside.core.model.place.PlaceCandidate]'s
 * old `ownerSwipe`/`memberSwipe` fields, a [PlaceSwipe]'s id is deterministic per (candidateId,
 * userId), so this never overwrites another user's record - no conflict-resolution concern here,
 * this class exists purely to plug into the existing durable sync-queue/WorkManager machinery.
 */
public class SyncingPlaceSwipeRepository
    @OptIn(ExperimentalUuidApi::class)
    constructor(
        private val local: PlaceSwipeRepository,
        private val store: SyncOperationStore,
        private val backgroundWorkScheduler: BackgroundWorkScheduler,
        private val clock: Clock = Clock.System,
        private val generateOpId: () -> String = { Uuid.random().toString() },
    ) : PlaceSwipeRepository {
        override suspend fun upsert(swipe: PlaceSwipe) {
            val now = clock.now()
            val stamped = swipe.copy(updatedAt = now, syncStatus = SyncStatus.PENDING)
            local.upsert(stamped)
            val operation =
                SyncOperation(
                    id = generateOpId(),
                    collectionPath = PlaceSwipeFirestoreMapper.COLLECTION_PATH,
                    documentId = stamped.id,
                    type = SyncOperationType.UPSERT,
                    fields = PlaceSwipeFirestoreMapper.toFields(stamped),
                )
            store.append(SyncOperationCodec.toPersisted(operation, enqueuedAt = now))
            backgroundWorkScheduler.scheduleOneOff(BackgroundJobKind.SYNC_QUEUE_FLUSH)
        }

        override suspend fun getById(id: String): PlaceSwipe? = local.getById(id)

        override fun observeByTrip(tripId: String): Flow<List<PlaceSwipe>> = local.observeByTrip(tripId)
    }
