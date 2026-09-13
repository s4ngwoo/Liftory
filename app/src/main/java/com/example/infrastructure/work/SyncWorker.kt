package com.example.infrastructure.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.StrengthLogApplication
import com.example.infrastructure.sync.SyncBatchProcessor

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
        
        return try {
            val hasFailures = SyncBatchProcessor.process(
                syncQueueRepository = syncQueueRepository,
                remoteSyncDataSource = remoteSyncDataSource
            )
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
