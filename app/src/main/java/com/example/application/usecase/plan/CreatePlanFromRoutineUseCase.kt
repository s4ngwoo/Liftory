package com.example.application.usecase.plan

import com.example.domain.model.LegacyMeasurementMapper
import com.example.domain.model.MeasurementValue
import com.example.domain.model.plan.PlannedExercise
import com.example.domain.model.plan.PlannedSet
import com.example.domain.model.plan.SessionPlan
import com.example.domain.port.IdGenerator
import com.example.domain.port.WallClock
import com.example.domain.repository.ExerciseRepository
import com.example.domain.repository.RoutineTemplateRepository
import com.example.domain.repository.SessionPlanRepository

class CreatePlanFromRoutineUseCase(
    private val routineRepository: RoutineTemplateRepository,
    private val exerciseRepository: ExerciseRepository,
    private val planRepository: SessionPlanRepository,
    private val idGenerator: IdGenerator,
    private val wallClock: WallClock
) {
    suspend operator fun invoke(routineId: String): Result<SessionPlan> {
        val routine = routineRepository.getById(routineId)
            ?: return Result.failure(IllegalArgumentException("Routine not found: $routineId"))

        val presets = routine.exercises.sortedBy { it.orderIndex }

        val plannedExercises = mutableListOf<PlannedExercise>()

        for (preset in presets) {
            val exerciseResult = exerciseRepository.getById(preset.exerciseId)
            val exercise = exerciseResult.getOrNull()
            val exerciseName = exercise?.name ?: "Exercise ${preset.exerciseId}"

            val targetMeasurement = if (exercise != null) {
                LegacyMeasurementMapper.toMeasurement(
                    exercise = exercise,
                    weight = preset.defaultWeight,
                    reps = preset.defaultReps
                )
            } else {
                MeasurementValue.WeightAndReps(
                    weightKg = preset.defaultWeight,
                    reps = preset.defaultReps
                )
            }

            val plannedSet = PlannedSet(
                id = idGenerator.generate(),
                orderIndex = 0,
                targetMeasurement = targetMeasurement,
                targetRestSeconds = 90
            )

            val plannedExercise = PlannedExercise(
                id = idGenerator.generate(), // Distinct plan item ID (PLAN-04)
                exerciseId = preset.exerciseId,
                exerciseName = exerciseName,
                orderIndex = preset.orderIndex,
                plannedSets = listOf(plannedSet)
            )
            plannedExercises.add(plannedExercise)
        }

        val now = wallClock.nowMillis()
        val plan = SessionPlan(
            id = idGenerator.generate(),
            routineId = routine.id,
            routineVersion = 1,
            name = routine.name,
            exercises = plannedExercises,
            isConfirmed = false, // Draft by default (PLAN-02)
            createdAt = now,
            updatedAt = now
        )

        return planRepository.savePlan(plan)
    }
}
