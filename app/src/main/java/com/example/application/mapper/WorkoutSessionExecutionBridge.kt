package com.example.application.mapper

import com.example.domain.model.Exercise
import com.example.domain.model.ExerciseSet
import com.example.domain.model.MeasurementValue
import com.example.domain.model.WorkoutSession
import com.example.domain.model.execution.ConfirmedSetRecord
import com.example.domain.model.execution.SessionExecutionState
import com.example.domain.model.execution.SetExecutionState
import com.example.domain.model.execution.WorkoutExecution
import com.example.domain.model.plan.PlannedExercise
import com.example.domain.model.plan.PlannedSet
import com.example.domain.model.plan.SessionPlan

object WorkoutSessionExecutionBridge {

    fun createPlanAndExecution(
        session: WorkoutSession,
        sets: List<ExerciseSet>,
        exercises: List<Exercise>
    ): Pair<SessionPlan, WorkoutExecution> {
        val exerciseMap = exercises.associateBy { it.id }

        // 세트에 나타난 순서대로 운동 고유 id 목록 추출
        val distinctExerciseIds = mutableListOf<String>()
        sets.forEach { set ->
            if (!distinctExerciseIds.contains(set.exerciseId)) {
                distinctExerciseIds.add(set.exerciseId)
            }
        }

        val plannedExercises = distinctExerciseIds.mapIndexed { exIndex, exId ->
            val exName = exerciseMap[exId]?.name ?: "운동 $exIndex"
            val exSets = sets.filter { it.exerciseId == exId }.sortedBy { it.orderIndex }
            val plannedSets = exSets.mapIndexed { setIndex, set ->
                val measurement = MeasurementValue.WeightAndReps(
                    weightKg = set.weight,
                    reps = set.reps
                )
                PlannedSet(
                    id = set.id,
                    orderIndex = setIndex,
                    targetMeasurement = measurement,
                    targetRestSeconds = set.restSeconds,
                    isCompleted = set.isCompleted
                )
            }
            PlannedExercise(
                id = "planned_${session.id}_${exId}_$exIndex",
                exerciseId = exId,
                exerciseName = exName,
                orderIndex = exIndex,
                plannedSets = plannedSets
            )
        }

        val plan = SessionPlan(
            id = session.id,
            name = session.notes.ifBlank { "오늘의 운동" },
            exercises = plannedExercises,
            isConfirmed = true,
            createdAt = session.createdAt,
            updatedAt = session.updatedAt
        )

        // 완료된 세트들을 ConfirmedSetRecord로 변환
        val confirmedRecords = mutableListOf<ConfirmedSetRecord>()
        var targetExIndex = 0
        var targetSetIndex = 0
        var foundIncomplete = false
        var lastCompletedMeasurement: MeasurementValue = MeasurementValue.WeightAndReps(0.0, 0)

        plannedExercises.forEachIndexed { exIndex, plannedEx ->
            val exSets = sets.filter { it.exerciseId == plannedEx.exerciseId }.sortedBy { it.orderIndex }
            exSets.forEachIndexed { setIndex, set ->
                val measurement = plannedEx.plannedSets.getOrNull(setIndex)?.targetMeasurement
                    ?: MeasurementValue.WeightAndReps(set.weight, set.reps)

                if (set.isCompleted) {
                    lastCompletedMeasurement = measurement
                    confirmedRecords.add(
                        ConfirmedSetRecord(
                            plannedExerciseId = plannedEx.id,
                            exerciseId = set.exerciseId,
                            exerciseIndex = exIndex,
                            setIndex = setIndex,
                            measurement = measurement,
                            performedDurationSeconds = set.restSeconds ?: 60,
                            startedAtEpochMs = session.startTime,
                            completedAtEpochMs = session.startTime
                        )
                    )
                } else if (!foundIncomplete) {
                    targetExIndex = exIndex
                    targetSetIndex = setIndex
                    foundIncomplete = true
                }
            }
        }

        // 모든 세트가 완료되었거나 세트가 없는데 운동은 있는 경우 마지막 인덱스 유지
        if (!foundIncomplete && plannedExercises.isNotEmpty()) {
            val lastExIndex = plannedExercises.lastIndex
            val lastSetIndex = (plannedExercises[lastExIndex].plannedSets.size - 1).coerceAtLeast(0)
            targetExIndex = lastExIndex
            targetSetIndex = lastSetIndex
        }

        val isSessionFinished = session.endTime != null
        val setState: SetExecutionState = if (isSessionFinished) {
            SetExecutionState.Completed(lastCompletedMeasurement, session.endTime ?: session.startTime)
        } else {
            SetExecutionState.Ready
        }

        val execution = WorkoutExecution(
            sessionId = session.id,
            planId = plan.id,
            planSnapshot = plan,
            sessionState = if (isSessionFinished) SessionExecutionState.COMPLETED else SessionExecutionState.ACTIVE,
            currentExerciseIndex = targetExIndex,
            currentSetIndex = targetSetIndex,
            setState = setState,
            revision = 1L,
            startedAtEpochMs = session.startTime,
            completedAtEpochMs = session.endTime,
            confirmedRecords = confirmedRecords
        )

        return Pair(plan, execution)
    }
}
