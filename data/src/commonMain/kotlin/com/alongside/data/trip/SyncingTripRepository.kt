package com.alongside.data.trip

import com.alongside.core.database.sync.SyncOperationStore
import com.alongside.core.domain.trip.TripRepository
import com.alongside.core.domain.work.BackgroundJobKind
import com.alongside.core.domain.work.BackgroundWorkScheduler
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.trip.Trip
import com.alongside.core.network.queue.SyncOperation
import com.alongside.core.network.queue.SyncOperationType
import com.alongside.data.sync.SyncOperationCodec
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Offline-first [TripRepository]: every write lands in Room stamped
 * `updatedAt = now, syncStatus = PENDING` and enqueues a durable sync operation;
 * reads come straight from Room. The actual push happens in `SyncCoordinator.sync()`, triggered
 * on a poller today plus a WorkManager backstop (docs/roadmap.md M12.11) for when the app isn't
 * running to poll at all.
 */
public class SyncingTripRepository
    @OptIn(ExperimentalUuidApi::class)
    constructor(
        private val local: TripRepository,
        private val store: SyncOperationStore,
        private val backgroundWorkScheduler: BackgroundWorkScheduler,
        private val clock: Clock = Clock.System,
        private val generateOpId: () -> String = { Uuid.random().toString() },
    ) : TripRepository {
        override suspend fun upsert(trip: Trip) {
            val now = clock.now()
            val stamped = trip.copy(updatedAt = now, syncStatus = SyncStatus.PENDING)
            local.upsert(stamped)
            val operation =
                SyncOperation(
                    id = generateOpId(),
                    collectionPath = TripFirestoreMapper.COLLECTION_PATH,
                    documentId = stamped.id,
                    type = SyncOperationType.UPSERT,
                    fields = TripFirestoreMapper.toFields(stamped),
                )
            store.append(SyncOperationCodec.toPersisted(operation, enqueuedAt = now))
            backgroundWorkScheduler.scheduleOneOff(BackgroundJobKind.SYNC_QUEUE_FLUSH)
        }

        override suspend fun getById(id: String): Trip? = local.getById(id)

        override fun observeById(id: String): Flow<Trip?> = local.observeById(id)

        override suspend fun delete(id: String) {
            local.delete(id)
            val operation =
                SyncOperation(
                    id = generateOpId(),
                    collectionPath = TripFirestoreMapper.COLLECTION_PATH,
                    documentId = id,
                    type = SyncOperationType.DELETE,
                )
            store.append(SyncOperationCodec.toPersisted(operation, enqueuedAt = clock.now()))
            backgroundWorkScheduler.scheduleOneOff(BackgroundJobKind.SYNC_QUEUE_FLUSH)
        }
    }
