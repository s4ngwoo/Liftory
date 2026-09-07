package com.example.domain.model

enum class EntityType {
    SESSION,
    SET,
    EXERCISE,
    ROUTINE
}

enum class SyncOperation {
    CREATE,
    UPDATE,
    DELETE
}

data class PendingUpload(
    val id: String,
    val entityType: EntityType,
    val entityId: String,
    val operation: SyncOperation,
    val payloadJson: String,
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val lastError: String? = null
)
