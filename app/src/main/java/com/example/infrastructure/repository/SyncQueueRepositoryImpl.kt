package com.example.infrastructure.repository

import com.example.domain.model.PendingUpload
import com.example.domain.repository.SyncQueueRepository
import com.example.infrastructure.db.dao.PendingUploadDao
import com.example.infrastructure.db.mapper.toDomain
import com.example.infrastructure.db.mapper.toEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class SyncQueueRepositoryImpl(
    private val pendingUploadDao: PendingUploadDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : SyncQueueRepository {

    override suspend fun enqueue(pendingUpload: PendingUpload): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            pendingUploadDao.insert(pendingUpload.toEntity())
        }
    }

    override suspend fun getNextPending(userId: String, limit: Int): List<PendingUpload> = withContext(ioDispatcher) {
        pendingUploadDao.getNextPending(userId, limit).map { it.toDomain() }
    }

    override suspend fun markCompleted(id: String): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            pendingUploadDao.deleteById(id)
        }
    }

    override suspend fun markFailed(id: String, error: String): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val item = pendingUploadDao.getById(id)
            if (item != null) {
                pendingUploadDao.update(
                    item.copy(
                        retryCount = item.retryCount + 1,
                        lastError = error
                    )
                )
            }
        }
    }

    override fun observePendingCount(): Flow<Int> {
        return pendingUploadDao.observePendingCount()
    }
}
