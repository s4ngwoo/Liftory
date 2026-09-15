package com.example.infrastructure.db.mapper

import com.example.domain.model.EntityType
import com.example.domain.model.Exercise
import com.example.domain.model.ExerciseSet
import com.example.domain.model.PendingUpload
import com.example.domain.model.SyncOperation
import com.example.domain.model.WorkoutSession
import com.example.infrastructure.db.entity.ExerciseEntity
import com.example.infrastructure.db.entity.ExerciseSetEntity
import com.example.infrastructure.db.entity.PendingUploadEntity
import com.example.infrastructure.db.entity.WorkoutSessionEntity

fun WorkoutSession.toEntity(): WorkoutSessionEntity = WorkoutSessionEntity(
    id = id,
    startTime = startTime,
    endTime = endTime,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun WorkoutSessionEntity.toDomain(): WorkoutSession = WorkoutSession(
    id = id,
    startTime = startTime,
    endTime = endTime,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun ExerciseSet.toEntity(): ExerciseSetEntity = ExerciseSetEntity(
    id = id,
    sessionId = sessionId,
    exerciseId = exerciseId,
    weight = weight,
    reps = reps,
    rpe = rpe,
    restSeconds = restSeconds,
    orderIndex = orderIndex,
    isCompleted = isCompleted,
    targetReps = targetReps,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun ExerciseSetEntity.toDomain(): ExerciseSet = ExerciseSet(
    id = id,
    sessionId = sessionId,
    exerciseId = exerciseId,
    weight = weight,
    reps = reps,
    rpe = rpe,
    restSeconds = restSeconds,
    orderIndex = orderIndex,
    isCompleted = isCompleted,
    targetReps = targetReps,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Exercise.toEntity(): ExerciseEntity = ExerciseEntity(
    id = id,
    name = name,
    isCustom = isCustom,
    muscleGroup = muscleGroup,
    equipmentType = equipmentType.name,
    machineBrand = machineBrand,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun ExerciseEntity.toDomain(): Exercise = Exercise(
    id = id,
    name = name,
    isCustom = isCustom,
    muscleGroup = muscleGroup,
    equipmentType = try {
        com.example.domain.model.EquipmentType.valueOf(equipmentType)
    } catch (_: Exception) {
        com.example.domain.model.EquipmentType.FREE_WEIGHT
    },
    machineBrand = machineBrand,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun PendingUpload.toEntity(): PendingUploadEntity = PendingUploadEntity(
    id = id,
    userId = userId,
    entityType = entityType.name,
    entityId = entityId,
    operation = operation.name,
    payloadJson = payloadJson,
    createdAt = createdAt,
    retryCount = retryCount,
    lastError = lastError
)

fun PendingUploadEntity.toDomain(): PendingUpload = PendingUpload(
    id = id,
    userId = userId,
    entityType = runCatching { EntityType.valueOf(entityType) }.getOrDefault(EntityType.SESSION),
    entityId = entityId,
    operation = runCatching { SyncOperation.valueOf(operation) }.getOrDefault(SyncOperation.CREATE),
    payloadJson = payloadJson,
    createdAt = createdAt,
    retryCount = retryCount,
    lastError = lastError
)
