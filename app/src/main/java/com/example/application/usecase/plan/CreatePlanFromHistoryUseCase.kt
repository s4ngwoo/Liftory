package com.example.application.usecase.plan

import com.example.domain.model.LegacyMeasurementMapper
import com.example.domain.model.MeasurementValue
import com.example.domain.model.plan.PlannedExercise
import com.example.domain.model.plan.PlannedSet
import com.example.domain.model.plan.SessionPlan
import com.example.domain.port.IdGenerator
import com.example.domain.port.WallClock
import com.example.domain.repository.ExerciseRepository
import com.example.domain.repository.ExerciseSetRepository
import com.example.domain.repository.SessionPlanRepository

class CreatePlanFromHistoryUseCase(
    private val exerciseSetRepository: ExerciseSetRepository,
    private val exerciseRepository: ExerciseRepository,
    private val planRepository: SessionPlanRepository,
    private val idGenerator: IdGenerator,
    private val wallClock: WallClock
) {
    suspend operator fun invoke(pastSessionId: String, planName: String = "Plan from Past Session"): Result<SessionPlan> {
        val pastSets = exerciseSetRepository.getBySession(pastSessionId)
            .sortedBy { it.orderIndex }

        if (pastSets.isEmpty()) {
            return Result.failure(IllegalArgumentException("No past sets found for session: $pastSessionId"))
        }

        // Group sets by exercise while preserving occurrence sequence
        val exerciseGroups = mutableListOf<Pair<String, MutableList<com.example.domain.model.ExerciseSet>>>()
        for (set in pastSets) {
            val lastGroup = exerciseGroups.lastOrNull()
            if (lastGroup != null && lastGroup.first == set.exerciseId) {
                lastGroup.second.add(set)
            } else {
                exerciseGroups.add(Pair(set.exerciseId, mutableListOf(set)))
            }
        }

        val plannedExercises = mutableListOf<PlannedExercise>()
        for ((groupIndex, group) in exerciseGroups.withIndex()) {
            val exerciseId = group.first
            val sets = group.second
            val exercise = exerciseRepository.getById(exerciseId).getOrNull()
            val exerciseName = exercise?.name ?: "Exercise $exerciseId"

            val plannedSets = sets.mapIndexed { index, set ->
                val targetMeasurement = if (exercise != null) {
                    LegacyMeasurementMapper.toMeasurement(
                        exercise = exercise,
                        weight = set.weight,
                        reps = set.reps
                    )
                } else {
                    MeasurementValue.WeightAndReps(
                        weightKg = set.weight,
                        reps = set.reps
                    )
                }

                PlannedSet(
                    id = idGenerator.generate(),
                    orderIndex = index,
                    targetMeasurement = targetMeasurement,
                    targetRestSeconds = set.restSeconds ?: 90,
                    isCompleted = false // Suggested plan only, NOT automatically counted as actual performed set (PLAN-05)
                )
            }

            plannedExercises.add(
                PlannedExercise(
                    id = idGenerator.generate(),
                    exerciseId = exerciseId,
                    exerciseName = exerciseName,
                    orderIndex = groupIndex,
                    plannedSets = plannedSets,
                    notes = "Suggested from past session $pastSessionId"
                )
            )
        }

        val now = wallClock.nowMillis()
        val plan = SessionPlan(
            id = idGenerator.generate(),
            routineId = null,
            name = planName,
            exercises = plannedExercises,
            isConfirmed = false,
            createdAt = now,
            updatedAt = now
        )

        return planRepository.savePlan(plan)
    }
}
