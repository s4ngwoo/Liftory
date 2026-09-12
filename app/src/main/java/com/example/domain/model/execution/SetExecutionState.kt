package com.example.domain.model.execution

import com.example.domain.model.MeasurementValue

/**
 * Explicit state model for an exercise set execution lifecycle (N04.1, ADR-004).
 *
 * Ready → Performing → AwaitingConfirmation → Resting → (next set)
 *                                              ↘ Completed (last set of exercise)
 * Ready → Skipped
 *
 * Unconfirmed values (AwaitingConfirmation) must not enter statistics aggregates.
 */
sealed interface SetExecutionState {
    data object Ready : SetExecutionState

    data class Performing(
        val startedAtEpochMs: Long,
        val startedAtMonotonicMs: Long
    ) : SetExecutionState

    data class AwaitingConfirmation(
        val startedAtEpochMs: Long,
        val completedAtEpochMs: Long,
        val durationSeconds: Int
    ) : SetExecutionState

    data class Resting(
        val restStartedAtEpochMs: Long,
        val targetRestSeconds: Int,
        val completedMeasurement: MeasurementValue
    ) : SetExecutionState

    /** Alias conceptually: ExerciseCompleted for the current set/exercise terminal. */
    data class Completed(
        val completedMeasurement: MeasurementValue,
        val completedAtEpochMs: Long
    ) : SetExecutionState

    data object Skipped : SetExecutionState
}

/**
 * Top-level session execution state (N04.1).
 * SessionCompleted is irreversible for timer resumption (EXEC-10).
 */
enum class SessionExecutionState {
    READY,
    ACTIVE,
    PAUSED,
    COMPLETED
}
