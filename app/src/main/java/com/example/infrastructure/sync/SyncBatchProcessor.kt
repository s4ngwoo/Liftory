package com.example.infrastructure.sync

import com.example.domain.repository.RemoteSyncDataSource
import com.example.domain.repository.SyncQueueRepository
import com.example.domain.sync.SyncOutboxPolicy

object SyncBatchProcessor {

    suspend fun process(
        syncQueueRepository: SyncQueueRepository,
        remoteSyncDataSource: RemoteSyncDataSource,
        batchSize: Int = 20,
        maxRetries: Int = 5
    ): Boolean {
        val pendingList = syncQueueRepository.getNextPending(batchSize)
        if (pendingList.isEmpty()) return false

        val superseded = SyncOutboxPolicy.supersededPendingIds(pendingList)
        for (id in superseded) {
            syncQueueRepository.markCompleted(id)
        }

        var hasFailures = false
        for (pending in pendingList) {
            if (pending.id in superseded) continue
            if (pending.retryCount >= maxRetries) continue

            remoteSyncDataSource.sync(pending)
                .onSuccess {
                    syncQueueRepository.markCompleted(pending.id)
                }
                .onFailure { error ->
                    hasFailures = true
                    syncQueueRepository.markFailed(pending.id, error.message ?: "Unknown error")
                }
        }
        return hasFailures
    }
}
