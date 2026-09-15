package com.example.infrastructure.sync

import com.example.domain.model.EntityType
import com.example.domain.model.PendingUpload
import com.example.domain.model.SyncOperation
import com.example.infrastructure.db.dao.PendingUploadDao
import com.example.infrastructure.db.mapper.toEntity
import java.util.UUID

internal suspend fun PendingUploadDao.enqueueOwnedBy(
    userId: String?,
    entityType: EntityType,
    entityId: String,
    operation: SyncOperation,
    payloadJson: String
) {
    if (userId.isNullOrBlank()) return
    insert(
        PendingUpload(
            id = UUID.randomUUID().toString(),
            userId = userId,
            entityType = entityType,
            entityId = entityId,
            operation = operation,
            payloadJson = payloadJson
        ).toEntity()
    )
}
