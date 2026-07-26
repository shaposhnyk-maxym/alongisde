package com.alongside.data.pretrip

import com.alongside.core.database.sync.SyncOperationStore
import com.alongside.core.domain.pretrip.PreTripPhotoRepository
import com.alongside.core.domain.work.BackgroundJobKind
import com.alongside.core.domain.work.BackgroundWorkScheduler
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.pretrip.PreTripPhoto
import com.alongside.core.network.queue.SyncOperation
import com.alongside.core.network.queue.SyncOperationType
import com.alongside.data.sync.SyncOperationCodec
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Offline-first [PreTripPhotoRepository]: every write lands in Room stamped `syncStatus = PENDING`
 * and enqueues a durable sync operation; reads come straight from Room. Unlike every other
 * `Syncing*Repository` in this package, [upsert] does NOT stamp `updatedAt` - [PreTripPhoto] has
 * no such field (deliberate since M19.5/M19.6). This is exactly why `SyncCoordinator.preflight()`
 * always pushes a `PreTripPhoto` operation unconditionally, never doing a remote preflight read:
 * `PreTripPhotoFirestoreMapper.toFields` never serializes an `updatedAt` key to compare against.
 */
public class SyncingPreTripPhotoRepository
    @OptIn(ExperimentalUuidApi::class)
    constructor(
        private val local: PreTripPhotoRepository,
        private val store: SyncOperationStore,
        private val backgroundWorkScheduler: BackgroundWorkScheduler,
        private val clock: Clock = Clock.System,
        private val generateOpId: () -> String = { Uuid.random().toString() },
    ) : PreTripPhotoRepository {
        override suspend fun upsert(photo: PreTripPhoto) {
            val stamped = photo.copy(syncStatus = SyncStatus.PENDING)
            local.upsert(stamped)
            val operation =
                SyncOperation(
                    id = generateOpId(),
                    collectionPath = PreTripPhotoFirestoreMapper.COLLECTION_PATH,
                    documentId = stamped.id,
                    type = SyncOperationType.UPSERT,
                    fields = PreTripPhotoFirestoreMapper.toFields(stamped),
                )
            store.append(SyncOperationCodec.toPersisted(operation, enqueuedAt = clock.now()))
            backgroundWorkScheduler.scheduleOneOff(BackgroundJobKind.SYNC_QUEUE_FLUSH)
        }

        override suspend fun getById(id: String): PreTripPhoto? = local.getById(id)

        override fun observeByTripAndUser(
            tripId: String,
            userId: String,
        ): Flow<List<PreTripPhoto>> = local.observeByTripAndUser(tripId, userId)

        override suspend fun delete(id: String) {
            local.delete(id)
            val operation =
                SyncOperation(
                    id = generateOpId(),
                    collectionPath = PreTripPhotoFirestoreMapper.COLLECTION_PATH,
                    documentId = id,
                    type = SyncOperationType.DELETE,
                )
            store.append(SyncOperationCodec.toPersisted(operation, enqueuedAt = clock.now()))
            backgroundWorkScheduler.scheduleOneOff(BackgroundJobKind.SYNC_QUEUE_FLUSH)
        }
    }
