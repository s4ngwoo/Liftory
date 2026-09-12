package com.example.domain.library

import com.example.domain.model.MeasurementProfile

data class PublishedExercise(
    val exerciseId: String,
    val exerciseName: String,
    val profile: MeasurementProfile,
    val targetReps: Int,
    val suggestedWeightKg: Double
)

data class PublishedRoutine(
    val id: String,
    val authorId: String,
    val title: String,
    val version: Int,
    val isPublic: Boolean,
    val exercises: List<PublishedExercise>
)

data class ImportedExercise(
    val exerciseId: String,
    val exerciseName: String,
    val profile: MeasurementProfile,
    val targetReps: Int,
    val personalTargetWeightKg: Double
)

data class ImportedRoutineCopy(
    val id: String,
    val ownerUserId: String,
    val title: String,
    val provenanceRoutineId: String,
    val provenanceVersion: Int,
    val exercises: List<ImportedExercise>
)

data class CompatibilityReport(
    val isCompatible: Boolean,
    val incompatibleReasons: List<String>
)

class RoutineLibraryService {
    private val publishedRoutines = mutableMapOf<String, PublishedRoutine>()
    private val importedCopies = mutableMapOf<String, MutableMap<String, ImportedRoutineCopy>>() // userId -> (commandId -> copy)

    fun publishOrSaveDraft(authorId: String, routine: PublishedRoutine) {
        publishedRoutines[routine.id] = routine
    }

    fun getPublishedRoutine(viewerUserId: String, routineId: String): PublishedRoutine? {
        val routine = publishedRoutines[routineId] ?: return null
        if (!routine.isPublic && routine.authorId != viewerUserId) {
            return null
        }
        return routine
    }

    fun updateRoutine(userId: String, routine: PublishedRoutine): Result<Unit> {
        val existing = publishedRoutines[routine.id]
        if (existing != null && existing.authorId != userId) {
            return Result.failure(IllegalAccessException("Not authorized to update routine"))
        }
        publishedRoutines[routine.id] = routine
        return Result.success(Unit)
    }

    fun deleteRoutine(userId: String, routineId: String): Result<Unit> {
        val existing = publishedRoutines[routineId]
        if (existing != null && existing.authorId != userId) {
            return Result.failure(IllegalAccessException("Not authorized to delete routine"))
        }
        publishedRoutines.remove(routineId)
        return Result.success(Unit)
    }

    fun importToMyRoutines(
        userId: String,
        commandId: String,
        published: PublishedRoutine,
        resetPersonalWeights: Boolean = true
    ): ImportedRoutineCopy {
        val userMap = importedCopies.getOrPut(userId) { mutableMapOf() }
        val existing = userMap[commandId]
        if (existing != null) {
            return existing
        }

        val copy = ImportedRoutineCopy(
            id = "imported_${published.id}_${System.currentTimeMillis()}",
            ownerUserId = userId,
            title = published.title,
            provenanceRoutineId = published.id,
            provenanceVersion = published.version,
            exercises = published.exercises.map { ex ->
                ImportedExercise(
                    exerciseId = ex.exerciseId,
                    exerciseName = ex.exerciseName,
                    profile = ex.profile,
                    targetReps = ex.targetReps,
                    personalTargetWeightKg = if (resetPersonalWeights) 0.0 else ex.suggestedWeightKg
                )
            }
        )
        userMap[commandId] = copy
        return copy
    }

    fun getUserImportedRoutines(userId: String): List<ImportedRoutineCopy> {
        return importedCopies[userId]?.values?.toList().orEmpty()
    }

    fun validateCompatibility(routine: PublishedRoutine): CompatibilityReport {
        val issues = mutableListOf<String>()
        for (ex in routine.exercises) {
            if (ex.profile == MeasurementProfile.LEGACY_UNKNOWN) {
                issues.add("Exercise ${ex.exerciseName} uses unsupported measurement profile: LEGACY_UNKNOWN")
            }
        }
        return CompatibilityReport(
            isCompatible = issues.isEmpty(),
            incompatibleReasons = issues
        )
    }
}

class LocalRoutineCache {
    private val cache = mutableMapOf<String, ImportedRoutineCopy>()

    fun save(routine: ImportedRoutineCopy) {
        cache[routine.id] = routine
    }

    fun getById(id: String): ImportedRoutineCopy? {
        return cache[id]
    }
}
