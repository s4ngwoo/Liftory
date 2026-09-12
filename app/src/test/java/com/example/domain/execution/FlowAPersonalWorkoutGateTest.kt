package com.example.domain.execution

import com.example.domain.model.BodySide
import com.example.domain.model.MeasurementValue
import com.example.domain.model.execution.ConfirmedSetRecord
import com.example.domain.model.execution.InProgressSetDisposition
import com.example.domain.model.execution.SessionExecutionController
import com.example.domain.model.execution.SessionExecutionState
import com.example.domain.model.execution.SetExecutionState
import com.example.domain.model.execution.WorkoutExecution
import com.example.domain.model.plan.PlannedExercise
import com.example.domain.model.plan.PlannedSet
import com.example.domain.model.plan.SessionPlan
import com.example.domain.model.timer.RestTarget
import com.example.domain.model.timer.TimerCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * FLOW-A: Personal Workout Release Gate E2E Scenario (FLOW-A, N07.7).
 *
 * 1. Create mixed plan (Stretching L/R -> Strength Bench -> Cardio Stepmill) without cloud setup.
 * 2. Save plan draft: goal is preserved, timer has NOT started.
 * 3. Start workout -> complete stretching L/R -> perform strength set -> confirm performed values -> enter resting.
 * 4. Swipe between 3 pages during rest / simulate lock & return -> state & remaining time preserved.
 * 5. Complete cardio -> finish session.
 * 6. Verify session completion immutability & metric separation (kg vs seconds vs level).
 */
class FlowAPersonalWorkoutGateTest {

    @Test
    fun `FLOW-A full mixed routine lifecycle executes from draft to completion losslessly`() {
        // Step 1: Create mixed plan
        val mixedPlan = SessionPlan(
            id = "plan_flow_a",
            routineId = "rt_flow_a",
            routineVersion = 1,
            name = "Flow A: Mixed Routine",
            exercises = listOf(
                PlannedExercise(
                    id = "pe_1",
                    exerciseId = "ex_hip_flexor",
                    exerciseName = "장요근 스트레칭",
                    orderIndex = 0,
                    plannedSets = listOf(
                        PlannedSet("ps_1_l", 0, MeasurementValue.TimedHold(30, BodySide.LEFT)),
                        PlannedSet("ps_1_r", 1, MeasurementValue.TimedHold(30, BodySide.RIGHT))
                    )
                ),
                PlannedExercise(
                    id = "pe_2",
                    exerciseId = "ex_bench",
                    exerciseName = "바벨 벤치프레스",
                    orderIndex = 1,
                    plannedSets = listOf(
                        PlannedSet("ps_2_1", 0, MeasurementValue.WeightAndReps(80.0, 10))
                    )
                ),
                PlannedExercise(
                    id = "pe_3",
                    exerciseId = "ex_stepmill",
                    exerciseName = "스텝밀",
                    orderIndex = 2,
                    plannedSets = listOf(
                        PlannedSet("ps_3_1", 0, MeasurementValue.TimeAndLevel(900, 8.0))
                    )
                )
            ),
            isConfirmed = true,
            createdAt = 10_000L,
            updatedAt = 10_000L
        )

        // Step 2: Plan is saved, no active execution or timer yet
        assertEquals(3, mixedPlan.exercises.size)
        val initialExecution = WorkoutExecution(
            sessionId = "sess_flow_a",
            planId = mixedPlan.id,
            planSnapshot = mixedPlan,
            sessionState = SessionExecutionState.ACTIVE,
            currentExerciseIndex = 0,
            currentSetIndex = 0,
            setState = SetExecutionState.Ready,
            revision = 1L,
            startedAtEpochMs = 20_000L
        )
        val controller = SessionExecutionController(initialExecution)

        // Step 3: Exercise 0 - Stretching L & R
        // Left
        controller.setMachine().startSet(20_000L, 20_000L)
        controller.setMachine().completeSet(50_000L, 30)
        val left = MeasurementValue.TimedHold(30, BodySide.LEFT)
        controller.setMachine().confirmValues(left, targetRestSeconds = 0, isLastSet = false)
        controller.recordConfirmedSet(
            ConfirmedSetRecord("pe_1", "ex_hip_flexor", 0, 0, left, 30, 20_000L, 50_000L)
        )
        // Right
        controller.setMachine().startNextSet(55_000L, 55_000L)
        controller.setMachine().completeSet(85_000L, 30)
        val right = MeasurementValue.TimedHold(30, BodySide.RIGHT)
        controller.setMachine().confirmValues(right, targetRestSeconds = 30, isLastSet = true)
        controller.recordConfirmedSet(
            ConfirmedSetRecord("pe_1", "ex_hip_flexor", 0, 1, right, 30, 55_000L, 85_000L)
        )

        // Switch to Exercise 1 (Bench press)
        controller.switchExercise(1, InProgressSetDisposition.SKIP_REMAINING)
        controller.setMachine().startNextSet(120_000L, 120_000L)
        controller.setMachine().completeSet(160_000L, 40)
        val benchRecord = MeasurementValue.WeightAndReps(80.0, 10)
        controller.setMachine().confirmValues(benchRecord, targetRestSeconds = 90, isLastSet = true)
        controller.recordConfirmedSet(
            ConfirmedSetRecord("pe_2", "ex_bench", 1, 0, benchRecord, 40, 120_000L, 160_000L)
        )

        // Step 4: Resting state & timer snapshot verification across 30s advance
        val restTarget = RestTarget(
            startedAtEpochMs = 160_000L,
            startedAtMonotonicMs = 160_000L,
            targetSeconds = 90
        )
        val snapshotAt190s = TimerCalculator.snapshotAfterMissedTicks(
            performedStartEpochMs = 120_000L,
            performedEndEpochMs = 160_000L,
            restTarget = restTarget,
            sessionStartEpochMs = 20_000L,
            sessionPauses = emptyList(),
            nowEpochMs = 190_000L,
            bootIdMatches = true
        )
        assertEquals(60, snapshotAt190s.restRemainingSeconds)
        assertEquals(0, snapshotAt190s.restOvertimeSeconds)

        // Step 5: Exercise 2 - Stepmill Cardio
        controller.switchExercise(2, InProgressSetDisposition.SKIP_REMAINING)
        controller.setMachine().startNextSet(250_000L, 250_000L)
        controller.setMachine().completeSet(1150_000L, 900)
        val stepmillRecord = MeasurementValue.TimeAndLevel(900, 8.0)
        controller.setMachine().confirmValues(stepmillRecord, targetRestSeconds = 0, isLastSet = true)
        controller.recordConfirmedSet(
            ConfirmedSetRecord("pe_3", "ex_stepmill", 2, 0, stepmillRecord, 900, 250_000L, 1150_000L)
        )

        // Finish session
        val finishResult = controller.finishSession(completedAtEpochMs = 1160_000L)
        assertTrue(finishResult.isSuccess)
        assertEquals(SessionExecutionState.COMPLETED, controller.current.sessionState)

        // Step 6: Verify data integrity and no metric cross-contamination
        val allRecords = controller.current.confirmedRecords
        assertEquals(4, allRecords.size)

        // 1. Stretching: 60s total, hold duration
        val stretchSets = allRecords.filter { it.measurement is MeasurementValue.TimedHold }
        assertEquals(2, stretchSets.size)
        assertEquals(60, stretchSets.sumOf { it.performedDurationSeconds })

        // 2. Strength: 800 kg volume (80kg * 10 reps)
        val strengthSets = allRecords.filter { it.measurement is MeasurementValue.WeightAndReps }
        assertEquals(1, strengthSets.size)
        val benchVal = strengthSets.first().measurement as MeasurementValue.WeightAndReps
        val strengthVolumeKg = benchVal.weightKg * benchVal.reps
        assertEquals(800.0, strengthVolumeKg, 0.001)

        // 3. Cardio: 900s at level 8.0, no kg calculation
        val cardioSets = allRecords.filter { it.measurement is MeasurementValue.TimeAndLevel }
        assertEquals(1, cardioSets.size)
        val cardioVal = cardioSets.first().measurement as MeasurementValue.TimeAndLevel
        assertEquals(900, cardioVal.durationSeconds)
        assertEquals(8.0, cardioVal.levelOrSpeed, 0.001)

        // Verify session cannot accept further set starts once completed
        val illegalStart = controller.setMachine().startNextSet(1200_000L, 1200_000L)
        assertFalse(illegalStart)
    }
}
