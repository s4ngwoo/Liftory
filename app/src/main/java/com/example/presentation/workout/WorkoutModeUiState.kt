package com.example.presentation.workout

import com.example.domain.model.MeasurementValue
import com.example.domain.model.execution.SessionExecutionState
import com.example.domain.model.execution.SetExecutionState
import com.example.domain.model.execution.WorkoutExecution
import com.example.domain.model.plan.SessionPlan
import com.example.domain.model.timer.TimerSnapshot

/**
 * Three-page workout mode display index (N06.1).
 * This is NOT the executing exercise index.
 */
enum class WorkoutModePage {
    TIMER,
    CURRENT_EXERCISE,
    TODAY_PLAN
}

data class PlannedExerciseRowUi(
    val planItemId: String,
    val exerciseId: String,
    val name: String,
    val orderIndex: Int,
    val status: PlanRowStatus,
    val isExecutionTarget: Boolean
)

enum class PlanRowStatus {
    COMPLETED,
    CURRENT,
    UPCOMING,
    SKIPPED
}

data class SetComparisonRowUi(
    val setIndex: Int,
    val planned: MeasurementValue?,
    val actual: MeasurementValue?,
    val previous: MeasurementValue?
)

/**
 * Single immutable UI state for workout mode (N06).
 * [displayPage] is independent from [execution.currentExerciseIndex].
 */
data class WorkoutModeUiState(
    val displayPage: WorkoutModePage = WorkoutModePage.TIMER,
    val execution: WorkoutExecution? = null,
    val plan: SessionPlan? = null,
    val timerSnapshot: TimerSnapshot? = null,
    val planRows: List<PlannedExerciseRowUi> = emptyList(),
    val currentExerciseComparisons: List<SetComparisonRowUi> = emptyList(),
    val primaryActionLabel: String = "시작",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val moreMenuOpen: Boolean = false,
    val pendingMeasurement: MeasurementValue? = null
) {
    val sessionState: SessionExecutionState?
        get() = execution?.sessionState

    val setState: SetExecutionState?
        get() = execution?.setState
}
