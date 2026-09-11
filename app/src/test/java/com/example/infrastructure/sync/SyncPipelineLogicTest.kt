package com.example.infrastructure.sync

import com.example.domain.model.EntityType
import com.example.domain.model.PendingUpload
import com.example.domain.model.SyncOperation
import com.example.domain.repository.RemoteSyncDataSource
import com.example.domain.repository.SyncQueueRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeSyncQueueRepository : SyncQueueRepository {
    val queue = mutableListOf<PendingUpload>()
    val completedIds = mutableListOf<String>()
    val failedItems = mutableMapOf<String, Pair<Int, String>>()

    override suspend fun enqueue(pendingUpload: PendingUpload): Result<Unit> {
        queue.add(pendingUpload)
        return Result.success(Unit)
    }

    override suspend fun getNextPending(limit: Int): List<PendingUpload> {
        return queue.take(limit)
    }

    override suspend fun markCompleted(id: String): Result<Unit> {
        queue.removeAll { it.id == id }
        completedIds.add(id)
        return Result.success(Unit)
    }

    override suspend fun markFailed(id: String, error: String): Result<Unit> {
        val item = queue.find { it.id == id }
        if (item != null) {
            val updated = item.copy(retryCount = item.retryCount + 1, lastError = error)
            queue.replaceAll { if (it.id == id) updated else it }
            failedItems[id] = Pair(updated.retryCount, error)
        }
        return Result.success(Unit)
    }

    override fun observePendingCount(): Flow<Int> = flowOf(queue.size)
}

class FakeRemoteSyncDataSource : RemoteSyncDataSource {
    var shouldFail = false
    val syncedItems = mutableListOf<PendingUpload>()

    override suspend fun sync(pendingUpload: PendingUpload): Result<Unit> {
        return if (shouldFail) {
            Result.failure(Exception("Network error"))
        } else {
            syncedItems.add(pendingUpload)
            Result.success(Unit)
        }
    }
}

class SyncPipelineLogicTest {

    private lateinit var syncQueueRepository: FakeSyncQueueRepository
    private lateinit var remoteSyncDataSource: FakeRemoteSyncDataSource

    @Before
    fun setUp() {
        syncQueueRepository = FakeSyncQueueRepository()
        remoteSyncDataSource = FakeRemoteSyncDataSource()
    }

    @Test
    fun `when remote sync succeeds, item should be removed from queue and marked completed`() = runTest {
        val upload = PendingUpload(
            id = "upload_1",
            entityType = EntityType.SESSION,
            entityId = "session_1",
            operation = SyncOperation.CREATE,
            payloadJson = "{}"
        )
        syncQueueRepository.enqueue(upload)

        val pending = syncQueueRepository.getNextPending(10).first()
        val result = remoteSyncDataSource.sync(pending)

        assertTrue(result.isSuccess)
        syncQueueRepository.markCompleted(pending.id)

        assertEquals(0, syncQueueRepository.queue.size)
        assertTrue(syncQueueRepository.completedIds.contains("upload_1"))
        assertEquals(1, remoteSyncDataSource.syncedItems.size)
    }

    @Test
    fun `when remote sync fails, retryCount should be incremented and error message recorded`() = runTest {
        val upload = PendingUpload(
            id = "upload_2",
            entityType = EntityType.SET,
            entityId = "set_1",
            operation = SyncOperation.UPDATE,
            payloadJson = "{}"
        )
        syncQueueRepository.enqueue(upload)
        remoteSyncDataSource.shouldFail = true

        val pending = syncQueueRepository.getNextPending(10).first()
        val result = remoteSyncDataSource.sync(pending)

        assertTrue(result.isFailure)
        syncQueueRepository.markFailed(pending.id, result.exceptionOrNull()?.message ?: "Unknown")

        assertEquals(1, syncQueueRepository.queue.size)
        val failureRecord = syncQueueRepository.failedItems["upload_2"]
        assertEquals(1, failureRecord?.first)
        assertEquals("Network error", failureRecord?.second)
    }
}
