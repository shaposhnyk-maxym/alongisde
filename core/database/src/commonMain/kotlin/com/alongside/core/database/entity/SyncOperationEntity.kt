package com.alongside.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.alongside.core.database.sync.PersistedSyncOperation
import com.alongside.core.database.sync.PersistedSyncOperationStatus
import com.alongside.core.database.sync.PersistedSyncOperationType
import kotlin.time.Instant

/**
 * [seq] (autoincrement) orders the queue - strict FIFO immune to clock ties.
 * [type]/[status] are enum names as plain strings; the mappers below own the conversion.
 */
@Entity(tableName = "sync_operations", indices = [Index(value = ["opId"], unique = true)])
internal data class SyncOperationEntity(
    @PrimaryKey(autoGenerate = true) val seq: Long = 0,
    val opId: String,
    val collectionPath: String,
    val documentId: String,
    val type: String,
    val fieldsJson: String,
    val attempts: Int,
    val status: String,
    val enqueuedAt: Instant,
)

internal fun SyncOperationEntity.toRecord(): PersistedSyncOperation =
    PersistedSyncOperation(
        id = opId,
        collectionPath = collectionPath,
        documentId = documentId,
        type = PersistedSyncOperationType.valueOf(type),
        fieldsJson = fieldsJson,
        attempts = attempts,
        status = PersistedSyncOperationStatus.valueOf(status),
        enqueuedAt = enqueuedAt,
    )

internal fun PersistedSyncOperation.toEntity(): SyncOperationEntity =
    SyncOperationEntity(
        opId = id,
        collectionPath = collectionPath,
        documentId = documentId,
        type = type.name,
        fieldsJson = fieldsJson,
        attempts = attempts,
        status = status.name,
        enqueuedAt = enqueuedAt,
    )
