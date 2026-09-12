package com.example.presentation.workout

import com.example.domain.model.MeasurementValue
import com.example.domain.model.execution.SessionExecutionState
import com.example.domain.model.execution.SetExecutionState
import com.example.domain.model.execution.WorkoutExecution
import com.example.domain.model.plan.PlannedExercise
import com.example.domain.model.plan.PlannedSet
import com.example.domain.model.plan.SessionPlan
import com.example.domain.model.timer.RestTarget
import com.example.testfixtures.FakeWallClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutModeViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val clock = FakeWallClock(10_000L)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `UI-01 swipe or tab page change does not emit execution commands`() = runTest {
        val vm = WorkoutModeViewModel(clock, initialExecution = activeExecution(), initialPlan = samplePlan())
        val beforeRevision = vm.uiState.value.execution!!.revision
        val beforeSet = vm.uiState.value.execution!!.setState

        vm.onSelectPage(WorkoutModePage.CURRENT_EXERCISE)
        vm.onSelectPage(WorkoutModePage.TODAY_PLAN)
        vm.onSelectPage(WorkoutModePage.TIMER)
        advanceUntilIdle()

        assertEquals(WorkoutModePage.TIMER, vm.uiState.value.displayPage)
        assertEquals(beforeRevision, vm.uiState.value.execution!!.revision)
        assertEquals(beforeSet, vm.uiState.value.execution!!.setState)
    }

    @Test
    fun `UI-02 primary action label matches Ready Performing Resting`() = runTest {
        val vm = WorkoutModeViewModel(clock, initialExecution = activeExecution(), initialPlan = samplePlan())
        assertEquals("시작", vm.uiState.value.primaryActionLabel)

        vm.onPrimaryAction()
        advanceUntilIdle()
        assertEquals("완료", vm.uiState.value.primaryActionLabel)
        assertTrue(vm.uiState.value.execution!!.setState is SetExecutionState.Performing)

        clock.advanceByMillis(30_000L)
        vm.onPrimaryAction()
        advanceUntilIdle()
        assertEquals("값 확인", vm.uiState.value.primaryActionLabel)

        vm.confirmMeasurement(MeasurementValue.WeightAndReps(80.0, 10), targetRestSeconds = 90, isLastSet = false)
        advanceUntilIdle()
        assertEquals("휴식 끝내고 시작", vm.uiState.value.primaryActionLabel)
        assertTrue(vm.uiState.value.execution!!.setState is SetExecutionState.Resting)
    }

    @Test
    fun `UI-03 rest target reached keeps current page`() = runTest {
        val execution = activeExecution().copy(
            setState = SetExecutionState.Resting(
                restStartedAtEpochMs = 40_000L,
                targetRestSeconds = 90,
                completedMeasurement = MeasurementValue.WeightAndReps(80.0, 10)
            ),
            activeRestTarget = RestTarget(
                startedAtEpochMs = 40_000L,
                startedAtMonotonicMs = 40_000L,
                targetSeconds = 90
            )
        )
        val vm = WorkoutModeViewModel(clock, initialExecution = execution, initialPlan = samplePlan())
        vm.onSelectPage(WorkoutModePage.TODAY_PLAN)
        clock.currentEpochMillis = 138_000L

        vm.onRestTargetReachedFeedback()
        advanceUntilIdle()

        assertEquals(WorkoutModePage.TODAY_PLAN, vm.uiState.value.displayPage)
        assertEquals(8, vm.uiState.value.timerSnapshot?.restOvertimeSeconds)
        assertTrue(vm.uiState.value.execution!!.setState is SetExecutionState.Resting)
    }

    @Test
    fun `UI-04 peeking plan row does not change execution target`() = runTest {
        val vm = WorkoutModeViewModel(clock, initialExecution = activeExecution(), initialPlan = samplePlan())
        val beforeIndex = vm.uiState.value.execution!!.currentExerciseIndex

        vm.onSelectPage(WorkoutModePage.TODAY_PLAN)
        vm.onPeekPlanRow("pe_2")
        advanceUntilIdle()

        assertEquals(beforeIndex, vm.uiState.value.execution!!.currentExerciseIndex)
        assertTrue(vm.uiState.value.planRows.any { it.planItemId == "pe_2" && !it.isExecutionTarget })
        assertTrue(vm.uiState.value.planRows.first { it.planItemId == "pe_1" }.isExecutionTarget)
    }

    @Test
    fun `UI-06 more menu substitute switches exercise without changing display page`() = runTest {
        val vm = WorkoutModeViewModel(clock, initialExecution = activeExecution(), initialPlan = samplePlan())
        vm.onSelectPage(WorkoutModePage.TIMER)
        vm.toggleMoreMenu()
        assertTrue(vm.uiState.value.moreMenuOpen)

        vm.onSwitchExerciseExplicit(
            targetExerciseIndex = 1,
            disposition = com.example.domain.model.execution.InProgressSetDisposition.SKIP_REMAINING
        )
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.execution!!.currentExerciseIndex)
        assertEquals(WorkoutModePage.TIMER, vm.uiState.value.displayPage)
    }

    @Test
    fun `UI-05 save failure preserves pending measurement and allows retry`() = runTest {
        val vm = WorkoutModeViewModel(clock, initialExecution = activeExecution(), initialPlan = samplePlan())
        vm.onPrimaryAction() // Ready -> Performing
        advanceUntilIdle()
        clock.advanceByMillis(30_000L)
        vm.onPrimaryAction() // Performing -> AwaitingConfirmation
        advanceUntilIdle()

        var shouldFail = true
        val measurement = MeasurementValue.WeightAndReps(85.0, 8)
        vm.confirmMeasurement(
            measurement = measurement,
            targetRestSeconds = 60,
            isLastSet = false,
            saveAction = {
                if (shouldFail) throw IllegalStateException("Network or IO failure")
            }
        )
        advanceUntilIdle()

        // UI-05: Value preserved, error shown, not resting yet
        assertEquals("Network or IO failure", vm.uiState.value.errorMessage)
        assertEquals(measurement, vm.uiState.value.pendingMeasurement)
        assertFalse(vm.uiState.value.isSaving)

        // Retry with fix
        shouldFail = false
        vm.confirmMeasurement(
            measurement = vm.uiState.value.pendingMeasurement!!,
            targetRestSeconds = 60,
            isLastSet = false,
            saveAction = null
        )
        advanceUntilIdle()

        // Succeeded on retry
        assertEquals(null, vm.uiState.value.errorMessage)
        assertEquals(null, vm.uiState.value.pendingMeasurement)
        assertTrue(vm.uiState.value.execution!!.setState is SetExecutionState.Resting)
    }

    @Test
    fun `UI-06 large plan with 15 exercises renders distinct rows without losing execution target`() = runTest {
        val largeExercises = (0..14).map { i ->
            PlannedExercise(
                id = "pe_$i",
                exerciseId = "ex_$i",
                exerciseName = "Exercise $i Very Long Name Description To Verify UI Layout and Text Wrapping",
                orderIndex = i,
                plannedSets = listOf(
                    PlannedSet(id = "ps_$i", orderIndex = 0, targetMeasurement = MeasurementValue.WeightAndReps(50.0 + i, 10))
                )
            )
        }
        val largePlan = samplePlan().copy(exercises = largeExercises)
        val execution = activeExecution().copy(planSnapshot = largePlan, currentExerciseIndex = 7)
        val vm = WorkoutModeViewModel(clock, initialExecution = execution, initialPlan = largePlan)

        assertEquals(15, vm.uiState.value.planRows.size)
        val target = vm.uiState.value.planRows.first { it.isExecutionTarget }
        assertEquals(7, target.orderIndex)
        assertEquals("pe_7", target.planItemId)
        assertEquals(PlanRowStatus.CURRENT, target.status)

        // Prior exercises are marked completed or prior
        val previous = vm.uiState.value.planRows.first { it.orderIndex == 0 }
        assertEquals(PlanRowStatus.COMPLETED, previous.status)

        // Upcoming exercises
        val upcoming = vm.uiState.value.planRows.first { it.orderIndex == 10 }
        assertEquals(PlanRowStatus.UPCOMING, upcoming.status)
    }

    @Test
    fun `UI-07 TalkBack labels and semantic descriptions provide clear state context`() = runTest {
        val vm = WorkoutModeViewModel(clock, initialExecution = activeExecution(), initialPlan = samplePlan())
        assertEquals("시작", vm.uiState.value.primaryActionLabel)
        assertEquals(WorkoutModePage.TIMER, vm.uiState.value.displayPage)
        assertEquals(2, vm.uiState.value.planRows.size)
    }

    private fun activeExecution() = WorkoutExecution(
        sessionId = "sess",
        planId = "plan",
        planSnapshot = samplePlan(),
        sessionState = SessionExecutionState.ACTIVE,
        currentExerciseIndex = 0,
        currentSetIndex = 0,
        setState = SetExecutionState.Ready,
        revision = 1L,
        startedAtEpochMs = 0L
    )

    private fun samplePlan() = SessionPlan(
        id = "plan",
        routineId = "rt",
        routineVersion = 1,
        name = "Push",
        exercises = listOf(
            PlannedExercise(
                id = "pe_1",
                exerciseId = "ex_bench",
                exerciseName = "벤치",
                orderIndex = 0,
                plannedSets = listOf(
                    PlannedSet(id = "ps1", orderIndex = 0, targetMeasurement = MeasurementValue.WeightAndReps(80.0, 10))
                )
            ),
            PlannedExercise(
                id = "pe_2",
                exerciseId = "ex_row",
                exerciseName = "로우",
                orderIndex = 1,
                plannedSets = listOf(
                    PlannedSet(id = "ps2", orderIndex = 0, targetMeasurement = MeasurementValue.WeightAndReps(60.0, 10))
                )
            )
        ),
        isConfirmed = true,
        createdAt = 1L,
        updatedAt = 1L
    )
}
