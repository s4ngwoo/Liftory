package com.example.infrastructure.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.StrengthLogApplication
import com.example.domain.sync.SyncOutboxOwnerGate

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("SyncWorker", "Starting sync work")
        val app = applicationContext as? StrengthLogApplication
        if (app == null) {
             Log.e("SyncWorker", "Application is not StrengthLogApplication")
             return Result.failure()
        }
        val syncQueueRepository = app.container.syncQueueRepository
        val remoteSyncDataSource = app.container.remoteSyncDataSource
        val currentUserId = app.container.authRepository.getCurrentUserId()
        if (currentUserId.isNullOrBlank()) {
            Log.d("SyncWorker", "No logged-in user; skipping outbox drain")
            return Result.success()
        }

        return try {
            val pendingList = syncQueueRepository.getNextPending(currentUserId, 20)
            if (pendingList.isEmpty()) {
                Log.d("SyncWorker", "No pending items to sync")
                return Result.success()
            }

            var hasFailures = false
            for (pending in pendingList) {
                // Ignore items with high retry count for now to avoid infinite loops
                if (pending.retryCount >= 5) {
                    Log.w("SyncWorker", "Skipping item ${pending.id} due to max retries")
                    continue
                }
                if (!SyncOutboxOwnerGate.canPush(pending.userId, currentUserId)) {
                    Log.w("SyncWorker", "Skipping item ${pending.id}: belongs to ${pending.userId}, current is $currentUserId")
                    continue
                }

                remoteSyncDataSource.sync(pending)
                    .onSuccess {
                        syncQueueRepository.markCompleted(pending.id)
                    }
                    .onFailure { error ->
                        hasFailures = true
                        syncQueueRepository.markFailed(pending.id, error.message ?: "Unknown error")
                        Log.e("SyncWorker", "Failed to sync item ${pending.id}", error)
                    }
            }
            if (hasFailures) {
                Result.retry()
            } else {
                Result.success()
            }
        } catch (e: Exception) {
            Log.e("SyncWorker", "Error in sync worker", e)
            Result.retry()
        }
    }
}
