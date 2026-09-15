package com.example.infrastructure

import com.example.domain.model.EntityType
import com.example.domain.model.EquipmentType
import com.example.domain.model.Exercise
import com.example.domain.model.ExerciseSet
import com.example.domain.model.PendingUpload
import com.example.domain.model.SyncOperation
import com.example.domain.model.WorkoutSession
import com.example.infrastructure.db.entity.ExerciseEntity
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
            orderIndex = 1,
            isCompleted = false,
            targetReps = 8
        )

        val entity = domain.toEntity()
        val restored = entity.toDomain()
        assertEquals(domain, restored)
        assertEquals(false, restored.isCompleted)
        assertEquals(8, restored.targetReps)
    }

    @Test
    fun exercise_mappingRoundTrip_preservesKnownEquipmentType() {
        val domain = Exercise(
            id = "ex_treadmill",
            name = "트레드밀",
            isCustom = false,
            muscleGroup = "Cardio",
            equipmentType = EquipmentType.CARDIO,
            machineBrand = "StairMaster",
            createdAt = 1000L,
            updatedAt = 2000L
        )

        val restored = domain.toEntity().toDomain()
        assertEquals(domain, restored)
        assertEquals(EquipmentType.CARDIO, restored.equipmentType)
    }

    @Test
    fun exercise_unknownEquipmentType_fallsBackToFreeWeight() {
        val entity = ExerciseEntity(
            id = "ex_future",
            name = "Future Cable",
            isCustom = true,
            muscleGroup = "Back",
            equipmentType = "CABLE_FUTURE",
            machineBrand = null,
            createdAt = 1000L,
            updatedAt = 1000L
        )

        val domain = entity.toDomain()
        assertEquals(EquipmentType.FREE_WEIGHT, domain.equipmentType)
        assertEquals("Future Cable", domain.name)
        assertEquals(true, domain.isCustom)
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
}
