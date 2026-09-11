package com.example.infrastructure.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.domain.port.SyncScheduler
import com.example.infrastructure.work.SyncWorker

/**
 * Infrastructure adapter implementing SyncScheduler via Android WorkManager.
 */
class WorkManagerSyncScheduler(
    private val context: Context
) : SyncScheduler {

    override fun scheduleImmediateSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueue(syncRequest)
    }
}
