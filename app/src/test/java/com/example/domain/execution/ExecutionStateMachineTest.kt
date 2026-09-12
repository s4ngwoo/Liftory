package com.example.domain.execution

import com.example.domain.model.MeasurementValue
import com.example.domain.model.execution.ExecutionCommandRunner
import com.example.domain.model.execution.ExecutionStateMachine
import com.example.domain.model.execution.InProgressSetDisposition
import com.example.domain.model.execution.PlanChange
import com.example.domain.model.execution.PlanChangeReason
import com.example.domain.model.execution.SessionExecutionController
import com.example.domain.model.execution.SessionExecutionState
import com.example.domain.model.execution.SetExecutionState
import com.example.domain.model.execution.WorkoutExecution
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExecutionStateMachineTest {

    @Test
    fun `EXEC-01 Ready to StartSet transitions to Performing with single start timestamp`() {
        val sm = ExecutionStateMachine()
        assertEquals(SetExecutionState.Ready, sm.currentState)

        val success = sm.startSet(startEpochMs = 10000L, startMonotonicMs = 10000L)
        assertTrue(success)
        assertTrue(sm.currentState is SetExecutionState.Performing)

        val performing = sm.currentState as SetExecutionState.Performing
        assertEquals(10000L, performing.startedAtEpochMs)
        assertEquals(10000L, performing.startedAtMonotonicMs)
    }

    @Test
    fun `EXEC-02 Performing to CompleteSet transitions to AwaitingConfirmation with fixed completion time`() {
        val sm = ExecutionStateMachine()
        sm.startSet(startEpochMs = 10000L, startMonotonicMs = 10000L)

        val success = sm.completeSet(completionEpochMs = 40000L, durationSeconds = 30)
        assertTrue(success)
        assertTrue(sm.currentState is SetExecutionState.AwaitingConfirmation)

        val awaiting = sm.currentState as SetExecutionState.AwaitingConfirmation
        assertEquals(10000L, awaiting.startedAtEpochMs)
        assertEquals(40000L, awaiting.completedAtEpochMs)
        assertEquals(30, awaiting.durationSeconds)
    }

    @Test
    fun `EXEC-03 F-TIME completion at 40s confirmed at 50s bases rest on 40s with 30s performed duration`() {
        val sm = ExecutionStateMachine()
        sm.startSet(startEpochMs = 10000L, startMonotonicMs = 10000L)
        sm.completeSet(completionEpochMs = 40000L, durationSeconds = 30)

        val measurement = MeasurementValue.WeightAndReps(80.0, 10)
        val confirmSuccess = sm.confirmValues(
            measurement = measurement,
            targetRestSeconds = 90,
            isLastSet = false
        )
        assertTrue(confirmSuccess)
        assertTrue(sm.currentState is SetExecutionState.Resting)

        val resting = sm.currentState as SetExecutionState.Resting
        assertEquals(40000L, resting.restStartedAtEpochMs)
        assertEquals(90, resting.targetRestSeconds)
        assertEquals(measurement, resting.completedMeasurement)
    }

    @Test
    fun `EXEC-04 same completion command ID called repeatedly executes exactly once (idempotent)`() {
        val runner = ExecutionCommandRunner()
        val sm = ExecutionStateMachine()
        sm.startSet(startEpochMs = 10000L, startMonotonicMs = 10000L)

        var recordWriteCount = 0
        val commandId = "cmd_complete_set_1"

        val result1 = runner.runCommand(commandId, expectedRevision = 1) {
            recordWriteCount++
            sm.completeSet(completionEpochMs = 40000L, durationSeconds = 30)
        }
        val result2 = runner.runCommand(commandId, expectedRevision = 1) {
            recordWriteCount++
            sm.completeSet(completionEpochMs = 40000L, durationSeconds = 30)
        }

        assertTrue(result1.isSuccess)
        assertTrue(result2.isSuccess)
        assertEquals("Action must only be executed once despite repeated command invocation", 1, recordWriteCount)
    }

    @Test
    fun `EXEC-05 conflicting revision request fails with conflict error`() {
        val runner = ExecutionCommandRunner(initialRevision = 2)

        val result = runner.runCommand("cmd_stale", expectedRevision = 1) {
            false
        }

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Revision conflict") == true)
    }

    @Test
    fun `EXEC-06 Resting to StartNextSet closes rest and opens Performing atomically`() {
        val sm = ExecutionStateMachine()
        sm.startSet(startEpochMs = 10000L, startMonotonicMs = 10000L)
        sm.completeSet(completionEpochMs = 40000L, durationSeconds = 30)
        sm.confirmValues(MeasurementValue.WeightAndReps(80.0, 10), targetRestSeconds = 90, isLastSet = false)

        val success = sm.startNextSet(nextStartEpochMs = 100000L, nextStartMonotonicMs = 100000L)
        assertTrue(success)
        assertTrue(sm.currentState is SetExecutionState.Performing)
        val performing = sm.currentState as SetExecutionState.Performing
        assertEquals(100000L, performing.startedAtEpochMs)
    }

    @Test
    fun `EXEC-07 completing last set transitions to Completed without creating fake next set or rest`() {
        val sm = ExecutionStateMachine()
        sm.startSet(startEpochMs = 10000L, startMonotonicMs = 10000L)
        sm.completeSet(completionEpochMs = 40000L, durationSeconds = 30)

        val measurement = MeasurementValue.WeightAndReps(80.0, 10)
        val confirmSuccess = sm.confirmValues(
            measurement = measurement,
            targetRestSeconds = 90,
            isLastSet = true
        )
        assertTrue(confirmSuccess)
        assertTrue("Last set must enter Completed without rest timer", sm.currentState is SetExecutionState.Completed)
    }

    @Test
    fun `EXEC-08 undoing completed set safely reverts state and clears rest`() {
        val sm = ExecutionStateMachine()
        sm.startSet(startEpochMs = 10000L, startMonotonicMs = 10000L)
        sm.completeSet(completionEpochMs = 40000L, durationSeconds = 30)
        sm.confirmValues(MeasurementValue.WeightAndReps(80.0, 10), targetRestSeconds = 90, isLastSet = false)
        assertTrue(sm.currentState is SetExecutionState.Resting)

        val undoSuccess = sm.undo()
        assertTrue(undoSuccess)
        assertEquals(SetExecutionState.Ready, sm.currentState)
    }

    @Test
    fun `EXEC-09 peeking another exercise does not change execution target while performing`() {
        val execution = baseExecution()
        val controller = SessionExecutionController(execution)
        controller.setMachine().startSet(10_000L, 10_000L)

        val before = controller.current
        val peeked = controller.peekExercise(targetExerciseIndex = 1)

        assertEquals(before.currentExerciseIndex, peeked.currentExerciseIndex)
        assertTrue(peeked.setState is SetExecutionState.Performing)
    }

    @Test
    fun `EXEC-09 switching while performing without explicit cancel is rejected`() {
        val execution = baseExecution()
        val controller = SessionExecutionController(execution)
        controller.setMachine().startSet(10_000L, 10_000L)

        val result = controller.switchExercise(
            targetExerciseIndex = 1,
            disposition = InProgressSetDisposition.REQUIRE_EXPLICIT_CANCEL
        )

        assertTrue(result.isFailure)
        assertTrue(controller.current.setState is SetExecutionState.Performing)
        assertEquals(0, controller.current.currentExerciseIndex)
    }

    @Test
    fun `EXEC-09 substitute preserves plan change without mutating original routine linkage`() {
        val execution = baseExecution()
        val controller = SessionExecutionController(execution)
        val change = PlanChange(
            id = "pc1",
            sessionId = execution.sessionId,
            reason = PlanChangeReason.SUBSTITUTE_EXERCISE,
            previousExerciseId = "ex_bench",
            previousTarget = MeasurementValue.WeightAndReps(80.0, 10),
            newExerciseId = "ex_dumbbell",
            newTarget = MeasurementValue.WeightAndReps(30.0, 12),
            createdAtEpochMs = 50_000L
        )

        val result = controller.switchExercise(
            targetExerciseIndex = 1,
            disposition = InProgressSetDisposition.SKIP_REMAINING,
            planChange = change
        )

        assertTrue(result.isSuccess)
        val updated = result.getOrThrow()
        assertEquals(1, updated.currentExerciseIndex)
        assertEquals(1, updated.planChanges.size)
        assertEquals(PlanChangeReason.SUBSTITUTE_EXERCISE, updated.planChanges.first().reason)
        assertEquals("plan_1", updated.planId)
    }

    @Test
    fun `EXEC-10 amending completed session record keeps end time fixed and does not resume`() {
        val execution = baseExecution().copy(
            sessionState = SessionExecutionState.COMPLETED,
            completedAtEpochMs = 200_000L,
            confirmedRecords = listOf(
                com.example.domain.model.execution.ConfirmedSetRecord(
                    plannedExerciseId = "pe1",
                    exerciseId = "ex_bench",
                    exerciseIndex = 0,
                    setIndex = 0,
                    measurement = MeasurementValue.WeightAndReps(80.0, 10),
                    performedDurationSeconds = 30,
                    startedAtEpochMs = 10_000L,
                    completedAtEpochMs = 40_000L
                )
            )
        )
        val controller = SessionExecutionController(execution)

        val amend = controller.amendCompletedRecord(
            exerciseIndex = 0,
            setIndex = 0,
            newMeasurement = MeasurementValue.WeightAndReps(82.5, 10)
        )
        assertTrue(amend.isSuccess)
        assertEquals(200_000L, amend.getOrThrow().completedAtEpochMs)
        assertEquals(
            MeasurementValue.WeightAndReps(82.5, 10),
            amend.getOrThrow().confirmedRecords.first().measurement
        )

        val lateTimer = controller.applyLateTimerEvent()
        assertTrue(lateTimer.isSuccess)
        assertEquals(SessionExecutionState.COMPLETED, lateTimer.getOrThrow().sessionState)

        val startRejected = controller.runCommand("cmd_late_start", expectedRevision = 1) {
            controller.setMachine().startSet(300_000L, 300_000L)
        }
        assertTrue(startRejected.isFailure)
    }

    @Test
    fun `illegal transition from Ready to CompleteSet is rejected`() {
        val sm = ExecutionStateMachine()
        assertFalse(sm.completeSet(completionEpochMs = 40000L, durationSeconds = 30))
        assertEquals(SetExecutionState.Ready, sm.currentState)
    }

    private fun baseExecution() = WorkoutExecution(
        sessionId = "sess_1",
        planId = "plan_1",
        planSnapshot = null,
        sessionState = SessionExecutionState.ACTIVE,
        currentExerciseIndex = 0,
        currentSetIndex = 0,
        setState = SetExecutionState.Ready,
        revision = 1L,
        startedAtEpochMs = 0L
    )
}
