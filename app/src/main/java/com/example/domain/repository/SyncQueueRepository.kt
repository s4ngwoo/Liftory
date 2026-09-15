package com.example.domain.repository

import com.example.domain.model.PendingUpload
import kotlinx.coroutines.flow.Flow

interface SyncQueueRepository {
    suspend fun enqueue(pendingUpload: PendingUpload): Result<Unit>
    suspend fun getNextPending(userId: String, limit: Int = 20): List<PendingUpload>
    suspend fun markCompleted(id: String): Result<Unit>
    suspend fun markFailed(id: String, error: String): Result<Unit>
    fun observePendingCount(): Flow<Int>
}
