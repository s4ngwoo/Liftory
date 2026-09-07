package com.example.infrastructure.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pending_uploads",
    indices = [
        Index(value = ["createdAt"]),
        Index(value = ["retryCount"])
    ]
)
data class PendingUploadEntity(
    @PrimaryKey
    val id: String,
    val entityType: String,
    val entityId: String,
    val operation: String,
    val payloadJson: String,
    val createdAt: Long,
    val retryCount: Int,
    val lastError: String?
)
