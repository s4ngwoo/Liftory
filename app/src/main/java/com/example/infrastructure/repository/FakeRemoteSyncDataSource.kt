package com.example.infrastructure.repository

import android.util.Log
import com.example.domain.model.PendingUpload
import com.example.domain.repository.RemoteSyncDataSource
import kotlinx.coroutines.delay

class FakeRemoteSyncDataSource : RemoteSyncDataSource {
    override suspend fun sync(pendingUpload: PendingUpload): Result<Unit> {
        Log.d("FakeRemoteSync", "Simulating upload for ${pendingUpload.entityType} ${pendingUpload.entityId}")
        delay(500) // Simulate network delay
        // In Sprint 4, we just mock success
        return Result.success(Unit)
    }
}
