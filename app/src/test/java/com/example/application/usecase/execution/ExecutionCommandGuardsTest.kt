package com.example.application.usecase.execution

import com.example.application.usecase.plan.FakePlanRepository
import com.example.domain.model.MeasurementValue
import com.example.domain.model.execution.SessionExecutionState
import com.example.domain.model.execution.SetExecutionState
import com.example.domain.model.plan.PlannedExercise
import com.example.domain.model.plan.PlannedSet
import com.example.domain.model.plan.SessionPlan
import com.example.testfixtures.FakeIdGenerator
import com.example.testfixtures.FakeMonotonicClock
import com.example.testfixtures.FakeTransactionProvider
import com.example.testfixtures.FakeWallClock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Application-layer guards around confirm / undo / finish that domain reducer
 * tests do not exercise (session endTime, confirmed records, rest target).
 */
class ExecutionCommandGuardsTest {

    private lateinit var planRepo: FakePlanRepository
    private lateinit var sessionRepo: FakeSessionRepoForExecution
    private lateinit var executionRepo: FakeExecutionRepository
    private lateinit var startSession: StartSessionUseCase
    private lateinit var startSet: StartSetUseCase
    private lateinit var completeSet: CompleteSetUseCase
    private lateinit var confirmValues: ConfirmPerformedValuesUseCase
    private lateinit var finishSession: FinishSessionUseCase
    private lateinit var undoCompletion: UndoSetCompletionUseCase

    private val wallClock = FakeWallClock(currentEpochMillis = 10_000L)
    private val monotonicClock = FakeMonotonicClock(currentNanos = 10_000L * 1_000_000L)
    private val idGenerator = FakeIdGenerator()
    private val tx = FakeTransactionProvider()

    @Before
    fun setUp() {
        planRepo = FakePlanRepository()
        sessionRepo = FakeSessionRepoForExecution()
        executionRepo = FakeExecutionRepository()

        startSession = StartSessionUseCase(
            planRepository = planRepo,
            sessionRepository = sessionRepo,
            executionRepository = executionRepo,
            transactionProvider = tx,
            idGenerator = idGenerator,
            wallClock = wallClock
        )
        startSet = StartSetUseCase(executionRepo, tx, wallClock, monotonicClock)
        completeSet = CompleteSetUseCase(executionRepo, tx, wallClock)
        confirmValues = ConfirmPerformedValuesUseCase(executionRepo, tx)
        finishSession = FinishSessionUseCase(executionRepo, sessionRepo, tx, wallClock)
        undoCompletion = UndoSetCompletionUseCase(executionRepo, tx)
    }

    @Test
    fun `FinishSession while performing is rejected and leaves session endTime unset`() = runTest {
        val sessionId = startConfirmedSession()
        startSet(sessionId, "cmd_start", 1L).getOrThrow()

        val result = finishSession(sessionId, "cmd_finish", 2L)

        assertTrue(result.isFailure)
        assertTrue(executionRepo.getBySessionId(sessionId)!!.setState is SetExecutionState.Performing)
        assertEquals(SessionExecutionState.ACTIVE, executionRepo.getBySessionId(sessionId)!!.sessionState)
        assertNull(sessionRepo.sessions[sessionId]?.endTime)
    }

    @Test
    fun `ConfirmPerformedValues from Ready fails without writing statistics records`() = runTest {
        val sessionId = startConfirmedSession()

        val result = confirmValues(
            sessionId = sessionId,
            commandId = "cmd_confirm",
            expectedRevision = 1L,
            measurement = MeasurementValue.WeightAndReps(80.0, 10),
            targetRestSeconds = 90,
            isLastSet = false
        )

        assertTrue(result.isFailure)
        val execution = executionRepo.getBySessionId(sessionId)!!
        assertTrue(execution.confirmedRecords.isEmpty())
        assertEquals(SetExecutionState.Ready, execution.setState)
    }

    @Test
    fun `confirming last set of an exercise clears rest and advances to next exercise Ready`() = runTest {
        val sessionId = startConfirmedSession()
        startSet(sessionId, "cmd_start", 1L).getOrThrow()
        wallClock.advanceByMillis(30_000L)
        completeSet(sessionId, "cmd_complete", 2L, durationSeconds = 30).getOrThrow()

        val afterConfirm = confirmValues(
            sessionId = sessionId,
            commandId = "cmd_confirm",
            expectedRevision = 3L,
            measurement = MeasurementValue.WeightAndReps(80.0, 10),
            targetRestSeconds = 90,
            isLastSet = true,
            isLastExercise = false
        ).getOrThrow()

        assertNull(afterConfirm.activeRestTarget)
        assertEquals(1, afterConfirm.currentExerciseIndex)
        assertEquals(0, afterConfirm.currentSetIndex)
        assertEquals(SetExecutionState.Ready, afterConfirm.setState)
        assertEquals(1, afterConfirm.confirmedRecords.size)
        assertTrue(afterConfirm.confirmedRecords.first().isIncludedInStatistics)
    }

    @Test
    fun `confirming last set of last exercise stays Completed without inventing rest`() = runTest {
        val sessionId = startConfirmedSession()
        startSet(sessionId, "cmd_start", 1L).getOrThrow()
        wallClock.advanceByMillis(30_000L)
        completeSet(sessionId, "cmd_complete", 2L, durationSeconds = 30).getOrThrow()

        val afterConfirm = confirmValues(
            sessionId = sessionId,
            commandId = "cmd_confirm",
            expectedRevision = 3L,
            measurement = MeasurementValue.WeightAndReps(80.0, 10),
            targetRestSeconds = 90,
            isLastSet = true,
            isLastExercise = true
        ).getOrThrow()

        assertNull(afterConfirm.activeRestTarget)
        assertTrue(afterConfirm.setState is SetExecutionState.Completed)
        assertEquals(0, afterConfirm.currentExerciseIndex)
        assertEquals(0, afterConfirm.currentSetIndex)
        assertEquals(SessionExecutionState.ACTIVE, afterConfirm.sessionState)
    }

    @Test
    fun `undo from AwaitingConfirmation returns to Ready without statistics records`() = runTest {
        val sessionId = startConfirmedSession()
        startSet(sessionId, "cmd_start", 1L).getOrThrow()
        wallClock.advanceByMillis(30_000L)
        completeSet(sessionId, "cmd_complete", 2L, durationSeconds = 30).getOrThrow()

        val afterUndo = undoCompletion(sessionId, "cmd_undo", 3L).getOrThrow()

        assertEquals(SetExecutionState.Ready, afterUndo.setState)
        assertTrue(afterUndo.confirmedRecords.isEmpty())
    }

    @Test
    fun `undo from Ready is rejected and does not consume the command`() = runTest {
        val sessionId = startConfirmedSession()

        val result = undoCompletion(sessionId, "cmd_undo", 1L)

        assertTrue(result.isFailure)
        val execution = executionRepo.getBySessionId(sessionId)!!
        assertEquals(SetExecutionState.Ready, execution.setState)
        assertEquals(1L, execution.revision)
        assertTrue(execution.executedCommandIds.isEmpty())
    }

    private suspend fun startConfirmedSession(): String {
        val plan = twoExercisePlan()
        planRepo.plans[plan.id] = plan
        return startSession(plan.id).getOrThrow().sessionId
    }

    private fun twoExercisePlan() = SessionPlan(
        id = "plan_guards",
        routineId = "rt_1",
        routineVersion = 1,
        name = "Push",
        exercises = listOf(
            PlannedExercise(
                id = "pe1",
                exerciseId = "ex_bench",
                exerciseName = "벤치프레스",
                orderIndex = 0,
                plannedSets = listOf(
                    PlannedSet(
                        id = "ps1",
                        orderIndex = 0,
                        targetMeasurement = MeasurementValue.WeightAndReps(80.0, 10),
                        targetRestSeconds = 90
                    )
                )
            ),
            PlannedExercise(
                id = "pe2",
                exerciseId = "ex_ohp",
                exerciseName = "오버헤드프레스",
                orderIndex = 1,
                plannedSets = listOf(
                    PlannedSet(
                        id = "ps2",
                        orderIndex = 0,
                        targetMeasurement = MeasurementValue.WeightAndReps(40.0, 8),
                        targetRestSeconds = 90
                    )
                )
            )
        ),
        isConfirmed = true,
        createdAt = 1_000L,
        updatedAt = 1_000L
    )
}
