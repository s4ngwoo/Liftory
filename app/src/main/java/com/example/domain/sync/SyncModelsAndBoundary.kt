package com.example.domain.sync

import com.example.domain.model.PendingUpload

data class SyncableEntity(
    val id: String,
    val revision: Long,
    val payload: String,
    val isDeleted: Boolean = false,
    val isConflict: Boolean = false,
    val updatedAtEpochMs: Long = 0L
)

object SyncConflictResolver {

    fun resolve(local: SyncableEntity, remote: SyncableEntity): SyncableEntity {
        // Tombstone check: if local is deleted with equal or higher revision, keep tombstone
        if (local.isDeleted && local.revision >= remote.revision) {
            return local
        }
        if (remote.isDeleted && remote.revision >= local.revision) {
            return remote
        }

        // Revision comparison: higher revision wins
        return when {
            local.revision > remote.revision -> local
            remote.revision > local.revision -> remote
            else -> {
                // Same revision: fallback to last timestamp
                if (local.updatedAtEpochMs >= remote.updatedAtEpochMs) local else remote
            }
        }
    }

    fun resolveConcurrentActiveSessions(local: SyncableEntity, remote: SyncableEntity): SyncableEntity {
        // Preserve active session with conflict flag; never silently terminate
        return local.copy(isConflict = true)
    }
}

class InMemorySyncOutbox {
    private val queuesByUser = mutableMapOf<String, MutableList<PendingUpload>>()

    fun enqueue(userId: String, upload: PendingUpload) {
        val list = queuesByUser.getOrPut(userId) { mutableListOf() }
        list.add(upload)
    }

    fun getPending(userId: String): List<PendingUpload> {
        return queuesByUser[userId]?.toList().orEmpty()
    }

    fun removeCompleted(userId: String, uploadId: String) {
        queuesByUser[userId]?.removeAll { it.id == uploadId }
    }
}

class SyncEngineStub(private val storage: MutableMap<String, String>) {
    private val processedCommandIds = mutableSetOf<String>()

    fun processUpload(userId: String, upload: PendingUpload) {
        if (processedCommandIds.contains(upload.id)) {
            // Idempotent retry: already processed, safe no-op
            return
        }
        processedCommandIds.add(upload.id)
        storage[upload.entityId] = upload.payloadJson
    }
}

object SharingPayloadFilter {

    fun filterPersonalData(
        rawHealthPayload: String,
        allowHealthSharing: Boolean,
        allowCohortSharing: Boolean
    ): String {
        // Simple JSON filtering of sensitive health and cohort fields
        var filtered = rawHealthPayload
        if (!allowHealthSharing) {
            // Remove sleep, stress, fatigue, heartRate
            filtered = filtered.replace(Regex("\"sleep\":\\s*\\d+,?"), "")
            filtered = filtered.replace(Regex("\"heartRate\":\\s*\\d+,?"), "")
            filtered = filtered.replace(Regex("\"stress\":\\s*\\d+,?"), "")
            filtered = filtered.replace(Regex("\"fatigue\":\\s*\\d+,?"), "")
        }
        if (!allowCohortSharing) {
            filtered = filtered.replace(Regex("\"cohortId\":\\s*\"[^\"]+\",?"), "")
        }
        // Clean up possible trailing commas before closing bracket
        filtered = filtered.replace(Regex(",\\s*\\}"), "}")
        return filtered
    }
}

data class SchemaValidationResult(
    val isSuccess: Boolean,
    val errorMessage: String? = null
)

object SyncSchemaGate {

    fun validateAndParse(payload: String, maxSupportedSchemaVersion: Int): SchemaValidationResult {
        val regex = Regex("\"schemaVersion\":\\s*(\\d+)")
        val match = regex.find(payload)
        val version = match?.groupValues?.get(1)?.toIntOrNull() ?: 1
        return if (version > maxSupportedSchemaVersion) {
            SchemaValidationResult(isSuccess = false, errorMessage = "Unsupported schema version: $version")
        } else {
            SchemaValidationResult(isSuccess = true)
        }
    }
}
