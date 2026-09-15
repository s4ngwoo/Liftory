package com.example.application.usecase.execution

import com.example.domain.model.MeasurementValue
import com.example.domain.model.execution.ConfirmedSetRecord
import com.example.domain.model.execution.SessionExecutionState
import com.example.domain.model.execution.SetExecutionState
import com.example.domain.model.execution.WorkoutExecution
import com.example.domain.model.timer.RestTarget
import com.example.domain.model.timer.TimerConfidence
import com.example.testfixtures.FakeWallClock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RestoreTimerSnapshotUseCaseTest {

    private lateinit var executionRepo: FakeExecutionRepository
    private val wallClock = FakeWallClock(currentEpochMillis = 100_000L)
    private lateinit var restore: RestoreTimerSnapshotUseCase

    @Before
    fun setUp() {
        executionRepo = FakeExecutionRepository()
        restore = RestoreTimerSnapshotUseCase(executionRepo, wallClock)
    }

    @Test
    fun `missing execution fails closed without inventing a snapshot`() = runTest {
        val result = restore("missing_session", currentBootId = "boot-1")
        assertTrue(result.isFailure)
        assertTrue(executionRepo.store.isEmpty())
    }

    @Test
    fun `boot mismatch marks snapshot estimated and does not mutate stored execution`() = runTest {
        val execution = restingExecution(bootId = "boot-old")
        executionRepo.store[execution.sessionId] = execution

        val result = restore(
            sessionId = execution.sessionId,
            currentBootId = "boot-new",
            nowEpochMs = 100_000L
        )

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().confidence is TimerConfidence.Estimated)
        assertEquals(execution, executionRepo.getBySessionId(execution.sessionId))
    }

    @Test
    fun `null boot ids are treated as a trusted match`() = runTest {
        val execution = restingExecution(bootId = null)
        executionRepo.store[execution.sessionId] = execution

        val snapshot = restore(
            sessionId = execution.sessionId,
            currentBootId = null,
            nowEpochMs = 100_000L
        ).getOrThrow()

        assertTrue(snapshot.confidence is TimerConfidence.Trusted)
        assertEquals(30, snapshot.performedSeconds)
        assertEquals(30, snapshot.restRemainingSeconds)
    }

    @Test
    fun `performed seconds come from last confirmed record not an open performing set`() = runTest {
        val execution = restingExecution(bootId = "boot-1").copy(
            setState = SetExecutionState.Performing(
                startedAtEpochMs = 80_000L,
                startedAtMonotonicMs = 80_000L
            ),
            activeRestTarget = null
        )
        executionRepo.store[execution.sessionId] = execution

        val snapshot = restore(
            sessionId = execution.sessionId,
            currentBootId = "boot-1",
            nowEpochMs = 100_000L
        ).getOrThrow()

        assertEquals(30, snapshot.performedSeconds)
        assertEquals(0, snapshot.restElapsedSeconds)
        assertEquals(0, snapshot.restRemainingSeconds)
    }

    private fun restingExecution(bootId: String?) = WorkoutExecution(
        sessionId = "sess_restore",
        planId = "plan_1",
        planSnapshot = null,
        sessionState = SessionExecutionState.ACTIVE,
        currentExerciseIndex = 0,
        currentSetIndex = 0,
        setState = SetExecutionState.Resting(
            restStartedAtEpochMs = 40_000L,
            targetRestSeconds = 90,
            completedMeasurement = MeasurementValue.WeightAndReps(80.0, 10)
        ),
        revision = 3L,
        startedAtEpochMs = 0L,
        confirmedRecords = listOf(
            ConfirmedSetRecord(
                plannedExerciseId = "pe1",
                exerciseId = "ex_bench",
                exerciseIndex = 0,
                setIndex = 0,
                measurement = MeasurementValue.WeightAndReps(80.0, 10),
                performedDurationSeconds = 30,
                startedAtEpochMs = 10_000L,
                completedAtEpochMs = 40_000L
            )
        ),
        activeRestTarget = RestTarget(
            startedAtEpochMs = 40_000L,
            startedAtMonotonicMs = 40_000L,
            targetSeconds = 90,
            bootId = bootId
        ),
        bootId = bootId
    )
}
