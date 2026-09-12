package com.example.domain.model.execution

import com.example.domain.model.MeasurementValue
import com.example.domain.model.plan.SessionPlan

/**
 * In-progress / completed workout execution aggregate (N04).
 * Owns session-level state separately from the current set reducer.
 */
data class WorkoutExecution(
    val sessionId: String,
    val planId: String?,
    val planSnapshot: SessionPlan?,
    val sessionState: SessionExecutionState,
    val currentExerciseIndex: Int,
    val currentSetIndex: Int,
    val setState: SetExecutionState,
    val revision: Long,
    val startedAtEpochMs: Long?,
    val completedAtEpochMs: Long? = null,
    val confirmedRecords: List<ConfirmedSetRecord> = emptyList(),
    val planChanges: List<PlanChange> = emptyList(),
    /** Persisted idempotency keys so duplicate commands survive process boundaries (EXEC-04). */
    val executedCommandIds: Set<String> = emptySet(),
    /** Active rest target derived from set completion (N05). Null when not resting. */
    val activeRestTarget: com.example.domain.model.timer.RestTarget? = null,
    val sessionPauseIntervals: List<com.example.domain.model.timer.PauseInterval> = emptyList(),
    val bootId: String? = null
) {
    val isFinished: Boolean get() = sessionState == SessionExecutionState.COMPLETED
}

data class ConfirmedSetRecord(
    val plannedExerciseId: String?,
    val exerciseId: String,
    val exerciseIndex: Int,
    val setIndex: Int,
    val measurement: MeasurementValue,
    val performedDurationSeconds: Int,
    val startedAtEpochMs: Long,
    val completedAtEpochMs: Long,
    val isIncludedInStatistics: Boolean = true
)

/**
 * Immutable record of an in-session plan adjustment (N04.7).
 * Does not mutate the original routine template.
 */
data class PlanChange(
    val id: String,
    val sessionId: String,
    val reason: PlanChangeReason,
    val previousExerciseId: String?,
    val previousTarget: MeasurementValue?,
    val newExerciseId: String?,
    val newTarget: MeasurementValue?,
    val note: String = "",
    val createdAtEpochMs: Long
)

enum class PlanChangeReason {
    SKIP_SET,
    SKIP_EXERCISE,
    SUBSTITUTE_EXERCISE,
    ADJUST_TARGET,
    REORDER,
    ADD_SET
}

/**
 * How an in-progress set must be resolved before switching exercises (EXEC-09).
 */
enum class InProgressSetDisposition {
    /** Browse-only: no execution target change. */
    PEEK_ONLY,
    /** Require explicit cancel/undo of the performing set first. */
    REQUIRE_EXPLICIT_CANCEL,
    /** Skip remaining work on current exercise and move. */
    SKIP_REMAINING
}
