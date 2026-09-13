package com.example.application.usecase.execution

import com.example.application.usecase.plan.FakePlanRepository
import com.example.domain.model.MeasurementValue
import com.example.domain.model.WorkoutSession
import com.example.domain.model.execution.SessionExecutionState
import com.example.domain.model.execution.SetExecutionState
import com.example.domain.model.execution.WorkoutExecution
import com.example.domain.model.plan.PlannedExercise
import com.example.domain.model.plan.PlannedSet
import com.example.domain.model.plan.SessionPlan
import com.example.domain.repository.WorkoutExecutionRepository
import com.example.domain.repository.WorkoutSessionRepository
import com.example.testfixtures.FakeIdGenerator
import com.example.testfixtures.FakeMonotonicClock
import com.example.testfixtures.FakeTransactionProvider
import com.example.testfixtures.FakeWallClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeExecutionRepository : WorkoutExecutionRepository {
    val store = mutableMapOf<String, WorkoutExecution>()

    override suspend fun save(execution: WorkoutExecution): Result<WorkoutExecution> {
        store[execution.sessionId] = execution
        return Result.success(execution)
    }

    override suspend fun getBySessionId(sessionId: String): WorkoutExecution? = store[sessionId]

    override fun observeBySessionId(sessionId: String): Flow<WorkoutExecution?> =
        flowOf(store[sessionId])

    override suspend fun getActiveExecution(): WorkoutExecution? =
        store.values.firstOrNull { it.sessionState == SessionExecutionState.ACTIVE }
}

class FakeSessionRepoForExecution : WorkoutSessionRepository {
    val sessions = mutableMapOf<String, WorkoutSession>()

    override suspend fun create(session: WorkoutSession): Result<WorkoutSession> {
        sessions[session.id] = session
        return Result.success(session)
    }

    override suspend fun getById(id: String): WorkoutSession? = sessions[id]

    override suspend fun update(session: WorkoutSession): Result<Unit> {
        sessions[session.id] = session
        return Result.success(Unit)
    }

    override suspend fun delete(id: String): Result<Unit> {
        sessions.remove(id)
        return Result.success(Unit)
    }

    override fun observeAll(): Flow<List<WorkoutSession>> = flowOf(sessions.values.toList())

    override fun observeActiveSession(): Flow<WorkoutSession?> =
        MutableStateFlow(sessions.values.firstOrNull { it.endTime == null }).asStateFlowish()

    override suspend fun getActiveSession(): WorkoutSession? =
        sessions.values.firstOrNull { it.endTime == null }

    override suspend fun getActiveSessions(): List<WorkoutSession> =
        sessions.values.filter { it.endTime == null }

    private fun <T> MutableStateFlow<T>.asStateFlowish(): Flow<T> = this
}

class ExecutionUseCasesTest {

    private lateinit var planRepo: FakePlanRepository
    private lateinit var sessionRepo: FakeSessionRepoForExecution
    private lateinit var executionRepo: FakeExecutionRepository
    private lateinit var startSession: StartSessionUseCase
    private lateinit var startSet: StartSetUseCase
    private lateinit var completeSet: CompleteSetUseCase
    private lateinit var confirmValues: ConfirmPerformedValuesUseCase
    private lateinit var startNextSet: StartNextSetUseCase
    private lateinit var finishSession: FinishSessionUseCase

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
        startNextSet = StartNextSetUseCase(executionRepo, tx, wallClock, monotonicClock)
        finishSession = FinishSessionUseCase(executionRepo, sessionRepo, tx, wallClock)
    }

    @Test
    fun `StartSession from confirmed plan creates Ready execution without performed sets`() = runTest {
        val plan = confirmedPlan()
        planRepo.plans[plan.id] = plan

        val result = startSession(plan.id)
        assertTrue(result.isSuccess)
        val execution = result.getOrThrow()
        assertEquals(SessionExecutionState.ACTIVE, execution.sessionState)
        assertEquals(SetExecutionState.Ready, execution.setState)
        assertTrue(execution.confirmedRecords.isEmpty())
        assertEquals(plan.id, execution.planId)
    }

    @Test
    fun `StartSession rejects unconfirmed draft plan`() = runTest {
        val draft = confirmedPlan().copy(isConfirmed = false)
        planRepo.plans[draft.id] = draft

        val result = startSession(draft.id)
        assertTrue(result.isFailure)
    }

    @Test
    fun `StartSession rejects missing plan without creating a session`() = runTest {
        val result = startSession("plan_missing")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertTrue(sessionRepo.sessions.isEmpty())
        assertTrue(executionRepo.store.isEmpty())
    }

    @Test
    fun `StartSession rejects existing active workout so a second live session cannot start`() = runTest {
        val plan = confirmedPlan()
        planRepo.plans[plan.id] = plan
        sessionRepo.sessions["live_import"] = WorkoutSession(
            id = "live_import",
            startTime = 1_000L,
            endTime = null,
            notes = "CSV restored without endTime"
        )

        val result = startSession(plan.id)

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is com.example.domain.exception.ActiveSessionAlreadyExistsException)
        val conflict = exception as com.example.domain.exception.ActiveSessionAlreadyExistsException
        assertEquals("live_import", conflict.activeSession.id)
        assertTrue(executionRepo.store.isEmpty())
        assertEquals(1, sessionRepo.sessions.size)
    }

    @Test
    fun `StartSession rejects orphan active execution even when no session row is open`() = runTest {
        val plan = confirmedPlan()
        planRepo.plans[plan.id] = plan
        executionRepo.store["orphan_exec"] = WorkoutExecution(
            sessionId = "orphan_exec",
            planId = "plan_old",
            planSnapshot = plan,
            sessionState = SessionExecutionState.ACTIVE,
            currentExerciseIndex = 0,
            currentSetIndex = 0,
            setState = SetExecutionState.Ready,
            revision = 1L,
            startedAtEpochMs = 1_000L
        )

        val result = startSession(plan.id)

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is com.example.domain.exception.ActiveSessionAlreadyExistsException)
        val conflict = exception as com.example.domain.exception.ActiveSessionAlreadyExistsException
        assertEquals("orphan_exec", conflict.activeSession.id)
        assertTrue(sessionRepo.sessions.isEmpty())
        assertEquals(1, executionRepo.store.size)
    }

    @Test
    fun `F-TIME flow CompleteSet does not create statistics record until confirm`() = runTest {
        val plan = confirmedPlan()
        planRepo.plans[plan.id] = plan
        val sessionId = startSession(plan.id).getOrThrow().sessionId

        startSet(sessionId, "cmd_start", 1L).getOrThrow()
        wallClock.advanceByMillis(30_000L)
        monotonicClock.advanceByMillis(30_000L)

        val afterComplete = completeSet(sessionId, "cmd_complete", 2L, durationSeconds = 30).getOrThrow()
        assertTrue(afterComplete.setState is SetExecutionState.AwaitingConfirmation)
        assertTrue(
            "Unconfirmed completion must not create statistics records",
            afterComplete.confirmedRecords.isEmpty()
        )

        wallClock.advanceByMillis(10_000L)
        val afterConfirm = confirmValues(
            sessionId = sessionId,
            commandId = "cmd_confirm",
            expectedRevision = 3L,
            measurement = MeasurementValue.WeightAndReps(80.0, 10),
            targetRestSeconds = 90,
            isLastSet = false
        ).getOrThrow()

        assertTrue(afterConfirm.setState is SetExecutionState.Resting)
        val resting = afterConfirm.setState as SetExecutionState.Resting
        // Rest anchored at completion epoch (40_000), not confirm time (50_000)
        assertEquals(40_000L, resting.restStartedAtEpochMs)
        assertEquals(1, afterConfirm.confirmedRecords.size)
        assertTrue(afterConfirm.confirmedRecords.first().isIncludedInStatistics)
        assertEquals(40_000L, afterConfirm.activeRestTarget?.startedAtEpochMs)
        assertEquals(90, afterConfirm.activeRestTarget?.targetSeconds)

        wallClock.currentEpochMillis = 100_000L
        val restored = RestoreTimerSnapshotUseCase.snapshotOf(
            execution = afterConfirm,
            currentBootId = afterConfirm.bootId,
            nowEpochMs = 100_000L
        )
        assertEquals(30, restored.performedSeconds)
        assertEquals(30, restored.restRemainingSeconds)
    }

    @Test
    fun `duplicate CompleteSet command ID writes once`() = runTest {
        val plan = confirmedPlan()
        planRepo.plans[plan.id] = plan
        val sessionId = startSession(plan.id).getOrThrow().sessionId
        startSet(sessionId, "cmd_start", 1L).getOrThrow()
        wallClock.advanceByMillis(30_000L)

        val first = completeSet(sessionId, "cmd_complete", 2L, 30)
        val second = completeSet(sessionId, "cmd_complete", 2L, 30)

        assertTrue(first.isSuccess)
        assertTrue(second.isSuccess)
        assertEquals(3L, second.getOrThrow().revision)
        assertTrue(second.getOrThrow().setState is SetExecutionState.AwaitingConfirmation)
    }

    @Test
    fun `FinishSession freezes endTime and rejects further StartSet`() = runTest {
        val plan = confirmedPlan()
        planRepo.plans[plan.id] = plan
        val sessionId = startSession(plan.id).getOrThrow().sessionId

        wallClock.advanceByMillis(120_000L)
        val finished = finishSession(sessionId, "cmd_finish", 1L).getOrThrow()
        assertEquals(SessionExecutionState.COMPLETED, finished.sessionState)
        assertEquals(130_000L, finished.completedAtEpochMs)
        assertEquals(130_000L, sessionRepo.sessions[sessionId]?.endTime)

        val lateStart = startSet(sessionId, "cmd_late", finished.revision)
        assertTrue(lateStart.isFailure)
    }

    @Test
    fun `zero-set session can finish without fake performed records`() = runTest {
        val plan = confirmedPlan().copy(
            exercises = listOf(
                PlannedExercise(
                    id = "pe_empty",
                    exerciseId = "ex_bench",
                    exerciseName = "벤치",
                    orderIndex = 0,
                    plannedSets = emptyList()
                )
            )
        )
        planRepo.plans[plan.id] = plan
        val sessionId = startSession(plan.id).getOrThrow().sessionId
        wallClock.advanceByMillis(5_000L)

        val finished = finishSession(sessionId, "cmd_finish", 1L).getOrThrow()
        assertTrue(finished.confirmedRecords.isEmpty())
        assertEquals(SessionExecutionState.COMPLETED, finished.sessionState)
    }

    private fun confirmedPlan() = SessionPlan(
        id = "plan_1",
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
                    ),
                    PlannedSet(
                        id = "ps2",
                        orderIndex = 1,
                        targetMeasurement = MeasurementValue.WeightAndReps(80.0, 10),
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
