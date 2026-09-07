package com.example.domain.repository

import com.example.domain.model.PendingUpload

interface RemoteSyncDataSource {
    suspend fun sync(pendingUpload: PendingUpload): Result<Unit>
}
