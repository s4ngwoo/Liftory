package com.example.domain.model.execution

import com.example.domain.model.MeasurementValue

/**
 * Pure reducer for a single set's execution lifecycle (EXEC-01~08, ADR-004).
 *
 * Ready → Performing → AwaitingConfirmation → Resting → (next Ready/Performing)
 *                                              ↘ Completed (last set)
 *
 * Illegal transitions return false and leave state unchanged.
 */
class ExecutionStateMachine(
    initialState: SetExecutionState = SetExecutionState.Ready
) {
    var currentState: SetExecutionState = initialState
        private set

    fun startSet(startEpochMs: Long, startMonotonicMs: Long): Boolean {
        if (currentState !is SetExecutionState.Ready) return false
        currentState = SetExecutionState.Performing(
            startedAtEpochMs = startEpochMs,
            startedAtMonotonicMs = startMonotonicMs
        )
        return true
    }

    fun completeSet(completionEpochMs: Long, durationSeconds: Int): Boolean {
        val performing = currentState as? SetExecutionState.Performing ?: return false
        if (completionEpochMs < performing.startedAtEpochMs) return false
        if (durationSeconds < 0) return false
        currentState = SetExecutionState.AwaitingConfirmation(
            startedAtEpochMs = performing.startedAtEpochMs,
            completedAtEpochMs = completionEpochMs,
            durationSeconds = durationSeconds
        )
        return true
    }

    /**
     * Confirms performed values. Rest start is always the set completion epoch
     * (F-TIME / EXEC-03), never the confirmation wall time.
     */
    fun confirmValues(
        measurement: MeasurementValue,
        targetRestSeconds: Int,
        isLastSet: Boolean
    ): Boolean {
        val awaiting = currentState as? SetExecutionState.AwaitingConfirmation ?: return false
        currentState = if (isLastSet) {
            SetExecutionState.Completed(
                completedMeasurement = measurement,
                completedAtEpochMs = awaiting.completedAtEpochMs
            )
        } else {
            SetExecutionState.Resting(
                restStartedAtEpochMs = awaiting.completedAtEpochMs,
                targetRestSeconds = targetRestSeconds,
                completedMeasurement = measurement
            )
        }
        return true
    }

    fun startNextSet(nextStartEpochMs: Long, nextStartMonotonicMs: Long): Boolean {
        if (currentState !is SetExecutionState.Resting) return false
        currentState = SetExecutionState.Performing(
            startedAtEpochMs = nextStartEpochMs,
            startedAtMonotonicMs = nextStartMonotonicMs
        )
        return true
    }

    /**
     * Cancels a confirmed set / active rest and returns to Ready.
     * Clears rest and discards the uncommitted confirmation path (EXEC-08).
     */
    fun undo(): Boolean {
        return when (currentState) {
            is SetExecutionState.Resting,
            is SetExecutionState.AwaitingConfirmation,
            is SetExecutionState.Completed -> {
                currentState = SetExecutionState.Ready
                true
            }
            else -> false
        }
    }

    /** Explicit skip of a ready set without performing it. */
    fun skip(): Boolean {
        if (currentState !is SetExecutionState.Ready) return false
        currentState = SetExecutionState.Skipped
        return true
    }

    fun resetToReady(): Boolean {
        currentState = SetExecutionState.Ready
        return true
    }
}
