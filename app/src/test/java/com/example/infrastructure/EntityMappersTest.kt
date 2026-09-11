package com.example.infrastructure

import com.example.domain.model.EntityType
import com.example.domain.model.Exercise
import com.example.domain.model.ExerciseSet
import com.example.domain.model.PendingUpload
import com.example.domain.model.SyncOperation
import com.example.domain.model.WorkoutSession
import com.example.infrastructure.db.entity.PendingUploadEntity
import com.example.infrastructure.db.mapper.toDomain
import com.example.infrastructure.db.mapper.toEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class EntityMappersTest {

    @Test
    fun workoutSession_mappingRoundTrip_preservesAllFields() {
        val domain = WorkoutSession(
            id = "sess-123",
            startTime = 1000L,
            endTime = 2000L,
            notes = "Strong performance",
            createdAt = 1000L,
            updatedAt = 2000L
        )

        val entity = domain.toEntity()
        assertEquals(domain.id, entity.id)
        assertEquals(domain.startTime, entity.startTime)
        assertEquals(domain.endTime, entity.endTime)
        assertEquals(domain.notes, entity.notes)

        val restored = entity.toDomain()
        assertEquals(domain, restored)
    }

    @Test
    fun exerciseSet_mappingRoundTrip_preservesAllFields() {
        val domain = ExerciseSet(
            id = "set-1",
            sessionId = "sess-1",
            exerciseId = "ex_squat",
            weight = 100.0,
            reps = 5,
            rpe = 8.5,
            restSeconds = 90,
            orderIndex = 1
        )

        val entity = domain.toEntity()
        val restored = entity.toDomain()
        assertEquals(domain, restored)
    }

    @Test
    fun pendingUpload_mappingRoundTrip_preservesAllFields() {
        val domain = PendingUpload(
            id = "upload-1",
            entityType = EntityType.SESSION,
            entityId = "sess-1",
            operation = SyncOperation.CREATE,
            payloadJson = "{\"id\":\"sess-1\"}"
        )

        val entity = domain.toEntity()
        val restored = entity.toDomain()
        assertEquals(domain, restored)
    }

    @Test
    fun pendingUpload_unknownEnums_fallBackToSafeDefaults() {
        val entity = PendingUploadEntity(
            id = "upload-bad",
            entityType = "NOT_A_TYPE",
            entityId = "sess-1",
            operation = "NOT_AN_OP",
            payloadJson = "{}",
            createdAt = 1L,
            retryCount = 2,
            lastError = "boom"
        )

        val restored = entity.toDomain()
        assertEquals(EntityType.SESSION, restored.entityType)
        assertEquals(SyncOperation.CREATE, restored.operation)
        assertEquals("sess-1", restored.entityId)
        assertEquals(2, restored.retryCount)
        assertEquals("boom", restored.lastError)
    }
}
