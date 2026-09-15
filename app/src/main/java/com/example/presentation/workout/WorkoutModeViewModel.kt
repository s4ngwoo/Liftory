package com.example.presentation.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.MeasurementValue
import com.example.domain.model.execution.ConfirmedSetRecord
import com.example.domain.model.execution.InProgressSetDisposition
import com.example.domain.model.execution.SessionExecutionController
import com.example.domain.model.execution.SessionExecutionState
import com.example.domain.model.execution.SetExecutionState
import com.example.domain.model.execution.WorkoutExecution
import com.example.domain.model.plan.SessionPlan
import com.example.domain.model.timer.TimerCalculator
import com.example.domain.port.WallClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.example.application.usecase.exercise.ObserveExercisesUseCase
import com.example.application.usecase.session.GetWorkoutSessionUseCase
import com.example.application.usecase.set.ObserveExerciseSetsUseCase
import kotlinx.coroutines.flow.combine

/**
 * Controls the 3-page workout mode. Page changes never emit execution commands (UI-01).
 */
class WorkoutModeViewModel(
    private val wallClock: WallClock,
    initialExecution: WorkoutExecution? = null,
    initialPlan: SessionPlan? = null,
    private val getWorkoutSessionUseCase: GetWorkoutSessionUseCase? = null,
    private val observeExerciseSetsUseCase: ObserveExerciseSetsUseCase? = null,
    private val observeExercisesUseCase: ObserveExercisesUseCase? = null
) : ViewModel() {

    class Factory(
        private val wallClock: WallClock,
        private val initialExecution: WorkoutExecution? = null,
        private val initialPlan: SessionPlan? = null,
        private val getWorkoutSessionUseCase: GetWorkoutSessionUseCase? = null,
        private val observeExerciseSetsUseCase: ObserveExerciseSetsUseCase? = null,
        private val observeExercisesUseCase: ObserveExercisesUseCase? = null
    ) : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WorkoutModeViewModel(
                wallClock,
                initialExecution,
                initialPlan,
                getWorkoutSessionUseCase,
                observeExerciseSetsUseCase,
                observeExercisesUseCase
            ) as T
        }
    }

    private val _uiState = MutableStateFlow(
        WorkoutModeUiState(
            execution = initialExecution,
            plan = initialPlan
        ).let { rebuildDerived(it) }
    )
    val uiState: StateFlow<WorkoutModeUiState> = _uiState.asStateFlow()

    fun loadSession(sessionId: String) {
        val getSession = getWorkoutSessionUseCase ?: return
        val observeSets = observeExerciseSetsUseCase ?: return
        val observeExercises = observeExercisesUseCase ?: return

        viewModelScope.launch {
            val session = getSession(sessionId) ?: return@launch
            combine(
                observeSets(sessionId),
                observeExercises()
            ) { sets, exercises ->
                com.example.application.mapper.WorkoutSessionExecutionBridge.createPlanAndExecution(
                    session = session,
                    sets = sets,
                    exercises = exercises
                )
            }.collect { (plan, execution) ->
                _uiState.update { current ->
                    if (current.execution != null &&
                        current.execution.sessionId == sessionId &&
                        current.execution.setState !is SetExecutionState.Ready
                    ) {
                        rebuildDerived(current.copy(plan = plan))
                    } else {
                        rebuildDerived(current.copy(execution = execution, plan = plan))
                    }
                }
            }
        }
    }

    fun onSelectPage(page: WorkoutModePage) {
        _uiState.update { state ->
            rebuildDerived(state.copy(displayPage = page))
        }
    }

    /** UI-04: viewing another plan row must not change execution target. */
    fun onPeekPlanRow(planItemId: String) {
        // No-op on execution; derived rows still reflect current target.
        _uiState.update { rebuildDerived(it) }
    }

    fun onRestTargetReachedFeedback() {
        // UI-03: rest expiry updates feedback only; keep current page.
        _uiState.update { state ->
            val execution = state.execution ?: return@update state
            rebuildDerived(
                state.copy(
                    timerSnapshot = snapshotFor(execution, wallClock.nowMillis())
                )
            )
        }
    }

    fun bindExecution(execution: WorkoutExecution, plan: SessionPlan? = execution.planSnapshot) {
        _uiState.update { state ->
            rebuildDerived(
                state.copy(
                    execution = execution,
                    plan = plan ?: state.plan
                )
            )
        }
    }

    fun onPrimaryAction() {
        viewModelScope.launch {
            val state = _uiState.value
            val execution = state.execution ?: return@launch
            if (execution.isFinished) return@launch

            val controller = SessionExecutionController(execution)
            val now = wallClock.nowMillis()
            when (val setState = execution.setState) {
                is SetExecutionState.Ready -> {
                    controller.runCommand("ui_start_${execution.revision}", execution.revision) {
                        controller.setMachine().startSet(now, now)
                    }
                }
                is SetExecutionState.Performing -> {
                    val duration = ((now - setState.startedAtEpochMs) / 1000L).toInt().coerceAtLeast(0)
                    controller.runCommand("ui_complete_${execution.revision}", execution.revision) {
                        controller.setMachine().completeSet(now, duration)
                    }
                }
                is SetExecutionState.AwaitingConfirmation -> {
                    // Confirmation requires measurement input from UI; label only.
                }
                is SetExecutionState.Resting -> {
                    controller.runCommand("ui_next_${execution.revision}", execution.revision) {
                        controller.setMachine().startNextSet(now, now)
                    }
                }
                else -> Unit
            }
            val updated = controller.current
            _uiState.update { rebuildDerived(it.copy(execution = updated)) }
        }
    }

    fun confirmMeasurement(
        measurement: MeasurementValue,
        targetRestSeconds: Int,
        isLastSet: Boolean,
        saveAction: (suspend () -> Unit)? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, pendingMeasurement = measurement, errorMessage = null) }
            try {
                saveAction?.invoke()
                val execution = _uiState.value.execution ?: return@launch
                val awaiting = execution.setState as? SetExecutionState.AwaitingConfirmation ?: return@launch
                val controller = SessionExecutionController(execution)
                controller.runCommand("ui_confirm_${execution.revision}", execution.revision) {
                    controller.setMachine().confirmValues(measurement, targetRestSeconds, isLastSet)
                }
                controller.recordConfirmedSet(
                    ConfirmedSetRecord(
                        plannedExerciseId = null,
                        exerciseId = "",
                        exerciseIndex = execution.currentExerciseIndex,
                        setIndex = execution.currentSetIndex,
                        measurement = measurement,
                        performedDurationSeconds = awaiting.durationSeconds,
                        startedAtEpochMs = awaiting.startedAtEpochMs,
                        completedAtEpochMs = awaiting.completedAtEpochMs
                    )
                )
                val updated = controller.current.copy(
                    activeRestTarget = if (isLastSet) null else {
                        val resting = controller.setMachine().currentState as? SetExecutionState.Resting
                        resting?.let {
                            com.example.domain.model.timer.RestTarget(
                                startedAtEpochMs = it.restStartedAtEpochMs,
                                startedAtMonotonicMs = it.restStartedAtEpochMs,
                                targetSeconds = it.targetRestSeconds
                            )
                        }
                    }
                )
                _uiState.update { rebuildDerived(it.copy(execution = updated, isSaving = false, pendingMeasurement = null, errorMessage = null)) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = e.message ?: "저장 실패") }
            }
        }
    }

    fun onSwitchExerciseExplicit(
        targetExerciseIndex: Int,
        disposition: InProgressSetDisposition
    ) {
        val execution = _uiState.value.execution ?: return
        val plan = _uiState.value.plan
        val exerciseCount = plan?.exercises?.size ?: 0
        val safeIndex = if (exerciseCount > 0) {
            targetExerciseIndex % exerciseCount
        } else {
            targetExerciseIndex
        }
        val controller = SessionExecutionController(execution)
        val result = controller.switchExercise(safeIndex, disposition)
        result.onSuccess { updated ->
            _uiState.update { rebuildDerived(it.copy(execution = updated)) }
        }
    }

    fun openMoreMenu(open: Boolean) {
        _uiState.update { it.copy(moreMenuOpen = open) }
    }

    fun toggleMoreMenu() {
        _uiState.update { it.copy(moreMenuOpen = !it.moreMenuOpen) }
    }

    private fun rebuildDerived(state: WorkoutModeUiState): WorkoutModeUiState {
        val execution = state.execution
        val plan = state.plan ?: execution?.planSnapshot
        val rows = buildPlanRows(plan, execution)
        val comparisons = buildComparisons(plan, execution)
        val label = primaryLabel(execution?.setState, execution?.sessionState)
        val snapshot = execution?.let { snapshotFor(it, wallClock.nowMillis()) }
        return state.copy(
            plan = plan,
            planRows = rows,
            currentExerciseComparisons = comparisons,
            primaryActionLabel = label,
            timerSnapshot = snapshot
        )
    }

    private fun buildPlanRows(
        plan: SessionPlan?,
        execution: WorkoutExecution?
    ): List<PlannedExerciseRowUi> {
        if (plan == null) return emptyList()
        val currentIndex = execution?.currentExerciseIndex ?: 0
        val confirmedByExercise = execution?.confirmedRecords
            ?.groupBy { it.exerciseIndex }
            .orEmpty()
        return plan.exercises.map { exercise ->
            val status = when {
                confirmedByExercise[exercise.orderIndex]?.size == exercise.plannedSets.size &&
                    exercise.plannedSets.isNotEmpty() -> PlanRowStatus.COMPLETED
                exercise.orderIndex == currentIndex -> PlanRowStatus.CURRENT
                exercise.orderIndex < currentIndex -> PlanRowStatus.COMPLETED
                else -> PlanRowStatus.UPCOMING
            }
            PlannedExerciseRowUi(
                planItemId = exercise.id,
                exerciseId = exercise.exerciseId,
                name = exercise.exerciseName,
                orderIndex = exercise.orderIndex,
                status = status,
                isExecutionTarget = exercise.orderIndex == currentIndex
            )
        }
    }

    private fun buildComparisons(
        plan: SessionPlan?,
        execution: WorkoutExecution?
    ): List<SetComparisonRowUi> {
        if (plan == null || execution == null) return emptyList()
        val exercise = plan.exercises.getOrNull(execution.currentExerciseIndex) ?: return emptyList()
        val actuals = execution.confirmedRecords
            .filter { it.exerciseIndex == execution.currentExerciseIndex }
            .associateBy { it.setIndex }
        return exercise.plannedSets.map { planned ->
            SetComparisonRowUi(
                setIndex = planned.orderIndex,
                planned = planned.targetMeasurement,
                actual = actuals[planned.orderIndex]?.measurement,
                previous = null
            )
        }
    }

    private fun primaryLabel(
        setState: SetExecutionState?,
        sessionState: SessionExecutionState?
    ): String {
        if (sessionState == SessionExecutionState.COMPLETED) return "종료됨"
        return when (setState) {
            is SetExecutionState.Ready -> "시작"
            is SetExecutionState.Performing -> "완료"
            is SetExecutionState.AwaitingConfirmation -> "값 확인"
            is SetExecutionState.Resting -> "휴식 끝내고 시작"
            is SetExecutionState.Completed -> "종목 완료"
            is SetExecutionState.Skipped -> "건너뜀"
            null -> "시작"
        }
    }

    private fun snapshotFor(execution: WorkoutExecution, now: Long) =
        TimerCalculator.snapshotAfterMissedTicks(
            performedStartEpochMs = execution.confirmedRecords.lastOrNull()?.startedAtEpochMs,
            performedEndEpochMs = execution.confirmedRecords.lastOrNull()?.completedAtEpochMs,
            restTarget = execution.activeRestTarget,
            sessionStartEpochMs = execution.startedAtEpochMs ?: now,
            sessionPauses = execution.sessionPauseIntervals,
            nowEpochMs = now,
            bootIdMatches = true
        )
}
