package com.example.application.mapper

import com.example.domain.model.EquipmentType
import com.example.domain.model.Exercise
import com.example.domain.model.ExerciseSet
import com.example.domain.model.WorkoutSession
import com.example.domain.model.execution.SessionExecutionState
import com.example.domain.model.execution.SetExecutionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutSessionExecutionBridgeTest {

    @Test
    fun `maps session with barbell row to session plan and workout execution`() {
        val now = 1000000L
        val session = WorkoutSession(
            id = "session_1",
            startTime = now,
            endTime = null,
            notes = "등 운동 루틴",
            createdAt = now,
            updatedAt = now
        )
        val exercise = Exercise(
            id = "ex_barbell_row",
            name = "바벨 로우",
            muscleGroup = "등",
            equipmentType = EquipmentType.FREE_WEIGHT
        )
        val sets = listOf(
            ExerciseSet(
                id = "set_1",
                sessionId = "session_1",
                exerciseId = "ex_barbell_row",
                orderIndex = 0,
                weight = 60.0,
                reps = 10,
                isCompleted = true,
                restSeconds = 90
            ),
            ExerciseSet(
                id = "set_2",
                sessionId = "session_1",
                exerciseId = "ex_barbell_row",
                orderIndex = 1,
                weight = 60.0,
                reps = 10,
                isCompleted = false,
                restSeconds = 90
            )
        )

        val (plan, execution) = WorkoutSessionExecutionBridge.createPlanAndExecution(
            session = session,
            sets = sets,
            exercises = listOf(exercise)
        )

        // SessionPlan 검증
        assertEquals("session_1", plan.id)
        assertEquals("등 운동 루틴", plan.name)
        assertEquals(1, plan.exercises.size)
        val plannedEx = plan.exercises[0]
        assertEquals("ex_barbell_row", plannedEx.exerciseId)
        assertEquals("바벨 로우", plannedEx.exerciseName)
        assertEquals(2, plannedEx.plannedSets.size)

        // WorkoutExecution 검증
        assertEquals("session_1", execution.sessionId)
        assertEquals(SessionExecutionState.ACTIVE, execution.sessionState)
        // 1세트는 완료, 2세트가 미완료이므로 currentSetIndex = 1
        assertEquals(0, execution.currentExerciseIndex)
        assertEquals(1, execution.currentSetIndex)
        assertEquals(1, execution.confirmedRecords.size)
        assertEquals(0, execution.confirmedRecords[0].exerciseIndex)
        assertEquals(0, execution.confirmedRecords[0].setIndex)
        assertTrue(execution.setState is SetExecutionState.Ready)
    }

    @Test
    fun `maps empty session gracefully`() {
        val now = 1000000L
        val session = WorkoutSession(
            id = "session_empty",
            startTime = now,
            endTime = null,
            createdAt = now,
            updatedAt = now
        )

        val (plan, execution) = WorkoutSessionExecutionBridge.createPlanAndExecution(
            session = session,
            sets = emptyList(),
            exercises = emptyList()
        )

        assertEquals("session_empty", plan.id)
        assertTrue(plan.exercises.isEmpty())
        assertEquals("session_empty", execution.sessionId)
        assertEquals(0, execution.currentExerciseIndex)
        assertEquals(0, execution.currentSetIndex)
        assertTrue(execution.confirmedRecords.isEmpty())
    }
}
